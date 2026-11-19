package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserDao {

    // Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT COUNT(*) FROM bookmarks")
    suspend fun getBookmarkCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBookmark(bookmark: BookmarkEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookmarks(bookmarks: List<BookmarkEntity>)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)

    // History
    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT 300")
    fun observeHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    // Site Rules (Allowlist & Custom Blocklist)
    @Query("SELECT * FROM site_rules ORDER BY createdAt DESC")
    fun observeSiteRules(): Flow<List<SiteRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSiteRule(rule: SiteRuleEntity)

    @Query("DELETE FROM site_rules WHERE domain = :domain AND ruleType = :ruleType")
    suspend fun deleteSiteRule(domain: String, ruleType: String)

    @Query("DELETE FROM site_rules WHERE id = :id")
    suspend fun deleteSiteRuleById(id: Long)

    // Blocked Domain Telemetry
    @Query("SELECT * FROM blocked_domain_stats ORDER BY count DESC LIMIT 20")
    fun observeTopBlockedDomains(): Flow<List<BlockedDomainStatEntity>>

    @Query("SELECT * FROM blocked_domain_stats ORDER BY lastBlockedAt DESC LIMIT 10")
    fun observeRecentBlockedDomains(): Flow<List<BlockedDomainStatEntity>>

    @Query(
        """
        INSERT INTO blocked_domain_stats(domain, category, count, lastBlockedAt)
        VALUES(:domain, :category, 1, :timestamp)
        ON CONFLICT(domain) DO UPDATE SET
            count = count + 1,
            category = :category,
            lastBlockedAt = :timestamp
        """
    )
    suspend fun recordBlockedDomain(domain: String, category: String, timestamp: Long)

    @Query("DELETE FROM blocked_domain_stats")
    suspend fun clearBlockedDomainStats()

    // Per-Website Hardware Permissions (Camera, Microphone, Location)
    @Query("SELECT * FROM site_permissions ORDER BY updatedAt DESC")
    fun observeSitePermissions(): Flow<List<SitePermissionEntity>>

    @Query("SELECT * FROM site_permissions WHERE domain = :domain LIMIT 1")
    suspend fun getSitePermissionByDomain(domain: String): SitePermissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSitePermission(entity: SitePermissionEntity)

    @Query("DELETE FROM site_permissions WHERE domain = :domain")
    suspend fun deleteSitePermission(domain: String)

    @Query("DELETE FROM site_permissions")
    suspend fun clearAllSitePermissions()

    // Browser Extensions
    @Query("SELECT * FROM extensions WHERE isBuiltIn = 0 ORDER BY createdAt DESC")
    fun observeExtensions(): Flow<List<BrowserExtensionEntity>>

    @Query("SELECT COUNT(*) FROM extensions WHERE isBuiltIn = 0")
    suspend fun getExtensionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExtension(extension: BrowserExtensionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExtensions(extensions: List<BrowserExtensionEntity>)

    @Query("UPDATE extensions SET enabled = :enabled WHERE id = :id")
    suspend fun setExtensionEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM extensions WHERE id = :id")
    suspend fun deleteExtensionById(id: String)

    @Query("DELETE FROM extensions WHERE isBuiltIn = 1")
    suspend fun deleteBuiltInExtensions()
}
