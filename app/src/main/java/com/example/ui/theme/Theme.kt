package com.example.ui.theme

import android.content.Context
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.util.findActivity

private const val THEME_PREFS_NAME = "whereditgo_theme_prefs"
private const val KEY_SURFACE_MODE = "surface_mode"
private const val KEY_COLOR_PRESET = "color_preset"
private const val KEY_GRADIENT_PRESET = "gradient_preset"
private const val KEY_GRADIENT_DIR = "gradient_direction"
private const val KEY_CUSTOM_START = "custom_start_argb"
private const val KEY_CUSTOM_END = "custom_end_argb"
private const val KEY_INTENSITY = "gradient_intensity"

object ThemePreferencesManager {
    private var loaded = false

    fun loadIfNeeded(context: Context) {
        if (loaded) return
        loaded = true
        val prefs = context.applicationContext.getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE)
        val surfaceId = prefs.getString(KEY_SURFACE_MODE, ThemeSurfaceMode.OBSIDIAN.id)
        val colorId = prefs.getString(KEY_COLOR_PRESET, ThemeColorPreset.VIOLET_CYAN.id)
        val gradId = prefs.getString(KEY_GRADIENT_PRESET, GradientPreset.NEBULA_PULSE.id)
        val dirId = prefs.getString(KEY_GRADIENT_DIR, GradientDirection.DIAGONAL.id)
        val startArgb = prefs.getInt(KEY_CUSTOM_START, Color(0xFF8B5CF6).toArgb())
        val endArgb = prefs.getInt(KEY_CUSTOM_END, Color(0xFF38BDF8).toArgb())
        val intensity = prefs.getFloat(KEY_INTENSITY, 0.28f)

        DynamicThemeState.surfaceMode = ThemeSurfaceMode.entries.firstOrNull { it.id == surfaceId } ?: ThemeSurfaceMode.OBSIDIAN
        DynamicThemeState.colorPreset = ThemeColorPreset.entries.firstOrNull { it.id == colorId } ?: ThemeColorPreset.VIOLET_CYAN
        DynamicThemeState.gradientPreset = GradientPreset.entries.firstOrNull { it.id == gradId } ?: GradientPreset.NEBULA_PULSE
        DynamicThemeState.gradientDirection = GradientDirection.entries.firstOrNull { it.id == dirId } ?: GradientDirection.DIAGONAL
        DynamicThemeState.customStartColor = Color(startArgb)
        DynamicThemeState.customEndColor = Color(endArgb)
        DynamicThemeState.gradientIntensity = intensity.coerceIn(0.05f, 0.60f)
    }

    fun save(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SURFACE_MODE, DynamicThemeState.surfaceMode.id)
            .putString(KEY_COLOR_PRESET, DynamicThemeState.colorPreset.id)
            .putString(KEY_GRADIENT_PRESET, DynamicThemeState.gradientPreset.id)
            .putString(KEY_GRADIENT_DIR, DynamicThemeState.gradientDirection.id)
            .putInt(KEY_CUSTOM_START, DynamicThemeState.customStartColor.toArgb())
            .putInt(KEY_CUSTOM_END, DynamicThemeState.customEndColor.toArgb())
            .putFloat(KEY_INTENSITY, DynamicThemeState.gradientIntensity)
            .apply()
    }

    fun applyThemePreset(
        context: Context,
        surfaceMode: ThemeSurfaceMode = DynamicThemeState.surfaceMode,
        colorPreset: ThemeColorPreset = DynamicThemeState.colorPreset,
        gradientPreset: GradientPreset = DynamicThemeState.gradientPreset,
        gradientDirection: GradientDirection = DynamicThemeState.gradientDirection,
        customStart: Color = DynamicThemeState.customStartColor,
        customEnd: Color = DynamicThemeState.customEndColor,
        intensity: Float = DynamicThemeState.gradientIntensity
    ) {
        DynamicThemeState.surfaceMode = surfaceMode
        DynamicThemeState.colorPreset = colorPreset
        DynamicThemeState.gradientPreset = gradientPreset
        DynamicThemeState.gradientDirection = gradientDirection
        DynamicThemeState.customStartColor = customStart
        DynamicThemeState.customEndColor = customEnd
        DynamicThemeState.gradientIntensity = intensity.coerceIn(0.05f, 0.60f)
        save(context)
    }
}

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = !DynamicThemeState.surfaceMode.isLight,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        ThemePreferencesManager.loadIfNeeded(context)
    }

    val isLight = DynamicThemeState.surfaceMode.isLight
    val colorScheme = if (isLight) {
        lightColorScheme(
            primary = ElectricViolet,
            onPrimary = Color.White,
            primaryContainer = DeepVioletContainer,
            onPrimaryContainer = ElectricViolet,
            secondary = ElectricBlue,
            onSecondary = Color.White,
            secondaryContainer = DeepBlueContainer,
            onSecondaryContainer = ElectricBlue,
            tertiary = MintSuccess,
            onTertiary = Color.White,
            background = DeepCharcoal,
            onBackground = TextPrimary,
            surface = ElevatedSlate,
            onSurface = TextPrimary,
            surfaceVariant = CardSlate,
            onSurfaceVariant = TextSecondary,
            outline = CardBorderSlate,
            error = CoralError,
            onError = Color.White
        )
    } else {
        darkColorScheme(
            primary = ElectricViolet,
            onPrimary = Color.White,
            primaryContainer = DeepVioletContainer,
            onPrimaryContainer = SoftViolet,
            secondary = ElectricBlue,
            onSecondary = DeepCharcoal,
            secondaryContainer = DeepBlueContainer,
            onSecondaryContainer = ElectricBlue,
            tertiary = MintSuccess,
            onTertiary = DeepCharcoal,
            background = DeepCharcoal,
            onBackground = TextPrimary,
            surface = ElevatedSlate,
            onSurface = TextPrimary,
            surfaceVariant = CardSlate,
            onSurfaceVariant = TextSecondary,
            outline = CardBorderSlate,
            error = CoralError,
            onError = Color.White
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val currentBg = DeepCharcoal
        val currentNav = ElevatedSlate
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            window.statusBarColor = currentBg.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = currentNav.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = isLight
            insetsController.isAppearanceLightNavigationBars = isLight
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
