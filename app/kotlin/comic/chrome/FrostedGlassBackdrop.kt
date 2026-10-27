package comic.chrome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import comic.theme.NoirSurface
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * Radius blur visual kaca frosted (dp) — diteruskan mentah ke Haze (blurRadius), tanpa
 * pembagian apa pun. 32dp ≈ 88px pada density 2.75x.
 */
private val GlassBlurRadius = 32.dp

/** Kepekatan scrim kaca gelap (smoked glass) di atas blur — konten di bawah cuma tembus samar. */
private const val GlassScrimAlpha = 0.82f

/**
 * Alpha fallback bila blur tidak tersedia/dinonaktifkan (gate) — dock tidak pernah lebih
 * bening dari surface solid.
 */
private const val GlassFallbackAlpha = 0.98f

/** Warna kaca dark — NoirSurface ditarik 60% ke arah hitam (#07070A, smoked glass Noir). */
private val GlassTint = lerp(Color.Black, NoirSurface, 0.4f)

/** Akses [FrostedGlassState] dari komposisi; null = kaca nonaktif (dock fallback surface polos). */
val LocalFrostedGlass = compositionLocalOf<FrostedGlassState?> { null }

/**
 * Wrapper [HazeState] (Haze 2.0.1, dev.chrisbanes.haze:haze + haze-blur) untuk frosted
 * glass UnifiedMorphingDock. Riwayat keputusan: pipeline custom record-GraphicsLayer +
 * RenderEffect + drawLayer (v1.2.20–v1.2.26) tidak pernah menghasilkan blur di perangkat
 * uji → migrasi Haze 1.6.10 (v1.2.27) terbukti jalan; user memutuskan upgrade ke Haze
 * 2.0.1 (v1.2.28) setelah toolchain naik (AGP 9.4.1 + compileSdk 37 + Gradle 9.6.0 +
 * Kotlin 2.4.20 + BOM 2026.09.00/Compose 1.12.1) — syarat keras AAR Haze 2.x.
 *
 * Arsitektur Haze 2: core `haze` menyediakan HazeState/hazeSource (perekaman sumber
 * hanya saat ada node effect aktif); modul `haze-blur` menyediakan hazeBlur dengan
 * HazeBlurStyle yang direplay ulang per frame — perekaman, alignment koordinat,
 * downsample performa, dan fallback dikelola library. Tanpa lapisan putih: warna kaca
 * MURNI refraksi konten di bawahnya (tint NoirSurface saja). [isActive] dipertahankan
 * sebagai gate: false = blurEnabled(false) → Haze menggambar fallbackColorEffect solid
 * (hemat GPU saat modal menutupi dock). Murni properti draw-time — kebal Developer
 * Options 0x; minSdk 35 menjamin RenderEffect (Android 12+) tersedia sehingga Haze
 * selalu memilih jalur blur.
 */
class FrostedGlassState(internal val haze: HazeState) {
    /**
     * Gate kaca: true hanya saat dock benar-benar tampil tanpa penutup (bukan
     * reader/modal). False = blur pada node effect dimatikan → fallback scrim.
     */
    var isActive: Boolean by mutableStateOf(false)
        internal set
}

@Composable
fun rememberFrostedGlassState(): FrostedGlassState {
    val haze = rememberHazeState()
    return remember { FrostedGlassState(haze) }
}

/**
 * Modifier sumber — pasang di container SELURUH latar (dasar Scaffold + ambient + konten
 * NavHost) di MainActivity. Mendelegasikan ke [hazeSource]: Haze merekam konten container
 * ini ke layer internal setiap kali ada node effect yang mengkonsumsinya.
 *
 * Kontrak urutan rantai (bug v1.2.22): modifier ini HARUS dipasang SEBELUM background()
 * warna dasar. Konten yang terekam hanyalah modifier DI DALAM (setelah) modifier ini —
 * background yang dipasang sebelum modifier ini tidak ikut terekam, sehingga zona tanpa
 * konten (mis. contentPadding feed di belakang dock) terekam TRANSPARAN dan bagian bawah
 * dock terlihat tanpa warna dasar.
 */
fun Modifier.frostedGlassSource(state: FrostedGlassState): Modifier = hazeSource(state.haze)

/**
 * Modifier efek — pasang pada node frost TERPISAH di dalam Surface dock (saudara konten
 * dock, di dalam clip rounded). Mendelegasikan ke [hazeBlur] dengan [HazeBlurStyle]:
 * blur [GlassBlurRadius] tanpa noise, scrim kaca gelap [GlassTint] [GlassScrimAlpha]
 * DIGAMBAR DI ATAS blur (refraksi konten bawah samar di balik smoked glass, tanpa
 * putih), fallback [GlassTint] [GlassFallbackAlpha] saat blur dinonaktifkan gate
 * ([FrostedGlassState.isActive]) atau tak tersedia. State null = no-op (dock jatuh ke
 * surface polos).
 */
fun Modifier.frostedGlassEffect(state: FrostedGlassState?): Modifier {
    if (state == null) return this
    val style = HazeBlurStyle {
        blurEnabled(state.isActive)
        blurRadius(GlassBlurRadius)
        noiseFactor(0f)
        backgroundColor(NoirSurface)
        colorEffects(listOf(HazeColorEffect.tint(GlassTint.copy(alpha = GlassScrimAlpha))))
        fallbackColorEffect(HazeColorEffect.tint(GlassTint.copy(alpha = GlassFallbackAlpha)))
    }
    return hazeBlur(input = HazeInput.Sources(state.haze), style = style)
}
