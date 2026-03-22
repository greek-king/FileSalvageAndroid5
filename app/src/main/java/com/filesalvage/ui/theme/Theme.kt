package com.filesalvage.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Brand colours ───────────────────────────────────────────────────────────

val BrandCyan    = Color(0xFF4ECDC4)
val BrandRed     = Color(0xFFFF6B6B)
val BrandPurple  = Color(0xFFA855F7)
val BrandBlue    = Color(0xFF3B82F6)
val BrandGreen   = Color(0xFF10B981)
val BrandAmber   = Color(0xFFF59E0B)

val SurfaceDark  = Color(0xFF0F0F1A)
val SurfaceCard  = Color(0xFF1A1A2E)
val SurfaceElevated = Color(0xFF252540)
val OnSurfaceMuted  = Color(0xFF9CA3AF)

// ─── Dark colour scheme ───────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary          = BrandCyan,
    onPrimary        = Color(0xFF003333),
    primaryContainer = Color(0xFF004D4D),
    secondary        = BrandPurple,
    onSecondary      = Color.White,
    tertiary         = BrandAmber,
    background       = SurfaceDark,
    surface          = SurfaceCard,
    surfaceVariant   = SurfaceElevated,
    onBackground     = Color.White,
    onSurface        = Color.White,
    onSurfaceVariant = OnSurfaceMuted,
    error            = BrandRed,
    outline          = Color(0xFF374151)
)

@Composable
fun FileSalvageTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography(),
        content     = content
    )
}
