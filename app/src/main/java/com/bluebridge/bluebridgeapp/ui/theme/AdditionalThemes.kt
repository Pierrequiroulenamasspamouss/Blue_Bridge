package com.bluebridge.bluebridgeapp.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Pink Theme
private val LightPinkColorScheme = lightColorScheme(
    primary = PinkLight,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFFCE4EC),
    onPrimaryContainer = Color(0xFF442C2E),
    secondary = Color(0xFFF48FB1),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFF8BBD0),
    onSecondaryContainer = Color(0xFF442C2E),
    tertiary = Color(0xFFEC407A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF8BBD0),
    onTertiaryContainer = Color(0xFF442C2E),
    background = Color(0xFFFCE4EC),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

private val DarkPinkColorScheme = darkColorScheme(
    primary = PinkDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF880E4F),
    onPrimaryContainer = Color(0xFFFCE4EC),
    secondary = Color(0xFFAD1457),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF880E4F),
    onSecondaryContainer = Color(0xFFFCE4EC),
    tertiary = Color(0xFFC2185B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF880E4F),
    onTertiaryContainer = Color(0xFFFCE4EC),
    background = Color(0xFF1A1617),
    onBackground = Color.White,
    surface = Color(0xFF1A1617),
    onSurface = Color.White
)

fun getPinkColorScheme(isDarkTheme: Boolean): ColorScheme {
    return if (isDarkTheme) DarkPinkColorScheme else LightPinkColorScheme
}

// Red Theme
private val LightRedColorScheme = lightColorScheme(
    primary = RedLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEBEE),
    onPrimaryContainer = Color(0xFF442C2E),
    secondary = Color(0xFFE57373),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFCDD2),
    onSecondaryContainer = Color(0xFF442C2E),
    tertiary = Color(0xFFEF5350),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFCDD2),
    onTertiaryContainer = Color(0xFF442C2E),
    background = Color(0xFFFFEBEE),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

private val DarkRedColorScheme = darkColorScheme(
    primary = RedDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB71C1C),
    onPrimaryContainer = Color(0xFFFFEBEE),
    secondary = Color(0xFFC62828),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB71C1C),
    onSecondaryContainer = Color(0xFFFFEBEE),
    tertiary = Color(0xFFD32F2F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB71C1C),
    onTertiaryContainer = Color(0xFFFFEBEE),
    background = Color(0xFF1A1617),
    onBackground = Color.White,
    surface = Color(0xFF1A1617),
    onSurface = Color.White
)

fun getRedColorScheme(isDarkTheme: Boolean): ColorScheme {
    return if (isDarkTheme) DarkRedColorScheme else LightRedColorScheme
}

// Purple Theme
private val LightPurpleColorScheme = lightColorScheme(
    primary = PurpleLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE7F6),
    onPrimaryContainer = Color(0xFF311B92),
    secondary = Color(0xFF9575CD),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1C4E9),
    onSecondaryContainer = Color(0xFF311B92),
    tertiary = Color(0xFF7E57C2),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1C4E9),
    onTertiaryContainer = Color(0xFF311B92),
    background = Color(0xFFEDE7F6),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

private val DarkPurpleColorScheme = darkColorScheme(
    primary = PurpleDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4527A0),
    onPrimaryContainer = Color(0xFFEDE7F6),
    secondary = Color(0xFF512DA8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4527A0),
    onSecondaryContainer = Color(0xFFEDE7F6),
    tertiary = Color(0xFF5E35B1),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF4527A0),
    onTertiaryContainer = Color(0xFFEDE7F6),
    background = Color(0xFF1A191D),
    onBackground = Color.White,
    surface = Color(0xFF1A191D),
    onSurface = Color.White
)

fun getPurpleColorScheme(isDarkTheme: Boolean): ColorScheme {
    return if (isDarkTheme) DarkPurpleColorScheme else LightPurpleColorScheme
}

// Yellow Theme
private val LightYellowColorScheme = lightColorScheme(
    primary = YellowLight,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFFFFDE7),
    onPrimaryContainer = Color(0xFF3E2723),
    secondary = Color(0xFFFFD54F),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFFF9C4),
    onSecondaryContainer = Color(0xFF3E2723),
    tertiary = Color(0xFFFFCA28),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFFF9C4),
    onTertiaryContainer = Color(0xFF3E2723),
    background = Color(0xFFFFFDE7),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

private val DarkYellowColorScheme = darkColorScheme(
    primary = YellowDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFF57F17),
    onPrimaryContainer = Color(0xFFFFFDE7),
    secondary = Color(0xFFFBC02D),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFF57F17),
    onSecondaryContainer = Color(0xFFFFFDE7),
    tertiary = Color(0xFFF9A825),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFF57F17),
    onTertiaryContainer = Color(0xFFFFFDE7),
    background = Color(0xFF1D1D1A),
    onBackground = Color.White,
    surface = Color(0xFF1D1D1A),
    onSurface = Color.White
)

fun getYellowColorScheme(isDarkTheme: Boolean): ColorScheme {
    return if (isDarkTheme) DarkYellowColorScheme else LightYellowColorScheme
}

// Tan Theme
private val LightTanColorScheme = lightColorScheme(
    primary = TanLight,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFEFEBE9),
    onPrimaryContainer = Color(0xFF3E2723),
    secondary = Color(0xFFBCAAA4),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFD7CCC8),
    onSecondaryContainer = Color(0xFF3E2723),
    tertiary = Color(0xFFA1887F),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFD7CCC8),
    onTertiaryContainer = Color(0xFF3E2723),
    background = Color(0xFFEFEBE9),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

private val DarkTanColorScheme = darkColorScheme(
    primary = TanDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF5D4037),
    onPrimaryContainer = Color(0xFFEFEBE9),
    secondary = Color(0xFF6D4C41),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF5D4037),
    onSecondaryContainer = Color(0xFFEFEBE9),
    tertiary = Color(0xFF795548),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF5D4037),
    onTertiaryContainer = Color(0xFFEFEBE9),
    background = Color(0xFF1A1917),
    onBackground = Color.White,
    surface = Color(0xFF1A1917),
    onSurface = Color.White
)

fun getTanColorScheme(isDarkTheme: Boolean): ColorScheme {
    return if (isDarkTheme) DarkTanColorScheme else LightTanColorScheme
}

// Orange Theme
private val LightOrangeColorScheme = lightColorScheme(
    primary = OrangeLight,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFFFF3E0),
    onPrimaryContainer = Color(0xFF3E2723),
    secondary = Color(0xFFFFB74D),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFFE0B2),
    onSecondaryContainer = Color(0xFF3E2723),
    tertiary = Color(0xFFFFA726),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFFE0B2),
    onTertiaryContainer = Color(0xFF3E2723),
    background = Color(0xFFFFF3E0),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

private val DarkOrangeColorScheme = darkColorScheme(
    primary = OrangeDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFE65100),
    onPrimaryContainer = Color(0xFFFFF3E0),
    secondary = Color(0xFFEF6C00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE65100),
    onSecondaryContainer = Color(0xFFFFF3E0),
    tertiary = Color(0xFFF57C00),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFE65100),
    onTertiaryContainer = Color(0xFFFFF3E0),
    background = Color(0xFF1D1B17),
    onBackground = Color.White,
    surface = Color(0xFF1D1B17),
    onSurface = Color.White
)

fun getOrangeColorScheme(isDarkTheme: Boolean): ColorScheme {
    return if (isDarkTheme) DarkOrangeColorScheme else LightOrangeColorScheme
}

// Cyan Theme
private val LightCyanColorScheme = lightColorScheme(
    primary = CyanLight,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFE0F7FA),
    onPrimaryContainer = Color(0xFF00363A),
    secondary = Color(0xFF4DD0E1),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFB2EBF2),
    onSecondaryContainer = Color(0xFF00363A),
    tertiary = Color(0xFF26C6DA),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFB2EBF2),
    onTertiaryContainer = Color(0xFF00363A),
    background = Color(0xFFE0F7FA),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

private val DarkCyanColorScheme = darkColorScheme(
    primary = CyanDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF006064),
    onPrimaryContainer = Color(0xFFE0F7FA),
    secondary = Color(0xFF00838F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF006064),
    onSecondaryContainer = Color(0xFFE0F7FA),
    tertiary = Color(0xFF0097A7),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF006064),
    onTertiaryContainer = Color(0xFFE0F7FA),
    background = Color(0xFF171C1D),
    onBackground = Color.White,
    surface = Color(0xFF171C1D),
    onSurface = Color.White
)

fun getCyanColorScheme(isDarkTheme: Boolean): ColorScheme {
    return if (isDarkTheme) DarkCyanColorScheme else LightCyanColorScheme
} 