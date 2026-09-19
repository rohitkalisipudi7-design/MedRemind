package com.example.medicalreminder.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// KINETIC APOTHECARY DARK MODE THEME PALETTE
// =============================================================================

// Base Dark Surfaces & Canvases
val Canvas = Color(0xFF121212)
val SurfaceBright = Color(0xFF262626)
val SurfaceCard = Color(0xFF1E1E1E)
val SurfaceBone = Color(0xFF2A2A2A)
val SurfaceDim = Color(0xFF181818)
val SurfaceContainerHigh = Color(0xFF333333)
val SurfaceDark = Color(0xFF0F0F0F)
val SurfaceDeep = Color(0xFF000000)

// High-Contrast Dark Mode Typography
val Ink = Color(0xFFFFFFFF)            // Bright white for primary text, titles & input values
val BodyText = Color(0xFFE4E4E4)       // Clean, highly readable light grey
val Charcoal = Color(0xFFB0B0B0)       // Medium-light subtext
val Mute = Color(0xFF8E8E8E)
val Ash = Color(0xFF6E6E6E)
val Stone = Color(0xFF555555)
val OnDark = Color(0xFFFFFFFF)
val OnPrimary = Color(0xFFFFFFFF)

// Kinetic Apothecary Vibrant Brand Signals
val PrimaryRed = Color(0xFFFF3B20)      // High-visibility glowing alert red
val PrimaryDeep = Color(0xFFD62208)
val PrimaryFixed = Color(0xFF42150E)
val PrimaryFixedDim = Color(0xFF661E14)
val HeroGlow = Color(0xFFFF6A3D)
val HeroPink = Color(0xFFF4A8A0)

// Clinical Semantic Accents
val BadgeSuccess = Color(0xFF34D399)    // Vivid emerald/mint
val BadgeSuccessBg = Color(0x3334D399)  // 20% opacity
val StatusWarning = Color(0xFFFBBF24)   // Vivid amber
val StatusWarningBg = Color(0x33FBBF24)  // 20% opacity
val StatusCritical = Color(0xFFFF3B20)
val ErrorRed = Color(0xFFF87171)
val ErrorContainer = Color(0x40EF4444)

// Borders & Focus Hairlines
val Hairline = Color(0x33FFFFFF)       // Subtle 20% white border for dark mode cards
val HairlineStrong = Color(0x66FFFFFF)
val RingFocus = Color(0x66FF3B20)

// =============================================================================
// COMPATIBILITY ALIASES
// =============================================================================
val BgPrimary = Canvas
val BgSecondary = SurfaceBone
val BgWhite = SurfaceCard
val BgLightGrey = SurfaceBone
val BgOrange = StatusWarningBg

val TextDark = Ink
val TextLight = Charcoal
val AccentGreen = BadgeSuccess
val AccentRed = PrimaryRed
val IconLight = Ash
val DarkNavy = SurfaceDark

val ColorTextPrimary = Ink
val ColorTextSecondary = Charcoal
val CardBackground = SurfaceCard
val AccentPrimary = PrimaryRed
val AccentLight = SurfaceBone
val ColorWarning = StatusWarning
val ColorSuccess = BadgeSuccess

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
