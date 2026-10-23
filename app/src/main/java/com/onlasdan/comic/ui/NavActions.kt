package com.onlasdan.comic.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Aksi navigasi "jelajahi komik" (kembali ke Beranda) yang disediakan
 * MainActivity di sekitar NavHost (Fase B1). Layar yang jauh dari akar
 * navigasi — Profil (markah/riwayat kosong) dan Unduhan — membaca aksi
 * ini untuk CTA EmptyState tanpa mengubah signature composable mereka
 * (signature ProfileScreen dibekukan baseline detekt).
 */
val LocalOnExploreHome = staticCompositionLocalOf<() -> Unit> { {} }
