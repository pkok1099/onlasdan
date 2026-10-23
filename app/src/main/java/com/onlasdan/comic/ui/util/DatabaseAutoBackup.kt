package com.onlasdan.comic.ui.util

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Debounced database auto-backup to Documents/ComicReader/comic.db (or user-picked SAF folder).
 *
 * Avoids exporting on every tiny write (bookmark/history/read) which can cause jank and IO churn.
 */
object DatabaseAutoBackup {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    fun schedule(context: Context, delayMs: Long = 1500L) {
        val appContext = context.applicationContext
        if (!DatabaseBackupStore.isBackupEnabled(appContext)) return
        job?.cancel()
        job = scope.launch {
            delay(delayMs)
            DatabaseBackupStore.exportDb(appContext)
        }
    }
}
