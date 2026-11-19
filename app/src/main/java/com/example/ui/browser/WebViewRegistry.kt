package com.example.ui.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.adblock.AdBlockEngine
import com.example.adblock.BlockCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class WebViewRegistry(
    private val adBlockEngine: AdBlockEngine,
    private val extensionEngine: ExtensionEngine,
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onPageStarted(tabId: String, url: String, canGoBack: Boolean, canGoForward: Boolean)
        fun onPageFinished(tabId: String, url: String, title: String, canGoBack: Boolean, canGoForward: Boolean)
        fun onProgressChanged(tabId: String, progress: Int)
        fun onTitleReceived(tabId: String, title: String)
        fun onNavigationHistoryUpdated(tabId: String, url: String?, canGoBack: Boolean, canGoForward: Boolean)
        fun onRequestBlocked(tabId: String, blockedHost: String, category: BlockCategory, requestUrl: String)
        fun onPopupBlocked(tabId: String, blockedHost: String)
        fun onOpenNewTabRequested(url: String)
        fun onPageError(tabId: String, description: String)
        fun onFindResult(activeMatchOrdinal: Int, numberOfMatches: Int)
        fun onWebPermissionRequest(tabId: String, originHost: String, request: PermissionRequest)
        fun onGeolocationPermissionRequest(
            tabId: String,
            originHost: String,
            origin: String,
            callback: GeolocationPermissions.Callback
        )
    }

    private val webViews = mutableMapOf<String, WebView>()
    private val tabPageHosts = ConcurrentHashMap<String, String>()
    private val tabZoomLevels = ConcurrentHashMap<String, Int>()
    private val tabDesktopModes = ConcurrentHashMap<String, Boolean>()
    private val recentRewriteTimestamps = ConcurrentHashMap<String, Long>()
    private var mobileUserAgent: String = ""
    @Volatile
    private var forcePinchZoomEnabled: Boolean = true

    private val _tabPreviews = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val tabPreviews: StateFlow<Map<String, Bitmap>> = _tabPreviews.asStateFlow()

    private val _tabFavicons = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val tabFavicons: StateFlow<Map<String, Bitmap>> = _tabFavicons.asStateFlow()

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreate(
        context: Context,
        tab: BrowserTab,
        javascriptEnabled: Boolean,
        forcePinchZoom: Boolean = true
    ): WebView {
        forcePinchZoomEnabled = forcePinchZoom
        tabZoomLevels[tab.id] = tab.zoomPercent.coerceIn(MIN_ZOOM_PERCENT, MAX_ZOOM_PERCENT)
        tabDesktopModes[tab.id] = tab.isDesktopMode
        val existing = webViews[tab.id]
        if (existing != null) {
            syncSettings(existing, tab, javascriptEnabled)
            return existing
        }

        val webView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            if (mobileUserAgent.isBlank()) {
                mobileUserAgent = buildMobileUserAgent(settings.userAgentString)
            }

            settings.apply {
                javaScriptEnabled = javascriptEnabled
                domStorageEnabled = !tab.isPrivate
                databaseEnabled = !tab.isPrivate
                setGeolocationEnabled(true)
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportMultipleWindows(true)
                javaScriptCanOpenWindowsAutomatically = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                cacheMode = if (tab.isPrivate) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
                userAgentString = if (tab.isDesktopMode) DESKTOP_USER_AGENT else mobileUserAgent
            }

            setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
                callbacks.onFindResult(activeMatchOrdinal, numberOfMatches)
            }

            webViewClient = object : WebViewClient() {

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val req = request ?: return null
                    val reqUrl = req.url?.toString() ?: return null
                    val pageHost = tabPageHosts[tab.id].orEmpty()

                    val decision = adBlockEngine.evaluateRequest(
                        requestUrl = reqUrl,
                        pageHost = pageHost,
                        isMainFrame = req.isForMainFrame
                    )
                    if (decision.shouldBlock && decision.category != null) {
                        callbacks.onRequestBlocked(
                            tabId = tab.id,
                            blockedHost = decision.matchedDomain.ifBlank { AdBlockEngine.extractHost(reqUrl) },
                            category = decision.category,
                            requestUrl = reqUrl
                        )
                        return adBlockEngine.createEmptyResponse()
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val req = request ?: return false
                    val uri = req.url ?: return false
                    val url = uri.toString()
                    val scheme = uri.scheme?.lowercase() ?: return false

                    if (scheme != "http" && scheme != "https" && scheme != "about") {
                        // Safely handle external intents like mailto:, tel:
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                        return true
                    }

                    val currentHost = tabPageHosts[tab.id].orEmpty()
                    if (adBlockEngine.shouldBlockPopupUrl(url, currentHost)) {
                        callbacks.onPopupBlocked(tab.id, AdBlockEngine.extractHost(url))
                        return true
                    }

                    if (req.isForMainFrame) {
                        val isDesktop = tabDesktopModes[tab.id] ?: false
                        val normalized = normalizeUrlForViewportMode(url, isDesktop)
                        if (normalized != url) {
                            val now = System.currentTimeMillis()
                            val lastRewrite = recentRewriteTimestamps[tab.id] ?: 0L
                            if (now - lastRewrite > REWRITE_COOLDOWN_MS) {
                                recentRewriteTimestamps[tab.id] = now
                                tabPageHosts[tab.id] = AdBlockEngine.extractHost(normalized)
                                view?.loadUrl(normalized)
                                return true
                            }
                        }
                    }

                    tabPageHosts[tab.id] = AdBlockEngine.extractHost(url)
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    val currentUrl = url.orEmpty()
                    val prevHost = tabPageHosts[tab.id].orEmpty()
                    val nextHost = AdBlockEngine.extractHost(currentUrl)
                    tabPageHosts[tab.id] = nextHost
                    if (prevHost != nextHost) {
                        _tabFavicons.update { it - tab.id }
                    }
                    if (favicon != null && !favicon.isRecycled) {
                        storeFavicon(tab.id, favicon)
                    }
                    callbacks.onPageStarted(
                        tabId = tab.id,
                        url = currentUrl,
                        canGoBack = view?.canGoBack() ?: false,
                        canGoForward = view?.canGoForward() ?: false
                    )
                }

                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    super.onPageCommitVisible(view, url)
                    injectCosmeticFiltersIfNeeded(view, url)
                    injectExtensionsIfNeeded(view, url)
                    applyZoomToWebView(view, tabZoomLevels[tab.id] ?: DEFAULT_ZOOM_PERCENT)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val currentUrl = url.orEmpty()
                    tabPageHosts[tab.id] = AdBlockEngine.extractHost(currentUrl)
                    injectCosmeticFiltersIfNeeded(view, currentUrl)
                    injectExtensionsIfNeeded(view, currentUrl)
                    applyZoomToWebView(view, tabZoomLevels[tab.id] ?: DEFAULT_ZOOM_PERCENT)
                    callbacks.onPageFinished(
                        tabId = tab.id,
                        url = currentUrl,
                        title = view?.title.orEmpty(),
                        canGoBack = view?.canGoBack() ?: false,
                        canGoForward = view?.canGoForward() ?: false
                    )
                    // Capture a fresh visual thumbnail shortly after layout & paint settle
                    view?.postDelayed({
                        if (webViews[tab.id] === view) {
                            captureTabPreview(tab.id)
                        }
                    }, PREVIEW_CAPTURE_DELAY_MS)
                }

                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    if (!url.isNullOrBlank()) {
                        tabPageHosts[tab.id] = AdBlockEngine.extractHost(url)
                        injectCosmeticFiltersIfNeeded(view, url)
                        applyZoomToWebView(view, tabZoomLevels[tab.id] ?: DEFAULT_ZOOM_PERCENT)
                    }
                    callbacks.onNavigationHistoryUpdated(
                        tabId = tab.id,
                        url = url,
                        canGoBack = view?.canGoBack() ?: false,
                        canGoForward = view?.canGoForward() ?: false
                    )
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        callbacks.onPageError(
                            tabId = tab.id,
                            description = error?.description?.toString()
                                ?: "ページを読み込めませんでした"
                        )
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    callbacks.onProgressChanged(tab.id, newProgress)
                    if (newProgress >= 65) {
                        injectCosmeticFiltersIfNeeded(view, view?.url)
                    }
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    if (!title.isNullOrBlank()) {
                        callbacks.onTitleReceived(tab.id, title)
                    }
                }

                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                    super.onReceivedIcon(view, icon)
                    if (icon != null && !icon.isRecycled) {
                        storeFavicon(tab.id, icon)
                    }
                }

                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    val config = adBlockEngine.getCurrentConfig()
                    val currentHost = tabPageHosts[tab.id].orEmpty()
                    if (!isUserGesture && config.adBlockEnabled && config.blockPopups && adBlockEngine.isSiteProtected(currentHost)) {
                        callbacks.onPopupBlocked(tab.id, currentHost)
                        return false
                    }

                    val hitTestResult = view?.hitTestResult
                    val targetUrl = hitTestResult?.extra
                    if (!targetUrl.isNullOrBlank() && (targetUrl.startsWith("http://") || targetUrl.startsWith("https://"))) {
                        if (adBlockEngine.shouldBlockPopupUrl(targetUrl, currentHost)) {
                            callbacks.onPopupBlocked(tab.id, AdBlockEngine.extractHost(targetUrl))
                            return false
                        }
                        callbacks.onOpenNewTabRequested(targetUrl)
                        return true
                    }

                    if (config.adBlockEnabled && config.blockPopups && adBlockEngine.isSiteProtected(currentHost)) {
                        callbacks.onPopupBlocked(tab.id, currentHost)
                    }
                    return false
                }

                override fun onPermissionRequest(request: PermissionRequest?) {
                    val req = request ?: return
                    val originHost = AdBlockEngine.extractHost(req.origin?.toString())
                        .ifBlank { tabPageHosts[tab.id].orEmpty() }
                    callbacks.onWebPermissionRequest(tab.id, originHost, req)
                }

                override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: GeolocationPermissions.Callback?
                ) {
                    val cb = callback ?: return
                    val safeOrigin = origin.orEmpty()
                    val originHost = AdBlockEngine.extractHost(safeOrigin)
                        .ifBlank { tabPageHosts[tab.id].orEmpty() }
                    callbacks.onGeolocationPermissionRequest(tab.id, originHost, safeOrigin, cb)
                }
            }
        }

        webViews[tab.id] = webView
        if (tab.url.isNotBlank()) {
            val normalizedUrl = normalizeUrlForViewportMode(tab.url, tab.isDesktopMode)
            tabPageHosts[tab.id] = AdBlockEngine.extractHost(normalizedUrl)
            webView.loadUrl(normalizedUrl)
        }
        return webView
    }

    private fun storeFavicon(tabId: String, icon: Bitmap) {
        val copy = runCatching {
            icon.copy(Bitmap.Config.ARGB_8888, false)
        }.getOrNull() ?: return
        _tabFavicons.update { it + (tabId to copy) }
    }

    /**
     * Captures a lightweight visual thumbnail of the currently visible viewport of [tabId].
     */
    fun captureTabPreview(tabId: String): Boolean {
        val webView = webViews[tabId] ?: return false
        val vw = webView.width
        val vh = webView.height
        if (vw <= 0 || vh <= 0) return false

        val targetWidth = vw.coerceAtMost(PREVIEW_MAX_WIDTH_PX)
        val scale = targetWidth.toFloat() / vw.toFloat()
        val targetHeight = (vh * scale).toInt().coerceIn(1, PREVIEW_MAX_HEIGHT_PX)

        val snapshot = runCatching {
            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            canvas.scale(scale, scale)
            canvas.translate(-webView.scrollX.toFloat(), -webView.scrollY.toFloat())
            webView.draw(canvas)
            bitmap
        }.getOrNull() ?: return false

        _tabPreviews.update { it + (tabId to snapshot) }
        return true
    }

    fun captureAllAvailablePreviews(tabIds: Collection<String>) {
        val existing = _tabPreviews.value
        tabIds.forEach { tabId ->
            if (!existing.containsKey(tabId)) {
                captureTabPreview(tabId)
            }
        }
    }

    fun clearTabPreview(tabId: String) {
        _tabPreviews.update { it - tabId }
        _tabFavicons.update { it - tabId }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun syncSettings(webView: WebView, tab: BrowserTab, javascriptEnabled: Boolean) {
        if (webView.settings.javaScriptEnabled != javascriptEnabled) {
            webView.settings.javaScriptEnabled = javascriptEnabled
        }
        tabDesktopModes[tab.id] = tab.isDesktopMode
        val desiredUa = if (tab.isDesktopMode) DESKTOP_USER_AGENT else mobileUserAgent
        if (desiredUa.isNotBlank() && webView.settings.userAgentString != desiredUa) {
            webView.settings.userAgentString = desiredUa
        }
        val desiredZoom = tab.zoomPercent.coerceIn(MIN_ZOOM_PERCENT, MAX_ZOOM_PERCENT)
        val prevZoom = tabZoomLevels.put(tab.id, desiredZoom)
        if (prevZoom != desiredZoom) {
            applyZoomToWebView(webView, desiredZoom)
        }
    }

    private fun applyZoomToWebView(view: WebView?, zoomPercent: Int) {
        val webView = view ?: return
        val clamped = zoomPercent.coerceIn(MIN_ZOOM_PERCENT, MAX_ZOOM_PERCENT)
        if (!webView.settings.javaScriptEnabled) {
            if (webView.settings.textZoom != clamped) {
                webView.settings.textZoom = clamped
            }
            return
        }
        if (webView.settings.textZoom != DEFAULT_ZOOM_PERCENT) {
            webView.settings.textZoom = DEFAULT_ZOOM_PERCENT
        }
        webView.evaluateJavascript(
            buildPageZoomScript(clamped, forcePinchZoomEnabled),
            null
        )
    }

    fun setPageZoom(tabId: String, zoomPercent: Int) {
        val clamped = zoomPercent.coerceIn(MIN_ZOOM_PERCENT, MAX_ZOOM_PERCENT)
        tabZoomLevels[tabId] = clamped
        applyZoomToWebView(webViews[tabId], clamped)
    }

    fun setForcePinchZoom(enabled: Boolean) {
        forcePinchZoomEnabled = enabled
        webViews.forEach { (tabId, webView) ->
            applyZoomToWebView(webView, tabZoomLevels[tabId] ?: DEFAULT_ZOOM_PERCENT)
        }
    }

    private fun injectCosmeticFiltersIfNeeded(view: WebView?, url: String?) {
        val webView = view ?: return
        val config = adBlockEngine.getCurrentConfig()
        if (!config.adBlockEnabled || !config.cosmeticFiltering) return
        val host = AdBlockEngine.extractHost(url)
        if (!adBlockEngine.isSiteProtected(host)) return
        webView.evaluateJavascript(adBlockEngine.buildCosmeticFilterScript(), null)
    }

    private fun injectExtensionsIfNeeded(view: WebView?, url: String?, forceReapply: Boolean = false) {
        val webView = view ?: return
        if (!webView.settings.javaScriptEnabled) return
        val script = extensionEngine.buildCombinedInjectionScript(url.orEmpty(), forceReapply = forceReapply) ?: return
        webView.evaluateJavascript(script, null)
    }

    fun applyExtensionsNow(tabId: String, url: String) {
        val wv = webViews[tabId] ?: return
        injectExtensionsIfNeeded(wv, url, forceReapply = true)
    }

    fun getWebView(tabId: String): WebView? = webViews[tabId]

    fun loadUrl(tabId: String, url: String) {
        val isDesktop = tabDesktopModes[tabId] ?: false
        val normalized = normalizeUrlForViewportMode(url, isDesktop)
        tabPageHosts[tabId] = AdBlockEngine.extractHost(normalized)
        webViews[tabId]?.loadUrl(normalized)
    }

    fun reload(tabId: String) {
        webViews[tabId]?.reload()
    }

    fun stopLoading(tabId: String) {
        webViews[tabId]?.stopLoading()
    }

    fun goBack(tabId: String): Boolean {
        val wv = webViews[tabId] ?: return false
        return if (wv.canGoBack()) {
            wv.goBack()
            true
        } else {
            false
        }
    }

    fun goForward(tabId: String): Boolean {
        val wv = webViews[tabId] ?: return false
        return if (wv.canGoForward()) {
            wv.goForward()
            true
        } else {
            false
        }
    }

    fun setDesktopModeAndReload(tabId: String, desktop: Boolean) {
        tabDesktopModes[tabId] = desktop
        val wv = webViews[tabId] ?: return
        if (mobileUserAgent.isBlank()) {
            mobileUserAgent = buildMobileUserAgent(wv.settings.userAgentString)
        }
        wv.settings.userAgentString = if (desktop) DESKTOP_USER_AGENT else mobileUserAgent
        wv.settings.useWideViewPort = true
        wv.settings.loadWithOverviewMode = true
        val currentUrl = wv.url.orEmpty()
        val normalizedUrl = normalizeUrlForViewportMode(currentUrl, desktop)
        if (currentUrl.isNotBlank() && normalizedUrl != currentUrl) {
            tabPageHosts[tabId] = AdBlockEngine.extractHost(normalizedUrl)
            wv.loadUrl(normalizedUrl)
        } else {
            wv.reload()
        }
    }

    fun findInPage(tabId: String, query: String) {
        val wv = webViews[tabId] ?: return
        if (query.isBlank()) {
            wv.clearMatches()
        } else {
            wv.findAllAsync(query)
        }
    }

    fun findNext(tabId: String, forward: Boolean) {
        webViews[tabId]?.findNext(forward)
    }

    fun clearFindMatches(tabId: String) {
        webViews[tabId]?.clearMatches()
    }

    fun clearAllBrowsingData() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        webViews.values.forEach { wv ->
            wv.clearCache(true)
            wv.clearFormData()
            wv.clearHistory()
        }
    }

    fun destroyTab(tabId: String) {
        tabPageHosts.remove(tabId)
        tabZoomLevels.remove(tabId)
        tabDesktopModes.remove(tabId)
        recentRewriteTimestamps.remove(tabId)
        clearTabPreview(tabId)
        val wv = webViews.remove(tabId) ?: return
        (wv.parent as? ViewGroup)?.removeView(wv)
        wv.stopLoading()
        wv.clearHistory()
        wv.removeAllViews()
        wv.destroy()
    }

    fun destroyAll() {
        val ids = webViews.keys.toList()
        ids.forEach { destroyTab(it) }
    }

    companion object {
        const val MIN_ZOOM_PERCENT = 50
        const val MAX_ZOOM_PERCENT = 300
        const val DEFAULT_ZOOM_PERCENT = 100
        const val ZOOM_STEP_PERCENT = 10

        private const val PREVIEW_MAX_WIDTH_PX = 420
        private const val PREVIEW_MAX_HEIGHT_PX = 720
        private const val PREVIEW_CAPTURE_DELAY_MS = 350L
        private const val REWRITE_COOLDOWN_MS = 1500L

        private const val FALLBACK_CHROME_VERSION = "131.0.0.0"

        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        private val CHROME_VERSION_REGEX = Regex("""Chrome/(\d+(?:\.\d+){1,3})""")

        private val YOUTUBE_EXCLUDED_PATH_PREFIXES = listOf(
            "/embed/",
            "/oembed",
            "/iframe_api",
            "/youtubei/",
            "/s/",
            "/redirect",
            "/signin",
            "/logout",
            "/account",
            "/paid_memberships",
            "/api/"
        )

        /**
         * Builds a clean standard Chrome-for-Android Mobile User-Agent string.
         * Removes WebView markers (`; wv`, `Version/4.0`), removes `x86_64` emulator device tokens
         * that trigger desktop-PC layout heuristics on sites such as YouTube, and guarantees the
         * `Mobile` token is always present.
         */
        fun buildMobileUserAgent(rawSystemUserAgent: String?): String {
            val extractedVersion = rawSystemUserAgent
                ?.let { CHROME_VERSION_REGEX.find(it)?.groupValues?.getOrNull(1) }
            val major = extractedVersion?.substringBefore('.')?.toIntOrNull() ?: 0
            val chromeVersion = if (major >= 110 && !extractedVersion.isNullOrBlank()) {
                extractedVersion
            } else {
                FALLBACK_CHROME_VERSION
            }
            return "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Mobile Safari/537.36"
        }

        /**
         * Normalizes well-known mobile/desktop host variants and query parameters so YouTube and
         * similar sites reliably serve their mobile layout when [isDesktopMode] is false, and
         * cleanly switch to their desktop site when [isDesktopMode] is true.
         */
        fun normalizeUrlForViewportMode(url: String, isDesktopMode: Boolean): String {
            if (url.isBlank()) return url
            val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return url
            val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return url
            if (scheme != "http" && scheme != "https") return url
            val rawHost = uri.host ?: return url
            val lowerHost = rawHost.lowercase(Locale.ROOT)
            val path = uri.encodedPath.orEmpty()

            if (!isDesktopMode) {
                val isYouTubeDesktopHost = lowerHost == "youtube.com" || lowerHost == "www.youtube.com"
                val isYouTubeMobileHost = lowerHost == "m.youtube.com"
                if (isYouTubeDesktopHost || isYouTubeMobileHost) {
                    val lowerPath = path.lowercase(Locale.ROOT)
                    if (YOUTUBE_EXCLUDED_PATH_PREFIXES.any { lowerPath.startsWith(it) }) {
                        return url
                    }
                    val targetHost = if (isYouTubeDesktopHost) "m.youtube.com" else rawHost
                    return rebuildUriWithHostAndFilteredQuery(
                        uri = uri,
                        newHost = targetHost,
                        stripQueryParams = setOf("app", "persist_app")
                    )
                }
                return url
            } else {
                val desktopHost = when {
                    lowerHost == "m.youtube.com" -> "www.youtube.com"
                    lowerHost.endsWith(".m.wikipedia.org") -> lowerHost.replace(".m.wikipedia.org", ".wikipedia.org")
                    lowerHost == "sp.nicovideo.jp" -> "www.nicovideo.jp"
                    lowerHost == "m.facebook.com" -> "www.facebook.com"
                    else -> null
                } ?: return url

                return rebuildUriWithHostAndFilteredQuery(
                    uri = uri,
                    newHost = desktopHost,
                    stripQueryParams = setOf("app", "persist_app")
                )
            }
        }

        private fun rebuildUriWithHostAndFilteredQuery(
            uri: Uri,
            newHost: String,
            stripQueryParams: Set<String>
        ): String {
            val builder = uri.buildUpon()
                .authority(
                    if (uri.port != -1) "$newHost:${uri.port}" else newHost
                )
            val queryNames = runCatching { uri.queryParameterNames }.getOrNull().orEmpty()
            val hasParamToStrip = queryNames.any { it.lowercase(Locale.ROOT) in stripQueryParams }
            if (hasParamToStrip) {
                builder.clearQuery()
                queryNames.forEach { key ->
                    if (key.lowercase(Locale.ROOT) !in stripQueryParams) {
                        uri.getQueryParameters(key).forEach { value ->
                            builder.appendQueryParameter(key, value)
                        }
                    }
                }
            }
            return builder.build().toString()
        }

        fun buildPageZoomScript(zoomPercent: Int, forcePinchZoom: Boolean): String {
            val clamped = zoomPercent.coerceIn(MIN_ZOOM_PERCENT, MAX_ZOOM_PERCENT)
            return buildString {
                append("(function(){try{")
                if (forcePinchZoom) {
                    append(
                        """
                        var vp=document.querySelector('meta[name="viewport"]');
                        if(vp){
                          var c=vp.getAttribute('content')||'';
                          if(/user-scalable\s*=\s*(no|0)/i.test(c)||/maximum-scale\s*=/i.test(c)){
                            c=c.replace(/user-scalable\s*=\s*[^,]+/ig,'user-scalable=yes')
                               .replace(/maximum-scale\s*=\s*[^,]+/ig,'maximum-scale=5.0');
                            vp.setAttribute('content',c);
                          }
                        }
                        """.trimIndent().replace("\n", "")
                    )
                }
                append("var sid='__mrr_page_zoom_style__';var el=document.getElementById(sid);")
                if (clamped == DEFAULT_ZOOM_PERCENT) {
                    append("if(el&&el.parentNode){el.parentNode.removeChild(el);}")
                    append("if(document.documentElement){document.documentElement.style.removeProperty('zoom');}")
                } else {
                    append("if(!el){el=document.createElement('style');el.id=sid;(document.head||document.documentElement).appendChild(el);}")
                    append("el.textContent='html { zoom: ")
                    append(clamped)
                    append("% !important; }';")
                }
                append("}catch(e){}})();")
            }
        }
    }
}
