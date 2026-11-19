package com.example.ui.browser

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.adblock.AdBlockEngine
import com.example.adblock.BlockCategory
import com.example.adblock.BlockedRequestRecord
import com.example.adblock.ShieldConfig
import com.example.data.BrowserRepository
import com.example.data.local.BrowserDatabase
import com.example.data.local.BrowserExtensionEntity
import com.example.data.local.BrowserMenuItem
import com.example.data.local.MenuLayoutStyle
import com.example.data.local.OmniboxShortcut
import com.example.data.local.PreferencesRepository
import com.example.data.local.SearchEngine
import com.example.data.local.ShieldDisplayMode
import com.example.data.local.SitePermissionEntity
import com.example.data.local.SitePermissionState
import com.example.data.local.SitePermissionType
import com.example.data.local.SiteRuleType
import com.example.data.local.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

private const val AI_TAG = "BrowserPageAi"

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BrowserDatabase.getInstance(application)
    private val preferencesRepository = PreferencesRepository(application)
    private val repository = BrowserRepository(database.browserDao(), preferencesRepository)
    private val pageAiAnalyzer: PageAiAnalyzer = FirebasePageAiAnalyzer()
    private val extensionScriptsDir = File(application.filesDir, "ext_scripts")
    private var aiAnalysisJob: Job? = null

    val adBlockEngine = AdBlockEngine()
    val extensionEngine = ExtensionEngine(scriptsDir = extensionScriptsDir)

    private val initialTab = BrowserTab()
    private val _uiState = MutableStateFlow(
        BrowserUiState(
            tabs = listOf(initialTab),
            activeTabId = initialTab.id
        )
    )
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureDefaultBookmarksSeeded()
            repository.removeBuiltInExtensions()
            repository.ensurePageZoomInCustomMenuIfNeeded()
        }

        viewModelScope.launch {
            combine(
                repository.preferences,
                repository.bookmarks,
                repository.history,
                repository.siteRules,
                repository.topBlockedDomains
            ) { prefs, bookmarks, history, rules, topBlocked ->
                DataBundle(prefs, bookmarks, history, rules, topBlocked)
            }.combine(repository.recentBlockedDomains) { bundle, recentBlocked ->
                bundle to recentBlocked
            }.combine(repository.sitePermissions) { (bundle, recentBlocked), sitePermissions ->
                CombinedState(bundle, recentBlocked, sitePermissions, emptyList())
            }.combine(repository.extensions) { combined, extensions ->
                combined.copy(extensions = extensions)
            }.collect { combined ->
                val bundle = combined.bundle
                val allowlist = bundle.rules
                    .filter { it.ruleType == SiteRuleType.ALLOWLIST.name }
                    .map { it.domain }
                    .toSet()
                val customBlocks = bundle.rules
                    .filter { it.ruleType == SiteRuleType.CUSTOM_BLOCK.name }
                    .map { it.domain }
                    .toSet()

                adBlockEngine.updateConfig(
                    ShieldConfig(
                        adBlockEnabled = bundle.prefs.adBlockEnabled,
                        blockAds = bundle.prefs.blockAds,
                        blockTrackers = bundle.prefs.blockTrackers,
                        blockSocial = bundle.prefs.blockSocial,
                        blockPopups = bundle.prefs.blockPopups,
                        cosmeticFiltering = bundle.prefs.cosmeticFiltering,
                        allowlistedDomains = allowlist,
                        customBlockedDomains = customBlocks
                    )
                )
                extensionEngine.updateState(
                    enabled = bundle.prefs.extensionsEnabled,
                    extensions = combined.extensions
                )

                _uiState.update { state ->
                    state.copy(
                        preferences = bundle.prefs,
                        bookmarks = bundle.bookmarks,
                        history = bundle.history,
                        siteRules = bundle.rules,
                        sitePermissions = combined.sitePermissions,
                        extensions = combined.extensions,
                        topBlockedDomains = bundle.topBlocked,
                        recentBlockedDomains = combined.recentBlocked
                    )
                }
            }
        }
    }

    private data class CombinedState(
        val bundle: DataBundle,
        val recentBlocked: List<com.example.data.local.BlockedDomainStatEntity>,
        val sitePermissions: List<SitePermissionEntity>,
        val extensions: List<BrowserExtensionEntity>
    )

    private data class DataBundle(
        val prefs: com.example.data.local.BrowserPreferences,
        val bookmarks: List<com.example.data.local.BookmarkEntity>,
        val history: List<com.example.data.local.HistoryEntity>,
        val rules: List<com.example.data.local.SiteRuleEntity>,
        val topBlocked: List<com.example.data.local.BlockedDomainStatEntity>
    )

    fun resolveNavigationUrl(rawInput: String): String {
        val prefs = _uiState.value.preferences
        return AdBlockEngine.normalizeInputToUrl(
            input = rawInput,
            searchEngine = prefs.searchEngine,
            httpsUpgrade = prefs.httpsUpgradeEnabled
        )
    }

    fun startNavigation(tabId: String, targetUrl: String) {
        updateTab(tabId) { tab ->
            tab.copy(
                url = targetUrl,
                isLoading = true,
                progress = 10,
                blockedCountOnPage = 0,
                blockedRecordsOnPage = emptyList(),
                lastErrorMessage = null
            )
        }
        _uiState.update {
            it.copy(
                destination = BrowserDestination.BROWSER,
                showSiteShieldSheet = false
            )
        }
    }

    fun navigateToStartPage(tabId: String = _uiState.value.activeTab.id) {
        updateTab(tabId) { tab ->
            tab.copy(
                url = "",
                title = "",
                isLoading = false,
                progress = 0,
                blockedCountOnPage = 0,
                blockedRecordsOnPage = emptyList(),
                lastErrorMessage = null
            )
        }
        _uiState.update {
            it.copy(
                destination = BrowserDestination.BROWSER,
                findInPage = FindInPageState(),
                showZoomBar = false
            )
        }
    }

    fun onPageStarted(tabId: String, url: String, canGoBack: Boolean, canGoForward: Boolean) {
        updateTab(tabId) { tab ->
            val hostChanged = AdBlockEngine.extractHost(tab.url) != AdBlockEngine.extractHost(url)
            tab.copy(
                url = url,
                isLoading = true,
                progress = 15,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                blockedCountOnPage = if (hostChanged || tab.url != url) 0 else tab.blockedCountOnPage,
                blockedRecordsOnPage = if (hostChanged || tab.url != url) emptyList() else tab.blockedRecordsOnPage,
                lastErrorMessage = null
            )
        }
    }

    fun onPageFinished(
        tabId: String,
        url: String,
        title: String,
        canGoBack: Boolean,
        canGoForward: Boolean
    ) {
        val tab = _uiState.value.tabs.firstOrNull { it.id == tabId }
        updateTab(tabId) { current ->
            current.copy(
                url = url,
                title = title.ifBlank { current.title.ifBlank { AdBlockEngine.extractHost(url) } },
                isLoading = false,
                progress = 100,
                canGoBack = canGoBack,
                canGoForward = canGoForward
            )
        }
        if (tab != null && !tab.isPrivate && url.isNotBlank() && !url.startsWith("about:")) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.recordHistory(title, url)
            }
        }
    }

    fun onProgressChanged(tabId: String, progress: Int) {
        updateTab(tabId) { tab ->
            tab.copy(
                progress = progress,
                isLoading = progress in 1..99
            )
        }
    }

    fun onTitleReceived(tabId: String, title: String) {
        if (title.isBlank()) return
        updateTab(tabId) { tab -> tab.copy(title = title) }
    }

    fun onNavigationHistoryUpdated(
        tabId: String,
        url: String?,
        canGoBack: Boolean,
        canGoForward: Boolean
    ) {
        updateTab(tabId) { tab ->
            val updatedUrl = if (!url.isNullOrBlank() && (url.startsWith("http://") || url.startsWith("https://"))) {
                url
            } else {
                tab.url
            }
            tab.copy(
                url = updatedUrl,
                canGoBack = canGoBack,
                canGoForward = canGoForward
            )
        }
    }

    fun onPageError(tabId: String, description: String) {
        updateTab(tabId) { tab ->
            tab.copy(
                isLoading = false,
                progress = 0,
                lastErrorMessage = description
            )
        }
    }

    fun onRequestBlocked(
        tabId: String,
        blockedHost: String,
        category: BlockCategory,
        requestUrl: String
    ) {
        viewModelScope.launch {
            updateTab(tabId) { tab ->
                val existing = tab.blockedRecordsOnPage.toMutableList()
                val idx = existing.indexOfFirst { it.host == blockedHost && it.category == category }
                if (idx >= 0) {
                    val item = existing[idx]
                    existing[idx] = item.copy(count = item.count + 1, lastUrl = requestUrl)
                } else {
                    existing.add(
                        0,
                        BlockedRequestRecord(
                            host = blockedHost,
                            category = category,
                            count = 1,
                            lastUrl = requestUrl
                        )
                    )
                }
                tab.copy(
                    blockedCountOnPage = tab.blockedCountOnPage + 1,
                    blockedRecordsOnPage = existing.take(60)
                )
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordBlockedRequest(blockedHost, category)
        }
    }

    fun onPopupBlocked(tabId: String, blockedHost: String) {
        onRequestBlocked(tabId, blockedHost.ifBlank { "popup" }, BlockCategory.POPUP, blockedHost)
        showMessage("ポップアップをブロックしました: ${blockedHost.ifBlank { "不明なウィンドウ" }}")
    }

    // Tab & Tab Group Management
    fun createNewTab(
        isPrivate: Boolean = false,
        initialUrl: String = "",
        groupId: String? = null,
        keepCurrentDestination: Boolean = false
    ): String {
        val defaultZoom = _uiState.value.preferences.defaultZoomPercent
        val newTab = BrowserTab(
            url = initialUrl,
            isPrivate = isPrivate,
            zoomPercent = defaultZoom,
            groupId = groupId,
            isLoading = initialUrl.isNotBlank()
        )
        _uiState.update { state ->
            val updatedGroups = if (groupId != null) {
                state.tabGroups.map { grp ->
                    if (grp.id == groupId) grp.copy(isCollapsed = false) else grp
                }
            } else {
                state.tabGroups
            }
            state.copy(
                tabs = state.tabs + newTab,
                tabGroups = updatedGroups,
                activeTabId = newTab.id,
                destination = if (keepCurrentDestination) state.destination else BrowserDestination.BROWSER,
                findInPage = FindInPageState(),
                showZoomBar = false
            )
        }
        return newTab.id
    }

    fun mergeTabsIntoGroup(sourceTabId: String, targetTabId: String) {
        if (sourceTabId == targetTabId) return
        val state = _uiState.value
        val sourceTab = state.tabs.firstOrNull { it.id == sourceTabId } ?: return
        val targetTab = state.tabs.firstOrNull { it.id == targetTabId } ?: return

        when {
            targetTab.groupId != null -> {
                if (sourceTab.groupId == targetTab.groupId) return
                assignTabToGroup(sourceTabId, targetTab.groupId)
            }
            sourceTab.groupId != null -> {
                assignTabToGroup(targetTabId, sourceTab.groupId)
            }
            else -> {
                val nextIndex = state.tabGroups.size
                val autoColor = TabGroupColor.entries[nextIndex % TabGroupColor.entries.size]
                val autoName = "グループ ${nextIndex + 1}"
                createTabGroup(
                    name = autoName,
                    color = autoColor,
                    tabIds = listOf(targetTabId, sourceTabId)
                )
            }
        }
    }

    fun createTabGroup(
        name: String,
        color: TabGroupColor,
        tabIds: List<String> = emptyList()
    ): String {
        val cleanName = name.trim().ifBlank { "グループ ${_uiState.value.tabGroups.size + 1}" }
        val newGroup = TabGroup(
            name = cleanName,
            color = color
        )
        _uiState.update { state ->
            val targetIds = tabIds.toSet()
            val updatedTabs = if (targetIds.isNotEmpty()) {
                state.tabs.map { tab ->
                    if (tab.id in targetIds) tab.copy(groupId = newGroup.id) else tab
                }
            } else {
                val freshTab = BrowserTab(groupId = newGroup.id)
                state.tabs + freshTab
            }
            val nextActiveId = if (targetIds.isEmpty()) {
                updatedTabs.last().id
            } else {
                state.activeTabId
            }
            val nonEmptyGroups = pruneEmptyGroups(state.tabGroups + newGroup, updatedTabs)
            state.copy(
                tabs = updatedTabs,
                tabGroups = nonEmptyGroups,
                activeTabId = nextActiveId
            )
        }
        showMessage("グループ「$cleanName」を作成しました")
        return newGroup.id
    }

    fun assignTabToGroup(tabId: String, groupId: String?) {
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == tabId) tab.copy(groupId = groupId) else tab
            }
            val expandedGroups = if (groupId != null) {
                state.tabGroups.map { grp ->
                    if (grp.id == groupId) grp.copy(isCollapsed = false) else grp
                }
            } else {
                state.tabGroups
            }
            state.copy(
                tabs = updatedTabs,
                tabGroups = pruneEmptyGroups(expandedGroups, updatedTabs)
            )
        }
        val groupName = _uiState.value.tabGroups.firstOrNull { it.id == groupId }?.name
        showMessage(
            if (groupName != null) "タブを「$groupName」に追加しました"
            else "タブをグループから外しました"
        )
    }

    fun updateTabGroup(groupId: String, name: String, color: TabGroupColor) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        _uiState.update { state ->
            state.copy(
                tabGroups = state.tabGroups.map { grp ->
                    if (grp.id == groupId) grp.copy(name = cleanName, color = color) else grp
                }
            )
        }
        showMessage("グループ「$cleanName」を更新しました")
    }

    fun toggleGroupCollapsed(groupId: String) {
        _uiState.update { state ->
            state.copy(
                tabGroups = state.tabGroups.map { grp ->
                    if (grp.id == groupId) grp.copy(isCollapsed = !grp.isCollapsed) else grp
                }
            )
        }
    }

    fun ungroupTabs(groupId: String) {
        val groupName = _uiState.value.tabGroups.firstOrNull { it.id == groupId }?.name
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.groupId == groupId) tab.copy(groupId = null) else tab
            }
            state.copy(
                tabs = updatedTabs,
                tabGroups = state.tabGroups.filterNot { it.id == groupId }
            )
        }
        if (groupName != null) {
            showMessage("グループ「$groupName」を解除しました")
        }
    }

    fun closeTabGroup(groupId: String): List<String> {
        val closedIds = _uiState.value.tabs.filter { it.groupId == groupId }.map { it.id }
        val groupName = _uiState.value.tabGroups.firstOrNull { it.id == groupId }?.name
        _uiState.update { state ->
            val remaining = state.tabs.filterNot { it.groupId == groupId }
            val remainingGroups = state.tabGroups.filterNot { it.id == groupId }
            if (remaining.isEmpty()) {
                val fresh = BrowserTab()
                state.copy(
                    tabs = listOf(fresh),
                    tabGroups = emptyList(),
                    activeTabId = fresh.id,
                    destination = BrowserDestination.BROWSER,
                    findInPage = FindInPageState()
                )
            } else {
                val nextActive = if (state.activeTabId in closedIds) {
                    remaining.last().id
                } else {
                    state.activeTabId
                }
                state.copy(
                    tabs = remaining,
                    tabGroups = pruneEmptyGroups(remainingGroups, remaining),
                    activeTabId = nextActive
                )
            }
        }
        if (groupName != null) {
            showMessage("グループ「$groupName」を閉じました")
        }
        return closedIds
    }

    fun selectTab(tabId: String) {
        _uiState.update { state ->
            if (state.tabs.any { it.id == tabId }) {
                state.copy(
                    activeTabId = tabId,
                    destination = BrowserDestination.BROWSER,
                    findInPage = FindInPageState(),
                    showZoomBar = false
                )
            } else {
                state
            }
        }
    }

    fun switchToAdjacentTab(delta: Int): Boolean {
        val state = _uiState.value
        if (state.tabs.size <= 1 || delta == 0) return false
        val currentIndex = state.tabs.indexOfFirst { it.id == state.activeTabId }
        if (currentIndex == -1) return false
        val targetIndex = currentIndex + delta
        if (targetIndex !in state.tabs.indices) return false
        val targetTab = state.tabs[targetIndex]
        _uiState.update {
            it.copy(
                activeTabId = targetTab.id,
                destination = BrowserDestination.BROWSER,
                findInPage = FindInPageState(),
                showZoomBar = false
            )
        }
        return true
    }

    fun closeTab(tabId: String): List<String> {
        val closedIds = mutableListOf(tabId)
        _uiState.update { state ->
            val remaining = state.tabs.filterNot { it.id == tabId }
            if (remaining.isEmpty()) {
                val fresh = BrowserTab()
                state.copy(
                    tabs = listOf(fresh),
                    tabGroups = emptyList(),
                    activeTabId = fresh.id,
                    destination = BrowserDestination.BROWSER,
                    findInPage = FindInPageState()
                )
            } else {
                val nextActive = if (state.activeTabId == tabId) {
                    remaining.last().id
                } else {
                    state.activeTabId
                }
                state.copy(
                    tabs = remaining,
                    tabGroups = pruneEmptyGroups(state.tabGroups, remaining),
                    activeTabId = nextActive
                )
            }
        }
        return closedIds
    }

    fun closeAllTabs(): List<String> {
        val allOldIds = _uiState.value.tabs.map { it.id }
        val fresh = BrowserTab()
        _uiState.update { state ->
            state.copy(
                tabs = listOf(fresh),
                tabGroups = emptyList(),
                activeTabId = fresh.id,
                destination = BrowserDestination.BROWSER,
                findInPage = FindInPageState()
            )
        }
        return allOldIds
    }

    private fun pruneEmptyGroups(groups: List<TabGroup>, tabs: List<BrowserTab>): List<TabGroup> {
        val usedGroupIds = tabs.mapNotNull { it.groupId }.toSet()
        return groups.filter { it.id in usedGroupIds }
    }

    fun toggleDesktopMode(tabId: String = _uiState.value.activeTab.id): Boolean {
        var newMode = false
        updateTab(tabId) { tab ->
            newMode = !tab.isDesktopMode
            tab.copy(isDesktopMode = newMode)
        }
        showMessage(if (newMode) "PC版サイト表示に切り替えました" else "モバイル版表示に切り替えました")
        return newMode
    }

    // Destination & Sheets
    fun navigateTo(destination: BrowserDestination) {
        _uiState.update {
            it.copy(
                destination = destination,
                showMoreMenuSheet = false,
                showSiteShieldSheet = false,
                showZoomBar = if (destination == BrowserDestination.BROWSER) it.showZoomBar else false,
                openMenuCustomizeOnSettings = if (destination == BrowserDestination.SETTINGS) {
                    it.openMenuCustomizeOnSettings
                } else {
                    false
                }
            )
        }
    }

    fun openMenuCustomizationInSettings() {
        _uiState.update {
            it.copy(
                destination = BrowserDestination.SETTINGS,
                showMoreMenuSheet = false,
                showSiteShieldSheet = false,
                showZoomBar = false,
                openMenuCustomizeOnSettings = true
            )
        }
    }

    fun consumeOpenMenuCustomizeFlag() {
        _uiState.update { it.copy(openMenuCustomizeOnSettings = false) }
    }

    fun setSiteShieldSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showSiteShieldSheet = visible) }
    }

    fun setMoreMenuSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showMoreMenuSheet = visible) }
    }

    // Screen / Page Zoom
    fun openZoomBar() {
        val active = _uiState.value.activeTab
        if (active.isStartPage) {
            showMessage("Webページを表示中にズーム調整できます")
            return
        }
        _uiState.update {
            it.copy(
                showMoreMenuSheet = false,
                showZoomBar = true
            )
        }
    }

    fun closeZoomBar() {
        _uiState.update { it.copy(showZoomBar = false) }
    }

    fun setActiveTabZoom(percent: Int): Int {
        val clamped = percent.coerceIn(
            WebViewRegistry.MIN_ZOOM_PERCENT,
            WebViewRegistry.MAX_ZOOM_PERCENT
        )
        val tabId = _uiState.value.activeTab.id
        updateTab(tabId) { tab -> tab.copy(zoomPercent = clamped) }
        return clamped
    }

    fun stepActiveTabZoom(delta: Int): Int {
        val current = _uiState.value.activeTab.zoomPercent
        val step = WebViewRegistry.ZOOM_STEP_PERCENT
        val next = when {
            delta > 0 -> ((current / step) + 1) * step
            delta < 0 -> if (current % step == 0) current - step else (current / step) * step
            else -> current
        }
        return setActiveTabZoom(next)
    }

    fun resetActiveTabZoom(): Int {
        return setActiveTabZoom(WebViewRegistry.DEFAULT_ZOOM_PERCENT)
    }

    fun setDefaultZoomPercent(percent: Int) {
        val clamped = percent.coerceIn(50, 200)
        viewModelScope.launch(Dispatchers.IO) {
            repository.setDefaultZoomPercent(clamped)
        }
        // Also apply immediately to currently open tabs that are on start page or at previous default
        val previousDefault = _uiState.value.preferences.defaultZoomPercent
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs.map { tab ->
                    if (tab.zoomPercent == previousDefault || tab.isStartPage) {
                        tab.copy(zoomPercent = clamped)
                    } else {
                        tab
                    }
                }
            )
        }
    }

    fun setForcePinchZoomEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setForcePinchZoomEnabled(enabled)
        }
    }

    // Custom Home Screen Background
    fun setHomeBackgroundFromUri(uri: Uri) {
        val app = getApplication<Application>()
        _uiState.update {
            it.copy(
                isImportingHomeBackground = true,
                homeBackgroundError = null
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                HomeBackgroundStorage.savePickedImage(app, uri)
            }
            result.onSuccess { savedPath ->
                repository.setHomeBackgroundPath(savedPath)
                _uiState.update {
                    it.copy(
                        isImportingHomeBackground = false,
                        homeBackgroundError = null
                    )
                }
                showMessage("ホーム画面の背景画像を変更しました")
            }.onFailure { err ->
                Log.e("HomeBackground", "Failed to load picked background image: $uri", err)
                val reason = err.message ?: "未対応の形式です"
                _uiState.update {
                    it.copy(
                        isImportingHomeBackground = false,
                        homeBackgroundError = "背景画像を読み込めませんでした: $reason"
                    )
                }
                showMessage("背景画像の読み込みに失敗しました: $reason")
            }
        }
    }

    fun clearHomeBackgroundError() {
        _uiState.update { it.copy(homeBackgroundError = null) }
    }

    fun setHomeBackgroundDimAlpha(alpha: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setHomeBackgroundDimAlpha(alpha)
        }
    }

    fun clearHomeBackground() {
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            HomeBackgroundStorage.deleteAllWallpapers(app)
            repository.setHomeBackgroundPath("")
            _uiState.update {
                it.copy(
                    isImportingHomeBackground = false,
                    homeBackgroundError = null
                )
            }
            showMessage("ホーム画面の背景をデフォルトに戻しました")
        }
    }

    // AI Page Problem Analysis
    fun startPageAiLoading(pageTitle: String, pageUrl: String) {
        aiAnalysisJob?.cancel()
        _uiState.update {
            it.copy(
                showMoreMenuSheet = false,
                pageAiState = PageAiUiState.Loading(
                    pageTitle = pageTitle.ifBlank { pageUrl },
                    pageUrl = pageUrl
                )
            )
        }
    }

    fun analyzeExtractedPage(content: ExtractedPageContent) {
        aiAnalysisJob?.cancel()
        aiAnalysisJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    pageAiState = PageAiUiState.Loading(
                        pageTitle = content.title.ifBlank { content.url },
                        pageUrl = content.url
                    )
                )
            }
            try {
                val result = pageAiAnalyzer.analyzePage(content)
                _uiState.update { it.copy(pageAiState = PageAiUiState.Success(result)) }
            } catch (e: TimeoutCancellationException) {
                Log.e(AI_TAG, "Firebase AI page analysis timed out", e)
                _uiState.update {
                    it.copy(
                        pageAiState = PageAiUiState.Error(
                            pageTitle = content.title.ifBlank { content.url },
                            pageUrl = content.url,
                            message = "AI分析がタイムアウトしました（60秒）。通信環境を確認して再分析をお試しください。"
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(AI_TAG, "Firebase AI page analysis failed", e)
                _uiState.update {
                    it.copy(
                        pageAiState = PageAiUiState.Error(
                            pageTitle = content.title.ifBlank { content.url },
                            pageUrl = content.url,
                            message = e.localizedMessage ?: "ページのAI分析中にエラーが発生しました。"
                        )
                    )
                }
            }
        }
    }

    fun showPageAiError(pageTitle: String, pageUrl: String, message: String) {
        aiAnalysisJob?.cancel()
        _uiState.update {
            it.copy(
                pageAiState = PageAiUiState.Error(
                    pageTitle = pageTitle.ifBlank { pageUrl },
                    pageUrl = pageUrl,
                    message = message
                )
            )
        }
    }

    fun dismissPageAiPopup() {
        aiAnalysisJob?.cancel()
        _uiState.update { it.copy(pageAiState = PageAiUiState.Hidden) }
    }

    // Find in page
    fun openFindInPage() {
        _uiState.update {
            it.copy(
                showMoreMenuSheet = false,
                findInPage = FindInPageState(isActive = true)
            )
        }
    }

    fun updateFindQuery(query: String) {
        _uiState.update {
            it.copy(
                findInPage = it.findInPage.copy(
                    query = query,
                    activeMatchIndex = 0,
                    totalMatches = if (query.isBlank()) 0 else it.findInPage.totalMatches
                )
            )
        }
    }

    fun onFindResult(activeMatchOrdinal: Int, numberOfMatches: Int) {
        _uiState.update {
            it.copy(
                findInPage = it.findInPage.copy(
                    activeMatchIndex = if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0,
                    totalMatches = numberOfMatches
                )
            )
        }
    }

    fun closeFindInPage() {
        _uiState.update { it.copy(findInPage = FindInPageState()) }
    }

    // Bookmarks & History
    fun toggleCurrentPageBookmark() {
        val state = _uiState.value
        val tab = state.activeTab
        if (tab.url.isBlank()) return
        val currentlyBookmarked = state.isCurrentUrlBookmarked
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleBookmark(tab.title, tab.url, currentlyBookmarked)
        }
        showMessage(if (currentlyBookmarked) "ブックマークから削除しました" else "ブックマークに追加しました")
    }

    fun addBookmark(title: String, rawUrl: String) {
        val url = resolveNavigationUrl(rawUrl)
        if (url.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.addBookmark(title, url)
        }
        showMessage("クイックアクセスに追加しました")
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBookmark(id)
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHistoryItem(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
        }
        showMessage("閲覧履歴を消去しました")
    }

    // Shield & Filter Preferences
    fun setAdBlockEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { repository.setAdBlockEnabled(enabled) }
    }

    fun setBlockAds(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { repository.setBlockAds(enabled) }
    }

    fun setBlockTrackers(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { repository.setBlockTrackers(enabled) }
    }

    fun setBlockSocial(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { repository.setBlockSocial(enabled) }
    }

    fun setBlockPopups(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { repository.setBlockPopups(enabled) }
    }

    fun setCosmeticFiltering(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { repository.setCosmeticFiltering(enabled) }
    }

    fun toggleSiteAllowlist(domain: String, allowlisted: Boolean) {
        val clean = AdBlockEngine.sanitizeDomainInput(domain)
        if (clean.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSiteAllowlisted(clean, allowlisted)
        }
        showMessage(
            if (allowlisted) "$clean の広告ブロックを一時停止しました"
            else "$clean の広告ブロックを再開しました"
        )
    }

    fun addCustomBlockDomain(domain: String) {
        val clean = AdBlockEngine.sanitizeDomainInput(domain)
        if (clean.isBlank()) {
            showMessage("有効なドメイン名を入力してください")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.addCustomBlockDomain(clean)
        }
        showMessage("カスタムブロックに追加しました: $clean")
    }

    fun deleteSiteRule(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSiteRule(id)
        }
    }

    // Per-Website Hardware Permissions (Camera, Microphone, Location)
    fun getSitePermissionForHost(host: String): SitePermissionEntity? {
        val clean = AdBlockEngine.sanitizeDomainInput(host)
        if (clean.isBlank()) return null
        return _uiState.value.sitePermissions.firstOrNull { it.domain == clean }
    }

    fun setSitePermission(
        domain: String,
        type: SitePermissionType,
        state: SitePermissionState,
        showFeedback: Boolean = true
    ) {
        val clean = AdBlockEngine.sanitizeDomainInput(domain)
        if (clean.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSitePermission(clean, type, state)
        }
        if (showFeedback) {
            showMessage("$clean の${type.labelJa}を「${state.labelJa}」に変更しました")
        }
    }

    fun upsertSitePermission(
        domain: String,
        camera: SitePermissionState,
        microphone: SitePermissionState,
        location: SitePermissionState
    ) {
        val clean = AdBlockEngine.sanitizeDomainInput(domain)
        if (clean.isBlank()) {
            showMessage("有効なドメイン名を入力してください")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertSitePermission(clean, camera, microphone, location)
        }
        showMessage("$clean の権限設定を保存しました")
    }

    fun deleteSitePermission(domain: String) {
        val clean = AdBlockEngine.sanitizeDomainInput(domain)
        if (clean.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSitePermission(clean)
        }
        showMessage("$clean の権限設定をリセットしました")
    }

    fun clearAllSitePermissions() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllSitePermissions()
        }
        showMessage("すべてのサイト権限設定をリセットしました")
    }

    // Settings
    fun setJavascriptEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { repository.setJavascriptEnabled(enabled) }
    }

    fun setHttpsUpgradeEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { repository.setHttpsUpgradeEnabled(enabled) }
    }

    fun setSearchEngine(engine: SearchEngine) {
        viewModelScope.launch(Dispatchers.IO) { repository.setSearchEngine(engine) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch(Dispatchers.IO) { repository.setThemeMode(mode) }
    }

    fun setShieldDisplayMode(mode: ShieldDisplayMode) {
        viewModelScope.launch(Dispatchers.IO) { repository.setShieldDisplayMode(mode) }
        val currentUrl = _uiState.value.activeTab.url
        val shouldDismissSheet = when (mode) {
            ShieldDisplayMode.ALWAYS_SHOW -> false
            ShieldDisplayMode.HIDE_ON_SEARCH -> AdBlockEngine.isSearchUrl(currentUrl)
            ShieldDisplayMode.HIDE_WHILE_BROWSING -> currentUrl.isNotBlank()
        }
        if (shouldDismissSheet) {
            _uiState.update { it.copy(showSiteShieldSheet = false) }
        }
        val msg = when (mode) {
            ShieldDisplayMode.ALWAYS_SHOW -> "広告ブロック表示を「常に表示」に設定しました"
            ShieldDisplayMode.HIDE_ON_SEARCH -> "検索時の広告ブロック表示を非表示にしました（設定から変更可）"
            ShieldDisplayMode.HIDE_WHILE_BROWSING -> "閲覧中の広告ブロック表示を非表示にしました（設定から変更可）"
        }
        showMessage(msg)
    }

    fun toggleOmniboxShortcut(shortcut: OmniboxShortcut) {
        val current = _uiState.value.preferences.omniboxShortcuts
        val next = if (shortcut in current) {
            if (current.size <= 1) {
                showMessage("ショートカットは1つ以上選択してください")
                return
            }
            current.filterNot { it == shortcut }
        } else {
            OmniboxShortcut.entries.filter { it in current || it == shortcut }
        }
        viewModelScope.launch(Dispatchers.IO) { repository.setOmniboxShortcuts(next) }
    }

    fun setMenuLayoutStyle(style: MenuLayoutStyle) {
        viewModelScope.launch(Dispatchers.IO) { repository.setMenuLayoutStyle(style) }
        showMessage("メニューレイアウトを「${style.shortLabel}」に変更しました")
    }

    fun toggleMenuItem(item: BrowserMenuItem) {
        val current = _uiState.value.preferences.menuItems
        val next = if (item in current) {
            if (current.size <= 1) {
                showMessage("メニュー項目は1つ以上有効にしてください")
                return
            }
            current.filterNot { it == item }
        } else {
            current + item
        }
        viewModelScope.launch(Dispatchers.IO) { repository.setMenuItems(next) }
    }

    fun moveMenuItem(item: BrowserMenuItem, moveUp: Boolean) {
        val current = _uiState.value.preferences.menuItems.toMutableList()
        val index = current.indexOf(item)
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex !in current.indices) return
        current.removeAt(index)
        current.add(targetIndex, item)
        viewModelScope.launch(Dispatchers.IO) { repository.setMenuItems(current) }
    }

    fun resetMenuCustomization() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setMenuLayoutStyle(MenuLayoutStyle.POPUP_LIST)
            repository.setMenuItems(BrowserMenuItem.DEFAULT_ORDER)
        }
        showMessage("メニュー設定を初期状態に戻しました")
    }

    // Browser Extensions
    fun setExtensionsEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { repository.setExtensionsEnabled(enabled) }
        showMessage(if (enabled) "拡張機能エンジンを有効にしました" else "拡張機能エンジンを一時停止しました")
    }

    fun toggleExtension(id: String, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { repository.setExtensionEnabled(id, enabled) }
    }

    fun saveExtension(extension: BrowserExtensionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val prepared = ExtensionEngine.externalizeLargeCodeIfNeeded(extension, extensionScriptsDir)
            repository.upsertExtension(prepared)
        }
        showMessage("拡張機能「${extension.name}」を保存しました")
    }

    fun deleteExtension(extension: BrowserExtensionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            ExtensionEngine.deleteExternalCodeFiles(extension.id, extensionScriptsDir)
            repository.deleteExtension(extension.id)
        }
        showMessage("拡張機能「${extension.name}」を削除しました")
    }

    fun importExtensionFromUri(uri: Uri, fileName: String) {
        val app = getApplication<Application>()
        val resolver = app.contentResolver
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val lowerName = fileName.lowercase()
                resolver.openInputStream(uri)?.use { stream ->
                    if (lowerName.endsWith(".zip") || lowerName.endsWith(".crx") || lowerName.endsWith(".xpi")) {
                        val tempFile = File.createTempFile("aegis_import_", ".crx", app.cacheDir)
                        try {
                            FileOutputStream(tempFile).use { out ->
                                stream.copyTo(out, bufferSize = 32 * 1024)
                            }
                            ExtensionEngine.parseCrxOrZipFile(tempFile, fallbackName = fileName)
                        } finally {
                            runCatching { tempFile.delete() }
                        }
                    } else {
                        val content = stream.bufferedReader(Charsets.UTF_8).readText()
                        ExtensionEngine.parseUserScript(content, fallbackName = fileName)
                    }
                } ?: throw IllegalStateException("ファイルを読み込めませんでした")
            }
            result.onSuccess { entity ->
                val prepared = ExtensionEngine.externalizeLargeCodeIfNeeded(entity, extensionScriptsDir)
                repository.upsertExtension(prepared)
                showMessage("拡張機能「${prepared.name}」を追加しました")
            }.onFailure { err ->
                showMessage("インポートに失敗しました: ${err.message ?: "未対応の形式です"}")
            }
        }
    }

    fun installFromChromeWebStore(urlOrId: String, fallbackName: String = "") {
        if (_uiState.value.chromeStoreInstallState is ChromeStoreInstallState.Downloading) {
            showMessage("別の拡張機能をダウンロード中です。完了までお待ちください")
            return
        }
        val extensionId = ChromeWebStoreDownloader.extractExtensionId(urlOrId)
        if (extensionId == null) {
            showMessage("有効な Chrome ウェブストアの URL または 32 文字の拡張機能 ID を入力してください")
            return
        }

        val displayName = fallbackName.ifBlank { "Chrome 拡張機能 (${extensionId.take(8)}…)" }
        _uiState.update {
            it.copy(
                chromeStoreInstallState = ChromeStoreInstallState.Downloading(
                    extensionId = extensionId,
                    displayName = displayName
                )
            )
        }

        val cacheDir = getApplication<Application>().cacheDir
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                ChromeWebStoreDownloader.downloadExtension(
                    extensionId = extensionId,
                    cacheDir = cacheDir,
                    fallbackName = fallbackName,
                    onProgress = { downloaded, total ->
                        _uiState.update { state ->
                            val current = state.chromeStoreInstallState
                            if (current is ChromeStoreInstallState.Downloading && current.extensionId == extensionId) {
                                state.copy(
                                    chromeStoreInstallState = current.copy(
                                        downloadedBytes = downloaded,
                                        totalBytes = total
                                    )
                                )
                            } else {
                                state
                            }
                        }
                    }
                )
            }
            _uiState.update { it.copy(chromeStoreInstallState = ChromeStoreInstallState.Idle) }
            result.onSuccess { entity ->
                val prepared = ExtensionEngine.externalizeLargeCodeIfNeeded(entity, extensionScriptsDir)
                repository.upsertExtension(prepared)
                val suffix = if (_uiState.value.preferences.extensionsEnabled) {
                    "（自動適用が有効です）"
                } else {
                    "（拡張機能エンジンをONにすると適用されます）"
                }
                showMessage("Chrome ウェブストアから「${prepared.name}」を追加しました$suffix")
            }.onFailure { err ->
                showMessage("Chrome 拡張機能の追加に失敗: ${err.message ?: "ダウンロードエラー"}")
            }
        }
    }

    fun resetBlockedStats() {
        viewModelScope.launch(Dispatchers.IO) { repository.resetBlockedStats() }
        showMessage("ブロック統計をリセットしました")
    }

    fun showMessage(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun updateTab(tabId: String, transform: (BrowserTab) -> BrowserTab) {
        _uiState.update { state ->
            val updated = state.tabs.map { tab ->
                if (tab.id == tabId) transform(tab) else tab
            }
            state.copy(tabs = updated)
        }
    }
}
