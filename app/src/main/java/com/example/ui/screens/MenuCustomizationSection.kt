package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BrowserMenuItem
import com.example.data.local.MenuLayoutStyle
import com.example.ui.components.icon
import com.example.ui.theme.AegisTheme

@Composable
fun MenuCustomizationDetailSection(
    selectedLayout: MenuLayoutStyle,
    enabledItems: List<BrowserMenuItem>,
    onSelectLayout: (MenuLayoutStyle) -> Unit,
    onToggleItem: (BrowserMenuItem) -> Unit,
    onMoveItem: (BrowserMenuItem, Boolean) -> Unit,
    onResetDefaults: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors
    val disabledItems = BrowserMenuItem.entries.filter { it !in enabledItems }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Menu Layout Style Selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "メニューの表示スタイル",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.onSurface
            )
            Text(
                text = "右下のメニューボタンを押したときのレイアウトをいつでも切り替えできます。",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )

            MenuLayoutStyle.entries.forEach { style ->
                val selected = selectedLayout == style
                val icon = when (style) {
                    MenuLayoutStyle.POPUP_LIST -> Icons.Filled.ViewList
                    MenuLayoutStyle.CLASSIC_SHEET -> Icons.Filled.ViewAgenda
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectLayout(style) }
                        .testTag("menu_layout_style_${style.name}"),
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) aegisColors.shieldActiveContainer else colorScheme.surface,
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) aegisColors.shieldActive else colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
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
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (selected) aegisColors.shieldActive else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = style.displayName,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                                ),
                                color = if (selected) aegisColors.shieldActive else colorScheme.onSurface
                            )
                            Text(
                                text = style.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        if (selected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = aegisColors.shieldActive,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Active Menu Items & Reordering
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "表示する項目と並び順（${enabledItems.size}件）",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface
                )
            }
            Text(
                text = "上下の矢印で並び替え、チェックを外すとメニューから非表示にできます。",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colorScheme.surface,
                border = BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Column {
                    enabledItems.forEachIndexed { idx, item ->
                        if (idx > 0) {
                            HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .padding(start = 8.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                                .testTag("menu_item_active_${item.name}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = true,
                                onCheckedChange = { onToggleItem(item) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = aegisColors.shieldActive,
                                    checkmarkColor = colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("menu_item_checkbox_${item.name}")
                            )
                            Icon(
                                imageVector = item.icon(),
                                contentDescription = null,
                                tint = aegisColors.shieldActive,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onMoveItem(item, true) },
                                enabled = idx > 0,
                                modifier = Modifier.testTag("menu_item_up_${item.name}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowUp,
                                    contentDescription = "上へ移動",
                                    tint = if (idx > 0) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.25f)
                                )
                            }
                            IconButton(
                                onClick = { onMoveItem(item, false) },
                                enabled = idx < enabledItems.lastIndex,
                                modifier = Modifier.testTag("menu_item_down_${item.name}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "下へ移動",
                                    tint = if (idx < enabledItems.lastIndex) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.25f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Hidden / Optional Items that can be added
        if (disabledItems.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "非表示の項目（タップで追加）",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colorScheme.surface,
                    border = BorderStroke(1.dp, colorScheme.outlineVariant)
                ) {
                    Column {
                        disabledItems.forEachIndexed { idx, item ->
                            if (idx > 0) {
                                HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clickable { onToggleItem(item) }
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                                    .testTag("menu_item_hidden_${item.name}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = false,
                                    onCheckedChange = { onToggleItem(item) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = aegisColors.shieldActive,
                                        checkmarkColor = colorScheme.onPrimary
                                    )
                                )
                                Icon(
                                    imageVector = item.icon(),
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = item.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedButton(
            onClick = onResetDefaults,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("menu_layout_reset_button")
        ) {
            Icon(
                imageVector = Icons.Filled.RestartAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.menu_layout_reset))
        }
    }
}
