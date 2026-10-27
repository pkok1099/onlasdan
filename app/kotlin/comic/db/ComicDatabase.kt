package comic.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BookmarkEntity::class,
        HistoryEntity::class,
        ReadChapterEntity::class,
        DownloadedChapterEntity::class,
        DownloadedPageEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class ComicDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun readDao(): ReadDao
    abstract fun downloadDao(): DownloadDao
    abstract fun backupPendingDao(): BackupPendingDao

    companion object {
        private const val DB_NAME = "comic.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE history ADD COLUMN lastPage INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE history ADD COLUMN totalPages INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE read_chapters ADD COLUMN lastPage INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE read_chapters ADD COLUMN totalPages INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS downloaded_chapters (
                        comicSlug TEXT NOT NULL,
                        source TEXT NOT NULL,
                        chapterSlug TEXT NOT NULL,
                        chapterTitle TEXT NOT NULL,
                        comicTitle TEXT NOT NULL,
                        coverUrl TEXT,
                        totalPages INTEGER NOT NULL,
                        downloadedPages INTEGER NOT NULL,
                        status INTEGER NOT NULL,
                        localDir TEXT NOT NULL,
                        sizeBytes INTEGER NOT NULL,
                        downloadedAt INTEGER NOT NULL,
                        PRIMARY KEY(comicSlug, source, chapterSlug)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_downloaded_chapters_comicSlug_source ON downloaded_chapters(comicSlug, source)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS downloaded_pages (
                        comicSlug TEXT NOT NULL,
                        source TEXT NOT NULL,
                        chapterSlug TEXT NOT NULL,
                        pageIndex INTEGER NOT NULL,
                        localFilePath TEXT NOT NULL,
                        PRIMARY KEY(comicSlug, source, chapterSlug, pageIndex)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_downloaded_pages_comicSlug_source_chapterSlug ON downloaded_pages(comicSlug, source, chapterSlug)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE history ADD COLUMN scrollOffset INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE read_chapters ADD COLUMN scrollOffset INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var instance: ComicDatabase? = null

        @OptIn(androidx.room.ExperimentalRoomApi::class)
        fun get(context: Context): ComicDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                ComicDatabase::class.java,
                DB_NAME,
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigrationOnDowngrade()
                .setInMemoryTrackingMode(false)
                .build()
                .also { instance = it }
        }
    }
}
