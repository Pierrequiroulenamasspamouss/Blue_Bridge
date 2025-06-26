package com.bluebridgeapp.bluebridge.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.bluebridgeapp.bluebridge.ui.theme.ThemeName.CYAN
import com.bluebridgeapp.bluebridge.ui.theme.ThemeName.DARK
import com.bluebridgeapp.bluebridge.ui.theme.ThemeName.GREEN
import com.bluebridgeapp.bluebridge.ui.theme.ThemeName.LIGHT
import com.bluebridgeapp.bluebridge.ui.theme.ThemeName.ORANGE
import com.bluebridgeapp.bluebridge.ui.theme.ThemeName.PINK
import com.bluebridgeapp.bluebridge.ui.theme.ThemeName.PURPLE
import com.bluebridgeapp.bluebridge.ui.theme.ThemeName.RED
import com.bluebridgeapp.bluebridge.ui.theme.ThemeName.SYSTEM_DEFAULT
import com.bluebridgeapp.bluebridge.ui.theme.ThemeName.TAN
import com.bluebridgeapp.bluebridge.ui.theme.ThemeName.YELLOW



enum class ThemeName(val value: Int) {
    SYSTEM_DEFAULT(0),
    LIGHT(1),
    DARK(2),
    GREEN(3),
    PINK(4),
    RED(5),
    PURPLE(6),
    YELLOW(7),
    TAN(8),
    ORANGE(9),
    CYAN(10)
}

fun getThemeNameFromValue(value: Int): ThemeName {
    return ThemeName.values().firstOrNull { it.value == value } ?: ThemeName.SYSTEM_DEFAULT
}

fun getColorScheme(themeName: ThemeName, isDarkTheme: Boolean): ColorScheme {
    return when (themeName) {
        PINK -> getPinkColorScheme(isDarkTheme)
        RED -> getRedColorScheme(isDarkTheme)
        PURPLE -> getPurpleColorScheme(isDarkTheme)
        YELLOW -> getYellowColorScheme(isDarkTheme)
        TAN -> getTanColorScheme(isDarkTheme)
        ORANGE -> getOrangeColorScheme(isDarkTheme)
        CYAN -> getCyanColorScheme(isDarkTheme)
        SYSTEM_DEFAULT -> if (isDarkTheme) DarkColorScheme else LightColorScheme
        LIGHT -> LightColorScheme
        DARK -> DarkColorScheme
        GREEN -> getGreenColorScheme(isDarkTheme)
    }
}

fun getGreenColorScheme(isDarkTheme: Boolean): ColorScheme {
    return if (isDarkTheme) DarkGreenColorScheme else LightGreenColorScheme
}

// Green Theme
private val LightGreenColorScheme = lightColorScheme(
    primary = GreenLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F5E9),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFF81C784),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFA5D6A7),
    onSecondaryContainer = Color(0xFF1B5E20),
    tertiary = Color(0xFF66BB6A),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFA5D6A7),
    onTertiaryContainer = Color(0xFF1B5E20),
    background = Color(0xFFE8F5E9),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

private val DarkGreenColorScheme = darkColorScheme(
    primary = GreenDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF2E7D32),
    onPrimaryContainer = Color(0xFFE8F5E9),
    secondary = Color(0xFF388E3C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E7D32),
    onSecondaryContainer = Color(0xFFE8F5E9),
    tertiary = Color(0xFF43A047),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF2E7D32),
    onTertiaryContainer = Color(0xFFE8F5E9),
    background = Color(0xFF121212), // Dark background for better contrast
    onBackground = Color.White,
    surface = Color(0xFF1A1D1A), // Slightly different surface color
    onSurface = Color.White
)



fun getOrangeColorScheme(isDarkTheme: Boolean): ColorScheme {
    return if (isDarkTheme) DarkOrangeColorScheme else LightOrangeColorScheme
}

fun getPinkColorScheme(isDarkTheme: Boolean): ColorScheme {
    return if (isDarkTheme) DarkPinkColorScheme else LightPinkColorScheme
}

fun getRedColorScheme(isDarkTheme: Boolean): ColorScheme {
    return if (isDarkTheme) DarkRedColorScheme else LightRedColorScheme
}

fun getPurpleColorScheme(isDarkTheme: Boolean): ColorScheme {
    return if (isDarkTheme) DarkPurpleColorScheme else LightPurpleColorScheme
}

fun getYellowColorScheme(isDarkTheme: Boolean): ColorScheme {
    return if (isDarkTheme) DarkYellowColorScheme else LightYellowColorScheme
}

fun getTanColorScheme(isDarkTheme: Boolean): ColorScheme {
    return if (isDarkTheme) DarkTanColorScheme else LightTanColorScheme
}

fun getCyanColorScheme(isDarkTheme: Boolean): ColorScheme {
    return if (isDarkTheme) DarkCyanColorScheme else LightCyanColorScheme
}

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
val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color.Black,
    surface = Color(0xFF1E1E1E),
    onBackground = Color.White,
    onSurface = Color.White,
)

val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color.Black,
    surface = Color(0xFF1E1E1E),
    onBackground = Color.White,
    onSurface = Color.White,
)


@Composable
fun My_second_appTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
