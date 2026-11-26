package com.cl9tr.obd2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgObsidian = Color(0xFF000000)
val BgShell = Color(0xFF040609)
val CardSlate = Color(0xFF0B0F15)
val CardElevated = Color(0xFF121822)
val AccentCyan = Color(0xFF00D4FF)
val AccentGreen = Color(0xFF22C55E)
val AccentAmber = Color(0xFFF59E0B)
val AccentRed = Color(0xFFEF4444)
val AccentBlue = Color(0xFF0EA5E9)
val AccentViolet = Color(0xFFA78BFA)
val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)

private val DarkScheme = darkColorScheme(
    primary = AccentCyan,
    secondary = AccentGreen,
    tertiary = AccentAmber,
    error = AccentRed,
    background = BgObsidian,
    surface = CardSlate,
    onPrimary = BgObsidian,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun Cl9ObdTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
