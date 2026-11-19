package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.rememberHomeBackgroundImage
import com.example.ui.theme.AegisTheme
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlin.math.roundToInt

private val DEFAULT_ZOOM_PRESETS = listOf(80, 100, 120, 150)

@Composable
internal fun PageZoomDetailSection(
    defaultZoomPercent: Int,
    forcePinchZoomEnabled: Boolean,
    onSelectDefaultZoom: (Int) -> Unit,
    onToggleForcePinchZoom: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors
    val clamped = defaultZoomPercent.coerceIn(50, 200)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "新しく開くページに適用される既定の拡大率と、2本指ピンチズームの動作を設定できます。",
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant
        )

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colorScheme.surface,
            border = BorderStroke(1.dp, colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "デフォルトの表示倍率",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = aegisColors.shieldActiveContainer,
                        border = BorderStroke(1.dp, aegisColors.shieldActive.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "${clamped}%",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = aegisColors.shieldActive,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(enabled = clamped > 50) {
                                onSelectDefaultZoom((clamped - 10).coerceAtLeast(50))
                            }
                            .testTag("settings_default_zoom_out"),
                        shape = CircleShape,
                        color = colorScheme.surfaceContainerHigh
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = stringResource(R.string.zoom_out),
                                tint = if (clamped > 50) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Slider(
                        value = clamped.toFloat(),
                        onValueChange = { raw ->
                            val snapped = (raw / 10f).roundToInt() * 10
                            onSelectDefaultZoom(snapped)
                        },
                        valueRange = 50f..200f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = aegisColors.shieldActive,
                            activeTrackColor = aegisColors.shieldActive,
                            inactiveTrackColor = colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_default_zoom_slider")
                    )

                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(enabled = clamped < 200) {
                                onSelectDefaultZoom((clamped + 10).coerceAtMost(200))
                            }
                            .testTag("settings_default_zoom_in"),
                        shape = CircleShape,
                        color = colorScheme.surfaceContainerHigh
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.zoom_in),
                                tint = if (clamped < 200) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DEFAULT_ZOOM_PRESETS.forEach { preset ->
                        val selected = clamped == preset
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectDefaultZoom(preset) }
                                .testTag("settings_zoom_preset_$preset"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) aegisColors.shieldActiveContainer else colorScheme.surfaceContainerLow,
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) aegisColors.shieldActive else colorScheme.outlineVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${preset}%",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (selected) aegisColors.shieldActive else colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colorScheme.surface,
            border = BorderStroke(1.dp, colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ピンチズームを常に許可",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "拡大縮小を制限しているWebサイトでも、2本指操作で自由にズームできるようにします",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = forcePinchZoomEnabled,
                    onCheckedChange = onToggleForcePinchZoom,
                    modifier = Modifier.testTag("settings_force_pinch_zoom_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colorScheme.onPrimary,
                        checkedTrackColor = aegisColors.shieldActive
                    )
                )
            }
        }
    }
}

@Composable
internal fun HomeBackgroundDetailSection(
    backgroundPath: String,
    dimAlpha: Float,
    isImporting: Boolean = false,
    errorMessage: String? = null,
    onPickImage: () -> Unit,
    onPickFromFile: () -> Unit,
    onDimAlphaChange: (Float) -> Unit,
    onResetBackground: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors
    val hasCustomImage = backgroundPath.isNotBlank()
    val previewBitmap = rememberHomeBackgroundImage(path = backgroundPath, maxEdgePx = 1000)
    val dimPercent = (dimAlpha * 100f).roundToInt()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = stringResource(R.string.home_bg_sheet_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = if (hasCustomImage) aegisColors.shieldActive.copy(alpha = 0.5f) else colorScheme.outlineVariant
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = stringResource(R.string.cd_home_background_image),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = dimAlpha.coerceIn(0f, 0.75f)))
                    )
                    if (!isImporting) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color.Black.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "カスタム背景を設定中",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                } else if (!isImporting) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Wallpaper,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "デフォルトのテーマ背景を使用中",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isImporting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.52f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                color = aegisColors.shieldActive,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = stringResource(R.string.home_bg_loading),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colorScheme.errorContainer,
                border = BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (hasCustomImage) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colorScheme.surface,
                border = BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.home_bg_dim_label),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "${dimPercent}%",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = aegisColors.shieldActive
                        )
                    }
                    Slider(
                        value = dimAlpha,
                        onValueChange = onDimAlphaChange,
                        valueRange = 0f..0.75f,
                        colors = SliderDefaults.colors(
                            thumbColor = aegisColors.shieldActive,
                            activeTrackColor = aegisColors.shieldActive,
                            inactiveTrackColor = colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("settings_home_bg_dim_slider")
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPickImage,
                enabled = !isImporting,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("settings_home_bg_pick_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = aegisColors.shieldActive,
                    contentColor = colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.home_bg_pick_photo),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            OutlinedButton(
                onClick = onPickFromFile,
                enabled = !isImporting,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("settings_home_bg_pick_file_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, aegisColors.shieldActive.copy(alpha = 0.55f))
            ) {
                Icon(
                    imageVector = Icons.Filled.FolderOpen,
                    contentDescription = null,
                    tint = aegisColors.shieldActive,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.home_bg_pick_file),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = colorScheme.onSurface
                )
            }
        }

        if (hasCustomImage) {
            OutlinedButton(
                onClick = onResetBackground,
                enabled = !isImporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_home_bg_reset_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Icon(
                    imageVector = Icons.Filled.RestartAlt,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_bg_reset),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colorScheme.onSurface
                )
            }
        }
    }
}
