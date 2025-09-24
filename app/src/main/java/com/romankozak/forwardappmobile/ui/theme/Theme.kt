package com.romankozak.forwardappmobile.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// region --- Theme Definitions ---

enum class ThemeName(val displayName: String) {
    DEFAULT("Default"),
    CYBERPUNK("Cyberpunk"),
    SCI_FI("Sci-Fi"),
    DRACULA("Dracula"),
    NORD("Nord"),
    SOLARIZED_DARK("Solarized Dark")
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

data class AppTheme(
    val name: ThemeName,
    val lightColors: ColorScheme,
    val darkColors: ColorScheme
)

data class ThemeSettings(
    val lightThemeName: ThemeName = ThemeName.DEFAULT,
    val darkThemeName: ThemeName = ThemeName.DEFAULT,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

// endregion

// region --- Color Schemes ---

private val DefaultDarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    surfaceContainer = PurpleGrey80.copy(alpha = 0.1f),
)

private val DefaultLightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    surfaceContainer = PurpleGrey40.copy(alpha = 0.1f),
)

private val CyberpunkDarkColorScheme = darkColorScheme(
    primary = CyberNeonCyan,
    secondary = CyberNeonMagenta,
    tertiary = CyberNeonMagenta,
    background = CyberDarkBlue,
    surface = CyberDarkBlue,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = CyberLightGray,
    onSurface = CyberLightGray,
    surfaceContainer = CyberDarkBlue.copy(alpha = 0.8f),
)

private val CyberpunkLightColorScheme = lightColorScheme(
    primary = CyberPink,
    secondary = CyberNeonMagenta,
    tertiary = CyberPink,
    background = CyberLightBlue,
    surface = CyberLightBlue,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = CyberDarkGray,
    onSurface = CyberDarkGray,
    surfaceContainer = CyberLightBlue.copy(alpha = 0.8f),
)

private val SciFiDarkColorScheme = darkColorScheme(
    primary = SciFiCyan,
    secondary = SciFiSilver,
    tertiary = SciFiCyan,
    background = SciFiDeepBlue,
    surface = SciFiDeepBlue,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = SciFiSilver,
    onSurface = SciFiSilver,
    surfaceContainer = SciFiDeepBlue.copy(alpha = 0.8f),
)

private val SciFiLightColorScheme = lightColorScheme(
    primary = SciFiDarkBlue,
    secondary = SciFiMidGray,
    tertiary = SciFiDarkBlue,
    background = SciFiLightBlue,
    surface = SciFiLightBlue,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    surfaceContainer = SciFiLightBlue.copy(alpha = 0.8f),
)

private val DraculaColorScheme = darkColorScheme(
    primary = DraculaPink,
    secondary = DraculaPurple,
    tertiary = DraculaCyan,
    background = DraculaBackground,
    surface = DraculaCurrentLine,
    onPrimary = DraculaForeground,
    onSecondary = DraculaForeground,
    onTertiary = Color.Black,
    onBackground = DraculaForeground,
    onSurface = DraculaForeground,
    surfaceContainer = DraculaCurrentLine.copy(alpha = 0.8f),
)

private val NordColorScheme = darkColorScheme(
    primary = Nord8,
    secondary = Nord3,
    tertiary = Nord11,
    background = Nord0,
    surface = Nord1,
    onPrimary = Color.Black,
    onSecondary = Nord4,
    onTertiary = Color.Black,
    onBackground = Nord4,
    onSurface = Nord4,
    surfaceContainer = Nord1.copy(alpha = 0.8f),
)

private val SolarizedDarkColorScheme = darkColorScheme(
    primary = SolarizedBlue,
    secondary = SolarizedCyan,
    tertiary = SolarizedGreen,
    background = SolarizedBase03,
    surface = SolarizedBase02,
    onPrimary = SolarizedBase0,
    onSecondary = SolarizedBase0,
    onTertiary = SolarizedBase0,
    onBackground = SolarizedBase0,
    onSurface = SolarizedBase0,
    surfaceContainer = SolarizedBase02.copy(alpha = 0.8f),
)

// endregion

object ThemeManager {
    val themes = listOf(
        AppTheme(ThemeName.DEFAULT, DefaultLightColorScheme, DefaultDarkColorScheme),
        AppTheme(ThemeName.CYBERPUNK, CyberpunkLightColorScheme, CyberpunkDarkColorScheme),
        AppTheme(ThemeName.SCI_FI, SciFiLightColorScheme, SciFiDarkColorScheme),
        AppTheme(ThemeName.DRACULA, DefaultLightColorScheme, DraculaColorScheme),
        AppTheme(ThemeName.NORD, DefaultLightColorScheme, NordColorScheme),
        AppTheme(ThemeName.SOLARIZED_DARK, DefaultLightColorScheme, SolarizedDarkColorScheme)
    )

    fun getTheme(name: ThemeName): AppTheme {
        return themes.first { it.name == name }
    }
}

@Composable
fun ForwardAppMobileTheme(
    themeSettings: ThemeSettings = ThemeSettings(),
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (themeSettings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when (useDarkTheme) {
        true -> ThemeManager.getTheme(themeSettings.darkThemeName).darkColors
        false -> ThemeManager.getTheme(themeSettings.lightThemeName).lightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surfaceContainer.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}