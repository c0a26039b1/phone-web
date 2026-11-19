package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.browser.WebViewRegistry
import com.example.ui.theme.AegisTheme
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlin.math.roundToInt

private val ZOOM_PRESETS = listOf(75, 100, 125, 150, 200)

@Composable
fun PageZoomControlBar(
    zoomPercent: Int,
    onZoomChange: (Int) -> Unit,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onResetZoom: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors
    val clamped = zoomPercent.coerceIn(
        WebViewRegistry.MIN_ZOOM_PERCENT,
        WebViewRegistry.MAX_ZOOM_PERCENT
    )
    val isDefault = clamped == WebViewRegistry.DEFAULT_ZOOM_PERCENT

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .testTag("page_zoom_control_bar"),
        shape = RoundedCornerShape(24.dp),
        color = colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(
            width = 1.dp,
            color = aegisColors.shieldActive.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row: icon + title + live percentage pill + close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.ZoomIn,
                    contentDescription = null,
                    tint = aegisColors.shieldActive,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_page_zoom),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(enabled = !isDefault, onClick = onResetZoom)
                        .testTag("zoom_reset_pill"),
                    shape = RoundedCornerShape(999.dp),
                    color = if (isDefault) {
                        colorScheme.surfaceContainerHighest
                    } else {
                        aegisColors.shieldActiveContainer
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isDefault) colorScheme.outlineVariant else aegisColors.shieldActive.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${clamped}%",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isDefault) colorScheme.onSurface else aegisColors.shieldActive
                        )
                        if (!isDefault) {
                            Icon(
                                imageVector = Icons.Filled.RestartAlt,
                                contentDescription = stringResource(R.string.zoom_reset),
                                tint = aegisColors.shieldActive,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("zoom_bar_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.zoom_close),
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Stepper + live Slider row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(
                            enabled = clamped > WebViewRegistry.MIN_ZOOM_PERCENT,
                            onClick = onZoomOut
                        )
                        .testTag("zoom_out_button"),
                    shape = CircleShape,
                    color = if (clamped > WebViewRegistry.MIN_ZOOM_PERCENT) {
                        colorScheme.surfaceContainerHighest
                    } else {
                        colorScheme.surfaceContainer
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = stringResource(R.string.zoom_out),
                            tint = if (clamped > WebViewRegistry.MIN_ZOOM_PERCENT) {
                                colorScheme.onSurface
                            } else {
                                colorScheme.onSurface.copy(alpha = 0.3f)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Slider(
                    value = clamped.toFloat(),
                    onValueChange = { raw ->
                        val snapped = (raw / 5f).roundToInt() * 5
                        onZoomChange(snapped)
                    },
                    valueRange = WebViewRegistry.MIN_ZOOM_PERCENT.toFloat()..WebViewRegistry.MAX_ZOOM_PERCENT.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = aegisColors.shieldActive,
                        activeTrackColor = aegisColors.shieldActive,
                        inactiveTrackColor = colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("zoom_slider")
                )

                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(
                            enabled = clamped < WebViewRegistry.MAX_ZOOM_PERCENT,
                            onClick = onZoomIn
                        )
                        .testTag("zoom_in_button"),
                    shape = CircleShape,
                    color = if (clamped < WebViewRegistry.MAX_ZOOM_PERCENT) {
                        colorScheme.surfaceContainerHighest
                    } else {
                        colorScheme.surfaceContainer
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.zoom_in),
                            tint = if (clamped < WebViewRegistry.MAX_ZOOM_PERCENT) {
                                colorScheme.onSurface
                            } else {
                                colorScheme.onSurface.copy(alpha = 0.3f)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Quick preset pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ZOOM_PRESETS.forEach { preset ->
                    val selected = clamped == preset
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onZoomChange(preset) }
                            .testTag("zoom_preset_$preset"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) {
                            aegisColors.shieldActiveContainer
                        } else {
                            colorScheme.surface
                        },
                        border = BorderStroke(
                            width = if (selected) 1.2.dp else 0.7.dp,
                            color = if (selected) aegisColors.shieldActive else colorScheme.outlineVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${preset}%",
                                style = MaterialTheme.typography.labelMedium.copy(
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
}

@Composable
fun ActiveZoomBadge(
    zoomPercent: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors

    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .testTag("active_zoom_badge"),
        shape = RoundedCornerShape(24.dp),
        color = colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, aegisColors.shieldActive.copy(alpha = 0.45f)),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ZoomIn,
                contentDescription = stringResource(R.string.action_page_zoom),
                tint = aegisColors.shieldActive,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "${zoomPercent}%",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = colorScheme.onSurface
            )
        }
    }
}
