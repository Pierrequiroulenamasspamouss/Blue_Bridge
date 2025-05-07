package com.wellconnect.wellmonitoring.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Light green theme colors
private val LightGreenColorScheme = lightColorScheme(
    primary = Color(0xFF1B5E20),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB9F6CA),
    onPrimaryContainer = Color(0xFF002200),
    secondary = Color(0xFF66BB6A),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Color(0xFF002200),
    tertiary = Color(0xFF2E7D32),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFAED581),
    onTertiaryContainer = Color(0xFF002200),
    error = Color(0xFFB71C1C),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFE8F5E9),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFDCE5DC),
    onSurfaceVariant = Color(0xFF414941),
    outline = Color(0xFF72796F),
    outlineVariant = Color(0xFFC1C9BF),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2E312E),
    inverseOnSurface = Color(0xFFECF3EC),
    inversePrimary = Color(0xFF81C784)
)

// Dark green theme colors
private val DarkGreenColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF003A03),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFB9F6CA),
    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF003A03),
    secondaryContainer = Color(0xFF2E7D32),
    onSecondaryContainer = Color(0xFFC8E6C9),
    tertiary = Color(0xFFAED581),
    onTertiary = Color(0xFF003A03),
    tertiaryContainer = Color(0xFF1B5E20),
    onTertiaryContainer = Color(0xFFAED581),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFFB71C1C),
    onErrorContainer = Color(0xFFFFCDD2),
    background = Color(0xFF1A1C19),
    onBackground = Color(0xFFE2E3DE),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE2E3DE),
    surfaceVariant = Color(0xFF414941),
    onSurfaceVariant = Color(0xFFC1C9BF),
    outline = Color(0xFF8B938A),
    outlineVariant = Color(0xFF414941),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE2E3DE),
    inverseOnSurface = Color(0xFF191C19),
    inversePrimary = Color(0xFF1B5E20)
)

// Function to get green theme color scheme based on dark mode
fun getGreenColorScheme(isDarkTheme: Boolean): ColorScheme {
    return if (isDarkTheme) DarkGreenColorScheme else LightGreenColorScheme
} 