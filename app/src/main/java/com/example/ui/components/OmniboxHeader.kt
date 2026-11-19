package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.adblock.AdBlockEngine
import com.example.data.local.OmniboxShortcut
import com.example.ui.browser.BrowserTab
import com.example.ui.browser.FindInPageState
import com.example.ui.theme.AegisTheme
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OmniboxHeader(
    tab: BrowserTab,
    isAdBlockGlobalEnabled: Boolean,
    isSiteProtected: Boolean,
    showShieldButton: Boolean,
    isBookmarked: Boolean,
    isFullscreen: Boolean,
    omniboxShortcuts: List<OmniboxShortcut>,
    findInPage: FindInPageState,
    urlFocusRequester: FocusRequester,
    onSubmitUrl: (String) -> Unit,
    onShieldClick: () -> Unit,
    onReloadOrStopClick: () -> Unit,
    onShortcutAction: (OmniboxShortcut) -> Unit,
    onOpenShortcutPicker: () -> Unit,
    onFindQueryChange: (String) -> Unit,
    onFindNext: () -> Unit,
    onFindPrevious: () -> Unit,
    onCloseFind: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aegisColors = AegisTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var isFocused by remember { mutableStateOf(false) }
    var showQuickMenu by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf(TextFieldValue(tab.url)) }

    LaunchedEffect(tab.id, tab.url, isFocused) {
        if (!isFocused) {
            val display = if (tab.url.isBlank()) "" else AdBlockEngine.extractHost(tab.url).ifBlank { tab.url }
            textValue = TextFieldValue(display)
        }
    }

    val isHttps = tab.url.startsWith("https://", ignoreCase = true)
    val shieldTint = when {
        !isAdBlockGlobalEnabled -> colorScheme.onSurfaceVariant
        !isSiteProtected -> aegisColors.shieldPaused
        else -> aegisColors.shieldActive
    }
    val shieldContainer = when {
        !isAdBlockGlobalEnabled -> colorScheme.surfaceContainerHigh
        !isSiteProtected -> aegisColors.shieldPausedContainer
        else -> aegisColors.shieldActiveContainer
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (tab.progress.coerceIn(0, 100)) / 100f,
        label = "page_load_progress"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (tab.isPrivate) aegisColors.privateContainer.copy(alpha = 0.55f) else colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Shield Pill Button with live blocked counter (hidden when user disables it on search/browsing)
                if (showShieldButton) {
                    Surface(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onShieldClick)
                            .testTag("omnibox_shield_button"),
                        shape = RoundedCornerShape(14.dp),
                        color = shieldContainer,
                        border = BorderStroke(1.dp, shieldTint.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isAdBlockGlobalEnabled && isSiteProtected) {
                                    Icons.Filled.Star
                                } else {
                                    Icons.Outlined.StarBorder
                                },
                                contentDescription = stringResource(R.string.cd_shield_icon),
                                tint = shieldTint,
                                modifier = Modifier.size(18.dp)
                            )
                            if (tab.blockedCountOnPage > 0) {
                                Text(
                                    text = if (tab.blockedCountOnPage > 99) "99+" else tab.blockedCountOnPage.toString(),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = shieldTint
                                )
                            }
                        }
                    }
                }

                // Editable URL / Search Pill
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = aegisColors.urlBarSurface,
                    border = BorderStroke(
                        width = if (isFocused) 1.5.dp else 1.dp,
                        color = when {
                            isFocused && tab.isPrivate -> aegisColors.privateAccent
                            isFocused -> colorScheme.primary
                            tab.isPrivate -> aegisColors.privateAccent.copy(alpha = 0.45f)
                            else -> aegisColors.cardBorder.copy(alpha = 0.6f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                tab.isPrivate -> Icons.Outlined.PrivacyTip
                                tab.isStartPage -> Icons.Filled.Search
                                isHttps -> Icons.Filled.Lock
                                else -> Icons.Outlined.Info
                            },
                            contentDescription = when {
                                isHttps -> stringResource(R.string.cd_secure_lock)
                                else -> stringResource(R.string.cd_insecure_info)
                            },
                            tint = when {
                                tab.isPrivate -> aegisColors.privateAccent
                                isHttps -> aegisColors.shieldActive
                                else -> colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (textValue.text.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.url_bar_placeholder),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            BasicTextField(
                                value = textValue,
                                onValueChange = { textValue = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(urlFocusRequester)
                                    .onFocusChanged { state ->
                                        isFocused = state.isFocused
                                        if (state.isFocused) {
                                            val full = tab.url
                                            textValue = TextFieldValue(
                                                text = full,
                                                selection = TextRange(0, full.length)
                                            )
                                        }
                                    }
                                    .testTag("omnibox_url_input"),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = colorScheme.onSurface,
                                    fontFamily = if (!tab.isStartPage && !isFocused) {
                                        JetBrainsMonoFontFamily
                                    } else {
                                        MaterialTheme.typography.bodyMedium.fontFamily
                                    }
                                ),
                                cursorBrush = SolidColor(colorScheme.primary),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Go
                                ),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        val query = textValue.text.trim()
                                        if (query.isNotEmpty()) {
                                            onSubmitUrl(query)
                                        }
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    }
                                )
                            )
                        }

                        if (isFocused && textValue.text.isNotEmpty()) {
                            IconButton(
                                onClick = { textValue = TextFieldValue("") },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("omnibox_clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.cd_clear_input),
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else if (!isFocused) {
                            val activeShortcuts = omniboxShortcuts.ifEmpty { OmniboxShortcut.DEFAULT_SELECTION }
                            val isMultiple = activeShortcuts.size > 1
                            val singleShortcut = activeShortcuts.first()
                            val shortcutTint = if (isMultiple) {
                                aegisColors.shieldActive
                            } else {
                                when (singleShortcut) {
                                    OmniboxShortcut.AI_SOLVER ->
                                        if (!tab.isStartPage) aegisColors.shieldActive else colorScheme.onSurfaceVariant
                                    OmniboxShortcut.EXTENSIONS -> aegisColors.shieldActive
                                    OmniboxShortcut.FULL_SCREENSHOT ->
                                        if (!tab.isStartPage) aegisColors.shieldActive else colorScheme.onSurfaceVariant
                                    OmniboxShortcut.FULLSCREEN -> aegisColors.shieldActive
                                    OmniboxShortcut.PAGE_ZOOM ->
                                        if (!tab.isStartPage && tab.zoomPercent != 100) aegisColors.shieldActive else colorScheme.onSurfaceVariant
                                    OmniboxShortcut.TRANSLATE ->
                                        if (!tab.isStartPage) aegisColors.shieldActive else colorScheme.onSurfaceVariant
                                    OmniboxShortcut.BOOKMARK ->
                                        if (isBookmarked) aegisColors.shieldActive else colorScheme.onSurfaceVariant
                                    OmniboxShortcut.FIND_IN_PAGE ->
                                        if (findInPage.isActive) aegisColors.shieldActive else colorScheme.onSurfaceVariant
                                    OmniboxShortcut.DESKTOP_MODE ->
                                        if (tab.isDesktopMode) aegisColors.shieldActive else colorScheme.onSurfaceVariant
                                    OmniboxShortcut.SHARE -> colorScheme.onSurfaceVariant
                                }
                            }

                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .combinedClickable(
                                            onClick = {
                                                if (isMultiple) {
                                                    showQuickMenu = true
                                                } else {
                                                    onShortcutAction(singleShortcut)
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onOpenShortcutPicker()
                                            }
                                        )
                                        .testTag("omnibox_shortcut_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isMultiple) {
                                            Icons.Filled.Bolt
                                        } else {
                                            singleShortcut.icon(
                                                isBookmarked = isBookmarked,
                                                isFullscreen = isFullscreen
                                            )
                                        },
                                        contentDescription = if (isMultiple) {
                                            "ショートカット（${activeShortcuts.size}個）"
                                        } else {
                                            singleShortcut.displayName
                                        },
                                        tint = shortcutTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    if (isMultiple) {
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(top = 2.dp, end = 2.dp),
                                            shape = CircleShape,
                                            color = aegisColors.shieldActive
                                        ) {
                                            Text(
                                                text = activeShortcuts.size.toString(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = SpaceGroteskFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    lineHeight = 9.sp
                                                ),
                                                color = colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 3.5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                DropdownMenu(
                                    expanded = showQuickMenu,
                                    onDismissRequest = { showQuickMenu = false },
                                    containerColor = colorScheme.surface
                                ) {
                                    activeShortcuts.forEach { action ->
                                        val isActionEnabled = action == OmniboxShortcut.FULLSCREEN ||
                                            action == OmniboxShortcut.EXTENSIONS ||
                                            !tab.isStartPage
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = when (action) {
                                                        OmniboxShortcut.BOOKMARK -> if (isBookmarked) {
                                                            stringResource(R.string.action_bookmark_remove)
                                                        } else {
                                                            stringResource(R.string.action_bookmark_add)
                                                        }
                                                        OmniboxShortcut.FULLSCREEN -> if (isFullscreen) {
                                                            stringResource(R.string.action_fullscreen_exit)
                                                        } else {
                                                            stringResource(R.string.action_fullscreen_enter)
                                                        }
                                                        OmniboxShortcut.PAGE_ZOOM -> "画面ズーム（${tab.zoomPercent}%）"
                                                        else -> action.displayName
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = action.icon(
                                                        isBookmarked = isBookmarked,
                                                        isFullscreen = isFullscreen
                                                    ),
                                                    contentDescription = null,
                                                    tint = if (isActionEnabled) aegisColors.shieldActive else colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            enabled = isActionEnabled,
                                            onClick = {
                                                showQuickMenu = false
                                                onShortcutAction(action)
                                            },
                                            modifier = Modifier.testTag("quick_menu_item_${action.name}")
                                        )
                                    }
                                    HorizontalDivider(color = colorScheme.outlineVariant)
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.shortcut_change_action),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colorScheme.primary
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Tune,
                                                contentDescription = null,
                                                tint = colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        onClick = {
                                            showQuickMenu = false
                                            onOpenShortcutPicker()
                                        },
                                        modifier = Modifier.testTag("quick_menu_change_shortcut")
                                    )
                                }
                            }
                        }
                    }
                }

                // Reload / Stop Button
                if (!tab.isStartPage) {
                    IconButton(
                        onClick = onReloadOrStopClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .testTag("omnibox_reload_button")
                    ) {
                        Icon(
                            imageVector = if (tab.isLoading) Icons.Filled.Close else Icons.Filled.Refresh,
                            contentDescription = stringResource(
                                if (tab.isLoading) R.string.action_stop else R.string.action_reload
                            ),
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Find-in-Page Bar
            AnimatedVisibility(
                visible = findInPage.isActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.surfaceContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (findInPage.query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.find_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        BasicTextField(
                            value = findInPage.query,
                            onValueChange = onFindQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface),
                            cursorBrush = SolidColor(colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("find_in_page_input")
                        )
                    }
                    if (findInPage.query.isNotEmpty()) {
                        Text(
                            text = "${findInPage.activeMatchIndex}/${findInPage.totalMatches}",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onFindPrevious,
                        modifier = Modifier.testTag("find_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.find_prev)
                        )
                    }
                    IconButton(
                        onClick = onFindNext,
                        modifier = Modifier.testTag("find_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.find_next)
                        )
                    }
                    IconButton(
                        onClick = onCloseFind,
                        modifier = Modifier.testTag("find_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.find_close)
                        )
                    }
                }
            }

            // Loading progress or hairline divider
            if (tab.isLoading) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = if (tab.isPrivate) aegisColors.privateAccent else aegisColors.shieldActive,
                    trackColor = colorScheme.surfaceVariant
                )
            } else {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = colorScheme.outlineVariant
                )
            }
        }
    }
}
