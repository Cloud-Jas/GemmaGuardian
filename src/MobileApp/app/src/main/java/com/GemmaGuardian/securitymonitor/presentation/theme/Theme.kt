package com.GemmaGuardian.securitymonitor.presentation.theme

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

// GemmaGuard Dark Color Scheme (Primary - matches splash screen)
private val GemmaGuardDarkColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,              // GemmaGuard dark blue
    onBackground = onBackgroundDark,
    surface = surfaceDark,                    // GemmaGuard navy
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,      // GemmaGuard deep blue
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
)

// GemmaGuard Light Color Scheme (Secondary - for accessibility)
private val GemmaGuardLightColorScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
)

@Composable
fun SecurityMonitorTheme(
    darkTheme: Boolean = true, // Default to dark theme (GemmaGuard style)
    dynamicColor: Boolean = false, // Disable dynamic colors to maintain brand consistency
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Disable dynamic colors for consistent GemmaGuard branding
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        
        // Use GemmaGuard color schemes
        darkTheme -> GemmaGuardDarkColorScheme
        else -> GemmaGuardLightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar to match GemmaGuard theme
            window.statusBarColor = if (darkTheme) {
                GemmaGuardDarkBlue.toArgb() // Dark blue for dark theme
            } else {
                Color.White.toArgb() // White for light theme
            }
            
            // Set navigation bar to match
            window.navigationBarColor = if (darkTheme) {
                GemmaGuardColors.ButtonSecondary.toArgb()
            } else {
                Color.White.toArgb()
            }
            
            // Set status bar content color
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GemmaGuardTypography, // Use enhanced typography
        content = content
    )
}

// Alternative theme name for clarity
@Composable
fun GemmaGuardTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    SecurityMonitorTheme(
        darkTheme = darkTheme,
        dynamicColor = false, // Always use GemmaGuard colors
        content = content
    )
}
