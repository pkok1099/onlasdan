package comic.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal val Context.uiDataStore: DataStore<Preferences> by preferencesDataStore(name = "ui_prefs")

/** Mode animasi in-app (MOTION.md §3). Default: FULL — hidup meski sistem 0x. */
enum class AnimationMode { FULL, MINIMAL }

object UiPreferences {
    private val ANIMATION_MODE = stringPreferencesKey("animation_mode")
    private val DOWNLOADS_SEEN_AT = longPreferencesKey("downloads_seen_at")
    private val SEARCH_HISTORY = stringPreferencesKey("search_history")
    private const val SEARCH_HISTORY_MAX = 8

    /** Riwayat kata kunci pencarian (F4) — terbaru dulu, unik (case-insensitive), maks 8. */
    fun searchHistory(context: Context): Flow<List<String>> = context.uiDataStore.data.map { prefs ->
        prefs[SEARCH_HISTORY]?.split('\n')?.filter { it.isNotBlank() }.orEmpty()
    }

    suspend fun addSearchHistory(context: Context, query: String) {
        val q = query.trim()
        if (q.length < 2) return
        context.uiDataStore.edit { prefs ->
            val existing = prefs[SEARCH_HISTORY]?.split('\n')?.filter { it.isNotBlank() }.orEmpty()
            val next = (listOf(q) + existing.filterNot { it.equals(q, ignoreCase = true) }).take(SEARCH_HISTORY_MAX)
            prefs[SEARCH_HISTORY] = next.joinToString("\n")
        }
    }

    suspend fun clearSearchHistory(context: Context) {
        context.uiDataStore.edit { it.remove(SEARCH_HISTORY) }
    }

    /** Kapan terakhir Download Manager dibuka; unduhan selesai setelah ini = belum dilihat (badge dock). */
    fun downloadsSeenAt(context: Context): Flow<Long> = context.uiDataStore.data.map { prefs -> prefs[DOWNLOADS_SEEN_AT] ?: 0L }

    suspend fun setDownloadsSeenAt(context: Context, timestamp: Long) {
        context.uiDataStore.edit { it[DOWNLOADS_SEEN_AT] = timestamp }
    }

    private val DOWNLOAD_PARALLEL = booleanPreferencesKey("download_parallel")

    /** Mode unduhan: paralel (beberapa chapter sekaligus) vs sekuensial. */
    fun downloadParallel(context: Context): Flow<Boolean> = context.uiDataStore.data.map { prefs -> prefs[DOWNLOAD_PARALLEL] ?: false }

    suspend fun setDownloadParallel(context: Context, parallel: Boolean) {
        context.uiDataStore.edit { it[DOWNLOAD_PARALLEL] = parallel }
    }

    fun animationMode(context: Context): Flow<AnimationMode> = context.uiDataStore.data.map { prefs ->
        when (prefs[ANIMATION_MODE]) {
            AnimationMode.MINIMAL.name -> AnimationMode.MINIMAL
            else -> AnimationMode.FULL
        }
    }

    suspend fun setAnimationMode(context: Context, mode: AnimationMode) {
        context.uiDataStore.edit { it[ANIMATION_MODE] = mode.name }
    }

    // Jembatan backup (bug B17): ekspor semua nilai apa adanya; impor hanya
    // menerima kunci yang dikenal object ini (daftar putih).
    suspend fun exportValues(context: Context): Map<String, String> = context.uiDataStore.data.first().asMap().entries.associate { (key, value) -> key.name to value.toString() }

    suspend fun importValues(context: Context, values: Map<String, String>) {
        context.uiDataStore.edit { prefs ->
            values[ANIMATION_MODE.name]?.let { prefs[ANIMATION_MODE] = it }
            values[SEARCH_HISTORY.name]?.let { prefs[SEARCH_HISTORY] = it }
        }
    }
}
