package comic.util

import android.content.ContentResolver
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import androidx.sqlite.db.SupportSQLiteDatabase
import comic.db.ComicDatabase
import java.io.File
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val PREFS_NAME = "comic_prefs"
internal const val KEY_TREE_URI = "backup_tree_uri"
private const val KEY_BACKUP_ENABLED = "db_backup_enabled"
private const val KEY_LAST_BACKUP_TIME = "last_backup_timestamp"
private const val DB_FILE_NAME = "comic.db"
private const val SETTINGS_FILE_NAME = "settings_backup.json"
private val DB_TABLES = listOf("bookmarks", "history", "read_chapters", "downloaded_chapters", "downloaded_pages")

data class BackupSummary(
    val bookmarksCount: Long,
    val historyCount: Long,
    val readCount: Long,
    val lastModified: Long,
)

/**
 * SQLite database & settings backup/restore via Storage Access Framework (SAF).
 * - Safe WAL checkpointing without tearing down active Room connection pools.
 * - Non-volatile external storage surviving app uninstallation.
 * - JSON preferences snapshot to preserve settings across installs.
 */
object DatabaseBackupStore {

    fun getCustomTreeUri(context: Context): Uri? {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TREE_URI, null)
        return raw?.let(Uri::parse)
    }

    fun setCustomTreeUri(context: Context, uri: Uri?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_TREE_URI, uri?.toString())
        }
    }

    fun isBackupEnabled(context: Context): Boolean = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_BACKUP_ENABLED, false)

    fun setBackupEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_BACKUP_ENABLED, enabled)
        }
    }

    fun getLastBackupTime(context: Context): Long = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getLong(KEY_LAST_BACKUP_TIME, 0L)

    private fun setLastBackupTime(context: Context, timeMs: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putLong(KEY_LAST_BACKUP_TIME, timeMs)
        }
        // Bangunkan pemirsa badge/waktu backup (D1/D2) tanpa menandai DB berubah.
        DbChangeNotifier.notifyBackupFinished()
    }

    /**
     * Checkpoint WAL safely into the main database file without closing active Room connections.
     */
    private fun checkpointWalSafely(context: Context) {
        runCatching {
            val db = ComicDatabase.get(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
                cursor.moveToFirst()
            }
        }.recoverCatching {
            val dbFile = context.getDatabasePath(DB_FILE_NAME)
            if (dbFile.exists()) {
                val directDb = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
                directDb.rawQuery("PRAGMA wal_checkpoint(FULL);", null).use { /* drain */ }
                directDb.close()
            }
        }
    }

    // null = gagal membaca (file terkunci/rusak): pemanggil wajib
    // memperlakukannya sebagai "tidak kosong" agar kegagalan sesaat tidak
    // memancing onboarding ulang / restore otomatis (bug BUG-3).
    fun isLogicallyEmpty(context: Context): Boolean? {
        val dbFile = context.getDatabasePath(DB_FILE_NAME)
        if (!dbFile.exists() || dbFile.length() <= 0L) return true
        return runCatching {
            val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
            val count = db.rawQuery(
                """
                SELECT
                  (SELECT COUNT(*) FROM bookmarks) +
                  (SELECT COUNT(*) FROM history) +
                  (SELECT COUNT(*) FROM read_chapters)
                """.trimIndent(),
                null,
            ).use { c ->
                if (!c.moveToFirst()) 0 else c.getLong(0)
            }
            db.close()
            count <= 0L
        }.getOrNull()
    }

    suspend fun autoRestoreIfNeeded(context: Context): Result<Boolean> {
        return runCatching {
            if (isLogicallyEmpty(context) != true) return@runCatching false
            if (!backupExists(context)) return@runCatching false
            restoreDb(context).getOrThrow()
            true
        }
    }

    // Kontrak threading store (bug B3): semua operasi backup berat adalah
    // suspend dan melompat ke Dispatchers.IO di dalam store, jadi pemanggil
    // mana pun tidak bisa memblokir main thread. Badan blocking lama utuh.
    suspend fun exportDb(context: Context): Result<Unit> {
        // Nilai DataStore ikut di-backup (bug B17); dikumpulkan di konteks
        // suspend di sini, badan blocking menerimanya sebagai peta jadi.
        val datastoreValues = SettingsBackupStore.collectDataStoreValues(context)
        return withContext(Dispatchers.IO) { exportDbBlocking(context, datastoreValues) }
    }

    private fun exportDbBlocking(context: Context, datastoreValues: Map<String, String>): Result<Unit> = runCatching {
        if (!isBackupEnabled(context)) error("Backup belum aktif")
        val treeUri = getCustomTreeUri(context) ?: error("Folder backup belum dipilih")
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: error("Folder backup tidak valid")

        val dbFile = context.getDatabasePath(DB_FILE_NAME)
        require(dbFile.exists()) { "Database belum ada" }

        // Checkpoint WAL without closing Room
        checkpointWalSafely(context)

        // 1. Export comic.db
        writeTreeFileAtomically(root, context.contentResolver, DB_FILE_NAME, "application/octet-stream") { output ->
            dbFile.inputStream().use { input -> input.copyTo(output) }
        }

        // 2. Export settings_backup.json
        runCatching {
            val settingsJson = SettingsBackupStore.exportSettingsToJson(context, datastoreValues)
            writeTreeFileAtomically(root, context.contentResolver, SETTINGS_FILE_NAME, "application/json") { output ->
                output.write(settingsJson.toByteArray(Charsets.UTF_8))
            }
        }

        setLastBackupTime(context, System.currentTimeMillis())
    }

    // Tulis file backup tanpa menimpa file lama secara in-place (bug B8):
    // tulis lengkap ke file .tmp di folder yang sama dulu; file lama hanya
    // dihapus setelah tulisan selesai, lalu .tmp diganti nama menimpanya.
    // Gagal di tengah jalan = backup lama tetap utuh.
    private fun writeTreeFileAtomically(
        root: DocumentFile,
        resolver: ContentResolver,
        name: String,
        mime: String,
        write: (OutputStream) -> Unit,
    ) {
        val tmpName = "$name.tmp"
        root.findFile(tmpName)?.delete()
        val tmp = root.createFile(mime, tmpName) ?: error("Gagal membuat file backup sementara")
        var written = false
        try {
            resolver.openOutputStream(tmp.uri, "wt")?.use { output ->
                write(output)
                output.flush()
            } ?: error("Gagal menulis file backup")
            written = true
        } finally {
            if (!written) tmp.delete()
        }
        val existing = root.findFile(name)
        if (existing != null && !existing.delete()) {
            tmp.delete()
            error("Gagal mengganti file backup lama")
        }
        if (!tmp.renameTo(name)) error("Gagal mengganti nama file backup (utuh tersimpan sebagai $tmpName)")
    }

    suspend fun restoreDb(context: Context): Result<Unit> {
        var datastoreValues: Map<String, String> = emptyMap()
        val result = withContext(Dispatchers.IO) {
            restoreDbBlocking(context) { parsed -> datastoreValues = parsed }
        }
        // Nilai DataStore diterapkan di konteks suspend (bug B17).
        if (result.isSuccess) SettingsBackupStore.applyDataStoreValues(context, datastoreValues)
        return result
    }

    private fun restoreDbBlocking(context: Context, onDataStoreValues: (Map<String, String>) -> Unit): Result<Unit> = runCatching {
        val treeUri = getCustomTreeUri(context) ?: error("Folder backup belum dipilih")
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: error("Folder backup tidak valid")
        val dbDoc = root.findFile(DB_FILE_NAME) ?: error("File comic.db tidak ditemukan di folder backup")

        // Restore SQLite Database
        copyBackupContents(context, dbDoc.uri)

        // Restore Settings if available
        root.findFile(SETTINGS_FILE_NAME)?.let { settingsDoc ->
            runCatching {
                context.contentResolver.openInputStream(settingsDoc.uri)?.use { input ->
                    val jsonStr = input.bufferedReader().readText()
                    onDataStoreValues(SettingsBackupStore.restoreSettingsFromJson(context, jsonStr))
                }
            }
        }
        // Basis data kini identik dengan isi file backup — waktu backup efektif
        // diperbarui agar badge "belum di-backup" tidak menghitung ulang baris
        // warisan arsip yang baru saja dipulihkan.
        setLastBackupTime(context, System.currentTimeMillis())
        DbChangeNotifier.onDbChanged(context)
    }

    suspend fun restoreFromUri(context: Context, sourceUri: Uri): Result<Unit> = withContext(Dispatchers.IO) { restoreFromUriBlocking(context, sourceUri) }

    private fun restoreFromUriBlocking(context: Context, sourceUri: Uri): Result<Unit> = runCatching {
        copyBackupContents(context, sourceUri)
        // Sama seperti restoreDbBlocking: DB = isi arsip -> waktu backup segar.
        setLastBackupTime(context, System.currentTimeMillis())
        DbChangeNotifier.onDbChanged(context)
    }

    // Restore tidak lagi menukar file basis data — cara lama menutup Room di
    // tengah sesi dan meninggalkan pemegang referensi lama menggantung (bug
    // B2). Isi backup disalin ke basis data yang hidup dalam satu transaksi:
    // kolektor Flow ter-invalidasi alami, UI langsung segar, dan kegagalan di
    // tengah berarti basis data aktif tidak berubah sama sekali.
    private fun copyBackupContents(context: Context, sourceUri: Uri) {
        val tempFile = File(context.cacheDir, "restore_temp.db")
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Gagal membaca backup database")

            val sqlite = ComicDatabase.get(context).openHelper.writableDatabase
            checkBackupFile(tempFile, sqlite.version)
            // Snapshot pengaman (Fase C): salin basis data hidup ke file
            // sementara SEBELUM ditimpa. Kegagalan sesudah titik ini
            // memulihkan snapshot agar data lama utuh, lalu error asli
            // diteruskan ke pemanggil.
            val safetyFile = File(context.cacheDir, "restore_safety.db")
            try {
                writeSafetySnapshot(context, safetyFile)
                val applyResult = runCatching {
                    replaceAllTables(sqlite, tempFile)
                    downgradeMissingDownloads(sqlite)
                }
                if (applyResult.isFailure) {
                    val failure = applyResult.exceptionOrNull() ?: error("restore gagal")
                    val rolledBack = runCatching { replaceAllTables(sqlite, safetyFile) }.isSuccess
                    android.util.Log.e("DatabaseBackupStore", "restore gagal; snapshot dipulihkan=$rolledBack", failure)
                    throw failure
                }
            } finally {
                safetyFile.delete()
            }
        } finally {
            tempFile.delete()
        }
    }

    // Salin basis data yang hidup (setelah checkpoint WAL, pola yang sama
    // dengan exportDb) sebagai jaring pengaman proses restore.
    private fun writeSafetySnapshot(context: Context, safetyFile: File) {
        checkpointWalSafely(context)
        val dbFile = context.getDatabasePath(DB_FILE_NAME)
        require(dbFile.exists()) { "Database belum ada" }
        dbFile.inputStream().use { input -> safetyFile.outputStream().use { output -> input.copyTo(output) } }
    }

    // Backup hanya membawa basis data, bukan file gambar unduhan (bug B6):
    // chapter yang diklaim completed tetapi file halamannya tidak ada di
    // perangkat ini diturunkan ke FAILED agar bisa diunduh ulang dari UI.
    private fun downgradeMissingDownloads(sqlite: SupportSQLiteDatabase) {
        val completed = mutableListOf<Triple<String, String, String>>()
        sqlite.query("SELECT comicSlug, source, chapterSlug FROM downloaded_chapters WHERE status = 2").use { cursor ->
            while (cursor.moveToNext()) {
                completed.add(Triple(cursor.getString(0), cursor.getString(1), cursor.getString(2)))
            }
        }
        for ((slug, source, chapter) in completed) {
            if (downloadFilesMissing(sqlite, slug, source, chapter)) {
                sqlite.execSQL(
                    "UPDATE downloaded_chapters SET status = 3, downloadedPages = 0, sizeBytes = 0 " +
                        "WHERE comicSlug = ? AND source = ? AND chapterSlug = ?",
                    arrayOf<Any>(slug, source, chapter),
                )
            }
        }
    }

    private fun downloadFilesMissing(sqlite: SupportSQLiteDatabase, slug: String, source: String, chapter: String): Boolean {
        sqlite.query(
            "SELECT localFilePath FROM downloaded_pages WHERE comicSlug = ? AND source = ? AND chapterSlug = ?",
            arrayOf<Any>(slug, source, chapter),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return true
            do {
                if (!File(cursor.getString(0)).exists()) return true
            } while (cursor.moveToNext())
        }
        return false
    }

    private fun replaceAllTables(sqlite: SupportSQLiteDatabase, backupFile: File) {
        sqlite.execSQL("ATTACH DATABASE '${backupFile.absolutePath.replace("'", "''")}' AS backup_src")
        try {
            sqlite.beginTransaction()
            try {
                for (table in DB_TABLES) {
                    sqlite.execSQL("DELETE FROM $table")
                    sqlite.execSQL("INSERT INTO $table SELECT * FROM backup_src.$table")
                }
                sqlite.setTransactionSuccessful()
            } finally {
                sqlite.endTransaction()
            }
        } finally {
            sqlite.execSQL("DETACH DATABASE backup_src")
        }
    }

    // Pagar versi: hanya backup dengan user_version yang sama dengan basis
    // data aktif yang boleh disalin; selain itu ditolak utuh sebelum ada
    // tulisan apa pun (daftar kolom antar versi skema tidak seragam).
    private fun checkBackupFile(backupFile: File, liveVersion: Int) {
        SQLiteDatabase.openDatabase(backupFile.path, null, SQLiteDatabase.OPEN_READONLY).use { backupDb ->
            if (backupDb.version != liveVersion) {
                error("Backup dari versi basis data ${backupDb.version} tidak didukung (versi saat ini $liveVersion)")
            }
            val tables = mutableSetOf<String>()
            backupDb.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'", null).use { cursor ->
                while (cursor.moveToNext()) tables.add(cursor.getString(0))
            }
            if (!tables.containsAll(DB_TABLES)) error("File backup tidak valid")
        }
    }

    fun backupExists(context: Context): Boolean {
        if (!isBackupEnabled(context)) return false
        val treeUri = getCustomTreeUri(context) ?: return false
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return false
        return root.findFile(DB_FILE_NAME) != null
    }

    suspend fun inspectBackup(context: Context, treeUri: Uri): BackupSummary? = withContext(Dispatchers.IO) { inspectBackupBlocking(context, treeUri) }

    private fun inspectBackupBlocking(context: Context, treeUri: Uri): BackupSummary? {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val dbDoc = root.findFile(DB_FILE_NAME) ?: return null
        val lastModified = dbDoc.lastModified()

        val tempFile = File(context.cacheDir, "temp_inspect_${System.currentTimeMillis()}.db")
        return try {
            context.contentResolver.openInputStream(dbDoc.uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return null

            val summary = SQLiteDatabase.openDatabase(tempFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                readBackupSummary(db, lastModified)
            }
            summary
        } catch (_: Exception) {
            null
        } finally {
            tempFile.delete()
        }
    }

    private fun readBackupSummary(db: SQLiteDatabase, lastModified: Long): BackupSummary? = db.rawQuery(
        """
            SELECT
              (SELECT COUNT(*) FROM bookmarks),
              (SELECT COUNT(*) FROM history),
              (SELECT COUNT(*) FROM read_chapters)
        """.trimIndent(),
        null,
    ).use { cursor ->
        if (cursor.moveToFirst()) {
            BackupSummary(
                bookmarksCount = cursor.getLong(0),
                historyCount = cursor.getLong(1),
                readCount = cursor.getLong(2),
                lastModified = lastModified,
            )
        } else {
            null
        }
    }
}
