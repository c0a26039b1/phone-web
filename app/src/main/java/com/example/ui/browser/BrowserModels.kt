package com.example.ui.browser

import com.example.adblock.BlockedRequestRecord
import com.example.data.local.BlockedDomainStatEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.BrowserExtensionEntity
import com.example.data.local.BrowserPreferences
import com.example.data.local.HistoryEntity
import com.example.data.local.SitePermissionEntity
import com.example.data.local.SiteRuleEntity
import java.util.UUID

enum class TabGroupColor(val labelJa: String) {
    EMERALD("エメラルド"),
    CYAN("シアン"),
    PURPLE("パープル"),
    AMBER("アンバー"),
    CORAL("コーラル"),
    BLUE("ブルー")
}

data class TabGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: TabGroupColor = TabGroupColor.EMERALD,
    val isCollapsed: Boolean = false
)

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "",
    val title: String = "",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isPrivate: Boolean = false,
    val isDesktopMode: Boolean = false,
    val zoomPercent: Int = 100,
    val groupId: String? = null,
    val blockedCountOnPage: Int = 0,
    val blockedRecordsOnPage: List<BlockedRequestRecord> = emptyList(),
    val lastErrorMessage: String? = null
) {
    val isStartPage: Boolean
        get() = url.isBlank()
}

enum class BrowserDestination {
    BROWSER,
    TABS,
    SHIELD_DASHBOARD,
    BOOKMARKS,
    HISTORY,
    SETTINGS,
    SITE_PERMISSIONS,
    EXTENSIONS
}

data class FindInPageState(
    val isActive: Boolean = false,
    val query: String = "",
    val activeMatchIndex: Int = 0,
    val totalMatches: Int = 0
)

sealed interface ChromeStoreInstallState {
    data object Idle : ChromeStoreInstallState
    data class Downloading(
        val extensionId: String,
        val displayName: String,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = -1L
    ) : ChromeStoreInstallState {
        val progressLabel: String
            get() {
                if (downloadedBytes <= 0L) return ""
                val dlMb = downloadedBytes / (1024.0 * 1024.0)
                return if (totalBytes > 0L) {
                    val totalMb = totalBytes / (1024.0 * 1024.0)
                    String.format(java.util.Locale.US, "%.1f / %.1f MB", dlMb, totalMb)
                } else {
                    String.format(java.util.Locale.US, "%.1f MB", dlMb)
                }
            }
    }
}

data class BrowserUiState(
    val tabs: List<BrowserTab> = listOf(BrowserTab()),
    val tabGroups: List<TabGroup> = emptyList(),
    val activeTabId: String = "",
    val destination: BrowserDestination = BrowserDestination.BROWSER,
    val showSiteShieldSheet: Boolean = false,
    val showMoreMenuSheet: Boolean = false,
    val showZoomBar: Boolean = false,
    val openMenuCustomizeOnSettings: Boolean = false,
    val pageAiState: PageAiUiState = PageAiUiState.Hidden,
    val chromeStoreInstallState: ChromeStoreInstallState = ChromeStoreInstallState.Idle,
    val findInPage: FindInPageState = FindInPageState(),
    val preferences: BrowserPreferences = BrowserPreferences(),
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val history: List<HistoryEntity> = emptyList(),
    val siteRules: List<SiteRuleEntity> = emptyList(),
    val sitePermissions: List<SitePermissionEntity> = emptyList(),
    val extensions: List<BrowserExtensionEntity> = emptyList(),
    val topBlockedDomains: List<BlockedDomainStatEntity> = emptyList(),
    val recentBlockedDomains: List<BlockedDomainStatEntity> = emptyList(),
    val isImportingHomeBackground: Boolean = false,
    val homeBackgroundError: String? = null,
    val snackbarMessage: String? = null
) {
    val activeTab: BrowserTab
        get() = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()

    val activeTabGroup: TabGroup?
        get() {
            val groupId = activeTab.groupId ?: return null
            return tabGroups.firstOrNull { it.id == groupId }
        }

    val tabsInActiveGroup: List<BrowserTab>
        get() {
            val groupId = activeTab.groupId ?: return emptyList()
            return tabs.filter { it.groupId == groupId }
        }

    val isCurrentUrlBookmarked: Boolean
        get() {
            val currentUrl = activeTab.url
            return currentUrl.isNotBlank() && bookmarks.any { it.url == currentUrl }
        }
}
