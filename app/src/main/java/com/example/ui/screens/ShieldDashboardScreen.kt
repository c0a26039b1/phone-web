package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.RemoveModerator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.adblock.BlockCategory
import com.example.data.local.BlockedDomainStatEntity
import com.example.data.local.BrowserPreferences
import com.example.data.local.ShieldDisplayMode
import com.example.data.local.SiteRuleEntity
import com.example.data.local.SiteRuleType
import com.example.ui.components.formatBytesSaved
import com.example.ui.components.formatCompactCount
import com.example.ui.components.formatTimeSaved
import com.example.ui.theme.AegisTheme
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShieldDashboardScreen(
    preferences: BrowserPreferences,
    siteRules: List<SiteRuleEntity>,
    topBlockedDomains: List<BlockedDomainStatEntity>,
    onBack: () -> Unit,
    onToggleMaster: (Boolean) -> Unit,
    onSelectShieldDisplayMode: (ShieldDisplayMode) -> Unit,
    onToggleAds: (Boolean) -> Unit,
    onToggleTrackers: (Boolean) -> Unit,
    onToggleSocial: (Boolean) -> Unit,
    onTogglePopups: (Boolean) -> Unit,
    onToggleCosmetic: (Boolean) -> Unit,
    onAddAllowlistDomain: (String) -> Unit,
    onAddCustomBlockDomain: (String) -> Unit,
    onDeleteSiteRule: (Long) -> Unit
) {
    BackHandler(onBack = onBack)

    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors

    val allowlist = remember(siteRules) {
        siteRules.filter { it.ruleType == SiteRuleType.ALLOWLIST.name }
    }
    val customBlocks = remember(siteRules) {
        siteRules.filter { it.ruleType == SiteRuleType.CUSTOM_BLOCK.name }
    }

    var customBlockInput by remember { mutableStateOf("") }
    var allowlistInput by remember { mutableStateOf("") }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.dashboard_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("dashboard_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Master Switch + Summary Strip
                item(key = "master_shield_card") {
                    val active = preferences.adBlockEnabled
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = if (active) aegisColors.shieldActiveContainer else colorScheme.surfaceContainer,
                        border = BorderStroke(
                            1.dp,
                            if (active) aegisColors.shieldActive.copy(alpha = 0.4f) else colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (active) {
                                        aegisColors.shieldActive.copy(alpha = 0.18f)
                                    } else {
                                        colorScheme.surfaceContainerHigh
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (active) Icons.Filled.Shield else Icons.Outlined.RemoveModerator,
                                        contentDescription = null,
                                        tint = if (active) aegisColors.shieldActive else colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.filter_master_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.filter_master_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = active,
                                    onCheckedChange = onToggleMaster,
                                    modifier = Modifier.testTag("dashboard_master_switch"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colorScheme.onPrimary,
                                        checkedTrackColor = aegisColors.shieldActive
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                DashboardStatTile(
                                    label = stringResource(R.string.home_stats_blocked),
                                    value = formatCompactCount(preferences.totalBlockedCount),
                                    modifier = Modifier.weight(1f)
                                )
                                DashboardStatTile(
                                    label = stringResource(R.string.home_stats_data_saved),
                                    value = formatBytesSaved(preferences.totalBytesSaved),
                                    modifier = Modifier.weight(1f)
                                )
                                DashboardStatTile(
                                    label = stringResource(R.string.home_stats_time_saved),
                                    value = formatTimeSaved(preferences.totalBlockedCount),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Shield Display Visibility Selector
                item(key = "shield_display_visibility") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "「広告ブロック稼働中」の表示設定",
                                style = MaterialTheme.typography.titleMedium,
                                color = colorScheme.onBackground
                            )
                            Text(
                                text = "検索時や閲覧中にシールドバッジ・パネルを非表示にするか選択できます",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ShieldDisplayMode.entries.forEach { mode ->
                                val selected = preferences.shieldDisplayMode == mode
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onSelectShieldDisplayMode(mode) }
                                        .testTag("dashboard_shield_display_${mode.name}"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) aegisColors.shieldActiveContainer else colorScheme.surface,
                                    border = BorderStroke(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) aegisColors.shieldActive else colorScheme.outlineVariant
                                    )
                                ) {
                                    Text(
                                        text = mode.shortLabel,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (selected) aegisColors.shieldActive else colorScheme.onSurface,
                                        modifier = Modifier
                                            .padding(vertical = 13.dp, horizontal = 4.dp)
                                            .fillMaxWidth(),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Filter Category Toggles
                item(key = "filter_categories") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.dashboard_filters_section),
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onBackground
                        )
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colorScheme.surface,
                            border = BorderStroke(1.dp, colorScheme.outlineVariant)
                        ) {
                            Column {
                                FilterToggleRow(
                                    title = stringResource(R.string.filter_ads_title),
                                    description = stringResource(R.string.filter_ads_desc),
                                    checked = preferences.blockAds,
                                    enabled = preferences.adBlockEnabled,
                                    onCheckedChange = onToggleAds,
                                    testTag = "filter_ads_switch"
                                )
                                HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                                FilterToggleRow(
                                    title = stringResource(R.string.filter_trackers_title),
                                    description = stringResource(R.string.filter_trackers_desc),
                                    checked = preferences.blockTrackers,
                                    enabled = preferences.adBlockEnabled,
                                    onCheckedChange = onToggleTrackers,
                                    testTag = "filter_trackers_switch"
                                )
                                HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                                FilterToggleRow(
                                    title = stringResource(R.string.filter_social_title),
                                    description = stringResource(R.string.filter_social_desc),
                                    checked = preferences.blockSocial,
                                    enabled = preferences.adBlockEnabled,
                                    onCheckedChange = onToggleSocial,
                                    testTag = "filter_social_switch"
                                )
                                HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                                FilterToggleRow(
                                    title = stringResource(R.string.filter_popups_title),
                                    description = stringResource(R.string.filter_popups_desc),
                                    checked = preferences.blockPopups,
                                    enabled = preferences.adBlockEnabled,
                                    onCheckedChange = onTogglePopups,
                                    testTag = "filter_popups_switch"
                                )
                                HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                                FilterToggleRow(
                                    title = stringResource(R.string.filter_cosmetic_title),
                                    description = stringResource(R.string.filter_cosmetic_desc),
                                    checked = preferences.cosmeticFiltering,
                                    enabled = preferences.adBlockEnabled,
                                    onCheckedChange = onToggleCosmetic,
                                    testTag = "filter_cosmetic_switch"
                                )
                            }
                        }
                    }
                }

                // Custom Blocklist Section
                item(key = "custom_blocklist_section") {
                    DomainRuleManagerCard(
                        title = stringResource(R.string.dashboard_custom_rules_section),
                        placeholder = "遮断するドメイン（例: ads.example.com）",
                        emptyHint = "独自に遮断したい広告・トラッカードメインを追加できます。",
                        input = customBlockInput,
                        onInputChange = { customBlockInput = it },
                        onAdd = {
                            if (customBlockInput.isNotBlank()) {
                                onAddCustomBlockDomain(customBlockInput)
                                customBlockInput = ""
                            }
                        },
                        rules = customBlocks,
                        onDelete = onDeleteSiteRule,
                        accentColor = aegisColors.blockedAccent,
                        inputTestTag = "custom_block_domain_input",
                        addTestTag = "custom_block_add_button"
                    )
                }

                // Allowlist Section
                item(key = "allowlist_section") {
                    DomainRuleManagerCard(
                        title = stringResource(R.string.dashboard_allowlist_section),
                        placeholder = "除外するドメイン（例: trusted-site.jp）",
                        emptyHint = "広告ブロックを一時停止するサイトはありません。",
                        input = allowlistInput,
                        onInputChange = { allowlistInput = it },
                        onAdd = {
                            if (allowlistInput.isNotBlank()) {
                                onAddAllowlistDomain(allowlistInput)
                                allowlistInput = ""
                            }
                        },
                        rules = allowlist,
                        onDelete = onDeleteSiteRule,
                        accentColor = aegisColors.shieldPaused,
                        inputTestTag = "allowlist_domain_input",
                        addTestTag = "allowlist_add_button"
                    )
                }

                // Top Blocked Domains Breakdown
                item(key = "top_blocked_domains") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.dashboard_top_domains_section),
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onBackground
                        )
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colorScheme.surface,
                            border = BorderStroke(1.dp, colorScheme.outlineVariant)
                        ) {
                            if (topBlockedDomains.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.home_no_blocks_yet),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            } else {
                                val maxCount = topBlockedDomains.maxOf { it.count }.coerceAtLeast(1L)
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    topBlockedDomains.take(10).forEach { item ->
                                        val category = BlockCategory.fromName(item.category)
                                        val barColor = when (category) {
                                            BlockCategory.AD -> aegisColors.blockedAccent
                                            BlockCategory.TRACKER -> aegisColors.trackerAccent
                                            BlockCategory.SOCIAL -> aegisColors.privateAccent
                                            BlockCategory.POPUP -> aegisColors.shieldPaused
                                            BlockCategory.CUSTOM -> aegisColors.shieldActive
                                        }
                                        val ratio = (item.count.toFloat() / maxCount.toFloat()).coerceIn(0.06f, 1f)
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = item.domain,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "${item.count} 件",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontFamily = SpaceGroteskFontFamily,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = barColor
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(colorScheme.surfaceContainerHigh)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(ratio)
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(barColor)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = colorScheme.surface.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FilterToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.45f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = colorScheme.onPrimary,
                checkedTrackColor = aegisColors.shieldActive
            )
        )
    }
}

@Composable
private fun DomainRuleManagerCard(
    title: String,
    placeholder: String,
    emptyHint: String,
    input: String,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
    rules: List<SiteRuleEntity>,
    onDelete: (Long) -> Unit,
    accentColor: androidx.compose.ui.graphics.Color,
    inputTestTag: String,
    addTestTag: String
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.onBackground
        )
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colorScheme.surface,
            border = BorderStroke(1.dp, colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = onInputChange,
                        placeholder = {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(inputTestTag),
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilledTonalIconButton(
                        onClick = onAdd,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag(addTestTag)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "追加"
                        )
                    }
                }

                if (rules.isEmpty()) {
                    Text(
                        text = emptyHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        rules.forEach { rule ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colorScheme.surfaceContainerLow
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Block,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = rule.domain,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onDelete(rule.id) },
                                        modifier = Modifier.testTag("delete_rule_${rule.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = stringResource(R.string.cd_delete_item),
                                            tint = colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
