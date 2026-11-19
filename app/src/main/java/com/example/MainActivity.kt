package com.example

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.adblock.AdBlockEngine
import com.example.adblock.BlockCategory
import com.example.data.local.MenuLayoutStyle
import com.example.data.local.OmniboxShortcut
import com.example.data.local.ShieldDisplayMode
import com.example.data.local.SitePermissionState
import com.example.data.local.SitePermissionType
import com.example.firebase.configureAppCheck
import com.example.ui.browser.BrowserDestination
import com.example.ui.browser.BrowserTab
import com.example.ui.browser.BrowserViewModel
import com.example.ui.browser.CapturedScreenshot
import com.example.ui.browser.ChromeStoreInstallState
import com.example.ui.browser.ChromeWebStoreDownloader
import com.example.ui.browser.FullPageScreenshotHelper
import com.example.ui.browser.PageAiUiState
import com.example.ui.browser.PageContentExtractor
import com.example.ui.browser.PageTranslationHelper
import com.example.ui.browser.WebViewRegistry
import com.example.ui.components.ActiveZoomBadge
import com.example.ui.components.AiAnswerPopupDialog
import com.example.ui.components.AssignTabToGroupSheet
import com.example.ui.components.BrowserBottomBar
import com.example.ui.components.BrowserMenuCallbacks
import com.example.ui.components.BrowserMoreMenuSheet
import com.example.ui.components.BrowserPopupListMenu
import com.example.ui.components.ChromeWebStoreInstallBar
import com.example.ui.components.CreateOrEditTabGroupDialog
import com.example.ui.components.HomeBackgroundBottomSheet
import com.example.ui.components.OmniboxHeader
import com.example.ui.components.OmniboxShortcutPickerSheet
import com.example.ui.components.PageZoomControlBar
import com.example.ui.components.PendingWebPermissionPrompt
import com.example.ui.components.ScreenshotCapturingOverlay
import com.example.ui.components.ScreenshotPreviewDialog
import com.example.ui.components.SiteShieldBottomSheet
import com.example.ui.components.TranslatePageBottomSheet
import com.example.ui.components.WebPermissionPromptDialog
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.ExtensionsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShieldDashboardScreen
import com.example.ui.screens.SitePermissionsScreen
import com.example.ui.screens.StartPageScreen
import com.example.ui.screens.TabSwitcherScreen
import com.example.ui.theme.AegisBrowserTheme
import com.example.ui.theme.AegisTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()
    private lateinit var webViewRegistry: WebViewRegistry
    private val pendingWebPermissionPrompt = mutableStateOf<PendingWebPermissionPrompt?>(null)
    private val pendingAutoGrantPermission = mutableStateOf<PendingWebPermissionPrompt?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before instantiating any WebView so full-page drawing works reliably
        WebView.enableSlowWholeDocumentDraw()
        super.onCreate(savedInstanceState)
        // Configure Firebase App Check with launch intent before any UI, ViewModel, or Firebase call
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        webViewRegistry = WebViewRegistry(
            adBlockEngine = viewModel.adBlockEngine,
            extensionEngine = viewModel.extensionEngine,
            callbacks = object : WebViewRegistry.Callbacks {
                override fun onPageStarted(
                    tabId: String,
                    url: String,
                    canGoBack: Boolean,
                    canGoForward: Boolean
                ) {
                    viewModel.onPageStarted(tabId, url, canGoBack, canGoForward)
                }

                override fun onPageFinished(
                    tabId: String,
                    url: String,
                    title: String,
                    canGoBack: Boolean,
                    canGoForward: Boolean
                ) {
                    viewModel.onPageFinished(tabId, url, title, canGoBack, canGoForward)
                }

                override fun onProgressChanged(tabId: String, progress: Int) {
                    viewModel.onProgressChanged(tabId, progress)
                }

                override fun onTitleReceived(tabId: String, title: String) {
                    viewModel.onTitleReceived(tabId, title)
                }

                override fun onNavigationHistoryUpdated(
                    tabId: String,
                    url: String?,
                    canGoBack: Boolean,
                    canGoForward: Boolean
                ) {
                    viewModel.onNavigationHistoryUpdated(tabId, url, canGoBack, canGoForward)
                }

                override fun onRequestBlocked(
                    tabId: String,
                    blockedHost: String,
                    category: BlockCategory,
                    requestUrl: String
                ) {
                    viewModel.onRequestBlocked(tabId, blockedHost, category, requestUrl)
                }

                override fun onPopupBlocked(tabId: String, blockedHost: String) {
                    viewModel.onPopupBlocked(tabId, blockedHost)
                }

                override fun onOpenNewTabRequested(url: String) {
                    runOnUiThread {
                        val currentActive = viewModel.uiState.value.activeTab
                        if (!currentActive.isStartPage) {
                            webViewRegistry.captureTabPreview(currentActive.id)
                        }
                        val activeGroupId = currentActive.groupId
                        val newTabId = viewModel.createNewTab(
                            isPrivate = false,
                            initialUrl = url,
                            groupId = activeGroupId
                        )
                        webViewRegistry.loadUrl(newTabId, url)
                    }
                }

                override fun onPageError(tabId: String, description: String) {
                    viewModel.onPageError(tabId, description)
                }

                override fun onFindResult(activeMatchOrdinal: Int, numberOfMatches: Int) {
                    viewModel.onFindResult(activeMatchOrdinal, numberOfMatches)
                }

                override fun onWebPermissionRequest(
                    tabId: String,
                    originHost: String,
                    request: PermissionRequest
                ) {
                    runOnUiThread {
                        val rawResources = request.resources?.toList().orEmpty()
                        val requestedTypes = buildList {
                            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in rawResources) {
                                add(SitePermissionType.CAMERA)
                            }
                            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in rawResources) {
                                add(SitePermissionType.MICROPHONE)
                            }
                        }
                        if (requestedTypes.isEmpty()) {
                            request.deny()
                            return@runOnUiThread
                        }

                        val saved = viewModel.getSitePermissionForHost(originHost)
                        val states = requestedTypes.map { type ->
                            saved?.stateFor(type) ?: SitePermissionState.ASK
                        }
                        val prompt = PendingWebPermissionPrompt.Media(
                            tabId = tabId,
                            originHost = originHost,
                            resources = rawResources,
                            requestedTypes = requestedTypes,
                            request = request
                        )
                        when {
                            states.any { it == SitePermissionState.DENY } -> {
                                request.deny()
                                viewModel.showMessage(
                                    "${originHost.ifBlank { "このサイト" }} の権限リクエストを拒否しました"
                                )
                            }
                            states.all { it == SitePermissionState.ALLOW } -> {
                                pendingAutoGrantPermission.value = prompt
                            }
                            else -> {
                                pendingWebPermissionPrompt.value = prompt
                            }
                        }
                    }
                }

                override fun onGeolocationPermissionRequest(
                    tabId: String,
                    originHost: String,
                    origin: String,
                    callback: GeolocationPermissions.Callback
                ) {
                    runOnUiThread {
                        val saved = viewModel.getSitePermissionForHost(originHost)
                        val prompt = PendingWebPermissionPrompt.Geolocation(
                            tabId = tabId,
                            originHost = originHost,
                            origin = origin,
                            callback = callback
                        )
                        when (saved?.locationState ?: SitePermissionState.ASK) {
                            SitePermissionState.DENY -> {
                                callback.invoke(origin, false, false)
                                viewModel.showMessage(
                                    "${originHost.ifBlank { "このサイト" }} の位置情報アクセスを拒否しました"
                                )
                            }
                            SitePermissionState.ALLOW -> {
                                pendingAutoGrantPermission.value = prompt
                            }
                            SitePermissionState.ASK -> {
                                pendingWebPermissionPrompt.value = prompt
                            }
                        }
                    }
                }
            }
        )

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            AegisBrowserTheme(themeMode = uiState.preferences.themeMode) {
                AegisBrowserApp(
                    viewModel = viewModel,
                    webViewRegistry = webViewRegistry,
                    pendingWebPermissionPrompt = pendingWebPermissionPrompt.value,
                    onConsumeWebPermissionPrompt = { pendingWebPermissionPrompt.value = null },
                    pendingAutoGrantPermission = pendingAutoGrantPermission.value,
                    onConsumeAutoGrantPermission = { pendingAutoGrantPermission.value = null }
                )
            }
        }
    }

    override fun onDestroy() {
        if (::webViewRegistry.isInitialized) {
            webViewRegistry.destroyAll()
        }
        super.onDestroy()
    }
}

@Composable
private fun AegisBrowserApp(
    viewModel: BrowserViewModel,
    webViewRegistry: WebViewRegistry,
    pendingWebPermissionPrompt: PendingWebPermissionPrompt?,
    onConsumeWebPermissionPrompt: () -> Unit,
    pendingAutoGrantPermission: PendingWebPermissionPrompt?,
    onConsumeAutoGrantPermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tabPreviews by webViewRegistry.tabPreviews.collectAsStateWithLifecycle()
    val tabFavicons by webViewRegistry.tabFavicons.collectAsStateWithLifecycle()
    val activeTab = uiState.activeTab
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val urlFocusRequester = remember { FocusRequester() }

    // Fullscreen, translation, home background & top-right shortcut customization states
    var isFullscreen by remember { mutableStateOf(false) }
    var showShortcutPickerSheet by remember { mutableStateOf(false) }
    var showTranslateSheet by remember { mutableStateOf(false) }
    var showHomeBackgroundSheet by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker & Storage Access Framework File Picker for home background
    val homeBackgroundDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.setHomeBackgroundFromUri(uri)
        }
    }

    val homeBackgroundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setHomeBackgroundFromUri(uri)
        }
    }

    fun launchHomeBackgroundDocumentPicker() {
        viewModel.clearHomeBackgroundError()
        runCatching {
            homeBackgroundDocumentLauncher.launch(arrayOf("image/*"))
        }.onFailure {
            viewModel.showMessage("ファイル選択画面を開けませんでした")
        }
    }

    fun launchHomeBackgroundPicker() {
        viewModel.clearHomeBackgroundError()
        val launched = runCatching {
            homeBackgroundPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }.isSuccess
        if (!launched) {
            launchHomeBackgroundDocumentPicker()
        }
    }

    LaunchedEffect(uiState.preferences.forcePinchZoomEnabled) {
        webViewRegistry.setForcePinchZoom(uiState.preferences.forcePinchZoomEnabled)
    }

    // Pending Android runtime permission grant for an approved WebView request
    var awaitingOsGrant by remember { mutableStateOf<PendingWebPermissionPrompt?>(null) }

    fun finishWebPermissionGrant(prompt: PendingWebPermissionPrompt) {
        when (prompt) {
            is PendingWebPermissionPrompt.Media -> {
                val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                val grantedResources = prompt.resources.filter { res ->
                    when (res) {
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> hasCam
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> hasMic
                        else -> false
                    }
                }
                if (grantedResources.isNotEmpty()) {
                    runCatching { prompt.request.grant(grantedResources.toTypedArray()) }
                } else {
                    runCatching { prompt.request.deny() }
                    viewModel.showMessage("端末側でカメラまたはマイクの権限が拒否されています")
                }
            }
            is PendingWebPermissionPrompt.Geolocation -> {
                val hasLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                prompt.callback.invoke(prompt.origin, hasLoc, false)
                if (!hasLoc) {
                    viewModel.showMessage("端末側で位置情報の権限が拒否されています")
                }
            }
        }
    }

    val osPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val pending = awaitingOsGrant
        awaitingOsGrant = null
        if (pending != null) {
            finishWebPermissionGrant(pending)
        }
    }

    fun grantWebPermissionWithOsCheck(prompt: PendingWebPermissionPrompt) {
        val needed = buildList {
            if (SitePermissionType.CAMERA in prompt.requestedTypes &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.CAMERA)
            }
            if (SitePermissionType.MICROPHONE in prompt.requestedTypes &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.RECORD_AUDIO)
            }
            if (SitePermissionType.LOCATION in prompt.requestedTypes &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
        if (needed.isEmpty()) {
            finishWebPermissionGrant(prompt)
        } else {
            awaitingOsGrant = prompt
            osPermissionLauncher.launch(needed.toTypedArray())
        }
    }

    LaunchedEffect(pendingAutoGrantPermission) {
        if (pendingAutoGrantPermission != null) {
            onConsumeAutoGrantPermission()
            grantWebPermissionWithOsCheck(pendingAutoGrantPermission)
        }
    }

    // In-browser Tab Group assignment modal states
    var showAssignActiveTabSheet by remember { mutableStateOf(false) }
    var showCreateGroupForActiveTabDialog by remember { mutableStateOf(false) }

    // Full-page screenshot capture & preview states
    var isCapturingScreenshot by remember { mutableStateOf(false) }
    var captureStep by remember { mutableIntStateOf(0) }
    var captureTotalSteps by remember { mutableIntStateOf(1) }
    var capturedScreenshot by remember { mutableStateOf<CapturedScreenshot?>(null) }
    var isSavingScreenshot by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.destination) {
        if (uiState.destination != BrowserDestination.BROWSER && isFullscreen) {
            isFullscreen = false
        }
        if (uiState.destination == BrowserDestination.TABS) {
            webViewRegistry.captureAllAvailablePreviews(
                uiState.tabs.filter { !it.isStartPage }.map { it.id }
            )
        }
    }

    DisposableEffect(isFullscreen) {
        val window = (context as? Activity)?.window
        val controller = if (window != null) {
            WindowCompat.getInsetsController(window, view)
        } else {
            null
        }
        if (isFullscreen && controller != null) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (isFullscreen) {
                controller?.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            capturedScreenshot?.bitmap?.let { bmp ->
                if (!bmp.isRecycled) bmp.recycle()
            }
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    fun navigateActiveTab(rawInput: String) {
        val targetUrl = viewModel.resolveNavigationUrl(rawInput)
        if (targetUrl.isBlank()) return
        val tabId = viewModel.uiState.value.activeTab.id
        viewModel.startNavigation(tabId, targetUrl)
        webViewRegistry.loadUrl(tabId, targetUrl)
    }

    fun shareCurrentTabUrl() {
        if (activeTab.url.isNotBlank()) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, activeTab.title)
                putExtra(Intent.EXTRA_TEXT, activeTab.url)
            }
            context.startActivity(Intent.createChooser(shareIntent, "URLを共有"))
        } else {
            viewModel.showMessage("Webページを表示してから共有してください")
        }
    }

    fun triggerFullPageScreenshot() {
        if (isCapturingScreenshot) return
        if (activeTab.isStartPage) {
            viewModel.showMessage("Webページ表示中に撮影できます（長押しでショートカット変更）")
            return
        }
        val webView = webViewRegistry.getWebView(activeTab.id)
        if (webView == null) {
            viewModel.showMessage("Webページを表示してから撮影してください")
            return
        }
        coroutineScope.launch {
            isCapturingScreenshot = true
            captureStep = 1
            captureTotalSteps = 1
            val result = FullPageScreenshotHelper.captureFullPage(
                webView = webView,
                pageUrl = activeTab.url,
                pageTitle = activeTab.title,
                onProgress = { step, total ->
                    captureStep = step
                    captureTotalSteps = total
                }
            )
            isCapturingScreenshot = false
            if (result != null) {
                capturedScreenshot?.bitmap?.let { old ->
                    if (!old.isRecycled) old.recycle()
                }
                capturedScreenshot = result
            } else {
                viewModel.showMessage("スクリーンショットの作成に失敗しました")
            }
        }
    }

    fun openTranslatePageSheet() {
        if (activeTab.isStartPage || activeTab.url.isBlank()) {
            viewModel.showMessage("Webページを表示してから翻訳してください")
            return
        }
        showTranslateSheet = true
    }

    fun triggerAiPageAnalysis() {
        if (activeTab.isStartPage || activeTab.url.isBlank()) {
            viewModel.showMessage("Webページを表示してからAI解析を実行してください")
            return
        }
        val webView = webViewRegistry.getWebView(activeTab.id)
        if (webView == null) {
            viewModel.showMessage("ページの読み込み完了後に実行してください")
            return
        }
        val pageTitle = activeTab.title
        val pageUrl = activeTab.url
        viewModel.startPageAiLoading(pageTitle, pageUrl)
        coroutineScope.launch {
            try {
                val extracted = PageContentExtractor.extractFromWebView(
                    webView = webView,
                    fallbackTitle = pageTitle,
                    fallbackUrl = pageUrl
                )
                viewModel.analyzeExtractedPage(extracted)
            } catch (e: Exception) {
                viewModel.showPageAiError(
                    pageTitle = pageTitle,
                    pageUrl = pageUrl,
                    message = e.localizedMessage ?: "ページ内容の取得に失敗しました。"
                )
            }
        }
    }

    fun applyActiveTabZoom(percent: Int) {
        val clamped = viewModel.setActiveTabZoom(percent)
        webViewRegistry.setPageZoom(activeTab.id, clamped)
    }

    fun stepActiveTabZoom(delta: Int) {
        val clamped = viewModel.stepActiveTabZoom(delta)
        webViewRegistry.setPageZoom(activeTab.id, clamped)
    }

    fun resetActiveTabZoom() {
        val clamped = viewModel.resetActiveTabZoom()
        webViewRegistry.setPageZoom(activeTab.id, clamped)
    }

    fun executeShortcutAction(action: OmniboxShortcut) {
        when (action) {
            OmniboxShortcut.AI_SOLVER -> triggerAiPageAnalysis()
            OmniboxShortcut.EXTENSIONS -> viewModel.navigateTo(BrowserDestination.EXTENSIONS)
            OmniboxShortcut.FULL_SCREENSHOT -> triggerFullPageScreenshot()
            OmniboxShortcut.FULLSCREEN -> {
                isFullscreen = !isFullscreen
                if (isFullscreen) {
                    viewModel.showMessage("全画面表示に切り替えました（右下のボタンまたは戻るキーで終了）")
                }
            }
            OmniboxShortcut.PAGE_ZOOM -> viewModel.openZoomBar()
            OmniboxShortcut.TRANSLATE -> openTranslatePageSheet()
            OmniboxShortcut.BOOKMARK -> {
                if (activeTab.isStartPage) {
                    viewModel.showMessage("Webページ表示中にブックマーク登録できます（長押しで変更可）")
                } else {
                    viewModel.toggleCurrentPageBookmark()
                }
            }
            OmniboxShortcut.FIND_IN_PAGE -> {
                if (activeTab.isStartPage) {
                    viewModel.showMessage("Webページ表示中にページ内検索できます（長押しで変更可）")
                } else {
                    viewModel.openFindInPage()
                }
            }
            OmniboxShortcut.DESKTOP_MODE -> {
                if (activeTab.isStartPage) {
                    viewModel.showMessage("Webページ表示中にPC版表示へ切り替えできます（長押しで変更可）")
                } else {
                    val desktop = viewModel.toggleDesktopMode(activeTab.id)
                    webViewRegistry.setDesktopModeAndReload(activeTab.id, desktop)
                }
            }
            OmniboxShortcut.SHARE -> shareCurrentTabUrl()
        }
    }

    fun handleBackAction() {
        when {
            uiState.showZoomBar -> {
                viewModel.closeZoomBar()
            }
            isFullscreen -> {
                isFullscreen = false
            }
            uiState.findInPage.isActive -> {
                webViewRegistry.clearFindMatches(activeTab.id)
                viewModel.closeFindInPage()
            }
            !activeTab.isStartPage && webViewRegistry.goBack(activeTab.id) -> {
                // Handled by WebView history
            }
            !activeTab.isStartPage -> {
                webViewRegistry.stopLoading(activeTab.id)
                webViewRegistry.clearTabPreview(activeTab.id)
                viewModel.navigateToStartPage(activeTab.id)
            }
        }
    }

    BackHandler(
        enabled = uiState.destination == BrowserDestination.BROWSER &&
            (uiState.showZoomBar || isFullscreen || uiState.findInPage.isActive || !activeTab.isStartPage)
    ) {
        handleBackAction()
    }

    val currentHost = AdBlockEngine.extractHost(activeTab.url)
    val isSiteProtected = viewModel.adBlockEngine.isSiteProtected(currentHost)
    val shouldShowShieldIndicator = when (uiState.preferences.shieldDisplayMode) {
        ShieldDisplayMode.ALWAYS_SHOW -> true
        ShieldDisplayMode.HIDE_ON_SEARCH -> !AdBlockEngine.isSearchUrl(activeTab.url)
        ShieldDisplayMode.HIDE_WHILE_BROWSING -> activeTab.isStartPage
    }

    when (uiState.destination) {
        BrowserDestination.TABS -> {
            TabSwitcherScreen(
                tabs = uiState.tabs,
                tabGroups = uiState.tabGroups,
                activeTabId = uiState.activeTabId,
                tabPreviews = tabPreviews,
                tabFavicons = tabFavicons,
                homeBackgroundPath = uiState.preferences.homeBackgroundPath,
                homeBackgroundDimAlpha = uiState.preferences.homeBackgroundDimAlpha,
                snackbarHostState = snackbarHostState,
                onBack = { viewModel.navigateTo(BrowserDestination.BROWSER) },
                onSelectTab = { tabId -> viewModel.selectTab(tabId) },
                onCloseTab = { tabId ->
                    val closed = viewModel.closeTab(tabId)
                    closed.forEach { webViewRegistry.destroyTab(it) }
                },
                onCloseAllTabs = {
                    val closed = viewModel.closeAllTabs()
                    closed.forEach { webViewRegistry.destroyTab(it) }
                },
                onNewTab = {
                    viewModel.createNewTab(
                        isPrivate = false,
                        keepCurrentDestination = true
                    )
                },
                onNewPrivateTab = {
                    viewModel.createNewTab(
                        isPrivate = true,
                        keepCurrentDestination = true
                    )
                },
                onNewTabInGroup = { groupId ->
                    viewModel.createNewTab(
                        isPrivate = false,
                        groupId = groupId,
                        keepCurrentDestination = true
                    )
                },
                onCreateTabGroup = { name, color, tabIds ->
                    viewModel.createTabGroup(name, color, tabIds)
                },
                onAssignTabToGroup = { tabId, groupId ->
                    viewModel.assignTabToGroup(tabId, groupId)
                },
                onMergeTabsIntoGroup = { sourceTabId, targetTabId ->
                    viewModel.mergeTabsIntoGroup(sourceTabId, targetTabId)
                },
                onUpdateTabGroup = { groupId, name, color ->
                    viewModel.updateTabGroup(groupId, name, color)
                },
                onToggleGroupCollapsed = { groupId ->
                    viewModel.toggleGroupCollapsed(groupId)
                },
                onUngroupTabs = { groupId ->
                    viewModel.ungroupTabs(groupId)
                },
                onCloseTabGroup = { groupId ->
                    val closed = viewModel.closeTabGroup(groupId)
                    closed.forEach { webViewRegistry.destroyTab(it) }
                },
                onOpenSettings = {
                    viewModel.navigateTo(BrowserDestination.SETTINGS)
                }
            )
        }

        BrowserDestination.SHIELD_DASHBOARD -> {
            ShieldDashboardScreen(
                preferences = uiState.preferences,
                siteRules = uiState.siteRules,
                topBlockedDomains = uiState.topBlockedDomains,
                onBack = { viewModel.navigateTo(BrowserDestination.BROWSER) },
                onToggleMaster = viewModel::setAdBlockEnabled,
                onSelectShieldDisplayMode = viewModel::setShieldDisplayMode,
                onToggleAds = viewModel::setBlockAds,
                onToggleTrackers = viewModel::setBlockTrackers,
                onToggleSocial = viewModel::setBlockSocial,
                onTogglePopups = viewModel::setBlockPopups,
                onToggleCosmetic = viewModel::setCosmeticFiltering,
                onAddAllowlistDomain = { domain -> viewModel.toggleSiteAllowlist(domain, true) },
                onAddCustomBlockDomain = viewModel::addCustomBlockDomain,
                onDeleteSiteRule = viewModel::deleteSiteRule
            )
        }

        BrowserDestination.BOOKMARKS -> {
            BookmarksScreen(
                bookmarks = uiState.bookmarks,
                onBack = { viewModel.navigateTo(BrowserDestination.BROWSER) },
                onOpenUrl = { url -> navigateActiveTab(url) },
                onAddBookmark = viewModel::addBookmark,
                onDeleteBookmark = viewModel::deleteBookmark
            )
        }

        BrowserDestination.HISTORY -> {
            HistoryScreen(
                history = uiState.history,
                onBack = { viewModel.navigateTo(BrowserDestination.BROWSER) },
                onOpenUrl = { url -> navigateActiveTab(url) },
                onDeleteItem = viewModel::deleteHistoryItem,
                onClearAll = viewModel::clearAllHistory
            )
        }

        BrowserDestination.SETTINGS -> {
            SettingsScreen(
                preferences = uiState.preferences,
                sitePermissionCount = uiState.sitePermissions.size,
                enabledExtensionCount = uiState.extensions.count { it.enabled },
                totalExtensionCount = uiState.extensions.size,
                openMenuCustomizeInitially = uiState.openMenuCustomizeOnSettings,
                onConsumeOpenMenuCustomize = viewModel::consumeOpenMenuCustomizeFlag,
                onBack = { viewModel.navigateTo(BrowserDestination.BROWSER) },
                onSelectSearchEngine = viewModel::setSearchEngine,
                onSelectThemeMode = viewModel::setThemeMode,
                isImportingHomeBackground = uiState.isImportingHomeBackground,
                homeBackgroundError = uiState.homeBackgroundError,
                onPickHomeBackground = { launchHomeBackgroundPicker() },
                onPickHomeBackgroundFile = { launchHomeBackgroundDocumentPicker() },
                onChangeHomeBackgroundDim = viewModel::setHomeBackgroundDimAlpha,
                onResetHomeBackground = viewModel::clearHomeBackground,
                onSelectDefaultZoomPercent = { pct ->
                    viewModel.setDefaultZoomPercent(pct)
                    if (!activeTab.isStartPage) {
                        webViewRegistry.setPageZoom(activeTab.id, pct)
                    }
                },
                onToggleForcePinchZoom = viewModel::setForcePinchZoomEnabled,
                onSelectMenuLayoutStyle = viewModel::setMenuLayoutStyle,
                onToggleMenuItem = viewModel::toggleMenuItem,
                onMoveMenuItem = viewModel::moveMenuItem,
                onResetMenuCustomization = viewModel::resetMenuCustomization,
                onSelectShieldDisplayMode = viewModel::setShieldDisplayMode,
                onToggleOmniboxShortcut = viewModel::toggleOmniboxShortcut,
                onOpenExtensions = { viewModel.navigateTo(BrowserDestination.EXTENSIONS) },
                onOpenSitePermissions = { viewModel.navigateTo(BrowserDestination.SITE_PERMISSIONS) },
                onToggleJavaScript = viewModel::setJavascriptEnabled,
                onToggleHttpsUpgrade = viewModel::setHttpsUpgradeEnabled,
                onClearBrowsingData = {
                    webViewRegistry.clearAllBrowsingData()
                    viewModel.clearAllHistory()
                    viewModel.showMessage("Cookie・キャッシュ・履歴をすべて削除しました")
                },
                onResetStats = viewModel::resetBlockedStats
            )
        }

        BrowserDestination.SITE_PERMISSIONS -> {
            SitePermissionsScreen(
                sitePermissions = uiState.sitePermissions,
                currentHost = currentHost,
                onBack = { viewModel.navigateTo(BrowserDestination.BROWSER) },
                onUpdatePermission = { domain, type, state ->
                    viewModel.setSitePermission(domain, type, state)
                },
                onAddSitePermission = { domain, cam, mic, loc ->
                    viewModel.upsertSitePermission(domain, cam, mic, loc)
                },
                onDeleteSitePermission = viewModel::deleteSitePermission,
                onClearAllPermissions = viewModel::clearAllSitePermissions
            )
        }

        BrowserDestination.EXTENSIONS -> {
            ExtensionsScreen(
                extensions = uiState.extensions,
                extensionsEnabled = uiState.preferences.extensionsEnabled,
                hasActiveWebPage = !activeTab.isStartPage,
                chromeStoreInstallState = uiState.chromeStoreInstallState,
                snackbarHostState = snackbarHostState,
                onBack = { viewModel.navigateTo(BrowserDestination.BROWSER) },
                onToggleMasterEnabled = viewModel::setExtensionsEnabled,
                onToggleExtension = { id, enabled ->
                    viewModel.toggleExtension(id, enabled)
                },
                onSaveExtension = viewModel::saveExtension,
                onDeleteExtension = viewModel::deleteExtension,
                onOpenChromeWebStore = { storeUrl ->
                    navigateActiveTab(storeUrl)
                },
                onInstallFromChromeStore = { urlOrId ->
                    viewModel.installFromChromeWebStore(urlOrId)
                },
                onApplyToActivePage = {
                    viewModel.navigateTo(BrowserDestination.BROWSER)
                    webViewRegistry.applyExtensionsNow(activeTab.id, activeTab.url)
                    viewModel.showMessage("有効な拡張機能を表示中のページへ適用しました")
                }
            )
        }

        BrowserDestination.BROWSER -> {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = if (isFullscreen) {
                    WindowInsets(0, 0, 0, 0)
                } else {
                    ScaffoldDefaults.contentWindowInsets
                },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    AnimatedVisibility(
                        visible = !isFullscreen,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        OmniboxHeader(
                            tab = activeTab,
                            isAdBlockGlobalEnabled = uiState.preferences.adBlockEnabled,
                            isSiteProtected = isSiteProtected,
                            showShieldButton = shouldShowShieldIndicator,
                            isBookmarked = uiState.isCurrentUrlBookmarked,
                            isFullscreen = isFullscreen,
                            omniboxShortcuts = uiState.preferences.omniboxShortcuts,
                            findInPage = uiState.findInPage,
                            urlFocusRequester = urlFocusRequester,
                            onSubmitUrl = { query -> navigateActiveTab(query) },
                            onShieldClick = { viewModel.setSiteShieldSheetVisible(true) },
                            onReloadOrStopClick = {
                                if (activeTab.isLoading) {
                                    webViewRegistry.stopLoading(activeTab.id)
                                } else {
                                    webViewRegistry.reload(activeTab.id)
                                }
                            },
                            onShortcutAction = { action -> executeShortcutAction(action) },
                            onOpenShortcutPicker = { showShortcutPickerSheet = true },
                            onFindQueryChange = { query ->
                                viewModel.updateFindQuery(query)
                                webViewRegistry.findInPage(activeTab.id, query)
                            },
                            onFindNext = { webViewRegistry.findNext(activeTab.id, true) },
                            onFindPrevious = { webViewRegistry.findNext(activeTab.id, false) },
                            onCloseFind = {
                                webViewRegistry.clearFindMatches(activeTab.id)
                                viewModel.closeFindInPage()
                            }
                        )
                    }
                },
                bottomBar = {
                    AnimatedVisibility(
                        visible = !isFullscreen,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        BrowserBottomBar(
                            tab = activeTab,
                            tabs = uiState.tabs,
                            activeGroup = uiState.activeTabGroup,
                            tabsInActiveGroup = uiState.tabsInActiveGroup,
                            onHomeClick = {
                                webViewRegistry.stopLoading(activeTab.id)
                                webViewRegistry.clearTabPreview(activeTab.id)
                                viewModel.navigateToStartPage(activeTab.id)
                            },
                            onBookmarksClick = {
                                viewModel.navigateTo(BrowserDestination.BOOKMARKS)
                            },
                            onSearchClick = {
                                runCatching { urlFocusRequester.requestFocus() }
                                keyboardController?.show()
                            },
                            onTabsClick = {
                                if (!activeTab.isStartPage) {
                                    webViewRegistry.captureTabPreview(activeTab.id)
                                }
                                viewModel.navigateTo(BrowserDestination.TABS)
                            },
                            onMoreClick = { viewModel.setMoreMenuSheetVisible(true) },
                            onSwitchAdjacentTab = { delta ->
                                if (!activeTab.isStartPage) {
                                    webViewRegistry.captureTabPreview(activeTab.id)
                                }
                                viewModel.switchToAdjacentTab(delta)
                            },
                            onSelectGroupTab = { tabId ->
                                if (!activeTab.isStartPage && activeTab.id != tabId) {
                                    webViewRegistry.captureTabPreview(activeTab.id)
                                }
                                viewModel.selectTab(tabId)
                            },
                            onCloseGroupTab = { tabId ->
                                val closed = viewModel.closeTab(tabId)
                                closed.forEach { webViewRegistry.destroyTab(it) }
                            },
                            onAddTabInActiveGroup = {
                                if (!activeTab.isStartPage) {
                                    webViewRegistry.captureTabPreview(activeTab.id)
                                }
                                val groupId = uiState.activeTabGroup?.id
                                viewModel.createNewTab(isPrivate = false, groupId = groupId)
                            }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (activeTab.isStartPage) {
                        StartPageScreen(
                            isPrivate = activeTab.isPrivate,
                            preferences = uiState.preferences,
                            bookmarks = uiState.bookmarks,
                            onOpenUrl = { url -> navigateActiveTab(url) },
                            onOpenShieldDashboard = {
                                viewModel.navigateTo(BrowserDestination.SHIELD_DASHBOARD)
                            },
                            onOpenBackgroundPicker = { showHomeBackgroundSheet = true },
                            onAddBookmark = viewModel::addBookmark,
                            onDeleteBookmark = viewModel::deleteBookmark
                        )
                    } else {
                        BrowserWebViewHost(
                            tab = activeTab,
                            javascriptEnabled = uiState.preferences.javascriptEnabled,
                            forcePinchZoom = uiState.preferences.forcePinchZoomEnabled,
                            webViewRegistry = webViewRegistry,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (activeTab.lastErrorMessage != null) {
                            WebErrorOverlay(
                                errorMessage = activeTab.lastErrorMessage,
                                onRetry = {
                                    viewModel.startNavigation(activeTab.id, activeTab.url)
                                    webViewRegistry.reload(activeTab.id)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    val detectedChromeExtId = remember(activeTab.url) {
                        if (ChromeWebStoreDownloader.isChromeWebStoreDetailUrl(activeTab.url)) {
                            ChromeWebStoreDownloader.extractExtensionId(activeTab.url)
                        } else {
                            null
                        }
                    }
                    var dismissedChromeExtId by remember { mutableStateOf<String?>(null) }
                    LaunchedEffect(detectedChromeExtId) {
                        if (detectedChromeExtId != null && detectedChromeExtId != dismissedChromeExtId) {
                            dismissedChromeExtId = null
                        }
                    }
                    val showChromeStoreBar = !isFullscreen &&
                        !activeTab.isStartPage &&
                        detectedChromeExtId != null &&
                        detectedChromeExtId != dismissedChromeExtId

                    AnimatedVisibility(
                        visible = showChromeStoreBar,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        val extId = detectedChromeExtId.orEmpty()
                        val cleanedTitle = remember(activeTab.title, extId) {
                            ChromeWebStoreDownloader.cleanStorePageTitle(activeTab.title, extId)
                        }
                        val isInstalled = remember(uiState.extensions, extId) {
                            uiState.extensions.any { it.id == "cws_$extId" }
                        }
                        val downloadingState = uiState.chromeStoreInstallState as? ChromeStoreInstallState.Downloading
                        val isDownloadingThis = downloadingState?.extensionId == extId
                        ChromeWebStoreInstallBar(
                            extensionId = extId,
                            extensionTitle = cleanedTitle,
                            isAlreadyInstalled = isInstalled,
                            isDownloading = isDownloadingThis,
                            downloadProgressLabel = if (isDownloadingThis) downloadingState?.progressLabel.orEmpty() else "",
                            onInstallClick = {
                                viewModel.installFromChromeWebStore(
                                    urlOrId = activeTab.url,
                                    fallbackName = cleanedTitle
                                )
                            },
                            onDismiss = { dismissedChromeExtId = extId }
                        )
                    }

                    if (isFullscreen) {
                        val aegisColors = AegisTheme.colors
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .navigationBarsPadding()
                                .padding(16.dp)
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { isFullscreen = false }
                                .testTag("exit_fullscreen_button"),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            border = BorderStroke(1.dp, aegisColors.shieldActive.copy(alpha = 0.45f)),
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FullscreenExit,
                                    contentDescription = stringResource(R.string.action_fullscreen_exit),
                                    tint = aegisColors.shieldActive,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.action_fullscreen_exit),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Active Zoom indicator pill when page is zoomed and control bar is hidden
                    AnimatedVisibility(
                        visible = !activeTab.isStartPage &&
                            !uiState.showZoomBar &&
                            activeTab.zoomPercent != WebViewRegistry.DEFAULT_ZOOM_PERCENT,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .then(if (isFullscreen) Modifier.navigationBarsPadding() else Modifier)
                            .padding(16.dp)
                    ) {
                        ActiveZoomBadge(
                            zoomPercent = activeTab.zoomPercent,
                            onClick = { viewModel.openZoomBar() }
                        )
                    }

                    // Live Page Zoom Control Bar
                    AnimatedVisibility(
                        visible = uiState.showZoomBar && !activeTab.isStartPage,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .then(if (isFullscreen) Modifier.navigationBarsPadding() else Modifier)
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        PageZoomControlBar(
                            zoomPercent = activeTab.zoomPercent,
                            onZoomChange = { pct -> applyActiveTabZoom(pct) },
                            onZoomOut = { stepActiveTabZoom(-WebViewRegistry.ZOOM_STEP_PERCENT) },
                            onZoomIn = { stepActiveTabZoom(WebViewRegistry.ZOOM_STEP_PERCENT) },
                            onResetZoom = { resetActiveTabZoom() },
                            onClose = { viewModel.closeZoomBar() }
                        )
                    }

                    if (isCapturingScreenshot) {
                        ScreenshotCapturingOverlay(
                            currentStep = captureStep,
                            totalSteps = captureTotalSteps,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            if (uiState.showSiteShieldSheet && shouldShowShieldIndicator) {
                SiteShieldBottomSheet(
                    tab = activeTab,
                    isGlobalAdBlockEnabled = uiState.preferences.adBlockEnabled,
                    isSiteProtected = isSiteProtected,
                    shieldDisplayMode = uiState.preferences.shieldDisplayMode,
                    totalBlockedCount = uiState.preferences.totalBlockedCount,
                    onDismiss = { viewModel.setSiteShieldSheetVisible(false) },
                    onToggleGlobalShield = { enabled ->
                        viewModel.setAdBlockEnabled(enabled)
                        if (!activeTab.isStartPage) {
                            webViewRegistry.reload(activeTab.id)
                        }
                    },
                    onToggleSiteProtection = { host, enableProtection ->
                        viewModel.toggleSiteAllowlist(host, allowlisted = !enableProtection)
                        if (!activeTab.isStartPage) {
                            webViewRegistry.reload(activeTab.id)
                        }
                    },
                    onSelectShieldDisplayMode = viewModel::setShieldDisplayMode,
                    onOpenSitePermissions = {
                        viewModel.navigateTo(BrowserDestination.SITE_PERMISSIONS)
                    },
                    onOpenDashboard = {
                        viewModel.navigateTo(BrowserDestination.SHIELD_DASHBOARD)
                    }
                )
            }

            if (uiState.showMoreMenuSheet) {
                val menuCallbacks = BrowserMenuCallbacks(
                    onDismiss = { viewModel.setMoreMenuSheetVisible(false) },
                    onNewTab = {
                        if (!activeTab.isStartPage) {
                            webViewRegistry.captureTabPreview(activeTab.id)
                        }
                        viewModel.createNewTab(isPrivate = false)
                    },
                    onNewPrivateTab = {
                        if (!activeTab.isStartPage) {
                            webViewRegistry.captureTabPreview(activeTab.id)
                        }
                        viewModel.createNewTab(isPrivate = true)
                    },
                    onAssignCurrentTabToGroup = { showAssignActiveTabSheet = true },
                    onAnalyzePageWithAi = { triggerAiPageAnalysis() },
                    onOpenExtensions = { viewModel.navigateTo(BrowserDestination.EXTENSIONS) },
                    onCaptureFullScreenshot = { triggerFullPageScreenshot() },
                    onEnterFullscreen = { isFullscreen = true },
                    onOpenZoomBar = { viewModel.openZoomBar() },
                    onZoomOut = { stepActiveTabZoom(-WebViewRegistry.ZOOM_STEP_PERCENT) },
                    onZoomIn = { stepActiveTabZoom(WebViewRegistry.ZOOM_STEP_PERCENT) },
                    onTranslatePage = { openTranslatePageSheet() },
                    onToggleBookmark = { viewModel.toggleCurrentPageBookmark() },
                    onOpenShortcutPicker = { showShortcutPickerSheet = true },
                    onOpenShieldDashboard = { viewModel.navigateTo(BrowserDestination.SHIELD_DASHBOARD) },
                    onOpenSitePermissions = { viewModel.navigateTo(BrowserDestination.SITE_PERMISSIONS) },
                    onOpenBookmarks = { viewModel.navigateTo(BrowserDestination.BOOKMARKS) },
                    onOpenHistory = { viewModel.navigateTo(BrowserDestination.HISTORY) },
                    onOpenSettings = { viewModel.navigateTo(BrowserDestination.SETTINGS) },
                    onCustomizeMenu = { viewModel.openMenuCustomizationInSettings() },
                    onToggleDesktopMode = {
                        val desktop = viewModel.toggleDesktopMode(activeTab.id)
                        webViewRegistry.setDesktopModeAndReload(activeTab.id, desktop)
                    },
                    onFindInPage = { viewModel.openFindInPage() },
                    onShareUrl = { shareCurrentTabUrl() },
                    onGoForward = { webViewRegistry.goForward(activeTab.id) },
                    onReload = { webViewRegistry.reload(activeTab.id) }
                )

                when (uiState.preferences.menuLayoutStyle) {
                    MenuLayoutStyle.POPUP_LIST -> {
                        BrowserPopupListMenu(
                            tab = activeTab,
                            activeGroup = uiState.activeTabGroup,
                            isCurrentUrlBookmarked = uiState.isCurrentUrlBookmarked,
                            selectedShortcuts = uiState.preferences.omniboxShortcuts,
                            menuItems = uiState.preferences.menuItems,
                            callbacks = menuCallbacks
                        )
                    }
                    MenuLayoutStyle.CLASSIC_SHEET -> {
                        BrowserMoreMenuSheet(
                            tab = activeTab,
                            activeGroup = uiState.activeTabGroup,
                            isCurrentUrlBookmarked = uiState.isCurrentUrlBookmarked,
                            selectedShortcuts = uiState.preferences.omniboxShortcuts,
                            menuItems = uiState.preferences.menuItems,
                            totalBlockedCount = uiState.preferences.totalBlockedCount,
                            callbacks = menuCallbacks
                        )
                    }
                }
            }

            if (uiState.pageAiState !is PageAiUiState.Hidden) {
                AiAnswerPopupDialog(
                    state = uiState.pageAiState,
                    onDismiss = { viewModel.dismissPageAiPopup() },
                    onReanalyze = { triggerAiPageAnalysis() },
                    onCopyText = { textToCopy ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("ai_answer", textToCopy))
                        viewModel.showMessage("回答をクリップボードにコピーしました")
                    }
                )
            }

            if (showShortcutPickerSheet) {
                OmniboxShortcutPickerSheet(
                    selectedShortcuts = uiState.preferences.omniboxShortcuts,
                    onToggleShortcut = { shortcut -> viewModel.toggleOmniboxShortcut(shortcut) },
                    onDismiss = { showShortcutPickerSheet = false }
                )
            }

            if (showTranslateSheet) {
                TranslatePageBottomSheet(
                    currentUrl = activeTab.url,
                    onSelectLanguage = { lang ->
                        val translatedUrl = PageTranslationHelper.buildTranslateUrl(activeTab.url, lang.code)
                        if (translatedUrl.isNotBlank()) {
                            navigateActiveTab(translatedUrl)
                            viewModel.showMessage("${lang.label}に翻訳しています…")
                        }
                    },
                    onRestoreOriginal = {
                        val original = PageTranslationHelper.extractOriginalUrl(activeTab.url)
                        if (!original.isNullOrBlank()) {
                            navigateActiveTab(original)
                            viewModel.showMessage("元のページを表示します")
                        }
                    },
                    onDismiss = { showTranslateSheet = false }
                )
            }

            if (showHomeBackgroundSheet) {
                HomeBackgroundBottomSheet(
                    backgroundPath = uiState.preferences.homeBackgroundPath,
                    dimAlpha = uiState.preferences.homeBackgroundDimAlpha,
                    isImporting = uiState.isImportingHomeBackground,
                    errorMessage = uiState.homeBackgroundError,
                    onPickImage = { launchHomeBackgroundPicker() },
                    onPickFromFile = { launchHomeBackgroundDocumentPicker() },
                    onDimAlphaChange = viewModel::setHomeBackgroundDimAlpha,
                    onResetBackground = viewModel::clearHomeBackground,
                    onDismiss = {
                        viewModel.clearHomeBackgroundError()
                        showHomeBackgroundSheet = false
                    }
                )
            }

            if (showAssignActiveTabSheet) {
                AssignTabToGroupSheet(
                    tab = activeTab,
                    groups = uiState.tabGroups,
                    onDismiss = { showAssignActiveTabSheet = false },
                    onAssignToGroup = { groupId ->
                        viewModel.assignTabToGroup(activeTab.id, groupId)
                    },
                    onCreateNewGroupWithTab = {
                        showCreateGroupForActiveTabDialog = true
                    }
                )
            }

            if (showCreateGroupForActiveTabDialog) {
                CreateOrEditTabGroupDialog(
                    existingGroup = null,
                    availableTabs = uiState.tabs,
                    preselectedTabIds = listOf(activeTab.id),
                    onDismiss = { showCreateGroupForActiveTabDialog = false },
                    onConfirm = { name, color, selectedIds ->
                        viewModel.createTabGroup(name, color, selectedIds)
                        showCreateGroupForActiveTabDialog = false
                    }
                )
            }

            val previewShot = capturedScreenshot
            if (previewShot != null) {
                ScreenshotPreviewDialog(
                    screenshot = previewShot,
                    isSaving = isSavingScreenshot,
                    onDismiss = {
                        capturedScreenshot = null
                        if (!previewShot.bitmap.isRecycled) {
                            previewShot.bitmap.recycle()
                        }
                    },
                    onSaveToGallery = {
                        coroutineScope.launch {
                            isSavingScreenshot = true
                            val saveResult = FullPageScreenshotHelper.saveToGallery(context, previewShot)
                            isSavingScreenshot = false
                            saveResult.onSuccess { path ->
                                capturedScreenshot = null
                                if (!previewShot.bitmap.isRecycled) {
                                    previewShot.bitmap.recycle()
                                }
                                viewModel.showMessage("保存しました: $path")
                            }.onFailure { err ->
                                viewModel.showMessage("保存に失敗しました: ${err.message ?: "不明なエラー"}")
                            }
                        }
                    },
                    onShare = {
                        coroutineScope.launch {
                            isSavingScreenshot = true
                            val shareResult = FullPageScreenshotHelper.createShareIntent(context, previewShot)
                            isSavingScreenshot = false
                            shareResult.onSuccess { intent ->
                                context.startActivity(Intent.createChooser(intent, "スクリーンショットを共有"))
                            }.onFailure { err ->
                                viewModel.showMessage("共有の準備に失敗しました: ${err.message ?: "不明なエラー"}")
                            }
                        }
                    }
                )
            }
        }
    }

    if (pendingWebPermissionPrompt != null) {
        WebPermissionPromptDialog(
            prompt = pendingWebPermissionPrompt,
            onAllow = { rememberChoice ->
                if (rememberChoice && pendingWebPermissionPrompt.originHost.isNotBlank()) {
                    pendingWebPermissionPrompt.requestedTypes.forEach { type ->
                        viewModel.setSitePermission(
                            domain = pendingWebPermissionPrompt.originHost,
                            type = type,
                            state = SitePermissionState.ALLOW,
                            showFeedback = false
                        )
                    }
                }
                onConsumeWebPermissionPrompt()
                grantWebPermissionWithOsCheck(pendingWebPermissionPrompt)
            },
            onDeny = { rememberChoice ->
                if (rememberChoice && pendingWebPermissionPrompt.originHost.isNotBlank()) {
                    pendingWebPermissionPrompt.requestedTypes.forEach { type ->
                        viewModel.setSitePermission(
                            domain = pendingWebPermissionPrompt.originHost,
                            type = type,
                            state = SitePermissionState.DENY,
                            showFeedback = false
                        )
                    }
                }
                onConsumeWebPermissionPrompt()
                when (pendingWebPermissionPrompt) {
                    is PendingWebPermissionPrompt.Media -> runCatching {
                        pendingWebPermissionPrompt.request.deny()
                    }
                    is PendingWebPermissionPrompt.Geolocation -> {
                        pendingWebPermissionPrompt.callback.invoke(
                            pendingWebPermissionPrompt.origin,
                            false,
                            false
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun BrowserWebViewHost(
    tab: BrowserTab,
    javascriptEnabled: Boolean,
    forcePinchZoom: Boolean,
    webViewRegistry: WebViewRegistry,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            FrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { container ->
            val webView = webViewRegistry.getOrCreate(
                context = container.context,
                tab = tab,
                javascriptEnabled = javascriptEnabled,
                forcePinchZoom = forcePinchZoom
            )
            if (container.childCount != 1 || container.getChildAt(0) !== webView) {
                container.removeAllViews()
                (webView.parent as? ViewGroup)?.removeView(webView)
                container.addView(
                    webView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            }
        },
        modifier = modifier.testTag("browser_webview_host")
    )
}

@Composable
private fun WebErrorOverlay(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .background(colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "ページを表示できませんでした",
            style = MaterialTheme.typography.titleLarge,
            color = colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.testTag("webview_retry_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("再読み込み")
        }
    }
}
