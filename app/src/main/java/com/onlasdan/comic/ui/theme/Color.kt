package com.onlasdan.comic.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val PureBlack = Color(0xFF000000)
val DarkSurface = Color(0xFF0D0D0F)
val DarkSurfaceVariant = Color(0xFF141418)
val DarkSurfaceElevated = Color(0xFF18181D)
val DarkOutline = Color(0xFF1F1F23)
val DarkOutlineVariant = Color(0xFF2B2B32)

val PrimaryPink = Color(0xFFE8457C)
val AccentPink = Color(0xFFF4A7B9)

/** Blush lembut untuk indikator & tombol slot halaman reader (spec 2026-10-19). */
val SoftBlush = Color(0xFFF4B8C6)
val PrimaryPinkDark = Color(0xFF8B1E43)

/** Background slot halaman reader saat error: merah/blush gelap tipis. */
val ReaderSlotErrorBg = Color(0xFF1C1316)

val TextPrimary = Color(0xFFF5F0F1)
val TextSecondary = Color(0xFF8A8A90)
val TextTertiary = Color(0xFF5A5A62)

val MutedRed = Color(0xFFE05353)

val ShimmerBase = Color(0xFF141418)
val ShimmerHighlight = Color(0xFF222228)

// Category & Status Badges
// Kategori SATU treatment netral (keputusan user 2026-10-21): dibedakan lewat
// teks label, bukan warna. Warna kuat dicadangkan untuk aksi dan status.
val BadgeNeutral = Color(0xFF2B2B32)
val StatusCompleted = Color(0xFF10B981)
val StatusOngoing = Color(0xFF3B82F6)

// Permukaan & teks kapsul Noir (dock, sheet, pagination, toast)
val NoirSurface = Color(0xFF141417)
val NoirInputBg = Color(0xFF1F1F24)
val NoirText = Color(0xFF1A1114)
val FieldSurface = Color(0xFF1C1C22)
val OverlaySurface = Color(0xFF16161A)

// Blush tint lembut (border/gradient/highlight di atas permukaan gelap)
val BlushTint = Color(0xFFFFD9E2)
val BlushWhite = Color(0xFFFFE4E6)

// Status semantik di luar badge (toast, ikon aksi)
val SuccessGreen = Color(0xFF34D399)
val ErrorRed = Color(0xFFEF4444)
val BookmarkAmber = Color(0xFFFBBF24)

// Tag status di kartu (New/Popular/U) — warna kuat memang untuk status
val TagOrange = Color(0xFFE65100)
val TagAmber = Color(0xFFFFB74D)
val TagRed = Color(0xFFD32F2F)

// Gradien scrim banner hero Beranda (dipakai HomeHeroBanner di HomeScreen).
val HomeHeroGradient = Brush.verticalGradient(
    colors = listOf(
        Color.Transparent,
        PureBlack.copy(alpha = 0.55f),
        PureBlack.copy(alpha = 0.98f),
    ),
    startY = 60f,
)
