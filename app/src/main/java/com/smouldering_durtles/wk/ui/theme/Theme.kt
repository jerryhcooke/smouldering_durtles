package com.smouldering_durtles.wk.ui.theme

import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceManager
import com.smouldering_durtles.wk.GlobalSettings
import com.smouldering_durtles.wk.enums.ActiveTheme

private const val THEME_PREF_KEY = "theme"
private const val NIGHT_THEME_PREF_KEY = "nightTheme"
private const val THEME_CUSTOMIZATION_PREFIX = "theme_customizations_"

/**
 * Tracks changes to theme-related preferences so Compose UI reacts to them
 * without an activity restart. The legacy UI recreates the activity instead;
 * here the preference change bumps [version] and every composable that
 * observed the theme through [currentTheme] or [SmoulderingDurtlesTheme]
 * recomposes with the new colors.
 */
object ThemePreferenceState {
    var version: Int by mutableIntStateOf(0)
        private set

    /**
     * Force every theme observer to re-read the preferences. Called
     * automatically by the preference listener in [SmoulderingDurtlesTheme].
     */
    fun refresh() {
        version++
    }

    /**
     * The theme currently selected by the user, with the same day/night
     * decision the legacy app makes: the "theme" preference in day mode and
     * the "nightTheme" preference when the system is in night mode.
     */
    @Composable
    fun currentTheme(): ActiveTheme {
        val darkTheme = isSystemInDarkTheme()
        return remember(darkTheme, version) { GlobalSettings.Display.getTheme() }
    }
}

/**
 * The application theme. The active theme comes from the user's theme
 * preferences (keys "theme" and "nightTheme" via GlobalSettings.Display),
 * not just from the system dark mode, and changes to those preferences or to
 * the per-theme color customizations take effect immediately.
 */
@Composable
fun SmoulderingDurtlesTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    DisposableEffect(context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key != null && (key == THEME_PREF_KEY || key == NIGHT_THEME_PREF_KEY || key.startsWith(THEME_CUSTOMIZATION_PREFIX))) {
                ThemePreferenceState.refresh()
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val activeTheme = ThemePreferenceState.currentTheme()
    val colorScheme = remember(activeTheme) { activeTheme.palette().toColorScheme() }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

/**
 * The legacy palette roles for one theme, taken from the matching style in
 * res/values/styles.xml and the colors it references in res/values/colors.xml.
 * The customizable per-theme color slots (subject types, SRS stages, level
 * progression, Anki mode) are not part of this; they remain available through
 * ActiveTheme exactly as before.
 */
private data class ThemePalette(
    val dark: Boolean,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
)

// Wanikani light: white background, black text, pink toolbar accent.
private val LIGHT_PALETTE = ThemePalette(
    dark = false,
    primary = Color(0xFFE80092), // wkLightToolbarColorBackground
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF202020), // wkLightColorAccent
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF9808F3), // wkLightButtonHighlightColor
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF), // wkLightColorBackground
    onBackground = Color(0xFF000000), // wkLightColorPrimary
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE8E8E8), // wkLightTileColorBackground
    onSurfaceVariant = Color(0xFF202020),
    outline = Color(0xFFC1C0C1),
    outlineVariant = Color(0xFFE8E8E8),
    error = Color(0xFFB00020),
    onError = Color(0xFFFFFFFF),
)

// Wanikani Dark: dark Breeze-like background with the WaniKani purple accent.
private val DARK_WANIKANI_PALETTE = ThemePalette(
    dark = true,
    primary = Color(0xFF925BDC), // wkDarkColorControlActivated
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFBCBCBC), // wkDarkColorPrimary
    onSecondary = Color(0xFF000000),
    tertiary = Color(0xFFD80088), // wkDarkColorWaterfallLine
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFF232629), // wkDarkColorBackground
    onBackground = Color(0xFFBCBCBC), // wkDarkColorPrimary
    surface = Color(0xFF232629),
    onSurface = Color(0xFFBCBCBC),
    surfaceVariant = Color(0xFF1D2023), // wkDarkTileColorBackground
    onSurfaceVariant = Color(0xFFBCBCBC),
    outline = Color(0xFF404040), // wkDarkBrowseLevelButtonColorBackground
    outlineVariant = Color(0xFF1D2023),
    error = Color(0xFFCF6679),
    onError = Color(0xFF000000),
)

private val BLACK_WANIKANI_PALETTE = DARK_WANIKANI_PALETTE.copy(
    background = Color(0xFF000000), // wkBlackColorBackground
    surface = Color(0xFF000000),
)

// Breeze Dark: dark background with the Breeze blue accent.
private val DARK_PALETTE = ThemePalette(
    dark = true,
    primary = Color(0xFF1D99F3), // wkDarkColorWaterfallLine
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFBCBCBC), // wkDarkColorPrimary
    onSecondary = Color(0xFF000000),
    tertiary = Color(0xFF1CDC9A), // Breeze green (SRS stage color)
    onTertiary = Color(0xFF000000),
    background = Color(0xFF232629), // wkDarkColorBackground
    onBackground = Color(0xFFBCBCBC), // wkDarkColorPrimary
    surface = Color(0xFF232629),
    onSurface = Color(0xFFBCBCBC),
    surfaceVariant = Color(0xFF1D2023), // wkDarkTileColorBackground
    onSurfaceVariant = Color(0xFFBCBCBC),
    outline = Color(0xFF404040), // wkDarkBrowseLevelButtonColorBackground
    outlineVariant = Color(0xFF1D2023),
    error = Color(0xFFCF6679),
    onError = Color(0xFF000000),
)

private val BLACK_BREEZE_PALETTE = DARK_PALETTE.copy(
    background = Color(0xFF000000), // wkBlackColorBackground
    surface = Color(0xFF000000),
)

// Nord (Polar Night / Aurora).
private val NORD_PALETTE = ThemePalette(
    dark = true,
    primary = Color(0xFF88C0D0), // wkNordColorAccent
    onPrimary = Color(0xFF2E3440),
    secondary = Color(0xFF81A1C1), // wkNordColorDivider
    onSecondary = Color(0xFF2E3440),
    tertiary = Color(0xFFA3BE8C), // Nord aurora green (SRS stage color)
    onTertiary = Color(0xFF2E3440),
    background = Color(0xFF2E3440), // wkNordColorBackground
    onBackground = Color(0xFFE5E9F0), // wkNordColorPrimary
    surface = Color(0xFF2E3440),
    onSurface = Color(0xFFE5E9F0),
    surfaceVariant = Color(0xFF434C5E),
    onSurfaceVariant = Color(0xFFD8DEE9), // wkNordColorPrimaryInverse
    outline = Color(0xFF434C5E),
    outlineVariant = Color(0xFF3B4252),
    error = Color(0xFFBF616A), // Nord red (Anki incorrect color)
    onError = Color(0xFF2E3440),
)

private fun ActiveTheme.palette(): ThemePalette = when (this) {
    ActiveTheme.LIGHT -> LIGHT_PALETTE
    ActiveTheme.DARK_WANIKANI -> DARK_WANIKANI_PALETTE
    ActiveTheme.BLACK_WANIKANI -> BLACK_WANIKANI_PALETTE
    ActiveTheme.DARK -> DARK_PALETTE
    ActiveTheme.BLACK_BREEZE -> BLACK_BREEZE_PALETTE
    ActiveTheme.NORD_POLAR_NIGHT -> NORD_PALETTE
}

/**
 * Map the legacy palette onto Material 3 roles. Container and surface-tier
 * colors are derived by blending the base roles, so all six themes get a
 * complete, consistent scheme without inventing new brand colors.
 */
private fun ThemePalette.toColorScheme(): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val onContainer = if (dark) onSurface else primary
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = lerp(surface, primary, if (dark) 0.32f else 0.12f),
        onPrimaryContainer = onContainer,
        inversePrimary = lerp(primary, onSurface, 0.4f),
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = lerp(surface, secondary, if (dark) 0.24f else 0.10f),
        onSecondaryContainer = if (dark) onSurface else secondary,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = lerp(surface, tertiary, if (dark) 0.24f else 0.10f),
        onTertiaryContainer = if (dark) onSurface else tertiary,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = primary,
        inverseSurface = onBackground,
        inverseOnSurface = background,
        error = error,
        onError = onError,
        errorContainer = lerp(surface, error, if (dark) 0.24f else 0.12f),
        onErrorContainer = if (dark) onSurface else error,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = Color(0xFF000000),
        surfaceBright = if (dark) lerp(surface, Color(0xFFFFFFFF), 0.08f) else surface,
        surfaceDim = if (dark) lerp(surface, Color(0xFF000000), 0.4f) else lerp(surface, Color(0xFF000000), 0.05f),
        surfaceContainerLowest = if (dark) lerp(surface, Color(0xFF000000), 0.3f) else surface,
        surfaceContainerLow = lerp(surface, onSurface, if (dark) 0.05f else 0.03f),
        surfaceContainer = lerp(surface, onSurface, if (dark) 0.08f else 0.05f),
        surfaceContainerHigh = lerp(surface, onSurface, if (dark) 0.12f else 0.08f),
        surfaceContainerHighest = lerp(surface, onSurface, if (dark) 0.16f else 0.11f),
    )
}
