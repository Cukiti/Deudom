package com.deudom.deudom.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = GoldPremium,
    secondary = BronzePremium,
    tertiary = GoldAccent,
    background = NavyBackground,
    surface = NavyDeep,
    onPrimary = NavyBackground,
    onSecondary = NavyBackground,
    onTertiary = NavyBackground,
    onBackground = WhitePrimary,
    onSurface = WhitePrimary,
)

private val LightColorScheme = lightColorScheme(
    primary = NavyDeep,
    secondary = GoldPremium,
    tertiary = BronzePremium,
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = NavyDeep,
    onSurface = NavyDeep,
)

private val ForestColorScheme = lightColorScheme(
    primary = ForestGreen,
    secondary = ForestLight,
    tertiary = GoldPremium,
    background = ForestBackground,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = ForestDark,
    onSurface = ForestDark
)

private val OceanColorScheme = lightColorScheme(
    primary = OceanBlue,
    secondary = OceanLight,
    tertiary = GoldPremium,
    background = OceanBackground,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = OceanDark,
    onSurface = OceanDark
)

@Composable
fun DeudomTheme(
    themeId: String = "default",
    darkTheme: Boolean = isSystemInDarkTheme(),
    iconPackVersion: Int = 0, // Dummy parameter to trigger recomposition
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to keep the premium Navy/Gold identity
    content: @Composable () -> Unit
) {
    val colorScheme = when(themeId) {
        "theme_forest" -> ForestColorScheme
        "theme_ocean" -> OceanColorScheme
        "theme_dark_gold" -> DarkColorScheme
        else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme && themeId != "theme_dark_gold"
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
