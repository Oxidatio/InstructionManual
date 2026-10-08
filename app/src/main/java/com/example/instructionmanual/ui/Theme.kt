package com.example.instructionmanual.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue = Color(0xFF2F6FED)
private val BlueDark = Color(0xFF9EC2FF)
private val Teal = Color(0xFF1E8E7E)

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = Color(0xFF00173A),
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC9EFE7),
    onSecondaryContainer = Color(0xFF00201B),
    background = Color(0xFFF7F8FB),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE2E5EC),
    onSurfaceVariant = Color(0xFF454A52),
    outline = Color(0xFF8A9099),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = BlueDark,
    onPrimary = Color(0xFF00305F),
    primaryContainer = Color(0xFF1B4A8F),
    onPrimaryContainer = Color(0xFFDCE7FF),
    secondary = Color(0xFF9ED4C9),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF005048),
    onSecondaryContainer = Color(0xFFC9EFE7),
    background = Color(0xFF121316),
    onBackground = Color(0xFFE3E5E9),
    surface = Color(0xFF1B1D21),
    onSurface = Color(0xFFE3E5E9),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7CF),
    outline = Color(0xFF8D9199),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
)

@Composable
fun InstructionManualTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
