package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.adblock.AdBlockEngine
import com.example.data.local.OmniboxShortcut
import com.example.ui.browser.PageTranslationHelper
import com.example.ui.browser.TranslationLanguage
import com.example.ui.theme.AegisTheme
import com.example.ui.theme.JetBrainsMonoFontFamily

fun OmniboxShortcut.icon(
    isBookmarked: Boolean = false,
    isFullscreen: Boolean = false
): ImageVector = when (this) {
    OmniboxShortcut.AI_SOLVER -> Icons.Filled.AutoAwesome
    OmniboxShortcut.EXTENSIONS -> Icons.Filled.Extension
    OmniboxShortcut.FULL_SCREENSHOT -> Icons.Filled.Screenshot
    OmniboxShortcut.FULLSCREEN -> if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen
    OmniboxShortcut.PAGE_ZOOM -> Icons.Filled.ZoomIn
    OmniboxShortcut.TRANSLATE -> Icons.Filled.Translate
    OmniboxShortcut.BOOKMARK -> if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder
    OmniboxShortcut.FIND_IN_PAGE -> Icons.Filled.FindInPage
    OmniboxShortcut.DESKTOP_MODE -> Icons.Filled.DesktopWindows
    OmniboxShortcut.SHARE -> Icons.Filled.Share
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniboxShortcutPickerSheet(
    selectedShortcuts: List<OmniboxShortcut>,
    onToggleShortcut: (OmniboxShortcut) -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.shortcut_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = aegisColors.shieldActiveContainer,
                    border = BorderStroke(1.dp, aegisColors.shieldActive.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "${selectedShortcuts.size}個選択中",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = aegisColors.shieldActive,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.shortcut_picker_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OmniboxShortcut.entries.forEach { shortcut ->
                    val selected = shortcut in selectedShortcuts
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onToggleShortcut(shortcut) }
                            .testTag("shortcut_picker_item_${shortcut.name}"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) aegisColors.shieldActiveContainer else colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) aegisColors.shieldActive else colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) {
                                    aegisColors.shieldActive.copy(alpha = 0.16f)
                                } else {
                                    colorScheme.surfaceContainerHigh
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = shortcut.icon(),
                                        contentDescription = null,
                                        tint = if (selected) aegisColors.shieldActive else colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = shortcut.displayName,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                                    ),
                                    color = if (selected) aegisColors.shieldActive else colorScheme.onSurface
                                )
                                Text(
                                    text = shortcut.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }

                            Checkbox(
                                checked = selected,
                                onCheckedChange = { onToggleShortcut(shortcut) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = aegisColors.shieldActive,
                                    checkmarkColor = colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("shortcut_picker_checkbox_${shortcut.name}")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("shortcut_picker_done_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = aegisColors.shieldActive,
                    contentColor = colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "完了",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatePageBottomSheet(
    currentUrl: String,
    onSelectLanguage: (TranslationLanguage) -> Unit,
    onRestoreOriginal: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors
    val isTranslated = PageTranslationHelper.isTranslatedUrl(currentUrl)
    val displayHost = AdBlockEngine.extractHost(
        PageTranslationHelper.extractOriginalUrl(currentUrl) ?: currentUrl
    ).ifBlank { currentUrl }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = aegisColors.shieldActiveContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Translate,
                            contentDescription = null,
                            tint = aegisColors.shieldActive,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.translate_sheet_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = displayHost,
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = stringResource(R.string.translate_sheet_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )

            if (isTranslated) {
                OutlinedButton(
                    onClick = {
                        onRestoreOriginal()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("translate_restore_original_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, aegisColors.shieldActive)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = null,
                        tint = aegisColors.shieldActive,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.translate_restore_original),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = aegisColors.shieldActive
                    )
                }
            }

            val rows = PageTranslationHelper.supportedLanguages.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { lang ->
                            val isJapanese = lang.code == "ja"
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        onSelectLanguage(lang)
                                        onDismiss()
                                    }
                                    .testTag("translate_lang_${lang.code}"),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isJapanese) aegisColors.shieldActiveContainer else colorScheme.surfaceContainerLow,
                                border = BorderStroke(
                                    width = if (isJapanese) 1.5.dp else 1.dp,
                                    color = if (isJapanese) aegisColors.shieldActive else colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Language,
                                        contentDescription = null,
                                        tint = if (isJapanese) aegisColors.shieldActive else colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = lang.label,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = if (isJapanese) FontWeight.Bold else FontWeight.SemiBold
                                            ),
                                            color = if (isJapanese) aegisColors.shieldActive else colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = lang.nativeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
