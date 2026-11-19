package com.example.data

import com.example.adblock.AdBlockEngine
import com.example.adblock.BlockCategory
import com.example.data.local.BlockedDomainStatEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.BrowserDao
import com.example.data.local.BrowserDatabase
import com.example.data.local.BrowserExtensionEntity
import com.example.data.local.BrowserMenuItem
import com.example.data.local.BrowserPreferences
import com.example.data.local.HistoryEntity
import com.example.data.local.MenuLayoutStyle
import com.example.data.local.OmniboxShortcut
import com.example.data.local.PreferencesRepository
import com.example.data.local.SearchEngine
import com.example.data.local.ShieldDisplayMode
import com.example.data.local.SitePermissionEntity
import com.example.data.local.SitePermissionState
import com.example.data.local.SitePermissionType
import com.example.data.local.SiteRuleEntity
import com.example.data.local.SiteRuleType
import com.example.data.local.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class BrowserRepository(
    private val dao: BrowserDao,
    private val prefs: PreferencesRepository
) {
    val preferences: Flow<BrowserPreferences> = prefs.preferencesFlow
    val bookmarks: Flow<List<BookmarkEntity>> = dao.observeBookmarks()
    val history: Flow<List<HistoryEntity>> = dao.observeHistory()
    val siteRules: Flow<List<SiteRuleEntity>> = dao.observeSiteRules()
    val sitePermissions: Flow<List<SitePermissionEntity>> = dao.observeSitePermissions()
    val extensions: Flow<List<BrowserExtensionEntity>> = dao.observeExtensions()
    val topBlockedDomains: Flow<List<BlockedDomainStatEntity>> = dao.observeTopBlockedDomains()
    val recentBlockedDomains: Flow<List<BlockedDomainStatEntity>> = dao.observeRecentBlockedDomains()

    suspend fun ensureDefaultBookmarksSeeded() {
        val currentPrefs = prefs.preferencesFlow.first()
        if (!currentPrefs.seededBookmarks) {
            if (dao.getBookmarkCount() == 0) {
                dao.insertBookmarks(BrowserDatabase.defaultBookmarks)
            }
            prefs.markBookmarksSeeded()
        }
    }

    suspend fun removeBuiltInExtensions() {
        dao.deleteBuiltInExtensions()
    }

    suspend fun toggleBookmark(title: String, url: String, currentlyBookmarked: Boolean) {
        if (currentlyBookmarked) {
            dao.deleteBookmarkByUrl(url)
        } else {
            val cleanTitle = title.ifBlank { AdBlockEngine.extractHost(url).ifBlank { url } }
            val accentIndex = (cleanTitle.hashCode() and 0x7FFFFFFF) % 6
            dao.upsertBookmark(
                BookmarkEntity(
                    title = cleanTitle,
                    url = url,
                    accentIndex = accentIndex,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun addBookmark(title: String, url: String) {
        val cleanTitle = title.trim().ifBlank { AdBlockEngine.extractHost(url).ifBlank { url } }
        val accentIndex = (cleanTitle.hashCode() and 0x7FFFFFFF) % 6
        dao.upsertBookmark(
            BookmarkEntity(
                title = cleanTitle,
                url = url.trim(),
                accentIndex = accentIndex,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteBookmark(id: Long) = dao.deleteBookmarkById(id)

    suspend fun recordHistory(title: String, url: String) {
        if (url.isBlank() || url.startsWith("about:")) return
        dao.insertHistory(
            HistoryEntity(
                title = title.ifBlank { AdBlockEngine.extractHost(url).ifBlank { url } },
                url = url,
                visitedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteHistoryItem(id: Long) = dao.deleteHistoryById(id)

    suspend fun clearHistory() = dao.clearHistory()

    suspend fun setSiteAllowlisted(domain: String, allowlisted: Boolean) {
        val sanitized = AdBlockEngine.sanitizeDomainInput(domain)
        if (sanitized.isBlank()) return
        if (allowlisted) {
            dao.upsertSiteRule(
                SiteRuleEntity(
                    domain = sanitized,
                    ruleType = SiteRuleType.ALLOWLIST.name
                )
            )
        } else {
            dao.deleteSiteRule(sanitized, SiteRuleType.ALLOWLIST.name)
        }
    }

    suspend fun addCustomBlockDomain(domain: String) {
        val sanitized = AdBlockEngine.sanitizeDomainInput(domain)
        if (sanitized.isBlank()) return
        dao.upsertSiteRule(
            SiteRuleEntity(
                domain = sanitized,
                ruleType = SiteRuleType.CUSTOM_BLOCK.name
            )
        )
    }

    suspend fun deleteSiteRule(id: Long) = dao.deleteSiteRuleById(id)

    suspend fun recordBlockedRequest(domain: String, category: BlockCategory) {
        if (domain.isBlank()) return
        dao.recordBlockedDomain(
            domain = domain,
            category = category.name,
            timestamp = System.currentTimeMillis()
        )
        prefs.incrementBlockedStats(
            countDelta = 1L,
            estimatedBytesDelta = category.estimatedBytes
        )
    }

    suspend fun resetBlockedStats() {
        dao.clearBlockedDomainStats()
        prefs.resetBlockedStats()
    }

    suspend fun setSitePermission(
        domain: String,
        type: SitePermissionType,
        state: SitePermissionState
    ) {
        val clean = AdBlockEngine.sanitizeDomainInput(domain)
        if (clean.isBlank()) return
        val existing = dao.getSitePermissionByDomain(clean) ?: SitePermissionEntity(domain = clean)
        dao.upsertSitePermission(existing.withState(type, state))
    }

    suspend fun upsertSitePermission(
        domain: String,
        camera: SitePermissionState,
        microphone: SitePermissionState,
        location: SitePermissionState
    ) {
        val clean = AdBlockEngine.sanitizeDomainInput(domain)
        if (clean.isBlank()) return
        dao.upsertSitePermission(
            SitePermissionEntity(
                domain = clean,
                camera = camera.name,
                microphone = microphone.name,
                location = location.name,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSitePermission(domain: String) {
        val clean = AdBlockEngine.sanitizeDomainInput(domain)
        if (clean.isBlank()) return
        dao.deleteSitePermission(clean)
    }

    suspend fun clearAllSitePermissions() = dao.clearAllSitePermissions()

    suspend fun setAdBlockEnabled(enabled: Boolean) = prefs.setAdBlockEnabled(enabled)
    suspend fun setBlockAds(enabled: Boolean) = prefs.setBlockAds(enabled)
    suspend fun setBlockTrackers(enabled: Boolean) = prefs.setBlockTrackers(enabled)
    suspend fun setBlockSocial(enabled: Boolean) = prefs.setBlockSocial(enabled)
    suspend fun setBlockPopups(enabled: Boolean) = prefs.setBlockPopups(enabled)
    suspend fun setCosmeticFiltering(enabled: Boolean) = prefs.setCosmeticFiltering(enabled)
    suspend fun setJavascriptEnabled(enabled: Boolean) = prefs.setJavascriptEnabled(enabled)
    suspend fun setHttpsUpgradeEnabled(enabled: Boolean) = prefs.setHttpsUpgradeEnabled(enabled)
    suspend fun setSearchEngine(engine: SearchEngine) = prefs.setSearchEngine(engine)
    suspend fun setThemeMode(mode: ThemeMode) = prefs.setThemeMode(mode)
    suspend fun setShieldDisplayMode(mode: ShieldDisplayMode) = prefs.setShieldDisplayMode(mode)
    suspend fun setOmniboxShortcuts(shortcuts: List<OmniboxShortcut>) = prefs.setOmniboxShortcuts(shortcuts)
    suspend fun setMenuLayoutStyle(style: MenuLayoutStyle) = prefs.setMenuLayoutStyle(style)
    suspend fun setMenuItems(items: List<BrowserMenuItem>) = prefs.setMenuItems(items)
    suspend fun setExtensionsEnabled(enabled: Boolean) = prefs.setExtensionsEnabled(enabled)
    suspend fun setDefaultZoomPercent(percent: Int) = prefs.setDefaultZoomPercent(percent)
    suspend fun setForcePinchZoomEnabled(enabled: Boolean) = prefs.setForcePinchZoomEnabled(enabled)
    suspend fun setHomeBackgroundPath(path: String) = prefs.setHomeBackgroundPath(path)
    suspend fun setHomeBackgroundDimAlpha(alpha: Float) = prefs.setHomeBackgroundDimAlpha(alpha)
    suspend fun ensurePageZoomInCustomMenuIfNeeded() = prefs.ensurePageZoomInCustomMenuIfNeeded()
    suspend fun upsertExtension(extension: BrowserExtensionEntity) = dao.upsertExtension(extension)
    suspend fun setExtensionEnabled(id: String, enabled: Boolean) = dao.setExtensionEnabled(id, enabled)
    suspend fun deleteExtension(id: String) = dao.deleteExtensionById(id)
}
