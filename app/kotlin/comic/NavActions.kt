package comic

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Aksi navigasi "jelajahi komik" (kembali ke Beranda) yang disediakan
 * MainActivity di sekitar NavHost (Fase B1). Layar yang jauh dari akar
 * navigasi — Profil (markah/riwayat kosong) dan Unduhan — membaca aksi
 * ini untuk CTA EmptyState tanpa mengubah signature composable mereka
 * (signature ProfileScreen dibekukan baseline detekt).
 */
val LocalOnExploreHome = staticCompositionLocalOf<() -> Unit> { {} }

// Aksi "Buka Komik Unduhan" untuk state error offline (Fase C): disediakan
// MainActivity (navigasi ke profile?tab=downloads); null = tidak tersedia
// di konteks ini sehingga tombolnya tidak ditampilkan.
val LocalOnOpenDownloads = staticCompositionLocalOf<(() -> Unit)?> { null }

// Aksi kembali yang ditawarkan state error layar-penuh (Fase C): diisi
// ReaderScreen agar ComicDataScreen bisa menampilkan tombol "Kembali"
// di cabangnya tanpa mengubah signature (baseline detekt beku).
val LocalErrorBackAction = staticCompositionLocalOf<(() -> Unit)?> { null }

// Aksi "Lanjutkan Membaca" dari hero resume beranda (C2): disediakan
// MainActivity — pindah source aktif lalu navigasi ke reader dengan chapter
// tersimpan. Lewat CompositionLocal (pola yang sama dengan di atas) agar
// signature HomeScreen tidak berubah (baseline detekt beku).
val LocalOnResumeRead = staticCompositionLocalOf<(comicSlug: String, sourceId: String, chapterSlug: String) -> Unit> {
    { _, _, _ -> }
}
