package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.browser.BrowserTab
import com.example.ui.browser.HomeBackgroundStorage
import com.example.ui.browser.TabGroup
import com.example.ui.browser.TabGroupColor
import com.example.ui.components.AssignTabToGroupSheet
import com.example.ui.components.CreateOrEditTabGroupDialog
import com.example.ui.components.TabCardSurfaceContent
import com.example.ui.components.tabGroupAccentColor
import com.example.ui.theme.AegisTheme
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSwitcherScreen(
    tabs: List<BrowserTab>,
    tabGroups: List<TabGroup>,
    activeTabId: String,
    tabPreviews: Map<String, Bitmap>,
    tabFavicons: Map<String, Bitmap>,
    homeBackgroundPath: String,
    homeBackgroundDimAlpha: Float,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onCloseAllTabs: () -> Unit,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onNewTabInGroup: (String) -> Unit,
    onCreateTabGroup: (name: String, color: TabGroupColor, tabIds: List<String>) -> Unit,
    onAssignTabToGroup: (tabId: String, groupId: String?) -> Unit,
    onMergeTabsIntoGroup: (sourceTabId: String, targetTabId: String) -> Unit,
    onUpdateTabGroup: (groupId: String, name: String, color: TabGroupColor) -> Unit,
    onToggleGroupCollapsed: (String) -> Unit,
    onUngroupTabs: (String) -> Unit,
    onCloseTabGroup: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    BackHandler(onBack = onBack)

    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val homeBackgroundBitmap by produceState<Bitmap?>(initialValue = null, key1 = homeBackgroundPath) {
        value = if (homeBackgroundPath.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                HomeBackgroundStorage.loadBitmap(homeBackgroundPath, maxEdgePx = 480)
            }
        }
    }

    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var preselectedTabForNewGroup by remember { mutableStateOf<List<String>>(emptyList()) }
    var editingGroup by remember { mutableStateOf<TabGroup?>(null) }
    var assigningTab by remember { mutableStateOf<BrowserTab?>(null) }
    var showBottomMoreMenu by remember { mutableStateOf(false) }

    // Drag-and-drop state for merging tabs into groups
    var overlayRootOrigin by remember { mutableStateOf(Offset.Zero) }
    val tabBoundsMap = remember { mutableStateMapOf<String, Rect>() }
    val groupBoundsMap = remember { mutableStateMapOf<String, Rect>() }

    var draggingTabId by remember { mutableStateOf<String?>(null) }
    var dragOriginBounds by remember { mutableStateOf<Rect?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var hoverTargetTabId by remember { mutableStateOf<String?>(null) }
    var hoverTargetGroupId by remember { mutableStateOf<String?>(null) }

    val groupedTabsMap = remember(tabs, tabGroups) {
        tabGroups.associateWith { grp -> tabs.filter { it.groupId == grp.id } }
    }
    val ungroupedTabs = remember(tabs) {
        tabs.filter { it.groupId == null }
    }

    fun updateHoverTarget(sourceTabId: String, origin: Rect, offset: Offset) {
        val draggedRect = Rect(
            left = origin.left + offset.x,
            top = origin.top + offset.y,
            right = origin.right + offset.x,
            bottom = origin.bottom + offset.y
        )
        val draggedCenter = draggedRect.center

        // Prefer the tab card with the largest overlap (at least 22% area overlap or center hit)
        var bestTabId: String? = null
        var bestScore = 0f
        tabBoundsMap.forEach { (candidateId, rect) ->
            if (candidateId != sourceTabId && tabs.any { it.id == candidateId }) {
                val overlap = calculateOverlapRatio(draggedRect, rect)
                val centerHit = rect.contains(draggedCenter)
                val score = if (centerHit) overlap + 1f else overlap
                if ((centerHit || overlap >= 0.22f) && score > bestScore) {
                    bestScore = score
                    bestTabId = candidateId
                }
            }
        }

        val sourceTab = tabs.firstOrNull { it.id == sourceTabId }
        val newTargetTabId = if (bestTabId != null) {
            val targetTab = tabs.firstOrNull { it.id == bestTabId }
            if (sourceTab != null && targetTab != null &&
                sourceTab.groupId != null && sourceTab.groupId == targetTab.groupId
            ) {
                null
            } else {
                bestTabId
            }
        } else {
            null
        }

        // If no individual tab card matched (e.g. hovering a collapsed group header), hit-test groups
        val newTargetGroupId = if (newTargetTabId == null) {
            groupBoundsMap.entries.firstOrNull { (groupId, rect) ->
                groupId != sourceTab?.groupId &&
                    tabGroups.any { it.id == groupId } &&
                    (rect.contains(draggedCenter) || calculateOverlapRatio(draggedRect, rect) >= 0.25f)
            }?.key
        } else {
            null
        }

        if (newTargetTabId != hoverTargetTabId || newTargetGroupId != hoverTargetGroupId) {
            if (newTargetTabId != null || newTargetGroupId != null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            hoverTargetTabId = newTargetTabId
            hoverTargetGroupId = newTargetGroupId
        }
    }

    fun finishDrag(commitDrop: Boolean) {
        val sourceId = draggingTabId
        val targetTab = hoverTargetTabId
        val targetGroup = hoverTargetGroupId

        draggingTabId = null
        dragOriginBounds = null
        dragOffset = Offset.Zero
        hoverTargetTabId = null
        hoverTargetGroupId = null

        if (commitDrop && sourceId != null) {
            when {
                targetTab != null -> onMergeTabsIntoGroup(sourceId, targetTab)
                targetGroup != null -> onAssignTabToGroup(sourceId, targetGroup)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                overlayRootOrigin = coords.positionInRoot()
            }
    ) {
        Scaffold(
            containerColor = colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.tabs_title),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = tabs.size.toString(),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = SpaceGroteskFontFamily
                                    ),
                                    color = aegisColors.shieldActive,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("tabs_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.nav_back)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                preselectedTabForNewGroup = emptyList()
                                showCreateGroupDialog = true
                            },
                            modifier = Modifier.testTag("tabs_create_group_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CreateNewFolder,
                                contentDescription = stringResource(R.string.tab_group_create),
                                tint = aegisColors.shieldActive
                            )
                        }
                        IconButton(
                            onClick = onCloseAllTabs,
                            modifier = Modifier.testTag("tabs_close_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = stringResource(R.string.action_close_all_tabs)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorScheme.surface
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = colorScheme.outlineVariant
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            // Centered '+' button for adding a new tab
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onNewTab)
                                    .testTag("tabs_new_normal_button"),
                                shape = CircleShape,
                                color = colorScheme.primary,
                                shadowElevation = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = stringResource(R.string.action_new_tab),
                                        tint = colorScheme.onPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            // Right-aligned vertical overflow menu (⋮)
                            Box(
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                IconButton(
                                    onClick = { showBottomMoreMenu = true },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("tabs_bottom_more_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = stringResource(R.string.nav_menu),
                                        tint = colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showBottomMoreMenu,
                                    onDismissRequest = { showBottomMoreMenu = false },
                                    containerColor = colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.testTag("tabs_bottom_more_menu")
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.action_new_tab),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = colorScheme.onSurface
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = null,
                                                tint = aegisColors.shieldActive,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        onClick = {
                                            showBottomMoreMenu = false
                                            onNewTab()
                                        },
                                        modifier = Modifier.testTag("tabs_menu_new_tab")
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.tabs_menu_privacy_tab),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = colorScheme.onSurface
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.PrivacyTip,
                                                contentDescription = null,
                                                tint = aegisColors.privateAccent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        onClick = {
                                            showBottomMoreMenu = false
                                            onNewPrivateTab()
                                        },
                                        modifier = Modifier.testTag("tabs_menu_privacy_tab")
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.action_close_all_tabs),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = colorScheme.error
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteSweep,
                                                contentDescription = null,
                                                tint = colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        onClick = {
                                            showBottomMoreMenu = false
                                            onCloseAllTabs()
                                        },
                                        modifier = Modifier.testTag("tabs_menu_close_all_tabs")
                                    )
                                    HorizontalDivider(
                                        color = colorScheme.outlineVariant,
                                        thickness = 0.5.dp
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.tabs_menu_settings),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = colorScheme.onSurface
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Settings,
                                                contentDescription = null,
                                                tint = colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        onClick = {
                                            showBottomMoreMenu = false
                                            onOpenSettings()
                                        },
                                        modifier = Modifier.testTag("tabs_menu_settings")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                userScrollEnabled = draggingTabId == null,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Subtle discoverability hint when user has 2+ tabs and no groups yet
                if (tabs.size >= 2 && tabGroups.isEmpty()) {
                    item(key = "drag_group_hint") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = aegisColors.shieldActiveContainer.copy(alpha = 0.65f),
                            border = BorderStroke(0.8.dp, aegisColors.shieldActive.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.TouchApp,
                                    contentDescription = null,
                                    tint = aegisColors.shieldActive,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "タブをドラッグして別のタブに重ねると自動でグループ化されます",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Grouped Tab Containers
                items(tabGroups, key = { it.id }) { group ->
                    val groupTabs = groupedTabsMap[group].orEmpty()
                    TabGroupSectionCard(
                        group = group,
                        tabs = groupTabs,
                        activeTabId = activeTabId,
                        tabPreviews = tabPreviews,
                        tabFavicons = tabFavicons,
                        homeBackgroundBitmap = homeBackgroundBitmap,
                        homeBackgroundDimAlpha = homeBackgroundDimAlpha,
                        draggingTabId = draggingTabId,
                        hoverTargetTabId = hoverTargetTabId,
                        isGroupDropTarget = hoverTargetGroupId == group.id,
                        onGroupBoundsChanged = { rect -> groupBoundsMap[group.id] = rect },
                        onGroupDisposed = { groupBoundsMap.remove(group.id) },
                        onTabBoundsChanged = { tabId, rect -> tabBoundsMap[tabId] = rect },
                        onTabDisposed = { tabId -> tabBoundsMap.remove(tabId) },
                        onDragStart = { tabId ->
                            val origin = tabBoundsMap[tabId]
                            if (origin != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                draggingTabId = tabId
                                dragOriginBounds = origin
                                dragOffset = Offset.Zero
                            }
                        },
                        onDragDelta = { delta ->
                            val currentSource = draggingTabId
                            val origin = dragOriginBounds
                            if (currentSource != null && origin != null) {
                                dragOffset += delta
                                updateHoverTarget(currentSource, origin, dragOffset)
                            }
                        },
                        onDragEnd = { finishDrag(commitDrop = true) },
                        onDragCancel = { finishDrag(commitDrop = false) },
                        onToggleCollapse = { onToggleGroupCollapsed(group.id) },
                        onAddTabInGroup = { onNewTabInGroup(group.id) },
                        onEditGroup = { editingGroup = group },
                        onUngroup = { onUngroupTabs(group.id) },
                        onCloseGroup = { onCloseTabGroup(group.id) },
                        onSelectTab = onSelectTab,
                        onCloseTab = onCloseTab,
                        onMoveTabGroup = { tab -> assigningTab = tab }
                    )
                }

                // Ungrouped Tabs Section
                if (ungroupedTabs.isNotEmpty()) {
                    if (tabGroups.isNotEmpty()) {
                        item(key = "ungrouped_header") {
                            Text(
                                text = stringResource(R.string.tab_group_ungrouped_label),
                                style = MaterialTheme.typography.titleSmall,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }
                    }
                    item(key = "ungrouped_grid") {
                        ResponsiveTabCardsGrid(
                            tabs = ungroupedTabs,
                            activeTabId = activeTabId,
                            tabPreviews = tabPreviews,
                            tabFavicons = tabFavicons,
                            homeBackgroundBitmap = homeBackgroundBitmap,
                            homeBackgroundDimAlpha = homeBackgroundDimAlpha,
                            groupAccent = null,
                            draggingTabId = draggingTabId,
                            hoverTargetTabId = hoverTargetTabId,
                            onTabBoundsChanged = { tabId, rect -> tabBoundsMap[tabId] = rect },
                            onTabDisposed = { tabId -> tabBoundsMap.remove(tabId) },
                            onDragStart = { tabId ->
                                val origin = tabBoundsMap[tabId]
                                if (origin != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    draggingTabId = tabId
                                    dragOriginBounds = origin
                                    dragOffset = Offset.Zero
                                }
                            },
                            onDragDelta = { delta ->
                                val currentSource = draggingTabId
                                val origin = dragOriginBounds
                                if (currentSource != null && origin != null) {
                                    dragOffset += delta
                                    updateHoverTarget(currentSource, origin, dragOffset)
                                }
                            },
                            onDragEnd = { finishDrag(commitDrop = true) },
                            onDragCancel = { finishDrag(commitDrop = false) },
                            onSelectTab = onSelectTab,
                            onCloseTab = onCloseTab,
                            onMoveTabGroup = { tab -> assigningTab = tab }
                        )
                    }
                }
            }
        }

        // Floating Dragged Tab Ghost Card Overlay
        val activeDraggedTab = draggingTabId?.let { id -> tabs.firstOrNull { it.id == id } }
        val originRect = dragOriginBounds
        if (activeDraggedTab != null && originRect != null) {
            val ghostWidthDp = with(density) { originRect.width.toDp() }
            val ghostHeightDp = with(density) { originRect.height.toDp() }
            val tx = (originRect.left - overlayRootOrigin.x) + dragOffset.x
            val ty = (originRect.top - overlayRootOrigin.y) + dragOffset.y
            val ghostGroupAccent = activeDraggedTab.groupId?.let { gid ->
                tabGroups.firstOrNull { it.id == gid }?.color
            }?.let { tabGroupAccentColor(it) }
            val isHoveringTarget = hoverTargetTabId != null || hoverTargetGroupId != null

            Box(
                modifier = Modifier
                    .width(ghostWidthDp)
                    .height(ghostHeightDp)
                    .graphicsLayer {
                        translationX = tx
                        translationY = ty
                        scaleX = 1.05f
                        scaleY = 1.05f
                        shadowElevation = 22f
                    }
            ) {
                TabCardSurfaceContent(
                    tab = activeDraggedTab,
                    previewBitmap = tabPreviews[activeDraggedTab.id],
                    faviconBitmap = tabFavicons[activeDraggedTab.id],
                    homeBackgroundBitmap = homeBackgroundBitmap,
                    homeBackgroundDimAlpha = homeBackgroundDimAlpha,
                    isActive = true,
                    groupAccent = ghostGroupAccent,
                    isDropTarget = isHoveringTarget,
                    dropHintText = if (isHoveringTarget) "離してグループ化" else null,
                    onClose = {},
                    onMoveGroup = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showCreateGroupDialog) {
        CreateOrEditTabGroupDialog(
            existingGroup = null,
            availableTabs = tabs,
            preselectedTabIds = preselectedTabForNewGroup,
            onDismiss = { showCreateGroupDialog = false },
            onConfirm = { name, color, selectedIds ->
                onCreateTabGroup(name, color, selectedIds)
                showCreateGroupDialog = false
            }
        )
    }

    val currentEditGroup = editingGroup
    if (currentEditGroup != null) {
        CreateOrEditTabGroupDialog(
            existingGroup = currentEditGroup,
            onDismiss = { editingGroup = null },
            onConfirm = { name, color, _ ->
                onUpdateTabGroup(currentEditGroup.id, name, color)
                editingGroup = null
            }
        )
    }

    val currentAssignTab = assigningTab
    if (currentAssignTab != null) {
        AssignTabToGroupSheet(
            tab = currentAssignTab,
            groups = tabGroups,
            onDismiss = { assigningTab = null },
            onAssignToGroup = { targetGroupId ->
                onAssignTabToGroup(currentAssignTab.id, targetGroupId)
            },
            onCreateNewGroupWithTab = {
                preselectedTabForNewGroup = listOf(currentAssignTab.id)
                showCreateGroupDialog = true
            }
        )
    }
}

private fun calculateOverlapRatio(a: Rect, b: Rect): Float {
    val left = maxOf(a.left, b.left)
    val top = maxOf(a.top, b.top)
    val right = minOf(a.right, b.right)
    val bottom = minOf(a.bottom, b.bottom)
    if (right <= left || bottom <= top) return 0f
    val intersectionArea = (right - left) * (bottom - top)
    val targetArea = (b.width * b.height).coerceAtLeast(1f)
    return intersectionArea / targetArea
}

@Composable
private fun TabGroupSectionCard(
    group: TabGroup,
    tabs: List<BrowserTab>,
    activeTabId: String,
    tabPreviews: Map<String, Bitmap>,
    tabFavicons: Map<String, Bitmap>,
    homeBackgroundBitmap: Bitmap?,
    homeBackgroundDimAlpha: Float,
    draggingTabId: String?,
    hoverTargetTabId: String?,
    isGroupDropTarget: Boolean,
    onGroupBoundsChanged: (Rect) -> Unit,
    onGroupDisposed: () -> Unit,
    onTabBoundsChanged: (String, Rect) -> Unit,
    onTabDisposed: (String) -> Unit,
    onDragStart: (String) -> Unit,
    onDragDelta: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onToggleCollapse: () -> Unit,
    onAddTabInGroup: () -> Unit,
    onEditGroup: () -> Unit,
    onUngroup: () -> Unit,
    onCloseGroup: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onMoveTabGroup: (BrowserTab) -> Unit
) {
    DisposableEffect(group.id) {
        onDispose { onGroupDisposed() }
    }

    val colorScheme = MaterialTheme.colorScheme
    val accent = tabGroupAccentColor(group.color)
    var showMenu by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isGroupDropTarget) 1.02f else 1f,
        animationSpec = spring(),
        label = "group_drop_scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onGloballyPositioned { coords ->
                onGroupBoundsChanged(coords.boundsInRoot())
            }
            .testTag("tab_group_section_${group.id}"),
        shape = RoundedCornerShape(22.dp),
        color = if (isGroupDropTarget) accent.copy(alpha = 0.14f) else colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = if (isGroupDropTarget) 2.5.dp else 1.2.dp,
            color = if (isGroupDropTarget) accent else accent.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Group Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleCollapse)
                    .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
                    .testTag("tab_group_header_${group.id}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isGroupDropTarget) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accent
                    ) {
                        Text(
                            text = "追加",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "${tabs.size}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                IconButton(
                    onClick = onAddTabInGroup,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("tab_group_add_tab_${group.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.tab_group_new_tab_in_group),
                        tint = accent,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Icon(
                    imageVector = if (group.isCollapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                    contentDescription = if (group.isCollapsed) "展開" else "折りたたむ",
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("tab_group_menu_${group.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.nav_menu),
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.tab_group_edit)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Edit, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                onEditGroup()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.tab_group_ungroup)) },
                            leadingIcon = {
                                Icon(Icons.Filled.FolderOff, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                onUngroup()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.tab_group_close_all),
                                    color = colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                onCloseGroup()
                            }
                        )
                    }
                }
            }

            // Collapsible Tab Grid inside Group
            AnimatedVisibility(
                visible = !group.isCollapsed,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                    ResponsiveTabCardsGrid(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        tabPreviews = tabPreviews,
                        tabFavicons = tabFavicons,
                        homeBackgroundBitmap = homeBackgroundBitmap,
                        homeBackgroundDimAlpha = homeBackgroundDimAlpha,
                        groupAccent = accent,
                        draggingTabId = draggingTabId,
                        hoverTargetTabId = hoverTargetTabId,
                        onTabBoundsChanged = onTabBoundsChanged,
                        onTabDisposed = onTabDisposed,
                        onDragStart = onDragStart,
                        onDragDelta = onDragDelta,
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel,
                        onSelectTab = onSelectTab,
                        onCloseTab = onCloseTab,
                        onMoveTabGroup = onMoveTabGroup
                    )
                }
            }
        }
    }
}

@Composable
private fun ResponsiveTabCardsGrid(
    tabs: List<BrowserTab>,
    activeTabId: String,
    tabPreviews: Map<String, Bitmap>,
    tabFavicons: Map<String, Bitmap>,
    homeBackgroundBitmap: Bitmap?,
    homeBackgroundDimAlpha: Float,
    groupAccent: Color?,
    draggingTabId: String?,
    hoverTargetTabId: String?,
    onTabBoundsChanged: (String, Rect) -> Unit,
    onTabDisposed: (String) -> Unit,
    onDragStart: (String) -> Unit,
    onDragDelta: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onMoveTabGroup: (BrowserTab) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 560.dp) 3 else 2
        val rows = tabs.chunked(columns)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { rowTabs ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowTabs.forEach { tab ->
                        DraggableTabCardItem(
                            tab = tab,
                            previewBitmap = tabPreviews[tab.id],
                            faviconBitmap = tabFavicons[tab.id],
                            homeBackgroundBitmap = homeBackgroundBitmap,
                            homeBackgroundDimAlpha = homeBackgroundDimAlpha,
                            isActive = tab.id == activeTabId,
                            groupAccent = groupAccent,
                            isBeingDragged = draggingTabId == tab.id,
                            isDropTarget = hoverTargetTabId == tab.id,
                            onBoundsChanged = { rect -> onTabBoundsChanged(tab.id, rect) },
                            onDisposed = { onTabDisposed(tab.id) },
                            onDragStart = { onDragStart(tab.id) },
                            onDragDelta = onDragDelta,
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                            onSelect = { onSelectTab(tab.id) },
                            onClose = { onCloseTab(tab.id) },
                            onMoveGroup = { onMoveTabGroup(tab) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(columns - rowTabs.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableTabCardItem(
    tab: BrowserTab,
    previewBitmap: Bitmap?,
    faviconBitmap: Bitmap?,
    homeBackgroundBitmap: Bitmap?,
    homeBackgroundDimAlpha: Float,
    isActive: Boolean,
    groupAccent: Color?,
    isBeingDragged: Boolean,
    isDropTarget: Boolean,
    onBoundsChanged: (Rect) -> Unit,
    onDisposed: () -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onMoveGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    DisposableEffect(tab.id) {
        onDispose { onDisposed() }
    }

    val viewConfig = LocalViewConfiguration.current
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragDelta by rememberUpdatedState(onDragDelta)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)

    val targetScale by animateFloatAsState(
        targetValue = if (isDropTarget) 1.04f else 1f,
        animationSpec = spring(),
        label = "tab_drop_scale"
    )

    val dropLabel = if (isDropTarget) {
        if (tab.groupId != null) "グループに追加" else "重ねてグループ化"
    } else {
        null
    }

    Box(
        modifier = modifier
            .height(228.dp)
            .graphicsLayer {
                scaleX = targetScale
                scaleY = targetScale
            }
            .alpha(if (isBeingDragged) 0.22f else 1f)
            .onGloballyPositioned { coords ->
                onBoundsChanged(coords.boundsInRoot())
            }
            .pointerInput(tab.id) {
                val touchSlop = viewConfig.touchSlop
                val holdToDragMs = 180L
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = down.uptimeMillis
                    var dragActive = false
                    var totalDrag = Offset.Zero
                    val pointerId = down.id

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                        if (change.changedToUpIgnoreConsumed()) {
                            if (dragActive) {
                                dragActive = false
                                change.consume()
                                currentOnDragEnd()
                            }
                            break
                        }

                        val delta = change.positionChange()
                        if (!dragActive) {
                            // If a child button already consumed it, abort drag tracking
                            if (change.isConsumed) break
                            totalDrag += delta
                            val elapsed = change.uptimeMillis - downTime
                            val movedHorizontally = abs(totalDrag.x) > touchSlop &&
                                abs(totalDrag.x) > abs(totalDrag.y) * 1.15f
                            val movedVertically = abs(totalDrag.y) > touchSlop &&
                                abs(totalDrag.y) >= abs(totalDrag.x) * 1.15f

                            if (movedVertically && elapsed < holdToDragMs) {
                                // Yield immediately to LazyColumn vertical scrolling
                                break
                            }

                            if (movedHorizontally || (elapsed >= holdToDragMs && totalDrag.getDistance() > touchSlop * 0.5f)) {
                                dragActive = true
                                change.consume()
                                currentOnDragStart()
                                currentOnDragDelta(totalDrag)
                            }
                        } else {
                            if (delta != Offset.Zero) {
                                change.consume()
                                currentOnDragDelta(delta)
                            }
                        }
                    }

                    if (dragActive) {
                        currentOnDragCancel()
                    }
                }
            }
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = !isBeingDragged, onClick = onSelect)
            .testTag("tab_card_${tab.id}")
    ) {
        TabCardSurfaceContent(
            tab = tab,
            previewBitmap = previewBitmap,
            faviconBitmap = faviconBitmap,
            homeBackgroundBitmap = homeBackgroundBitmap,
            homeBackgroundDimAlpha = homeBackgroundDimAlpha,
            isActive = isActive,
            groupAccent = groupAccent,
            isDropTarget = isDropTarget,
            dropHintText = dropLabel,
            onClose = onClose,
            onMoveGroup = onMoveGroup,
            modifier = Modifier.fillMaxSize()
        )
    }
}
