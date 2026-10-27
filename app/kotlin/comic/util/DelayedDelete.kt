package comic.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Mesin hapus-tertunda untuk pola soft-delete + Undo (Fase A2 UX).
 *
 * Item yang dijadwalkan langsung hilang dari UI (key-nya masuk [pending]),
 * tetapi aksi hapus sesungguhnya baru dijalankan setelah jendela undo
 * berakhir. Menekan "Urungkan" membatalkan job-nya — data tidak pernah
 * tersentuh. Bila proses mati di tengah jendela, penghapusan tidak jadi
 * terjadi (gagal ke arah aman: item tetap ada).
 */
object DelayedDelete {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val jobs = mutableMapOf<String, Job>()
    private val _pending = MutableStateFlow<Set<String>>(emptySet())
    val pending: StateFlow<Set<String>> = _pending

    fun schedule(key: String, delayMs: Long = 5000L, action: () -> Unit) {
        jobs.remove(key)?.cancel()
        _pending.value = _pending.value + key
        jobs[key] = scope.launch {
            delay(delayMs)
            jobs.remove(key)
            _pending.value = _pending.value - key
            action()
        }
    }

    fun cancel(key: String) {
        jobs.remove(key)?.cancel()
        _pending.value = _pending.value - key
    }
}

/** Key hapus-tertunda untuk satu chapter terunduh. */
fun downloadDeleteKey(source: String, comicSlug: String, chapterSlug: String): String = "dl|$source|$comicSlug|$chapterSlug"
