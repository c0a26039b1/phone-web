package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.adblock.AdBlockEngine
import com.example.data.local.BrowserMenuItem
import com.example.data.local.OmniboxShortcut
import com.example.ui.browser.BrowserTab
import com.example.ui.browser.TabGroup
import com.example.ui.theme.AegisTheme
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun BrowserBottomBar(
    tab: BrowserTab,
    tabs: List<BrowserTab>,
    activeGroup: TabGroup?,
    tabsInActiveGroup: List<BrowserTab>,
    onHomeClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onSearchClick: () -> Unit,
    onTabsClick: () -> Unit,
    onMoreClick: () -> Unit,
    onSwitchAdjacentTab: (delta: Int) -> Unit,
    onSelectGroupTab: (String) -> Unit,
    onCloseGroupTab: (String) -> Unit,
    onAddTabInActiveGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors
    val groupAccent = activeGroup?.let { tabGroupAccentColor(it.color) }
    val density = LocalDensity.current
    val viewConfig = LocalViewConfiguration.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val tabCount = tabs.size
    val activeIndex = remember(tabs, tab.id) {
        tabs.indexOfFirst { it.id == tab.id }.coerceAtLeast(0)
    }
    val previousTab = remember(tabs, activeIndex) {
        tabs.getOrNull(activeIndex - 1)
    }
    val nextTab = remember(tabs, activeIndex) {
        tabs.getOrNull(activeIndex + 1)
    }

    val currentPreviousTab by rememberUpdatedState(previousTab)
    val currentNextTab by rememberUpdatedState(nextTab)
    val currentOnSwitchAdjacentTab by rememberUpdatedState(onSwitchAdjacentTab)

    val swipeThresholdPx = with(density) { 48.dp.toPx() }
    val maxVisualDragPx = with(density) { 52.dp.toPx() }
    val visualOffsetX = remember { Animatable(0f) }
    var activeSwipeDeltaPx by remember { mutableFloatStateOf(0f) }
    var isSwipingTabs by remember { mutableStateOf(false) }

    // Swipe right (+X) -> previous tab (index - 1); Swipe left (-X) -> next tab (index + 1)
    val previewTargetTab = when {
        !isSwipingTabs -> null
        activeSwipeDeltaPx > 0f -> previousTab
        activeSwipeDeltaPx < 0f -> nextTab
        else -> null
    }
    val previewTargetIndex = when {
        !isSwipingTabs -> -1
        activeSwipeDeltaPx > 0f && previousTab != null -> activeIndex - 1
        activeSwipeDeltaPx < 0f && nextTab != null -> activeIndex + 1
        else -> -1
    }
    val swipeTriggered = isSwipingTabs &&
        previewTargetTab != null &&
        abs(activeSwipeDeltaPx) >= swipeThresholdPx

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column {
            // Live swipe preview strip showing which adjacent tab will be selected on release
            AnimatedVisibility(
                visible = isSwipingTabs && previewTargetTab != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val target = previewTargetTab
                if (target != null) {
                    val isMovingToPrev = activeSwipeDeltaPx > 0f
                    val targetTitle = when {
                        target.title.isNotBlank() -> target.title
                        target.url.isNotBlank() -> AdBlockEngine.extractHost(target.url).ifBlank { target.url }
                        target.isPrivate -> stringResource(R.string.tabs_private_page)
                        else -> stringResource(R.string.tabs_new_page)
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("bottom_bar_swipe_preview"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (swipeTriggered) {
                            aegisColors.shieldActiveContainer
                        } else {
                            colorScheme.surfaceContainerHigh
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (swipeTriggered) {
                                aegisColors.shieldActive.copy(alpha = 0.65f)
                            } else {
                                colorScheme.outlineVariant
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isMovingToPrev) {
                                    Icons.AutoMirrored.Filled.ArrowBack
                                } else {
                                    Icons.AutoMirrored.Filled.ArrowForward
                                },
                                contentDescription = null,
                                tint = if (swipeTriggered) aegisColors.shieldActive else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = targetTitle,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (swipeTriggered) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (previewTargetIndex >= 0) {
                                Text(
                                    text = "${previewTargetIndex + 1} / $tabCount",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (swipeTriggered) aegisColors.shieldActive else colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (activeGroup != null && tabsInActiveGroup.isNotEmpty()) {
                ActiveTabGroupStrip(
                    group = activeGroup,
                    tabsInGroup = tabsInActiveGroup,
                    activeTabId = tab.id,
                    onSelectTab = onSelectGroupTab,
                    onCloseTab = onCloseGroupTab,
                    onAddTabInGroup = onAddTabInActiveGroup
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant)

            // Subtle swipe handle affordance when 2+ tabs exist
            if (tabCount > 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .pointerInput(tabCount) {
                        val touchSlop = viewConfig.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial
                            )
                            val pointerId = down.id
                            var totalX = 0f
                            var totalY = 0f
                            var swiping = false
                            var hapticFired = false

                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                                if (change.changedToUpIgnoreConsumed()) {
                                    if (swiping) {
                                        change.consume()
                                        val shouldGoPrev = totalX >= swipeThresholdPx && currentPreviousTab != null
                                        val shouldGoNext = totalX <= -swipeThresholdPx && currentNextTab != null
                                        isSwipingTabs = false
                                        activeSwipeDeltaPx = 0f
                                        coroutineScope.launch {
                                            visualOffsetX.animateTo(0f, animationSpec = spring())
                                        }
                                        when {
                                            shouldGoPrev -> currentOnSwitchAdjacentTab(-1)
                                            shouldGoNext -> currentOnSwitchAdjacentTab(+1)
                                        }
                                    }
                                    break
                                }

                                val delta = change.positionChange()
                                totalX += delta.x
                                totalY += delta.y

                                if (!swiping) {
                                    if (abs(totalY) > touchSlop && abs(totalY) > abs(totalX)) {
                                        break
                                    }
                                    if (abs(totalX) > touchSlop && abs(totalX) > abs(totalY) * 1.15f) {
                                        swiping = true
                                        isSwipingTabs = true
                                    }
                                }

                                if (swiping) {
                                    change.consume()
                                    activeSwipeDeltaPx = totalX
                                    val hasTarget = (totalX > 0f && currentPreviousTab != null) ||
                                        (totalX < 0f && currentNextTab != null)
                                    val damping = if (hasTarget) 0.48f else 0.16f
                                    val visual = (totalX * damping).coerceIn(-maxVisualDragPx, maxVisualDragPx)
                                    coroutineScope.launch {
                                        visualOffsetX.snapTo(visual)
                                    }
                                    if (hasTarget && abs(totalX) >= swipeThresholdPx && !hapticFired) {
                                        hapticFired = true
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } else if (abs(totalX) < swipeThresholdPx * 0.85f) {
                                        hapticFired = false
                                    }
                                }
                            }

                            if (swiping) {
                                isSwipingTabs = false
                                activeSwipeDeltaPx = 0f
                                coroutineScope.launch {
                                    visualOffsetX.animateTo(0f, animationSpec = spring())
                                }
                            }
                        }
                    }
                    .graphicsLayer {
                        translationX = visualOffsetX.value
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("browser_bottom_bar_row"),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. ホームページに戻る
                IconButton(
                    onClick = onHomeClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("bottom_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = stringResource(R.string.nav_home),
                        tint = if (tab.isStartPage) aegisColors.shieldActive else colorScheme.onSurface
                    )
                }

                // 2. ブックマークを表示
                IconButton(
                    onClick = onBookmarksClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("bottom_bookmarks_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bookmarks,
                        contentDescription = stringResource(R.string.menu_bookmarks),
                        tint = colorScheme.onSurface
                    )
                }

                // 3. 検索
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("bottom_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.nav_search),
                        tint = aegisColors.shieldActive
                    )
                }

                // 4. タブ表示（ちゃんとした◆ダイヤ形状）
                val tabsContentDesc = stringResource(R.string.nav_tabs)
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .clickable(
                            role = Role.Button,
                            onClick = onTabsClick
                        )
                        .semantics { contentDescription = tabsContentDesc }
                        .testTag("bottom_tabs_button"),
                    contentAlignment = Alignment.Center
                ) {
                    val diamondFillColor = when {
                        groupAccent != null -> groupAccent
                        tab.isPrivate -> aegisColors.privateAccent
                        else -> colorScheme.onSurface
                    }
                    val diamondTextColor = when {
                        groupAccent != null -> if (groupAccent.luminance() > 0.5f) Color.Black else Color.White
                        tab.isPrivate -> if (aegisColors.privateAccent.luminance() > 0.5f) colorScheme.surface else Color.White
                        else -> colorScheme.surface
                    }
                    Box(
                        modifier = Modifier.size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val diamondPath = Path().apply {
                                moveTo(cx, 0f)
                                lineTo(size.width, cy)
                                lineTo(cx, size.height)
                                lineTo(0f, cy)
                                close()
                            }
                            drawPath(path = diamondPath, color = diamondFillColor)
                        }
                        Text(
                            text = if (tabCount > 99) "∞" else tabCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (tabCount in 10..99) 10.sp else 11.sp,
                                lineHeight = 11.sp
                            ),
                            color = diamondTextColor,
                            maxLines = 1
                        )
                    }
                }

                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("bottom_more_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.nav_menu),
                        tint = colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserMoreMenuSheet(
    tab: BrowserTab,
    activeGroup: TabGroup?,
    isCurrentUrlBookmarked: Boolean,
    selectedShortcuts: List<OmniboxShortcut>,
    menuItems: List<BrowserMenuItem>,
    totalBlockedCount: Long,
    callbacks: BrowserMenuCallbacks
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors
    val effectiveItems = menuItems.ifEmpty { BrowserMenuItem.DEFAULT_ORDER }
        .filter { !it.requiresWebPage || !tab.isStartPage }

    ModalBottomSheet(
        onDismissRequest = callbacks.onDismiss,
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
            // Quick Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MenuQuickTile(
                    icon = Icons.Filled.Settings,
                    label = "設定",
                    onClick = {
                        callbacks.onDismiss()
                        callbacks.onOpenSettings()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("menu_quick_settings_button")
                )
                MenuQuickTile(
                    icon = Icons.Outlined.PrivacyTip,
                    label = stringResource(R.string.private_mode_badge),
                    accentColor = aegisColors.privateAccent,
                    onClick = {
                        callbacks.onDismiss()
                        callbacks.onNewPrivateTab()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("menu_private_tab_button")
                )
                MenuQuickTile(
                    icon = Icons.Filled.Screenshot,
                    label = stringResource(R.string.action_full_screenshot_short),
                    accentColor = aegisColors.shieldActive,
                    enabled = !tab.isStartPage,
                    onClick = {
                        callbacks.onDismiss()
                        callbacks.onCaptureFullScreenshot()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("menu_full_screenshot_tile")
                )
                MenuQuickTile(
                    icon = Icons.Filled.Share,
                    label = stringResource(R.string.action_share),
                    enabled = !tab.isStartPage,
                    onClick = {
                        callbacks.onDismiss()
                        callbacks.onShareUrl()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("menu_share_button")
                )
            }

            // Highlighted Shield Dashboard Banner Row
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        callbacks.onDismiss()
                        callbacks.onOpenShieldDashboard()
                    }
                    .testTag("menu_shield_dashboard_button"),
                shape = RoundedCornerShape(16.dp),
                color = aegisColors.shieldActiveContainer,
                border = BorderStroke(1.dp, aegisColors.shieldActive.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = aegisColors.shieldActive,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.menu_shield_dashboard),
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "累計 ${totalBlockedCount} 件の広告・トラッカーを遮断",
                            style = MaterialTheme.typography.bodySmall,
                            color = aegisColors.shieldActive
                        )
                    }
                }
            }

            // Customizable Menu List Items
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Column {
                    effectiveItems.forEachIndexed { idx, item ->
                        if (idx > 0) {
                            HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                        }
                        when (item) {
                            BrowserMenuItem.PAGE_ZOOM -> {
                                PopupZoomMenuRow(
                                    zoomPercent = tab.zoomPercent,
                                    enabled = !tab.isStartPage,
                                    onOpenZoomBar = {
                                        callbacks.onDismiss()
                                        callbacks.onOpenZoomBar()
                                    },
                                    onZoomOut = callbacks.onZoomOut,
                                    onZoomIn = callbacks.onZoomIn
                                )
                            }
                            BrowserMenuItem.DESKTOP_MODE -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = callbacks.onToggleDesktopMode)
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                        .testTag("menu_desktop_mode_toggle"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = item.icon(),
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = stringResource(R.string.action_desktop_site),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = tab.isDesktopMode,
                                        onCheckedChange = { callbacks.onToggleDesktopMode() },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = colorScheme.onPrimary,
                                            checkedTrackColor = aegisColors.shieldActive
                                        )
                                    )
                                }
                            }
                            else -> {
                                MenuListRow(
                                    icon = item.icon(isBookmarked = isCurrentUrlBookmarked),
                                    title = item.displayLabel(
                                        activeGroup = activeGroup,
                                        isBookmarked = isCurrentUrlBookmarked,
                                        selectedShortcuts = selectedShortcuts
                                    ),
                                    onClick = { executeBrowserMenuItem(item, callbacks) },
                                    testTag = "menu_item_${item.name}"
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                    MenuListRow(
                        icon = Icons.Filled.Build,
                        title = stringResource(R.string.menu_customize),
                        onClick = {
                            callbacks.onDismiss()
                            callbacks.onCustomizeMenu()
                        },
                        testTag = "menu_customize_row"
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuQuickTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: androidx.compose.ui.graphics.Color? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val tint = accentColor ?: colorScheme.onSurface

    Surface(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) tint else colorScheme.onSurface.copy(alpha = 0.35f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.35f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MenuListRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    testTag: String
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = colorScheme.onSurface
        )
    }
}
