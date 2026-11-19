package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.BrowserMenuItem
import com.example.data.local.OmniboxShortcut
import com.example.ui.browser.BrowserTab
import com.example.ui.browser.TabGroup
import com.example.ui.theme.AegisTheme

fun BrowserMenuItem.icon(isBookmarked: Boolean = false): ImageVector = when (this) {
    BrowserMenuItem.NEW_TAB -> Icons.Filled.Add
    BrowserMenuItem.NEW_PRIVATE_TAB -> Icons.Outlined.PrivacyTip
    BrowserMenuItem.ADD_TO_GROUP -> Icons.Outlined.DriveFileMove
    BrowserMenuItem.HISTORY -> Icons.Filled.History
    BrowserMenuItem.BOOKMARKS -> Icons.Filled.Bookmarks
    BrowserMenuItem.AI_PAGE_SOLVER -> Icons.Filled.AutoAwesome
    BrowserMenuItem.EXTENSIONS -> Icons.Filled.Extension
    BrowserMenuItem.TRANSLATE_PAGE -> Icons.Filled.Translate
    BrowserMenuItem.FULL_SCREENSHOT -> Icons.Filled.Screenshot
    BrowserMenuItem.FULLSCREEN -> Icons.Filled.Fullscreen
    BrowserMenuItem.PAGE_ZOOM -> Icons.Filled.ZoomIn
    BrowserMenuItem.FIND_IN_PAGE -> Icons.Filled.FindInPage
    BrowserMenuItem.DESKTOP_MODE -> Icons.Filled.DesktopWindows
    BrowserMenuItem.TOGGLE_BOOKMARK -> if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder
    BrowserMenuItem.SHIELD_DASHBOARD -> Icons.Filled.Star
    BrowserMenuItem.SHORTCUT_PICKER -> Icons.Filled.Tune
    BrowserMenuItem.SITE_PERMISSIONS -> Icons.Filled.Security
    BrowserMenuItem.SETTINGS -> Icons.Filled.Settings
}

fun BrowserMenuItem.displayLabel(
    activeGroup: TabGroup?,
    isBookmarked: Boolean,
    selectedShortcuts: List<OmniboxShortcut>
): String = when (this) {
    BrowserMenuItem.ADD_TO_GROUP -> if (activeGroup != null) {
        "グループ: ${activeGroup.name}"
    } else {
        displayName
    }
    BrowserMenuItem.TOGGLE_BOOKMARK -> if (isBookmarked) {
        "ブックマークを解除"
    } else {
        "ブックマークに追加"
    }
    BrowserMenuItem.SHORTCUT_PICKER -> "右上ショートカット（${selectedShortcuts.size}個）"
    else -> displayName
}

data class BrowserMenuCallbacks(
    val onDismiss: () -> Unit,
    val onNewTab: () -> Unit,
    val onNewPrivateTab: () -> Unit,
    val onAssignCurrentTabToGroup: () -> Unit,
    val onAnalyzePageWithAi: () -> Unit,
    val onOpenExtensions: () -> Unit,
    val onCaptureFullScreenshot: () -> Unit,
    val onEnterFullscreen: () -> Unit,
    val onOpenZoomBar: () -> Unit,
    val onZoomOut: () -> Unit,
    val onZoomIn: () -> Unit,
    val onTranslatePage: () -> Unit,
    val onToggleBookmark: () -> Unit,
    val onOpenShortcutPicker: () -> Unit,
    val onOpenShieldDashboard: () -> Unit,
    val onOpenSitePermissions: () -> Unit,
    val onOpenBookmarks: () -> Unit,
    val onOpenHistory: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onCustomizeMenu: () -> Unit,
    val onToggleDesktopMode: () -> Unit,
    val onFindInPage: () -> Unit,
    val onShareUrl: () -> Unit,
    val onGoForward: () -> Unit,
    val onReload: () -> Unit
)

fun executeBrowserMenuItem(
    item: BrowserMenuItem,
    callbacks: BrowserMenuCallbacks
) {
    when (item) {
        BrowserMenuItem.NEW_TAB -> {
            callbacks.onDismiss()
            callbacks.onNewTab()
        }
        BrowserMenuItem.NEW_PRIVATE_TAB -> {
            callbacks.onDismiss()
            callbacks.onNewPrivateTab()
        }
        BrowserMenuItem.ADD_TO_GROUP -> {
            callbacks.onDismiss()
            callbacks.onAssignCurrentTabToGroup()
        }
        BrowserMenuItem.HISTORY -> {
            callbacks.onDismiss()
            callbacks.onOpenHistory()
        }
        BrowserMenuItem.BOOKMARKS -> {
            callbacks.onDismiss()
            callbacks.onOpenBookmarks()
        }
        BrowserMenuItem.AI_PAGE_SOLVER -> {
            callbacks.onDismiss()
            callbacks.onAnalyzePageWithAi()
        }
        BrowserMenuItem.EXTENSIONS -> {
            callbacks.onDismiss()
            callbacks.onOpenExtensions()
        }
        BrowserMenuItem.TRANSLATE_PAGE -> {
            callbacks.onDismiss()
            callbacks.onTranslatePage()
        }
        BrowserMenuItem.FULL_SCREENSHOT -> {
            callbacks.onDismiss()
            callbacks.onCaptureFullScreenshot()
        }
        BrowserMenuItem.FULLSCREEN -> {
            callbacks.onDismiss()
            callbacks.onEnterFullscreen()
        }
        BrowserMenuItem.PAGE_ZOOM -> {
            callbacks.onDismiss()
            callbacks.onOpenZoomBar()
        }
        BrowserMenuItem.FIND_IN_PAGE -> {
            callbacks.onDismiss()
            callbacks.onFindInPage()
        }
        BrowserMenuItem.DESKTOP_MODE -> {
            callbacks.onToggleDesktopMode()
        }
        BrowserMenuItem.TOGGLE_BOOKMARK -> {
            callbacks.onDismiss()
            callbacks.onToggleBookmark()
        }
        BrowserMenuItem.SHIELD_DASHBOARD -> {
            callbacks.onDismiss()
            callbacks.onOpenShieldDashboard()
        }
        BrowserMenuItem.SHORTCUT_PICKER -> {
            callbacks.onDismiss()
            callbacks.onOpenShortcutPicker()
        }
        BrowserMenuItem.SITE_PERMISSIONS -> {
            callbacks.onDismiss()
            callbacks.onOpenSitePermissions()
        }
        BrowserMenuItem.SETTINGS -> {
            callbacks.onDismiss()
            callbacks.onOpenSettings()
        }
    }
}

/**
 * Compact vertical floating popup menu modeled after the classic vertical list layout,
 * with user-customizable item order and a bottom circular quick-action bar.
 */
@Composable
fun BrowserPopupListMenu(
    tab: BrowserTab,
    activeGroup: TabGroup?,
    isCurrentUrlBookmarked: Boolean,
    selectedShortcuts: List<OmniboxShortcut>,
    menuItems: List<BrowserMenuItem>,
    callbacks: BrowserMenuCallbacks
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors
    val effectiveItems = menuItems.ifEmpty { BrowserMenuItem.DEFAULT_ORDER }

    Dialog(
        onDismissRequest = callbacks.onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = callbacks.onDismiss
                )
                .padding(end = 12.dp, bottom = 12.dp, top = 24.dp, start = 48.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 256.dp, max = 308.dp)
                    .heightIn(max = 640.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .testTag("browser_popup_list_menu"),
                shape = RoundedCornerShape(24.dp),
                color = colorScheme.surfaceContainerHigh,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Scrollable vertical menu items
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp)
                    ) {
                        effectiveItems.forEachIndexed { index, item ->
                            val enabled = !item.requiresWebPage || !tab.isStartPage
                            val prevItem = effectiveItems.getOrNull(index - 1)
                            val showSectionDivider = prevItem != null &&
                                sectionOf(prevItem) != sectionOf(item)

                            if (showSectionDivider) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                                    thickness = 0.7.dp,
                                    color = colorScheme.outlineVariant
                                )
                            }

                            if (item == BrowserMenuItem.PAGE_ZOOM) {
                                PopupZoomMenuRow(
                                    zoomPercent = tab.zoomPercent,
                                    enabled = enabled,
                                    onOpenZoomBar = {
                                        callbacks.onDismiss()
                                        callbacks.onOpenZoomBar()
                                    },
                                    onZoomOut = callbacks.onZoomOut,
                                    onZoomIn = callbacks.onZoomIn
                                )
                            } else {
                                PopupMenuItemRow(
                                    icon = item.icon(isBookmarked = isCurrentUrlBookmarked),
                                    label = item.displayLabel(
                                        activeGroup = activeGroup,
                                        isBookmarked = isCurrentUrlBookmarked,
                                        selectedShortcuts = selectedShortcuts
                                    ),
                                    enabled = enabled,
                                    accentTint = when (item) {
                                        BrowserMenuItem.AI_PAGE_SOLVER,
                                        BrowserMenuItem.EXTENSIONS,
                                        BrowserMenuItem.SHIELD_DASHBOARD -> aegisColors.shieldActive
                                        BrowserMenuItem.NEW_PRIVATE_TAB -> aegisColors.privateAccent
                                        else -> null
                                    },
                                    checkedState = if (item == BrowserMenuItem.DESKTOP_MODE) tab.isDesktopMode else null,
                                    onClick = { executeBrowserMenuItem(item, callbacks) },
                                    testTag = "popup_menu_item_${item.name}"
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                            thickness = 0.7.dp,
                            color = colorScheme.outlineVariant
                        )

                        PopupMenuItemRow(
                            icon = Icons.Filled.Build,
                            label = stringResource(R.string.menu_customize),
                            enabled = true,
                            accentTint = colorScheme.primary,
                            checkedState = null,
                            onClick = {
                                callbacks.onDismiss()
                                callbacks.onCustomizeMenu()
                            },
                            testTag = "popup_menu_customize_button"
                        )
                    }

                    // Bottom Circular Action Row (Forward / Share / Full Screenshot / Reload)
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PopupBottomCircleAction(
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.nav_forward),
                            enabled = !tab.isStartPage && tab.canGoForward,
                            onClick = {
                                callbacks.onDismiss()
                                callbacks.onGoForward()
                            },
                            testTag = "popup_action_forward"
                        )
                        PopupBottomCircleAction(
                            icon = Icons.Filled.Share,
                            contentDescription = stringResource(R.string.action_share),
                            enabled = !tab.isStartPage,
                            onClick = {
                                callbacks.onDismiss()
                                callbacks.onShareUrl()
                            },
                            testTag = "popup_action_share"
                        )
                        PopupBottomCircleAction(
                            icon = Icons.Filled.Screenshot,
                            contentDescription = stringResource(R.string.action_full_screenshot_short),
                            enabled = !tab.isStartPage,
                            onClick = {
                                callbacks.onDismiss()
                                callbacks.onCaptureFullScreenshot()
                            },
                            testTag = "popup_action_screenshot"
                        )
                        PopupBottomCircleAction(
                            icon = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.action_reload),
                            enabled = !tab.isStartPage,
                            onClick = {
                                callbacks.onDismiss()
                                callbacks.onReload()
                            },
                            testTag = "popup_action_reload"
                        )
                    }
                }
            }
        }
    }
}

private fun sectionOf(item: BrowserMenuItem): Int = when (item) {
    BrowserMenuItem.NEW_TAB,
    BrowserMenuItem.NEW_PRIVATE_TAB,
    BrowserMenuItem.ADD_TO_GROUP -> 0
    BrowserMenuItem.SETTINGS,
    BrowserMenuItem.SHIELD_DASHBOARD,
    BrowserMenuItem.SHORTCUT_PICKER,
    BrowserMenuItem.SITE_PERMISSIONS -> 2
    else -> 1
}

@Composable
private fun PopupMenuItemRow(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    accentTint: androidx.compose.ui.graphics.Color?,
    checkedState: Boolean?,
    onClick: () -> Unit,
    testTag: String
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors
    val contentAlpha = if (enabled) 1f else 0.4f
    val resolvedIconTint = (accentTint ?: colorScheme.onSurfaceVariant).copy(alpha = contentAlpha)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = resolvedIconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = colorScheme.onSurface.copy(alpha = contentAlpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (checkedState != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Checkbox(
                checked = checkedState,
                onCheckedChange = if (enabled) { { onClick() } } else null,
                enabled = enabled,
                colors = CheckboxDefaults.colors(
                    checkedColor = aegisColors.shieldActive,
                    checkmarkColor = colorScheme.onPrimary
                ),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun PopupBottomCircleAction(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        shape = CircleShape,
        color = if (enabled) {
            colorScheme.surfaceContainerHighest
        } else {
            colorScheme.surfaceContainer
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) {
                    colorScheme.onSurface
                } else {
                    colorScheme.onSurface.copy(alpha = 0.32f)
                },
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
internal fun PopupZoomMenuRow(
    zoomPercent: Int,
    enabled: Boolean,
    onOpenZoomBar: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors
    val contentAlpha = if (enabled) 1f else 0.4f
    val isZoomed = zoomPercent != com.example.ui.browser.WebViewRegistry.DEFAULT_ZOOM_PERCENT

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(enabled = enabled, onClick = onOpenZoomBar)
            .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
            .testTag("popup_menu_item_PAGE_ZOOM"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.ZoomIn,
            contentDescription = null,
            tint = (if (isZoomed) aegisColors.shieldActive else colorScheme.onSurfaceVariant).copy(alpha = contentAlpha),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = stringResource(R.string.action_page_zoom),
            style = MaterialTheme.typography.bodyLarge,
            color = colorScheme.onSurface.copy(alpha = contentAlpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(
                        enabled = enabled && zoomPercent > com.example.ui.browser.WebViewRegistry.MIN_ZOOM_PERCENT,
                        onClick = onZoomOut
                    )
                    .testTag("menu_zoom_out_button"),
                shape = CircleShape,
                color = colorScheme.surfaceContainerHighest.copy(alpha = contentAlpha)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = stringResource(R.string.zoom_out),
                        tint = colorScheme.onSurface.copy(alpha = contentAlpha),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = enabled, onClick = onOpenZoomBar)
                    .testTag("menu_zoom_percent_chip"),
                shape = RoundedCornerShape(8.dp),
                color = if (isZoomed) aegisColors.shieldActiveContainer else colorScheme.surfaceContainer,
                border = BorderStroke(
                    0.8.dp,
                    if (isZoomed) aegisColors.shieldActive.copy(alpha = 0.5f) else colorScheme.outlineVariant
                )
            ) {
                Text(
                    text = "${zoomPercent}%",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = com.example.ui.theme.SpaceGroteskFontFamily,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    ),
                    color = (if (isZoomed) aegisColors.shieldActive else colorScheme.onSurface).copy(alpha = contentAlpha),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                )
            }
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(
                        enabled = enabled && zoomPercent < com.example.ui.browser.WebViewRegistry.MAX_ZOOM_PERCENT,
                        onClick = onZoomIn
                    )
                    .testTag("menu_zoom_in_button"),
                shape = CircleShape,
                color = colorScheme.surfaceContainerHighest.copy(alpha = contentAlpha)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.zoom_in),
                        tint = colorScheme.onSurface.copy(alpha = contentAlpha),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
