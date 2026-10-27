package comic.db

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "bookmarks",
    primaryKeys = ["comicSlug", "source"],
)
data class BookmarkEntity(
    val comicSlug: String,
    val source: String,
    val title: String,
    val thumbnail: String?,
    val createdAt: Long,
)

@Entity(
    tableName = "history",
    primaryKeys = ["comicSlug", "source"],
)
data class HistoryEntity(
    val comicSlug: String,
    val source: String,
    val title: String,
    val thumbnail: String?,
    val chapterSlug: String,
    val chapterTitle: String?,
    val readAt: Long,
    val lastPage: Int = 0,
    val totalPages: Int = 0,
    val scrollOffset: Int = 0,
)

@Entity(
    tableName = "read_chapters",
    primaryKeys = ["comicSlug", "source", "chapterSlug"],
)
data class ReadChapterEntity(
    val comicSlug: String,
    val source: String,
    val chapterSlug: String,
    val readAt: Long,
    val lastPage: Int = 0,
    val totalPages: Int = 0,
    val scrollOffset: Int = 0,
)

@Entity(
    tableName = "downloaded_chapters",
    primaryKeys = ["comicSlug", "source", "chapterSlug"],
    indices = [Index(value = ["comicSlug", "source"])],
)
data class DownloadedChapterEntity(
    val comicSlug: String,
    val source: String,
    val chapterSlug: String,
    val chapterTitle: String,
    val comicTitle: String,
    val coverUrl: String?,
    val totalPages: Int,
    val downloadedPages: Int,
    val status: Int, // 0 = Queued, 1 = Downloading, 2 = Completed, 3 = Failed
    val localDir: String,
    val sizeBytes: Long,
    val downloadedAt: Long,
)

@Entity(
    tableName = "downloaded_pages",
    primaryKeys = ["comicSlug", "source", "chapterSlug", "pageIndex"],
    indices = [Index(value = ["comicSlug", "source", "chapterSlug"])],
)
data class DownloadedPageEntity(
    val comicSlug: String,
    val source: String,
    val chapterSlug: String,
    val pageIndex: Int,
    val localFilePath: String,
)
