package com.onlasdan.comic.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    suspend fun getAll(): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BookmarkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<BookmarkEntity>)

    @Query("DELETE FROM bookmarks WHERE comicSlug = :comicSlug AND source = :source")
    suspend fun delete(comicSlug: String, source: String)

    @Query("DELETE FROM bookmarks")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM bookmarks WHERE comicSlug = :comicSlug AND source = :source")
    suspend fun exists(comicSlug: String, source: String): Int

    @Query(
        """
        SELECT * FROM bookmarks
        WHERE title LIKE :queryLike
        ORDER BY createdAt DESC
        LIMIT :limit
        """,
    )
    suspend fun searchByTitle(queryLike: String, limit: Int): List<BookmarkEntity>
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY readAt DESC")
    suspend fun getAll(): List<HistoryEntity>

    @Query("SELECT * FROM history WHERE comicSlug = :comicSlug AND source = :source LIMIT 1")
    fun observeHistory(comicSlug: String, source: String): Flow<HistoryEntity?>

    @Query("SELECT * FROM history WHERE comicSlug = :comicSlug AND source = :source LIMIT 1")
    suspend fun getHistory(comicSlug: String, source: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HistoryEntity)

    // Metadata selalu diperbarui, tapi progres baca baris lama dipertahankan bila
    // babnya sama — dibaca dan ditulis dalam satu transaksi agar tulisan progres
    // dari ReaderProgressTracker tidak bisa menyela di tengahnya (bug B4).
    @Transaction
    suspend fun upsertPreservingProgress(entity: HistoryEntity) {
        val existing = getHistory(entity.comicSlug, entity.source)
        upsert(
            if (existing != null && existing.chapterSlug == entity.chapterSlug) {
                entity.copy(
                    lastPage = existing.lastPage,
                    totalPages = existing.totalPages,
                    scrollOffset = existing.scrollOffset,
                )
            } else {
                entity
            },
        )
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<HistoryEntity>)

    @Query("DELETE FROM history WHERE comicSlug = :comicSlug AND source = :source")
    suspend fun delete(comicSlug: String, source: String)

    @Query("DELETE FROM history")
    suspend fun clear()

    @Query(
        """
        SELECT * FROM history
        WHERE title LIKE :queryLike
        ORDER BY readAt DESC
        LIMIT :limit
        """,
    )
    suspend fun searchByTitle(queryLike: String, limit: Int): List<HistoryEntity>
}

@Dao
interface ReadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReadChapterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ReadChapterEntity>)

    // Penandaan baca tidak boleh menimpa progres (lastPage/totalPages/
    // scrollOffset) yang sudah dicatat tracker (bug B11): sisipkan hanya
    // bila baris belum ada. Mengembalikan -1 bila diabaikan.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: ReadChapterEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIfAbsent(entities: List<ReadChapterEntity>)

    @Query(
        "SELECT COUNT(*) FROM read_chapters WHERE comicSlug = :comicSlug AND source = :source AND chapterSlug = :chapterSlug",
    )
    suspend fun isRead(comicSlug: String, source: String, chapterSlug: String): Int

    @Query("DELETE FROM read_chapters")
    suspend fun clear()

    @Query("SELECT chapterSlug FROM read_chapters WHERE comicSlug = :comicSlug AND source = :source")
    suspend fun getReadChapters(comicSlug: String, source: String): List<String>

    @Query("SELECT chapterSlug FROM read_chapters WHERE comicSlug = :comicSlug AND source = :source")
    fun observeReadChapters(comicSlug: String, source: String): Flow<List<String>>

    @Query("SELECT * FROM read_chapters")
    suspend fun getAll(): List<ReadChapterEntity>
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloaded_chapters WHERE comicSlug = :comicSlug AND source = :source")
    fun observeDownloads(comicSlug: String, source: String): Flow<List<DownloadedChapterEntity>>

    @Query("SELECT * FROM downloaded_chapters WHERE comicSlug = :comicSlug AND source = :source AND chapterSlug = :chapterSlug LIMIT 1")
    suspend fun getChapterDownload(comicSlug: String, source: String, chapterSlug: String): DownloadedChapterEntity?

    @Query("SELECT * FROM downloaded_chapters ORDER BY downloadedAt DESC")
    fun observeAllDownloads(): Flow<List<DownloadedChapterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChapter(entity: DownloadedChapterEntity)

    @Query(
        """
        UPDATE downloaded_chapters
        SET downloadedPages = :downloadedPages, status = :status, sizeBytes = :sizeBytes
        WHERE comicSlug = :comicSlug AND source = :source AND chapterSlug = :chapterSlug
        """,
    )
    suspend fun updateProgress(
        comicSlug: String,
        source: String,
        chapterSlug: String,
        downloadedPages: Int,
        status: Int,
        sizeBytes: Long,
    )

    // Baris DOWNLOADING yang ditinggal mati oleh proses sebelumnya dikembalikan
    // ke QUEUED agar dilanjutkan job UIDT (auto-resume beta.21). Baris QUEUED
    // memang belum mulai; baris FAILED adalah kegagalan nyata, tidak disentuh.
    @Query("UPDATE downloaded_chapters SET status = :queuedStatus WHERE status = :downloadingStatus")
    suspend fun requeueStuckDownloads(downloadingStatus: Int, queuedStatus: Int): Int

    @Query("SELECT * FROM downloaded_chapters WHERE status = :queuedStatus ORDER BY downloadedAt ASC LIMIT 1")
    suspend fun nextQueued(queuedStatus: Int): DownloadedChapterEntity?

    // Klaim atomik satu antrean untuk satu pekerja: hanya berhasil (1) bila
    // baris masih QUEUED, jadi dua pekerja paralel tak pernah mengambil
    // chapter yang sama.
    @Query(
        "UPDATE downloaded_chapters SET status = :downloadingStatus WHERE comicSlug = :comicSlug AND source = :source AND chapterSlug = :chapterSlug AND status = :queuedStatus",
    )
    suspend fun claimChapter(comicSlug: String, source: String, chapterSlug: String, queuedStatus: Int, downloadingStatus: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<DownloadedPageEntity>)

    @Query(
        """
        SELECT * FROM downloaded_pages
        WHERE comicSlug = :comicSlug AND source = :source AND chapterSlug = :chapterSlug
        ORDER BY pageIndex ASC
        """,
    )
    suspend fun getDownloadedPages(comicSlug: String, source: String, chapterSlug: String): List<DownloadedPageEntity>

    @Query("DELETE FROM downloaded_chapters WHERE comicSlug = :comicSlug AND source = :source AND chapterSlug = :chapterSlug")
    suspend fun deleteChapter(comicSlug: String, source: String, chapterSlug: String)

    @Query("DELETE FROM downloaded_pages WHERE comicSlug = :comicSlug AND source = :source AND chapterSlug = :chapterSlug")
    suspend fun deletePages(comicSlug: String, source: String, chapterSlug: String)

    @Query("DELETE FROM downloaded_chapters WHERE comicSlug = :comicSlug AND source = :source")
    suspend fun deleteComicDownloads(comicSlug: String, source: String)

    @Query("DELETE FROM downloaded_pages WHERE comicSlug = :comicSlug AND source = :source")
    suspend fun deleteComicPages(comicSlug: String, source: String)
}
