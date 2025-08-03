package com.GemmaGuardian.securitymonitor.presentation.theme

import androidx.compose.ui.graphics.Color

// GemmaGuard Dark Theme - Primary Colors (from splash screen)
val GemmaGuardDarkBlue = Color(0xFF1A1A2E)      // Primary dark blue
val GemmaGuardNavy = Color(0xFF16213E)          // Medium navy blue  
val GemmaGuardDeepBlue = Color(0xFF0F3460)      // Deep blue
val GemmaGuardPurple = Color(0xFF533483)        // Accent purple
val GemmaGuardLightPurple = Color(0xFF7B5AA6)   // Lighter purple variant

// GemmaGuard Light Theme - Professional Colors
val GemmaGuardLightGray = Color(0xFFF8FAFC)     // Very light gray background
val GemmaGuardMediumGray = Color(0xFFE2E8F0)    // Medium gray
val GemmaGuardDarkGray = Color(0xFF475569)      // Dark gray text

// Semantic Colors - Enhanced for Security Context
val SecurityCritical = Color(0xFFDC2626)        // Critical alerts - Red
val SecurityHigh = Color(0xFFEA580C)            // High alerts - Orange  
val SecurityMedium = Color(0xFFD97706)          // Medium alerts - Amber
val SecurityLow = Color(0xFF059669)             // Low alerts - Green
val SecuritySuccess = Color(0xFF16A34A)         // Success states
val SecurityWarning = Color(0xFFCA8A04)         // Warning states

// Dark Theme Semantic Colors
val SecurityCriticalDark = Color(0xFFEF4444)
val SecurityHighDark = Color(0xFFF97316)
val SecurityMediumDark = Color(0xFFF59E0B)
val SecurityLowDark = Color(0xFF10B981)
val SecuritySuccessDark = Color(0xFF22C55E)
val SecurityWarningDark = Color(0xFFEAB308)

// =================== LIGHT THEME ===================
val primaryLight = GemmaGuardPurple
val onPrimaryLight = Color.White
val primaryContainerLight = Color(0xFFE8E2F0)   // Very light purple
val onPrimaryContainerLight = GemmaGuardDarkBlue
val secondaryLight = GemmaGuardDarkGray
val onSecondaryLight = Color.White
val secondaryContainerLight = GemmaGuardLightGray
val onSecondaryContainerLight = GemmaGuardDarkBlue
val tertiaryLight = SecuritySuccess
val onTertiaryLight = Color.White
val tertiaryContainerLight = Color(0xFFD1FAE5)
val onTertiaryContainerLight = Color(0xFF064E3B)
val errorLight = SecurityCritical
val onErrorLight = Color.White
val errorContainerLight = Color(0xFFFEE2E2)
val onErrorContainerLight = Color(0xFF7F1D1D)
val backgroundLight = Color.White
val onBackgroundLight = GemmaGuardDarkBlue
val surfaceLight = Color.White
val onSurfaceLight = GemmaGuardDarkBlue
val surfaceVariantLight = GemmaGuardLightGray
val onSurfaceVariantLight = GemmaGuardDarkGray
val outlineLight = GemmaGuardMediumGray
val outlineVariantLight = Color(0xFFE2E8F0)
val scrimLight = Color(0xFF000000)
val inverseSurfaceLight = GemmaGuardDarkBlue
val inverseOnSurfaceLight = Color.White
val inversePrimaryLight = GemmaGuardLightPurple

// =================== DARK THEME ===================
val primaryDark = GemmaGuardPurple
val onPrimaryDark = Color.White
val primaryContainerDark = GemmaGuardNavy
val onPrimaryContainerDark = Color(0xFFE8E2F0)
val secondaryDark = Color(0xFF94A3B8)
val onSecondaryDark = GemmaGuardDarkBlue
val secondaryContainerDark = GemmaGuardNavy
val onSecondaryContainerDark = Color(0xFFB0BEC5)
val tertiaryDark = SecuritySuccessDark
val onTertiaryDark = Color(0xFF064E3B)
val tertiaryContainerDark = Color(0xFF047857)
val onTertiaryContainerDark = Color(0xFFD1FAE5)
val errorDark = SecurityCriticalDark
val onErrorDark = Color(0xFF7F1D1D)
val errorContainerDark = Color(0xFFB91C1C)
val onErrorContainerDark = Color(0xFFFEE2E2)
val backgroundDark = GemmaGuardDarkBlue        // Primary background
val onBackgroundDark = Color(0xFFF1F5F9)
val surfaceDark = GemmaGuardNavy              // Card/surface background
val onSurfaceDark = Color(0xFFF1F5F9)
val surfaceVariantDark = GemmaGuardDeepBlue   // Elevated surfaces
val onSurfaceVariantDark = Color(0xFF94A3B8)
val outlineDark = Color(0xFF64748B)
val outlineVariantDark = Color(0xFF475569)
val scrimDark = Color(0xFF000000)
val inverseSurfaceDark = Color(0xFFF1F5F9)
val inverseOnSurfaceDark = GemmaGuardDarkBlue
val inversePrimaryDark = GemmaGuardPurple

// Enhanced GemmaGuard-specific colors and utilities
object GemmaGuardColors {
    // Core Brand Colors
    val Primary = GemmaGuardPurple
    val PrimaryDark = GemmaGuardDarkBlue
    val PrimaryMedium = GemmaGuardNavy
    val PrimaryDeep = GemmaGuardDeepBlue
    val PrimaryLight = GemmaGuardLightPurple
    
    // Gradient Definitions (matching splash screen)
    val backgroundGradientColors = listOf(
        GemmaGuardDarkBlue,
        GemmaGuardNavy, 
        GemmaGuardDeepBlue
    )
    
    val cardGradientColors = listOf(
        Color(0xFF1E1E3F).copy(alpha = 0.7f),
        Color(0xFF2A2A4F).copy(alpha = 0.5f)
    )
    
    val primaryGradientColors = listOf(
        GemmaGuardPurple,
        GemmaGuardLightPurple
    )
    
    // Threat Level Colors with Enhanced Contrast
    val ThreatCritical = SecurityCritical
    val ThreatCriticalDark = SecurityCriticalDark
    val ThreatHigh = SecurityHigh
    val ThreatHighDark = SecurityHighDark
    val ThreatMedium = SecurityMedium
    val ThreatMediumDark = SecurityMediumDark
    val ThreatLow = SecurityLow
    val ThreatLowDark = SecurityLowDark
    
    // Status Indicators
    val Online = SecuritySuccess
    val OnlineDark = SecuritySuccessDark
    val Offline = Color(0xFF6B7280)
    val OfflineDark = Color(0xFF9CA3AF)
    val Processing = Color(0xFF3B82F6)
    val ProcessingDark = Color(0xFF60A5FA)
    
    // Glass Morphism Effects
    val GlassMorphismLight = Color(0x1AFFFFFF)
    val GlassMorphismDark = Color(0x1A000000)
    val GlassMorphismPurple = GemmaGuardPurple.copy(alpha = 0.1f)
    
    // Text Colors with Enhanced Readability
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFB0BEC5)
    val TextTertiary = Color(0xFF78909C)
    val TextDisabled = Color(0xFF607D8B)
    
    // Card and Surface Enhancements
    val CardBackground = Color(0xFF1E1E3F).copy(alpha = 0.7f)
    val CardBackgroundElevated = Color(0xFF2A2A4F).copy(alpha = 0.8f)
    val SurfaceOverlay = Color(0xFF533483).copy(alpha = 0.1f)
    
    // Interactive States
    val ButtonPrimary = GemmaGuardPurple
    val ButtonSecondary = GemmaGuardNavy
    val ButtonDisabled = Color(0xFF37474F)
    val Ripple = GemmaGuardPurple.copy(alpha = 0.1f)
    
    // Border and Outline Colors
    val BorderPrimary = GemmaGuardPurple.copy(alpha = 0.3f)
    val BorderSecondary = Color(0xFF455A64)
    val BorderFocus = GemmaGuardPurple
    val BorderError = SecurityCritical
    
    // Shadow and Elevation
    val ShadowColor = Color(0xFF000000).copy(alpha = 0.2f)
    val ElevationOverlay = Color(0xFF533483).copy(alpha = 0.05f)
}
