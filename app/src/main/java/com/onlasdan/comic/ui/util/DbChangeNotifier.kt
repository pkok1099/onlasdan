package com.onlasdan.comic.ui.util

import android.content.Context

/**
 * Central hook for DB write side-effects (currently: debounce auto-backup).
 * Keep it tiny and Android-only.
 */
object DbChangeNotifier {
    private const val DEFAULT_BACKUP_DELAY_MS = 15_000L

    fun onDbChanged(context: Context, delayMs: Long = DEFAULT_BACKUP_DELAY_MS) {
        DatabaseAutoBackup.schedule(context, delayMs)
    }
}
