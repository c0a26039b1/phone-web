package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.adblock.AdBlockEngine
import com.example.ui.browser.BrowserTab
import com.example.ui.theme.AegisTheme
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun TabCardSurfaceContent(
    tab: BrowserTab,
    previewBitmap: Bitmap?,
    faviconBitmap: Bitmap?,
    homeBackgroundBitmap: Bitmap?,
    homeBackgroundDimAlpha: Float,
    isActive: Boolean,
    groupAccent: Color?,
    isDropTarget: Boolean,
    dropHintText: String?,
    onClose: () -> Unit,
    onMoveGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors
    val host = remember(tab.url) { AdBlockEngine.extractHost(tab.url) }
    val activeAccent = groupAccent ?: if (tab.isPrivate) aegisColors.privateAccent else aegisColors.shieldActive

    val borderColor = when {
        isDropTarget -> aegisColors.shieldActive
        isActive -> activeAccent
        groupAccent != null -> groupAccent.copy(alpha = 0.42f)
        tab.isPrivate -> aegisColors.privateAccent.copy(alpha = 0.42f)
        else -> colorScheme.outlineVariant
    }

    val headerContainerColor = when {
        isDropTarget -> aegisColors.shieldActiveContainer
        tab.isPrivate -> aegisColors.privateContainer
        isActive -> colorScheme.surfaceContainerHighest
        else -> colorScheme.surfaceContainerHigh
    }

    val titleText = when {
        tab.title.isNotBlank() -> tab.title
        host.isNotBlank() -> host
        tab.isPrivate -> stringResource(R.string.tabs_private_page)
        else -> stringResource(R.string.tabs_new_page)
    }

    val faviconImage = remember(faviconBitmap) {
        faviconBitmap?.takeIf { !it.isRecycled }?.asImageBitmap()
    }
    val previewImage = remember(previewBitmap) {
        previewBitmap?.takeIf { !it.isRecycled }?.asImageBitmap()
    }
    val homeBgImage = remember(homeBackgroundBitmap) {
        homeBackgroundBitmap?.takeIf { !it.isRecycled }?.asImageBitmap()
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = colorScheme.surface,
        border = BorderStroke(
            width = when {
                isDropTarget -> 2.5.dp
                isActive -> 2.dp
                else -> 1.dp
            },
            color = borderColor
        ),
        tonalElevation = if (isActive || isDropTarget) 4.dp else 1.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Compact Tab Header Strip: Favicon + Title + Move + Close
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(headerContainerColor)
                        .padding(start = 8.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (tab.isPrivate) {
                                    aegisColors.privateAccent.copy(alpha = 0.2f)
                                } else {
                                    colorScheme.surface.copy(alpha = 0.72f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!tab.isStartPage && faviconImage != null) {
                            Image(
                                bitmap = faviconImage,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        } else {
                            Icon(
                                imageVector = if (tab.isPrivate) Icons.Outlined.PrivacyTip else Icons.Filled.Public,
                                contentDescription = null,
                                tint = activeAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onMoveGroup,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("move_tab_group_${tab.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DriveFileMove,
                            contentDescription = stringResource(R.string.tab_group_move_to),
                            tint = groupAccent ?: colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("close_tab_${tab.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.action_close_tab),
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = colorScheme.outlineVariant.copy(alpha = 0.7f)
                )

                // Live Webpage / Start Page Content Preview Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            if (tab.isPrivate) {
                                aegisColors.privateContainer.copy(alpha = 0.45f)
                            } else {
                                colorScheme.surfaceContainerLowest
                            }
                        )
                ) {
                    when {
                        !tab.isStartPage && previewImage != null -> {
                            Image(
                                bitmap = previewImage,
                                contentDescription = titleText,
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.TopCenter,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("tab_preview_image_${tab.id}")
                            )

                            // Bottom readability scrim so badges stand out on light & dark pages
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.68f)
                                            )
                                        )
                                    )
                            )
                        }

                        tab.isStartPage -> {
                            StartPageMiniPreview(
                                isPrivate = tab.isPrivate,
                                homeBgImage = homeBgImage,
                                homeBackgroundDimAlpha = homeBackgroundDimAlpha,
                                accentColor = activeAccent,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        else -> {
                            // Webpage fallback while initial snapshot is being generated
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                colorScheme.surfaceContainerLow,
                                                colorScheme.surfaceContainer
                                            )
                                        )
                                    )
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (tab.isLoading) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            color = activeAccent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Surface(
                                            shape = CircleShape,
                                            color = activeAccent.copy(alpha = 0.14f)
                                        ) {
                                            Icon(
                                                imageVector = if (tab.isPrivate) {
                                                    Icons.Outlined.PrivacyTip
                                                } else {
                                                    Icons.Filled.Public
                                                },
                                                contentDescription = null,
                                                tint = activeAccent,
                                                modifier = Modifier
                                                    .padding(10.dp)
                                                    .size(20.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = host.ifBlank { titleText },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Metadata Overlay Row (host pill, blocked badge, active indicator)
                    if (dropHintText == null) {
                        val hasDarkBackdrop = (!tab.isStartPage && previewImage != null) ||
                            (tab.isStartPage && homeBgImage != null)
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 7.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (host.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (hasDarkBackdrop) {
                                            Color.Black.copy(alpha = 0.58f)
                                        } else {
                                            colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
                                        }
                                    ) {
                                        Text(
                                            text = host,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 10.sp
                                            ),
                                            color = if (hasDarkBackdrop) Color.White else colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (tab.blockedCountOnPage > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = aegisColors.shieldActiveContainer.copy(alpha = 0.92f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Shield,
                                                contentDescription = null,
                                                tint = aegisColors.shieldActive,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = "${tab.blockedCountOnPage}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = aegisColors.shieldActive
                                            )
                                        }
                                    }
                                }
                            }

                            if (isActive) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = activeAccent
                                ) {
                                    Text(
                                        text = "表示中",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (dropHintText != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(50),
                    color = aegisColors.shieldActive
                ) {
                    Text(
                        text = dropHintText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StartPageMiniPreview(
    isPrivate: Boolean,
    homeBgImage: androidx.compose.ui.graphics.ImageBitmap?,
    homeBackgroundDimAlpha: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors

    Box(modifier = modifier) {
        if (homeBgImage != null && !isPrivate) {
            Image(
                bitmap = homeBgImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = homeBackgroundDimAlpha.coerceIn(0f, 0.75f)))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (isPrivate) {
                                listOf(
                                    aegisColors.privateContainer.copy(alpha = 0.85f),
                                    colorScheme.background
                                )
                            } else {
                                listOf(
                                    colorScheme.surfaceContainerLow,
                                    colorScheme.background
                                )
                            }
                        )
                    )
            )
        }

        // Mini visual Start Page representation (emblem + mini search bar + speed dial placeholders)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = if (homeBgImage != null && !isPrivate) 0.28f else 0.16f)
            ) {
                Icon(
                    imageVector = if (isPrivate) Icons.Outlined.PrivacyTip else Icons.Filled.Shield,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mini search pill
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp),
                shape = RoundedCornerShape(11.dp),
                color = if (homeBgImage != null && !isPrivate) {
                    Color.Black.copy(alpha = 0.42f)
                } else {
                    colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
                },
                border = BorderStroke(
                    width = 0.6.dp,
                    color = if (homeBgImage != null && !isPrivate) {
                        Color.White.copy(alpha = 0.28f)
                    } else {
                        colorScheme.outlineVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = if (homeBgImage != null && !isPrivate) {
                            Color.White.copy(alpha = 0.8f)
                        } else {
                            colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = if (isPrivate) "プライベート検索" else "MrRブラウザ",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (homeBgImage != null && !isPrivate) {
                            Color.White.copy(alpha = 0.85f)
                        } else {
                            colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mini speed-dial grid dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (homeBgImage != null && !isPrivate) {
                                    Color.White.copy(alpha = 0.22f)
                                } else {
                                    colorScheme.surfaceContainerHigh.copy(alpha = 0.75f)
                                }
                            )
                    )
                }
            }
        }
    }
}
