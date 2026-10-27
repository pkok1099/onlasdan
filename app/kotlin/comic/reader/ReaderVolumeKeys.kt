package comic.reader

import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Jembatan tombol volume fisik -> paging halaman reader (laporan detail-kecil B1).
 *
 * [comic.MainActivity.dispatchKeyEvent] memanggil [consume] SEBELUM sistem
 * memproses volume; saat reader aktif, tombol volume menjadi tombol pindah
 * halaman (volume bawah = maju, atas = mundur; [invert] menukar arah).
 * Handler paging diisi/dilepas ReaderContent mengikuti siklus hidup reader,
 * preferensi enabled/invert dimuat ReaderScreen dari SharedPreferences.
 */
object ReaderVolumeKeys {
    var isActive: Boolean by mutableStateOf(false)
    var enabled: Boolean by mutableStateOf(true)
    var invert: Boolean by mutableStateOf(false)
    var onPageStep: ((forward: Boolean) -> Unit)? = null

    /** @return true bila tombol dikonsumsi untuk paging (volume sistem dilewati). */
    fun consume(keyCode: Int, action: Int): Boolean {
        if (!isActive || !enabled || action != KeyEvent.ACTION_DOWN) return false
        val forward = when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> true
            KeyEvent.KEYCODE_VOLUME_UP -> false
            else -> return false
        }
        onPageStep?.invoke(forward != invert)
        return true
    }
}
