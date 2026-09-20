package com.itantra.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── iTantra Brand Palette ──────────────────────────────────────────────────
// Primary: Deep navy-teal — conveys reliability and precision
val ItantraTeal80       = Color(0xFF4DD0C4)   // Light (dark theme surfaces)
val ItantraTeal40       = Color(0xFF006B63)   // Dark  (light theme surfaces)
val ItantraTealOnDark   = Color(0xFF00E5CC)   // Accent highlight on dark bg

// Secondary: Amber — used for status and action indicators
val ItantraAmber80      = Color(0xFFFFCC02)
val ItantraAmber40      = Color(0xFFB98000)

// Tertiary: Deep indigo — for "received" message bubbles and accents
val ItantraIndigo80     = Color(0xFFB0C6FF)
val ItantraIndigo40     = Color(0xFF284EA6)

// ── Semantic Status Colors ─────────────────────────────────────────────────
val StatusOnline        = Color(0xFF00C853)   // Connected / active
val StatusOffline       = Color(0xFFFF6D00)   // Disconnected / offline warning
val StatusError         = Color(0xFFCF2D2D)   // Error / failure
val StatusWarning       = Color(0xFFFFCC02)   // Warning / pending

// ── Alert Priority Colors ──────────────────────────────────────────────────
val PriorityNormal      = Color(0xFF006B63)   // Matches primary
val PriorityImportant   = Color(0xFFE6A800)   // Warm amber
val PriorityAlert       = Color(0xFFCF2D2D)   // Red alert
val ItantraEmergency    = Color(0xFFE53935)   // High visibility red for emergency alerts

// ── Dark Background Surfaces ───────────────────────────────────────────────
val DarkBackground           = Color(0xFF0A1628)
val DarkSurface              = Color(0xFF0F1E36)
val DarkSurfaceVariant       = Color(0xFF162744)
val DarkCardBackground       = Color(0xFF1A2E48)
val DarkSurfaceContainerLow  = Color(0xFF0D1A2E)
val DarkSurfaceContainerHigh = Color(0xFF1E3350)

// ── Light Background Surfaces ─────────────────────────────────────────────
val LightBackground           = Color(0xFFF4F6FA)
val LightSurface              = Color(0xFFFFFFFF)
val LightSurfaceVariant       = Color(0xFFE8EDF5)
val LightCardBackground       = Color(0xFFEEF2F9)
val LightSurfaceContainerLow  = Color(0xFFF8F9FC)
val LightSurfaceContainerHigh = Color(0xFFE2E7F0)

// ── Onboarding Gradient Colors ─────────────────────────────────────────────
val OnboardingGradientStart = Color(0xFF006B63)
val OnboardingGradientEnd   = Color(0xFF284EA6)
