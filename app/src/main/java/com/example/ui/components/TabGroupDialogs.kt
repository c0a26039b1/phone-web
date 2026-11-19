package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.adblock.AdBlockEngine
import com.example.ui.browser.BrowserTab
import com.example.ui.browser.TabGroup
import com.example.ui.browser.TabGroupColor
import com.example.ui.theme.AegisTheme

@Composable
fun tabGroupAccentColor(color: TabGroupColor): Color {
    val aegisColors = AegisTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    return when (color) {
        TabGroupColor.EMERALD -> aegisColors.shieldActive
        TabGroupColor.CYAN -> aegisColors.trackerAccent
        TabGroupColor.PURPLE -> aegisColors.privateAccent
        TabGroupColor.AMBER -> aegisColors.shieldPaused
        TabGroupColor.CORAL -> aegisColors.blockedAccent
        TabGroupColor.BLUE -> colorScheme.secondary
    }
}

@Composable
fun CreateOrEditTabGroupDialog(
    existingGroup: TabGroup? = null,
    availableTabs: List<BrowserTab> = emptyList(),
    preselectedTabIds: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: TabGroupColor, selectedTabIds: List<String>) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var groupName by remember(existingGroup) { mutableStateOf(existingGroup?.name ?: "") }
    var selectedColor by remember(existingGroup) {
        mutableStateOf(existingGroup?.color ?: TabGroupColor.EMERALD)
    }
    val selectedIds = remember(preselectedTabIds) {
        mutableStateListOf<String>().apply { addAll(preselectedTabIds) }
    }

    val isEditMode = existingGroup != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (isEditMode) R.string.tab_group_edit else R.string.tab_group_create
                ),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.tab_group_name_placeholder),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tab_group_name_input")
                )

                // Color Picker Row
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "グループカラー",
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TabGroupColor.entries.forEach { groupColor ->
                            val swatch = tabGroupAccentColor(groupColor)
                            val isSelected = selectedColor == groupColor
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(swatch.copy(alpha = if (isSelected) 1f else 0.72f))
                                    .clickable { selectedColor = groupColor }
                                    .testTag("tab_group_color_${groupColor.name}"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = groupColor.labelJa,
                                        tint = colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Multi-tab selection when creating a new group from the Tab Switcher
                if (!isEditMode && availableTabs.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "含めるタブを選択（未選択なら新規タブを作成）",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(availableTabs, key = { it.id }) { tab ->
                                val checked = tab.id in selectedIds
                                val label = tab.title.ifBlank {
                                    AdBlockEngine.extractHost(tab.url).ifBlank { "新しいタブ" }
                                }
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            if (checked) selectedIds.remove(tab.id)
                                            else selectedIds.add(tab.id)
                                        }
                                        .testTag("group_dialog_tab_${tab.id}"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (checked) {
                                        tabGroupAccentColor(selectedColor).copy(alpha = 0.12f)
                                    } else {
                                        colorScheme.surfaceContainerLow
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = { isChecked ->
                                                if (isChecked) {
                                                    if (tab.id !in selectedIds) selectedIds.add(tab.id)
                                                } else {
                                                    selectedIds.remove(tab.id)
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = tabGroupAccentColor(selectedColor)
                                            )
                                        )
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(groupName, selectedColor, selectedIds.toList()) },
                enabled = !isEditMode || groupName.isNotBlank(),
                modifier = Modifier.testTag("tab_group_confirm_button")
            ) {
                Text(if (isEditMode) "保存" else "作成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignTabToGroupSheet(
    tab: BrowserTab,
    groups: List<TabGroup>,
    onDismiss: () -> Unit,
    onAssignToGroup: (String?) -> Unit,
    onCreateNewGroupWithTab: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val tabTitle = tab.title.ifBlank {
        AdBlockEngine.extractHost(tab.url).ifBlank { stringResource(R.string.tabs_new_page) }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.tab_group_move_to),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface
                )
                Text(
                    text = tabTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Column {
                    // Create new group option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable {
                                onDismiss()
                                onCreateNewGroupWithTab()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("assign_sheet_create_group"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CreateNewFolder,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "新しいグループを作成して追加",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = colorScheme.primary
                        )
                    }

                    // Existing groups
                    groups.forEach { group ->
                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                        val accent = tabGroupAccentColor(group.color)
                        val isCurrent = tab.groupId == group.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clickable {
                                    onDismiss()
                                    onAssignToGroup(group.id)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .testTag("assign_sheet_group_${group.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (isCurrent) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "選択中",
                                    tint = accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Remove from group option if currently grouped
                    if (tab.groupId != null) {
                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clickable {
                                    onDismiss()
                                    onAssignToGroup(null)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .testTag("assign_sheet_remove_from_group"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FolderOff,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.tab_group_remove_from),
                                style = MaterialTheme.typography.bodyLarge,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveTabGroupStrip(
    group: TabGroup,
    tabsInGroup: List<BrowserTab>,
    activeTabId: String,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onAddTabInGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val accent = tabGroupAccentColor(group.color)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Column {
            HorizontalDivider(thickness = 0.5.dp, color = accent.copy(alpha = 0.35f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Group Tag Pill
                Surface(
                    modifier = Modifier.padding(start = 10.dp, end = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accent)
                        )
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Scrollable Sibling Tabs in Group
                LazyRow(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(tabsInGroup, key = { it.id }) { tab ->
                        val isActive = tab.id == activeTabId
                        val label = tab.title.ifBlank {
                            AdBlockEngine.extractHost(tab.url).ifBlank { "新しいタブ" }
                        }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectTab(tab.id) }
                                .testTag("group_strip_tab_${tab.id}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isActive) accent.copy(alpha = 0.22f) else colorScheme.surface,
                            border = BorderStroke(
                                width = if (isActive) 1.2.dp else 0.5.dp,
                                color = if (isActive) accent else colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isActive) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.heightIn(max = 18.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .clickable { onCloseTab(tab.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.action_close_tab),
                                        tint = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Add New Tab directly into active group
                IconButton(
                    onClick = onAddTabInGroup,
                    modifier = Modifier
                        .size(38.dp)
                        .padding(end = 6.dp)
                        .testTag("group_strip_add_tab")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.tab_group_new_tab_in_group),
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
