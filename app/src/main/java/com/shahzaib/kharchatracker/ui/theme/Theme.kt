package com.shahzaib.kharchatracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Emerald40,
    onPrimary = Color.White,
    primaryContainer = EmeraldLight,
    onPrimaryContainer = EmeraldDeep,
    secondary = Gold40,
    onSecondary = Color.White,
    secondaryContainer = GoldLight,
    onSecondaryContainer = EmeraldDeep,
    tertiary = IncomeGreen,
    error = ExpenseRed,
    background = AppBackgroundLight,
    onBackground = Color(0xFF1A2B22),
    surface = Color.White,
    onSurface = Color(0xFF1A2B22),
    surfaceVariant = Color(0xFFE8EDE6),
    onSurfaceVariant = Color(0xFF5F6F64)
)

private val DarkColors = darkColorScheme(
    primary = EmeraldOnDark,
    onPrimary = EmeraldDeep,
    primaryContainer = EmeraldDeep,
    onPrimaryContainer = EmeraldLight,
    secondary = Gold40,
    onSecondary = EmeraldDeep,
    secondaryContainer = Color(0xFF3A2F10),
    onSecondaryContainer = GoldLight,
    tertiary = EmeraldOnDark,
    error = Color(0xFFE57373),
    background = AppBackgroundDark,
    onBackground = Color(0xFFE4EEE7),
    surface = CardDark,
    onSurface = Color(0xFFE4EEE7),
    surfaceVariant = Color(0xFF1E2D25),
    onSurfaceVariant = Color(0xFF9DB0A3)
)

@Composable
fun KharchaTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
