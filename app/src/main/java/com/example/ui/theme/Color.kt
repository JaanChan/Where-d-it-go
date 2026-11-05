package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class ThemeSurfaceMode(
    val id: String,
    val label: String,
    val background: Color,
    val elevatedSurface: Color,
    val cardSurface: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val isLight: Boolean
) {
    OBSIDIAN(
        id = "OBSIDIAN",
        label = "Obsidian Dark",
        background = Color(0xFF0B0D13),
        elevatedSurface = Color(0xFF141824),
        cardSurface = Color(0xFF1C2232),
        cardBorder = Color(0xFF2B3450),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        textMuted = Color(0xFF64748B),
        isLight = false
    ),
    AMOLED(
        id = "AMOLED",
        label = "AMOLED Black",
        background = Color(0xFF000000),
        elevatedSurface = Color(0xFF0A0C12),
        cardSurface = Color(0xFF11141E),
        cardBorder = Color(0xFF252B3D),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFA1A1AA),
        textMuted = Color(0xFF71717A),
        isLight = false
    ),
    COSMIC(
        id = "COSMIC",
        label = "Cosmic Indigo",
        background = Color(0xFF0D0A1A),
        elevatedSurface = Color(0xFF17132B),
        cardSurface = Color(0xFF211B3A),
        cardBorder = Color(0xFF372E5E),
        textPrimary = Color(0xFFF5F3FF),
        textSecondary = Color(0xFFC4B5FD),
        textMuted = Color(0xFF8B7EC8),
        isLight = false
    ),
    DAYLIGHT(
        id = "DAYLIGHT",
        label = "Daylight Pearl",
        background = Color(0xFFF3F6FC),
        elevatedSurface = Color(0xFFFFFFFF),
        cardSurface = Color(0xFFFFFFFF),
        cardBorder = Color(0xFFD8E0F0),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
        textMuted = Color(0xFF64748B),
        isLight = true
    )
}

enum class ThemeColorPreset(
    val id: String,
    val label: String,
    val primary: Color,
    val softPrimary: Color,
    val primaryContainerDark: Color,
    val primaryContainerLight: Color,
    val secondary: Color,
    val secondaryContainerDark: Color,
    val secondaryContainerLight: Color
) {
    VIOLET_CYAN(
        id = "VIOLET_CYAN",
        label = "Electric Violet",
        primary = Color(0xFF8B5CF6),
        softPrimary = Color(0xFFA78BFA),
        primaryContainerDark = Color(0xFF281C4D),
        primaryContainerLight = Color(0xFFEDE9FE),
        secondary = Color(0xFF38BDF8),
        secondaryContainerDark = Color(0xFF0F2942),
        secondaryContainerLight = Color(0xFFE0F2FE)
    ),
    EMERALD_MINT(
        id = "EMERALD_MINT",
        label = "Emerald Aurora",
        primary = Color(0xFF10B981),
        softPrimary = Color(0xFF34D399),
        primaryContainerDark = Color(0xFF093528),
        primaryContainerLight = Color(0xFFD1FAE5),
        secondary = Color(0xFF2DD4BF),
        secondaryContainerDark = Color(0xFF0D3333),
        secondaryContainerLight = Color(0xFFCCFBF1)
    ),
    SUNSET_CORAL(
        id = "SUNSET_CORAL",
        label = "Sunset Coral",
        primary = Color(0xFFF43F5E),
        softPrimary = Color(0xFFFB7185),
        primaryContainerDark = Color(0xFF421422),
        primaryContainerLight = Color(0xFFFFE4E6),
        secondary = Color(0xFFFB923C),
        secondaryContainerDark = Color(0xFF3D2010),
        secondaryContainerLight = Color(0xFFFFEDD5)
    ),
    OCEAN_SAPPHIRE(
        id = "OCEAN_SAPPHIRE",
        label = "Ocean Sapphire",
        primary = Color(0xFF3B82F6),
        softPrimary = Color(0xFF60A5FA),
        primaryContainerDark = Color(0xFF13284C),
        primaryContainerLight = Color(0xFFDBEAFE),
        secondary = Color(0xFF06B6D4),
        secondaryContainerDark = Color(0xFF0C313E),
        secondaryContainerLight = Color(0xFFCFFAFE)
    ),
    ROYAL_GOLD(
        id = "ROYAL_GOLD",
        label = "Royal Gold",
        primary = Color(0xFFF59E0B),
        softPrimary = Color(0xFFFBBF24),
        primaryContainerDark = Color(0xFF3E2709),
        primaryContainerLight = Color(0xFFFEF3C7),
        secondary = Color(0xFFEC4899),
        secondaryContainerDark = Color(0xFF3F132B),
        secondaryContainerLight = Color(0xFFFCE7F3)
    ),
    NEON_SYNTH(
        id = "NEON_SYNTH",
        label = "Neon Magenta",
        primary = Color(0xFFD946EF),
        softPrimary = Color(0xFFE879F9),
        primaryContainerDark = Color(0xFF3B1245),
        primaryContainerLight = Color(0xFFFAE8FF),
        secondary = Color(0xFF818CF8),
        secondaryContainerDark = Color(0xFF1B1F4A),
        secondaryContainerLight = Color(0xFFE0E7FF)
    )
}

enum class GradientDirection(val id: String, val label: String) {
    DIAGONAL("DIAGONAL", "Diagonal Glow"),
    VERTICAL("VERTICAL", "Vertical Wave"),
    HORIZONTAL("HORIZONTAL", "Horizontal Sweep"),
    RADIAL("RADIAL", "Radial Spotlight")
}

enum class GradientPreset(
    val id: String,
    val label: String,
    val startColor: Color,
    val midColor: Color,
    val endColor: Color
) {
    NEBULA_PULSE(
        id = "NEBULA_PULSE",
        label = "Nebula Pulse",
        startColor = Color(0xFF8B5CF6),
        midColor = Color(0xFF4F46E5),
        endColor = Color(0xFF06B6D4)
    ),
    NORTHERN_LIGHTS(
        id = "NORTHERN_LIGHTS",
        label = "Northern Lights",
        startColor = Color(0xFF10B981),
        midColor = Color(0xFF0D9488),
        endColor = Color(0xFF2563EB)
    ),
    SUNSET_HORIZON(
        id = "SUNSET_HORIZON",
        label = "Sunset Horizon",
        startColor = Color(0xFFF43F5E),
        midColor = Color(0xFFEC4899),
        endColor = Color(0xFFF59E0B)
    ),
    CYBERPUNK_GLOW(
        id = "CYBERPUNK_GLOW",
        label = "Cyberpunk Glow",
        startColor = Color(0xFFD946EF),
        midColor = Color(0xFF7C3AED),
        endColor = Color(0xFF38BDF8)
    ),
    GOLDEN_HOUR(
        id = "GOLDEN_HOUR",
        label = "Golden Hour",
        startColor = Color(0xFFF59E0B),
        midColor = Color(0xFFEA580C),
        endColor = Color(0xFFE11D48)
    ),
    ARCTIC_FROST(
        id = "ARCTIC_FROST",
        label = "Arctic Frost",
        startColor = Color(0xFF38BDF8),
        midColor = Color(0xFF3B82F6),
        endColor = Color(0xFF6366F1)
    ),
    CUSTOM(
        id = "CUSTOM",
        label = "Custom Mix",
        startColor = Color(0xFF8B5CF6),
        midColor = Color(0xFF6366F1),
        endColor = Color(0xFF38BDF8)
    )
}

val CUSTOM_SWATCH_COLORS = listOf(
    Color(0xFF8B5CF6), // Violet
    Color(0xFF6366F1), // Indigo
    Color(0xFF3B82F6), // Blue
    Color(0xFF38BDF8), // Sky
    Color(0xFF06B6D4), // Cyan
    Color(0xFF14B8A6), // Teal
    Color(0xFF10B981), // Emerald
    Color(0xFF84CC16), // Lime
    Color(0xFFF59E0B), // Amber
    Color(0xFFFB923C), // Orange
    Color(0xFFF43F5E), // Rose
    Color(0xFFEC4899), // Pink
    Color(0xFFD946EF)  // Fuchsia
)

object DynamicThemeState {
    var surfaceMode by mutableStateOf(ThemeSurfaceMode.OBSIDIAN)
    var colorPreset by mutableStateOf(ThemeColorPreset.VIOLET_CYAN)
    var gradientPreset by mutableStateOf(GradientPreset.NEBULA_PULSE)
    var gradientDirection by mutableStateOf(GradientDirection.DIAGONAL)
    var customStartColor by mutableStateOf(Color(0xFF8B5CF6))
    var customEndColor by mutableStateOf(Color(0xFF38BDF8))
    var gradientIntensity by mutableStateOf(0.28f) // 0.05f .. 0.55f
}

val DeepCharcoal: Color
    get() = DynamicThemeState.surfaceMode.background

val ElevatedSlate: Color
    get() = DynamicThemeState.surfaceMode.elevatedSurface

val CardSlate: Color
    get() = DynamicThemeState.surfaceMode.cardSurface

val CardBorderSlate: Color
    get() = DynamicThemeState.surfaceMode.cardBorder

val ElectricViolet: Color
    get() = DynamicThemeState.colorPreset.primary

val SoftViolet: Color
    get() = if (DynamicThemeState.surfaceMode.isLight) {
        DynamicThemeState.colorPreset.primary
    } else {
        DynamicThemeState.colorPreset.softPrimary
    }

val DeepVioletContainer: Color
    get() = if (DynamicThemeState.surfaceMode.isLight) {
        DynamicThemeState.colorPreset.primaryContainerLight
    } else {
        DynamicThemeState.colorPreset.primaryContainerDark
    }

val ElectricBlue: Color
    get() = DynamicThemeState.colorPreset.secondary

val DeepBlueContainer: Color
    get() = if (DynamicThemeState.surfaceMode.isLight) {
        DynamicThemeState.colorPreset.secondaryContainerLight
    } else {
        DynamicThemeState.colorPreset.secondaryContainerDark
    }

val MintSuccess = Color(0xFF10B981)
val AmberWarning = Color(0xFFF59E0B)
val CoralError = Color(0xFFEF4444)

val TextPrimary: Color
    get() = DynamicThemeState.surfaceMode.textPrimary

val TextSecondary: Color
    get() = DynamicThemeState.surfaceMode.textSecondary

val TextMuted: Color
    get() = DynamicThemeState.surfaceMode.textMuted

fun activeGradientColors(): List<Color> {
    return if (DynamicThemeState.gradientPreset == GradientPreset.CUSTOM) {
        val start = DynamicThemeState.customStartColor
        val end = DynamicThemeState.customEndColor
        val mid = Color(
            red = (start.red + end.red) / 2f,
            green = (start.green + end.green) / 2f,
            blue = (start.blue + end.blue) / 2f,
            alpha = 1f
        )
        listOf(start, mid, end)
    } else {
        listOf(
            DynamicThemeState.gradientPreset.startColor,
            DynamicThemeState.gradientPreset.midColor,
            DynamicThemeState.gradientPreset.endColor
        )
    }
}

fun buildAccentGradientBrush(direction: GradientDirection = DynamicThemeState.gradientDirection): Brush {
    val colors = activeGradientColors()
    return when (direction) {
        GradientDirection.HORIZONTAL -> Brush.horizontalGradient(colors)
        GradientDirection.VERTICAL -> Brush.verticalGradient(colors)
        GradientDirection.RADIAL -> Brush.radialGradient(
            colors = colors,
            center = Offset(300f, 150f),
            radius = 900f
        )
        GradientDirection.DIAGONAL -> Brush.linearGradient(
            colors = colors,
            start = Offset(0f, 0f),
            end = Offset(900f, 900f)
        )
    }
}

fun buildAppBackgroundBrush(): Brush {
    val base = DeepCharcoal
    val intensity = DynamicThemeState.gradientIntensity.coerceIn(0.05f, 0.60f)
    val gradColors = activeGradientColors()
    val topGlow = gradColors.first().copy(alpha = if (DynamicThemeState.surfaceMode.isLight) intensity * 0.45f else intensity)
    val midGlow = gradColors[1].copy(alpha = if (DynamicThemeState.surfaceMode.isLight) intensity * 0.25f else intensity * 0.45f)
    val endGlow = gradColors.last().copy(alpha = if (DynamicThemeState.surfaceMode.isLight) intensity * 0.35f else intensity * 0.65f)

    val blendedTop = blendOver(base, topGlow)
    val blendedMid = blendOver(base, midGlow)
    val blendedEnd = blendOver(base, endGlow)

    return when (DynamicThemeState.gradientDirection) {
        GradientDirection.HORIZONTAL -> Brush.horizontalGradient(listOf(blendedTop, base, blendedEnd))
        GradientDirection.VERTICAL -> Brush.verticalGradient(listOf(blendedTop, blendedMid, base, blendedEnd))
        GradientDirection.RADIAL -> Brush.radialGradient(
            colors = listOf(blendedTop, blendedMid, base),
            center = Offset(400f, 250f),
            radius = 1400f
        )
        GradientDirection.DIAGONAL -> Brush.linearGradient(
            colors = listOf(blendedTop, blendedMid, base, blendedEnd),
            start = Offset(0f, 0f),
            end = Offset(1200f, 1800f)
        )
    }
}

private fun blendOver(background: Color, overlay: Color): Color {
    val a = overlay.alpha
    return Color(
        red = background.red * (1f - a) + overlay.red * a,
        green = background.green * (1f - a) + overlay.green * a,
        blue = background.blue * (1f - a) + overlay.blue * a,
        alpha = 1f
    )
}
