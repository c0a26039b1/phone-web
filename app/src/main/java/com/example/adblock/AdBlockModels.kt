package com.example.adblock

enum class BlockCategory(val labelJa: String, val estimatedBytes: Long) {
    AD("広告", 64_000L),
    TRACKER("トラッカー", 18_000L),
    SOCIAL("SNS追跡", 28_000L),
    POPUP("ポップアップ", 42_000L),
    CUSTOM("カスタムルール", 36_000L);

    companion object {
        fun fromName(name: String): BlockCategory =
            entries.firstOrNull { it.name == name } ?: AD
    }
}

data class BlockDecision(
    val shouldBlock: Boolean,
    val category: BlockCategory? = null,
    val matchedDomain: String = ""
)

data class BlockedRequestRecord(
    val host: String,
    val category: BlockCategory,
    val count: Int = 1,
    val lastUrl: String = ""
)

data class ShieldConfig(
    val adBlockEnabled: Boolean = true,
    val blockAds: Boolean = true,
    val blockTrackers: Boolean = true,
    val blockSocial: Boolean = true,
    val blockPopups: Boolean = true,
    val cosmeticFiltering: Boolean = true,
    val allowlistedDomains: Set<String> = emptySet(),
    val customBlockedDomains: Set<String> = emptySet()
)
