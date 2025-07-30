package com.pauwma.glyphbeat.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NothingDarkColorScheme = darkColorScheme(
    primary = NothingRed,
    onPrimary = NothingWhite,
    secondary = NothingMediumGrey,
    onSecondary = NothingWhite,
    tertiary = NothingLightGrey,
    onTertiary = NothingBlack,
    background = NothingBlack,
    onBackground = NothingWhite,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceDark,
    outline = OutlineDark,
    error = NothingRed,
    onError = NothingWhite
)

@Composable
fun NothingAndroidSDKDemoTheme(
    darkTheme: Boolean = true, // Force dark theme always
    // Disable dynamic color to maintain Nothing brand consistency
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Always use dark color scheme - ignore system theme and dynamic colors
    val colorScheme = NothingDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}