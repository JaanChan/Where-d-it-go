package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val WhereDidItGoDarkColorScheme = darkColorScheme(
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

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WhereDidItGoDarkColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
