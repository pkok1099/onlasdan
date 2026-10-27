package comic.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Central hook for DB write side-effects (currently: debounce auto-backup).
 * Keep it tiny and Android-only.
 */
object DbChangeNotifier {
    private const val DEFAULT_BACKUP_DELAY_MS = 15_000L

    /**
     * Tick monotonik — naik setiap perubahan DB DAN setiap backup selesai.
     * Pemicu murah (StateFlow conflated) untuk UI yang perlu memuat ulang
     * data turunan DB tanpa observasi Flow Room satu per satu: badge
     * "belum di-backup", waktu backup terakhir, dsb.
     */
    val dataVersion = MutableStateFlow(0L)

    fun onDbChanged(context: Context, delayMs: Long = DEFAULT_BACKUP_DELAY_MS) {
        dataVersion.value += 1
        DatabaseAutoBackup.schedule(context, delayMs)
    }

    /** Backup selesai (waktu terakhir berubah) — tanpa menandai DB berubah. */
    fun notifyBackupFinished() {
        dataVersion.value += 1
    }
}
