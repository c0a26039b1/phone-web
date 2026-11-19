package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["url"], unique = true)]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val accentIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val visitedAt: Long = System.currentTimeMillis()
)

enum class SiteRuleType {
    ALLOWLIST,
    CUSTOM_BLOCK
}

@Entity(
    tableName = "site_rules",
    indices = [Index(value = ["domain", "ruleType"], unique = true)]
)
data class SiteRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val ruleType: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocked_domain_stats")
data class BlockedDomainStatEntity(
    @PrimaryKey val domain: String,
    val category: String,
    val count: Long = 1,
    val lastBlockedAt: Long = System.currentTimeMillis()
)

enum class SitePermissionState(val labelJa: String) {
    ALLOW("許可"),
    ASK("毎回確認"),
    DENY("拒否");

    companion object {
        fun fromName(name: String?): SitePermissionState =
            entries.firstOrNull { it.name == name } ?: ASK
    }
}

enum class SitePermissionType(val labelJa: String) {
    CAMERA("カメラ"),
    MICROPHONE("マイク"),
    LOCATION("位置情報")
}

@Entity(tableName = "site_permissions")
data class SitePermissionEntity(
    @PrimaryKey val domain: String,
    val camera: String = SitePermissionState.ASK.name,
    val microphone: String = SitePermissionState.ASK.name,
    val location: String = SitePermissionState.ASK.name,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val cameraState: SitePermissionState
        get() = SitePermissionState.fromName(camera)

    val microphoneState: SitePermissionState
        get() = SitePermissionState.fromName(microphone)

    val locationState: SitePermissionState
        get() = SitePermissionState.fromName(location)

    fun stateFor(type: SitePermissionType): SitePermissionState = when (type) {
        SitePermissionType.CAMERA -> cameraState
        SitePermissionType.MICROPHONE -> microphoneState
        SitePermissionType.LOCATION -> locationState
    }

    fun withState(
        type: SitePermissionType,
        state: SitePermissionState,
        now: Long = System.currentTimeMillis()
    ): SitePermissionEntity = when (type) {
        SitePermissionType.CAMERA -> copy(camera = state.name, updatedAt = now)
        SitePermissionType.MICROPHONE -> copy(microphone = state.name, updatedAt = now)
        SitePermissionType.LOCATION -> copy(location = state.name, updatedAt = now)
    }
}

@Entity(tableName = "extensions")
data class BrowserExtensionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val version: String = "1.0.0",
    val matchPatterns: String = "*",
    val jsCode: String = "",
    val cssCode: String = "",
    val enabled: Boolean = false,
    val isBuiltIn: Boolean = false,
    val iconKey: String = "extension",
    val createdAt: Long = System.currentTimeMillis()
)
