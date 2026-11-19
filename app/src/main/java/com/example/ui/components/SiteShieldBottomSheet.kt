package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.adblock.AdBlockEngine
import com.example.adblock.BlockCategory
import com.example.data.local.ShieldDisplayMode
import com.example.ui.browser.BrowserTab
import com.example.ui.theme.AegisTheme
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteShieldBottomSheet(
    tab: BrowserTab,
    isGlobalAdBlockEnabled: Boolean,
    isSiteProtected: Boolean,
    shieldDisplayMode: ShieldDisplayMode,
    totalBlockedCount: Long,
    onDismiss: () -> Unit,
    onToggleGlobalShield: (Boolean) -> Unit,
    onToggleSiteProtection: (String, Boolean) -> Unit,
    onSelectShieldDisplayMode: (ShieldDisplayMode) -> Unit,
    onOpenSitePermissions: () -> Unit,
    onOpenDashboard: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors
    val currentHost = AdBlockEngine.extractHost(tab.url)
    val isActive = isGlobalAdBlockEnabled && isSiteProtected

    val statusColor = when {
        !isGlobalAdBlockEnabled -> colorScheme.onSurfaceVariant
        !isSiteProtected -> aegisColors.shieldPaused
        else -> aegisColors.shieldActive
    }
    val statusContainer = when {
        !isGlobalAdBlockEnabled -> colorScheme.surfaceContainerHigh
        !isSiteProtected -> aegisColors.shieldPausedContainer
        else -> aegisColors.shieldActiveContainer
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = statusContainer,
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = statusColor.copy(alpha = 0.16f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.padding(11.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (currentHost.isNotBlank()) currentHost else stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = if (currentHost.isNotBlank()) JetBrainsMonoFontFamily else MaterialTheme.typography.titleMedium.fontFamily
                            ),
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                !isGlobalAdBlockEnabled -> stringResource(R.string.shield_status_disabled)
                                !isSiteProtected -> stringResource(R.string.shield_status_paused)
                                else -> stringResource(R.string.shield_status_active)
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = statusColor
                        )
                    }

                    Switch(
                        checked = isActive,
                        onCheckedChange = { enable ->
                            if (currentHost.isNotBlank() && isGlobalAdBlockEnabled) {
                                onToggleSiteProtection(currentHost, enable)
                            } else {
                                onToggleGlobalShield(enable)
                            }
                        },
                        modifier = Modifier.testTag("site_shield_toggle"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorScheme.onPrimary,
                            checkedTrackColor = aegisColors.shieldActive
                        )
                    )
                }
            }

            // Metrics Pair
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.shield_sheet_blocked_on_page),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tab.blockedCountOnPage.toString(),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = SpaceGroteskFontFamily
                            ),
                            color = aegisColors.shieldActive
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "累計ブロック数",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCompactCount(totalBlockedCount),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = SpaceGroteskFontFamily
                            ),
                            color = colorScheme.onSurface
                        )
                    }
                }
            }

            // Live Blocked Requests Log for Current Page
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.shield_sheet_blocked_domains),
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurfaceVariant
                )

                if (tab.blockedRecordsOnPage.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = colorScheme.surfaceContainerLow
                    ) {
                        Text(
                            text = stringResource(R.string.shield_sheet_no_blocked),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(tab.blockedRecordsOnPage, key = { "${it.host}_${it.category.name}" }) { record ->
                            val badgeColor = when (record.category) {
                                BlockCategory.AD -> aegisColors.blockedAccent
                                BlockCategory.TRACKER -> aegisColors.trackerAccent
                                BlockCategory.SOCIAL -> aegisColors.privateAccent
                                BlockCategory.POPUP -> aegisColors.shieldPaused
                                BlockCategory.CUSTOM -> aegisColors.shieldActive
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colorScheme.surfaceContainerLow,
                                border = BorderStroke(0.5.dp, colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = badgeColor.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = record.category.labelJa,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(
                                        text = record.host,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "×${record.count}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = SpaceGroteskFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Toggle: Hide this Shield display when searching
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "検索時にこの表示を非表示にする",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "広告ブロック自体は裏で稼働したまま表示だけ隠します",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(
                        checked = shieldDisplayMode != ShieldDisplayMode.ALWAYS_SHOW,
                        onCheckedChange = { hide ->
                            onSelectShieldDisplayMode(
                                if (hide) ShieldDisplayMode.HIDE_ON_SEARCH
                                else ShieldDisplayMode.ALWAYS_SHOW
                            )
                        },
                        modifier = Modifier.testTag("site_shield_hide_on_search_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorScheme.onPrimary,
                            checkedTrackColor = aegisColors.shieldActive
                        )
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenSitePermissions()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("shield_sheet_site_permissions_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = aegisColors.shieldActive,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "サイト別の権限管理（カメラ・マイク・位置情報）",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colorScheme.onSurface
                )
            }

            Button(
                onClick = {
                    onDismiss()
                    onOpenDashboard()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("open_shield_dashboard_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.shield_sheet_open_dashboard),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

fun formatCompactCount(count: Long): String {
    return when {
        count >= 1_000_000L -> String.format("%.1fM", count / 1_000_000.0)
        count >= 10_000L -> String.format("%.1fK", count / 1_000.0)
        else -> "%,d".format(count)
    }
}

fun formatBytesSaved(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824L -> String.format("%.2f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576L -> String.format("%.1f MB", bytes / 1_048_576.0)
        bytes >= 1_024L -> String.format("%.0f KB", bytes / 1_024.0)
        else -> "$bytes B"
    }
}

fun formatTimeSaved(blockedCount: Long): String {
    // Average ~55ms latency saved per blocked ad/tracker request
    val totalSeconds = (blockedCount * 55L) / 1000L
    return when {
        totalSeconds >= 3600L -> "${totalSeconds / 3600L}時間${(totalSeconds % 3600L) / 60L}分"
        totalSeconds >= 60L -> "${totalSeconds / 60L}分${totalSeconds % 60L}秒"
        else -> "${totalSeconds}秒"
    }
}
