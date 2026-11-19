package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.local.ThemeMode

@Immutable
data class AegisExtendedColors(
    val shieldActive: Color,
    val shieldActiveContainer: Color,
    val shieldPaused: Color,
    val shieldPausedContainer: Color,
    val blockedAccent: Color,
    val blockedContainer: Color,
    val privateAccent: Color,
    val privateContainer: Color,
    val trackerAccent: Color,
    val urlBarSurface: Color,
    val cardBorder: Color
)

private val DarkExtendedColors = AegisExtendedColors(
    shieldActive = Emerald500,
    shieldActiveContainer = EmeraldContainerDark,
    shieldPaused = AmberWarnDark,
    shieldPausedContainer = AmberContainerDark,
    blockedAccent = CrimsonBlockDark,
    blockedContainer = CrimsonContainerDark,
    privateAccent = PrivatePurpleDark,
    privateContainer = PrivateContainerDark,
    trackerAccent = CyanInfoDark,
    urlBarSurface = Obsidian800,
    cardBorder = Obsidian600
)

private val LightExtendedColors = AegisExtendedColors(
    shieldActive = Emerald700,
    shieldActiveContainer = EmeraldContainerLight,
    shieldPaused = AmberWarnLight,
    shieldPausedContainer = AmberContainerLight,
    blockedAccent = CrimsonBlockLight,
    blockedContainer = CrimsonContainerLight,
    privateAccent = PrivatePurpleLight,
    privateContainer = PrivateContainerLight,
    trackerAccent = CyanInfoLight,
    urlBarSurface = Alabaster200,
    cardBorder = Alabaster300
)

val LocalAegisColors = staticCompositionLocalOf { DarkExtendedColors }

private val DarkColorScheme = darkColorScheme(
    primary = Emerald500,
    onPrimary = Obsidian950,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = Emerald400,
    secondary = CyanInfoDark,
    onSecondary = Obsidian950,
    secondaryContainer = Obsidian700,
    onSecondaryContainer = SlateLightText,
    tertiary = PrivatePurpleDark,
    onTertiary = Obsidian950,
    tertiaryContainer = PrivateContainerDark,
    onTertiaryContainer = Color(0xFFDDD6FE),
    background = Obsidian950,
    onBackground = SlateLightText,
    surface = Obsidian900,
    onSurface = SlateLightText,
    surfaceVariant = Obsidian800,
    onSurfaceVariant = SlateMutedText,
    surfaceContainerLowest = Obsidian950,
    surfaceContainerLow = Obsidian900,
    surfaceContainer = Obsidian800,
    surfaceContainerHigh = Obsidian700,
    surfaceContainerHighest = Obsidian600,
    outline = Obsidian600,
    outlineVariant = Obsidian700,
    error = CrimsonBlockDark,
    onError = Obsidian950,
    errorContainer = CrimsonContainerDark,
    onErrorContainer = Color(0xFFFECACA)
)

private val LightColorScheme = lightColorScheme(
    primary = Emerald700,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = Color(0xFF004D39),
    secondary = CyanInfoLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF075985),
    tertiary = PrivatePurpleLight,
    onTertiary = Color.White,
    tertiaryContainer = PrivateContainerLight,
    onTertiaryContainer = Color(0xFF4C1D95),
    background = Alabaster50,
    onBackground = SlateInk900,
    surface = Alabaster100,
    onSurface = SlateInk900,
    surfaceVariant = Alabaster200,
    onSurfaceVariant = SlateInk500,
    surfaceContainerLowest = Alabaster100,
    surfaceContainerLow = Alabaster50,
    surfaceContainer = Alabaster200,
    surfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerHighest = Alabaster300,
    outline = Alabaster300,
    outlineVariant = Alabaster200,
    error = CrimsonBlockLight,
    onError = Color.White,
    errorContainer = CrimsonContainerLight,
    onErrorContainer = Color(0xFF991B1B)
)

val AegisShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AegisBrowserTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAegisColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AegisTypography,
            shapes = AegisShapes,
            content = content
        )
    }
}

object AegisTheme {
    val colors: AegisExtendedColors
        @Composable
        get() = LocalAegisColors.current
}
