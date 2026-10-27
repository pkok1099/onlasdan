package comic.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val PureBlack = Color(0xFF060609)
val DarkSurface = Color(0xFF0E0E14)
val DarkSurfaceVariant = Color(0xFF121218)
val DarkSurfaceElevated = Color(0xFF16161F)

// Rombak komik 2026-10-27: garis tinta global dinaikkan kepekatan agar setiap panel
// terbaca "di-tinta" tegas ala cetak komik, bukan garis semu yang nyaris hilang.
val DarkOutline = Color(0xFF34344E)
val DarkOutlineVariant = Color(0xFF464660)

val PrimaryPink = Color(0xFFFF2A85)
val AccentPink = Color(0xFFFF5CA1)

/** Blush lembut untuk indikator & tombol slot halaman reader (spec 2026-10-19). */
val SoftBlush = Color(0xFFF4B8C6)
val PrimaryPinkDark = Color(0xFF9E1452)

/** Background slot halaman reader saat error: merah/blush gelap tipis. */
val ReaderSlotErrorBg = Color(0xFF1C1316)

// Rombak komik 2026-10-27: teks dipanaskan ke arah kertas cetak (paper white),
// bukan putih kebiruan — seluruh UI terasa di atas kertas komik saat lampu mati.
val TextPrimary = Color(0xFFFAF6EE)
val TextSecondary = Color(0xFF9C96A8)
val TextTertiary = Color(0xFF6F6979)

val MutedRed = Color(0xFFE05353)

val ShimmerBase = Color(0xFF121218)
val ShimmerHighlight = Color(0xFF22222E)

// Category & Status Badges
// Kategori SATU treatment netral (keputusan user 2026-10-21): dibedakan lewat
// teks label, bukan warna. Warna kuat dicadangkan untuk aksi dan status.
val BadgeNeutral = Color(0xFF262632)
val StatusCompleted = Color(0xFF10B981)
val StatusOngoing = Color(0xFF3B82F6)

// Permukaan & teks kapsul Noir (dock, sheet, pagination, toast)
val NoirSurface = Color(0xFF12121A)
val NoirInputBg = Color(0xFF1C1C26)
val NoirText = Color(0xFF1A1114)
val FieldSurface = Color(0xFF1A1A24)
val OverlaySurface = Color(0xFF14141D)

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

// Palet Aksentuasi Komik SFX Pull-to-Refresh (Elastic Noir v2.4)
val ComicSfxPink = Color(0xFFFF2A85)
val ComicSfxRose = Color(0xFFFF5CA1)
val ComicSfxCyan = Color(0xFF00F0FF)
val ComicSfxCyanLight = Color(0xFFE0FFFF)
val ComicSfxYellow = Color(0xFFFFE600)
val ComicSfxYellowLight = Color(0xFFFFF9C4)
val ComicSfxPurple = Color(0xFFA855F7)
val ComicSfxOrange = Color(0xFFFF5500)

val LiquidArchTop = Color(0xFF1F0918)
val LiquidArchBottom = Color(0xFF0B0B12)
val BurstBubbleStart = Color(0xFF0D0D12)
val BurstBubbleEnd = Color(0xFF1D0C18)
val ComicStampSurface = Color(0xFF0C0C12)
val ComicSublabelSurface = Color(0xFF0A0A10)

// ===================== Comic Pop Art Tokens (rombak komik 2026-10-27) =====================
// Bahasa visual "Inked Pop Noir": fondasi gelap tetap (void tinta), tapi semua elemen
// interaktif dibaca sebagai objek komik — sticker kuning, frame tinta krem, hard shadow
// tanpa blur, dan tipografi display komik. Token di bawah dipakai lintas komponen.

/** Hitam tinta untuk outline/teks di atas elemen POP terang (sticker kuning, burst). */
val InkBlack = Color(0xFF0B0B12)

/** "Tinta terbalik" krem kertas — frame panel tebal di atas permukaan gelap. */
val InkLine = Color(0xFFF3EADF)

/** Sticker kuning klasik halaman komik (badge rating, nomor halaman terpilih, burst). */
val PopYellow = Color(0xFFFFE600)

/** Aksen burst/error bergaya cetakan merah pop. */
val PopRed = Color(0xFFFF4D4D)
