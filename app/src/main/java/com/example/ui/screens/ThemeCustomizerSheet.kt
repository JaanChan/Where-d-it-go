package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CUSTOM_SWATCH_COLORS
import com.example.ui.theme.CardBorderSlate
import com.example.ui.theme.CardSlate
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.DynamicThemeState
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.GradientDirection
import com.example.ui.theme.GradientPreset
import com.example.ui.theme.SoftViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThemeColorPreset
import com.example.ui.theme.ThemePreferencesManager
import com.example.ui.theme.ThemeSurfaceMode
import com.example.ui.theme.buildAccentGradientBrush
import com.example.ui.theme.buildAppBackgroundBrush

@Composable
fun ThemeCustomizerDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = ElevatedSlate,
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, buildAccentGradientBrush()),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .testTag("theme_customizer_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(buildAccentGradientBrush()),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Theme & Gradient Studio",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Customize colors, surface mode & live gradients",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Theme Studio",
                            tint = TextSecondary
                        )
                    }
                }

                ThemeCustomizerContent()

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricViolet,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("apply_theme_done_button")
                ) {
                    Text("Done", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ThemeCustomizerCard(
    modifier: Modifier = Modifier
) {
    Surface(
        color = CardSlate,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, buildAccentGradientBrush()),
        modifier = modifier
            .fillMaxWidth()
            .testTag("theme_customizer_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(buildAccentGradientBrush()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Theme, Color & Gradient Studio",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Personalize your memory vault's theme mode, accent colors, and custom gradient flow",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            ThemeCustomizerContent()
        }
    }
}

@Composable
fun ThemeCustomizerContent() {
    val context = LocalContext.current
    val currentSurface = DynamicThemeState.surfaceMode
    val currentColor = DynamicThemeState.colorPreset
    val currentGradient = DynamicThemeState.gradientPreset
    val currentDirection = DynamicThemeState.gradientDirection
    val customStart = DynamicThemeState.customStartColor
    val customEnd = DynamicThemeState.customEndColor
    val intensity = DynamicThemeState.gradientIntensity

    // 1. Live Interactive Preview Card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(buildAppBackgroundBrush())
            .border(1.5.dp, buildAccentGradientBrush(), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${currentSurface.label} • ${currentColor.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = SoftViolet,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Gradient: ${currentGradient.label} (${currentDirection.label})",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(buildAccentGradientBrush())
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Live Preview",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // 2. Surface Mode (Dark, AMOLED, Cosmic, Daylight Light)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "1. Base Theme Mode",
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ThemeSurfaceMode.entries.forEach { mode ->
                val selected = currentSurface == mode
                Surface(
                    onClick = {
                        ThemePreferencesManager.applyThemePreset(
                            context = context,
                            surfaceMode = mode
                        )
                    },
                    color = mode.cardSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) ElectricViolet else mode.cardBorder
                    ),
                    modifier = Modifier.testTag("theme_surface_${mode.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(mode.background)
                                .border(1.dp, mode.textSecondary, CircleShape)
                        )
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = mode.textPrimary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // 3. Accent Color Palettes
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "2. Accent Color Theme",
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ThemeColorPreset.entries.forEach { preset ->
                val selected = currentColor == preset
                Surface(
                    onClick = {
                        ThemePreferencesManager.applyThemePreset(
                            context = context,
                            colorPreset = preset
                        )
                    },
                    color = if (selected) preset.primary.copy(alpha = 0.18f) else DeepCharcoal,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) preset.primary else CardBorderSlate
                    ),
                    modifier = Modifier.testTag("theme_color_${preset.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(preset.primary, preset.secondary)
                                    )
                                )
                        )
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // 4. Gradient Presets
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "3. Theme Gradient Style",
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GradientPreset.entries.forEach { grad ->
                val selected = currentGradient == grad
                val previewColors = if (grad == GradientPreset.CUSTOM) {
                    listOf(customStart, customEnd)
                } else {
                    listOf(grad.startColor, grad.midColor, grad.endColor)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(previewColors))
                        .border(
                            width = if (selected) 2.5.dp else 1.dp,
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            ThemePreferencesManager.applyThemePreset(
                                context = context,
                                gradientPreset = grad
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("gradient_preset_${grad.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = grad.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // 5. Custom Gradient Color Mixer (Start & End Swatches)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "4. Custom Gradient Color Mixer (Tap to Mix Your Own)",
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Start Color",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CUSTOM_SWATCH_COLORS.forEachIndexed { idx, swatch ->
                val isSelected = currentGradient == GradientPreset.CUSTOM && customStart == swatch
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                        .clickable {
                            ThemePreferencesManager.applyThemePreset(
                                context = context,
                                gradientPreset = GradientPreset.CUSTOM,
                                customStart = swatch
                            )
                        }
                        .testTag("custom_grad_start_$idx"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = "End Color",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CUSTOM_SWATCH_COLORS.forEachIndexed { idx, swatch ->
                val isSelected = currentGradient == GradientPreset.CUSTOM && customEnd == swatch
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                        .clickable {
                            ThemePreferencesManager.applyThemePreset(
                                context = context,
                                gradientPreset = GradientPreset.CUSTOM,
                                customEnd = swatch
                            )
                        }
                        .testTag("custom_grad_end_$idx"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    // 6. Gradient Direction & Angle
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "5. Gradient Direction & Flow",
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GradientDirection.entries.forEach { dir ->
                val selected = currentDirection == dir
                FilterChip(
                    selected = selected,
                    onClick = {
                        ThemePreferencesManager.applyThemePreset(
                            context = context,
                            gradientDirection = dir
                        )
                    },
                    label = { Text(dir.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricViolet.copy(alpha = 0.25f),
                        selectedLabelColor = SoftViolet
                    ),
                    modifier = Modifier.testTag("gradient_dir_${dir.id}")
                )
            }
        }
    }

    // 7. Gradient Glow Intensity Slider
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "6. Background Gradient Glow Intensity",
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${(intensity * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = ElectricBlue,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = intensity,
            onValueChange = { newVal ->
                ThemePreferencesManager.applyThemePreset(
                    context = context,
                    intensity = newVal
                )
            },
            valueRange = 0.05f..0.55f,
            colors = SliderDefaults.colors(
                thumbColor = ElectricViolet,
                activeTrackColor = ElectricBlue,
                inactiveTrackColor = CardBorderSlate
            ),
            modifier = Modifier.testTag("gradient_intensity_slider")
        )
    }
}
