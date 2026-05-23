package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    secondary = AmberSecondaryDark,
    tertiary = MintTertiaryDark,
    background = CosmicForestBackground,
    surface = SacredDeepSurface,
    onPrimary = CosmicForestBackground,
    onSecondary = CosmicForestBackground,
    onTertiary = CosmicForestBackground,
    onBackground = GoldenTextDark,
    onSurface = GoldenTextDark,
    surfaceVariant = SacredDeepSurface,
    onSurfaceVariant = GoldenTextDark
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    secondary = AmberSecondary,
    tertiary = MintTertiary,
    background = WarmSandBackground,
    surface = ParchmentSurface,
    onPrimary = WarmSandBackground,
    onSecondary = WarmSandBackground,
    onTertiary = WarmSandBackground,
    onBackground = ForestDarkText,
    onSurface = ForestDarkText,
    surfaceVariant = ParchmentSurface,
    onSurfaceVariant = ForestDarkText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamicColor false by default to showcase the hand-crafted Emerald theme on all devices!
    dynamicColor: Boolean = false,
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
