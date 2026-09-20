package com.itantra.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Centralized spacing, radius, and sizing tokens for the iTantra design system.
 * Using these constants ensures visual consistency and simplifies future updates.
 */
object ItantraDimens {
    // ── Spacing ────────────────────────────────────────────────────────────
    val SpacingXxs  = 2.dp
    val SpacingXs   = 4.dp
    val SpacingSm   = 8.dp
    val SpacingMd   = 12.dp
    val SpacingLg   = 16.dp
    val SpacingXl   = 20.dp
    val SpacingXxl  = 24.dp
    val SpacingHuge = 32.dp

    // ── Card & Container Radii ─────────────────────────────────────────────
    val RadiusSmall  = 8.dp
    val RadiusMedium = 12.dp
    val RadiusLarge  = 16.dp
    val RadiusXl     = 20.dp
    val RadiusPill   = 50.dp   // Fully rounded pill shape

    // ── Elevation ──────────────────────────────────────────────────────────
    val ElevationNone   = 0.dp
    val ElevationLow    = 1.dp
    val ElevationMedium = 2.dp

    // ── Touch Targets (Material 3 minimum: 48dp) ──────────────────────────
    val MinTouchTarget = 48.dp
    val IconButtonSize = 48.dp
    val SmallButtonHeight = 36.dp
    val MediumButtonHeight = 44.dp
    val LargeButtonHeight = 52.dp

    // ── PTT Button ─────────────────────────────────────────────────────────
    val PttButtonSize = 140.dp
    val PttPulseMultiplier = 1.5f

    // ── Bottom Navigation ──────────────────────────────────────────────────
    val BottomNavHeight = 80.dp

    // ── Icons ──────────────────────────────────────────────────────────────
    val IconXs  = 14.dp
    val IconSm  = 18.dp
    val IconMd  = 24.dp
    val IconLg  = 28.dp
    val IconXl  = 48.dp
    val IconHuge = 72.dp

    // ── Content Padding ────────────────────────────────────────────────────
    val ScreenPaddingHorizontal = 16.dp
    val ScreenPaddingVertical   = 8.dp
    val CardPadding             = 16.dp
    val CardPaddingLarge        = 20.dp
}
