package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.RemoveModerator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.adblock.AdBlockEngine
import com.example.data.local.BookmarkEntity
import com.example.data.local.BrowserPreferences
import com.example.ui.components.formatBytesSaved
import com.example.ui.components.formatCompactCount
import com.example.ui.components.formatTimeSaved
import com.example.ui.components.rememberHomeBackgroundImage
import com.example.ui.theme.AegisTheme
import com.example.ui.theme.CyanInfoDark
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Obsidian950
import com.example.ui.theme.PrivatePurpleDark
import com.example.ui.theme.SpaceGroteskFontFamily

@Composable
fun StartPageScreen(
    isPrivate: Boolean,
    preferences: BrowserPreferences,
    bookmarks: List<BookmarkEntity>,
    onOpenUrl: (String) -> Unit,
    onOpenShieldDashboard: () -> Unit,
    onOpenBackgroundPicker: () -> Unit,
    onAddBookmark: (String, String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors
    var showAddDialog by remember { mutableStateOf(false) }
    val customBgBitmap = rememberHomeBackgroundImage(path = preferences.homeBackgroundPath)
    val hasCustomBg = customBgBitmap != null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        if (customBgBitmap != null) {
            Image(
                bitmap = customBgBitmap,
                contentDescription = stringResource(R.string.cd_home_background_image),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("start_custom_background_image")
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = preferences.homeBackgroundDimAlpha.coerceIn(0f, 0.75f)))
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 76.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Private Mode Banner (when in private tab)
            if (isPrivate) {
                item(key = "private_banner") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = aegisColors.privateContainer,
                        border = BorderStroke(1.dp, aegisColors.privateAccent.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PrivacyTip,
                                contentDescription = null,
                                tint = aegisColors.privateAccent,
                                modifier = Modifier.size(26.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "プライベートブラウジング有効",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "このタブでの閲覧履歴・キャッシュ・DOMストレージは端末に保存されません。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Editorial Shield Hero Stats Card
            item(key = "shield_hero_card") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onOpenShieldDashboard)
                        .testTag("start_shield_hero_card"),
                    shape = RoundedCornerShape(24.dp),
                    color = Obsidian950,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (preferences.adBlockEnabled) {
                            Emerald500.copy(alpha = 0.35f)
                        } else {
                            colorScheme.outline
                        }
                    )
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_shield_hero_1795105295881),
                            contentDescription = stringResource(R.string.cd_hero_banner),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Obsidian950.copy(alpha = 0.92f),
                                            Obsidian950.copy(alpha = 0.72f),
                                            Obsidian950.copy(alpha = 0.35f)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (preferences.adBlockEnabled) {
                                        Emerald500.copy(alpha = 0.18f)
                                    } else {
                                        Color.White.copy(alpha = 0.12f)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (preferences.adBlockEnabled) Emerald500.copy(alpha = 0.5f)
                                        else Color.White.copy(alpha = 0.25f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (preferences.adBlockEnabled) Icons.Filled.Shield else Icons.Outlined.RemoveModerator,
                                            contentDescription = null,
                                            tint = if (preferences.adBlockEnabled) Emerald500 else Color(0xFF94A3B8),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = if (preferences.adBlockEnabled) {
                                                stringResource(R.string.shield_status_active)
                                            } else {
                                                stringResource(R.string.shield_status_disabled)
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (preferences.adBlockEnabled) Emerald500 else Color(0xFFCBD5E1)
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = stringResource(R.string.home_stats_title),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = formatCompactCount(preferences.totalBlockedCount),
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontFamily = SpaceGroteskFontFamily
                                    ),
                                    color = Color.White
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HeroStatChip(
                                    label = stringResource(R.string.home_stats_blocked),
                                    value = "${formatCompactCount(preferences.totalBlockedCount)} 件",
                                    accent = Emerald500,
                                    modifier = Modifier.weight(1f)
                                )
                                HeroStatChip(
                                    label = stringResource(R.string.home_stats_data_saved),
                                    value = formatBytesSaved(preferences.totalBytesSaved),
                                    accent = CyanInfoDark,
                                    modifier = Modifier.weight(1f)
                                )
                                HeroStatChip(
                                    label = stringResource(R.string.home_stats_time_saved),
                                    value = formatTimeSaved(preferences.totalBlockedCount),
                                    accent = PrivatePurpleDark,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Access Bookmarks Grid
            item(key = "quick_access_section") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (hasCustomBg) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color.Black.copy(alpha = 0.45f),
                                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = stringResource(R.string.home_quick_access),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { showAddDialog = true }
                                    .testTag("start_add_bookmark_button"),
                                shape = RoundedCornerShape(999.dp),
                                color = Color.Black.copy(alpha = 0.45f),
                                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.22f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.home_add_bookmark),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.home_quick_access),
                                style = MaterialTheme.typography.titleMedium,
                                color = colorScheme.onBackground
                            )
                            TextButton(
                                onClick = { showAddDialog = true },
                                modifier = Modifier.testTag("start_add_bookmark_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = stringResource(R.string.home_add_bookmark))
                            }
                        }
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val columns = when {
                            maxWidth >= 560.dp -> 5
                            maxWidth >= 380.dp -> 4
                            else -> 3
                        }
                        val rows = bookmarks.chunked(columns)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            rows.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { bookmark ->
                                        SpeedDialCard(
                                            bookmark = bookmark,
                                            translucentOverWallpaper = hasCustomBg,
                                            onClick = { onOpenUrl(bookmark.url) },
                                            onDelete = { onDeleteBookmark(bookmark.id) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(columns - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom-right quick Wallpaper / Background customization pill
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onOpenBackgroundPicker)
                .testTag("start_change_background_button"),
            shape = RoundedCornerShape(24.dp),
            color = if (hasCustomBg) {
                Color.Black.copy(alpha = 0.62f)
            } else {
                colorScheme.surfaceContainerHigh.copy(alpha = 0.94f)
            },
            border = BorderStroke(
                width = 1.dp,
                color = if (hasCustomBg) {
                    Color.White.copy(alpha = 0.28f)
                } else {
                    aegisColors.shieldActive.copy(alpha = 0.4f)
                }
            ),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Wallpaper,
                    contentDescription = stringResource(R.string.home_bg_change_button),
                    tint = if (hasCustomBg) Color.White else aegisColors.shieldActive,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.home_bg_change_button),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = if (hasCustomBg) Color.White else colorScheme.onSurface
                )
            }
        }
    }

    if (showAddDialog) {
        AddBookmarkDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, url ->
                onAddBookmark(title, url)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun HeroStatChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.07f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SpeedDialCard(
    bookmark: BookmarkEntity,
    translucentOverWallpaper: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors
    val palette = listOf(
        aegisColors.shieldActive,
        aegisColors.trackerAccent,
        aegisColors.privateAccent,
        aegisColors.shieldPaused,
        aegisColors.blockedAccent,
        colorScheme.primary
    )
    val badgeColor = palette[bookmark.accentIndex.coerceIn(0, palette.lastIndex)]
    val monogram = bookmark.title.trim().firstOrNull()?.uppercaseChar()?.toString()
        ?: AdBlockEngine.extractHost(bookmark.url).firstOrNull()?.uppercaseChar()?.toString()
        ?: "W"

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("speed_dial_item_${bookmark.id}"),
        shape = RoundedCornerShape(16.dp),
        color = if (translucentOverWallpaper) colorScheme.surface.copy(alpha = 0.84f) else colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (translucentOverWallpaper) Color.White.copy(alpha = 0.24f) else colorScheme.outlineVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = badgeColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = monogram,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = badgeColor
                        )
                    }
                }
                Text(
                    text = bookmark.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.cd_delete_item),
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
fun AddBookmarkDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, url: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("クイックアクセスに追加") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("サイト名") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_bookmark_title_input")
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL（例: example.com）") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_bookmark_url_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, url) },
                enabled = url.isNotBlank(),
                modifier = Modifier.testTag("dialog_bookmark_confirm_button")
            ) {
                Text("追加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
