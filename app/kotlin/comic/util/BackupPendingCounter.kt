package comic.util

import android.content.Context
import comic.db.ComicDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Penghitung item yang "belum di-backup": jumlah baris di empat tabel utama
 * yang berubah SETELAH waktu backup terakhir (D1 laporan detail kecil).
 *
 * Semua tabel punya kolom waktu (bookmarks.createdAt, history.readAt,
 * read_chapters.readAt, downloaded_chapters.downloadedAt), jadi delta cukup
 * dihitung dengan query COUNT — tanpa hook di tiap titik tulis dan tanpa
 * perubahan skema. Baris warisan restore membawa timestamp asli backup, jadi
 * tidak dihitung ulang. Baris yang DIHAPUS sejak backup terakhir tidak bisa
 * dihitung — badge selalu menampilkan batas bawah item yang berpotensi hilang.
 *
 * lastBackupTime = 0 (belum pernah) berarti SELURUH isi tabel dihitung —
 * tepat saat pengguna paling perlu ditejuk untuk backup pertama.
 */
object BackupPendingCounter {

    suspend fun count(context: Context, lastBackupTime: Long): Int = withContext(Dispatchers.IO) {
        val dao = ComicDatabase.get(context).backupPendingDao()
        val since = if (lastBackupTime > 0L) lastBackupTime else 0L
        dao.countHistoryChangedSince(since) +
            dao.countBookmarksChangedSince(since) +
            dao.countReadChangedSince(since) +
            dao.countDownloadsChangedSince(since)
    }
}
