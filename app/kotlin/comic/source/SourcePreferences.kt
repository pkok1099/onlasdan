@file:JvmName("SourcePreferencesKt")

package comic.source

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal val Context.sourceDataStore: DataStore<Preferences> by preferencesDataStore(name = "source_prefs")

/**
 * Persists the active source and the default source in DataStore.
 */
object SourcePreferences {
    private val SELECTED_SOURCE = stringPreferencesKey("selected_source")
    private val DEFAULT_SOURCE = stringPreferencesKey("default_source_id")

    private val fallbackDefault: String
        get() = SourceRegistry.defaultSource.id

    fun selectedSourceId(context: Context): Flow<String> = context.sourceDataStore.data.map { it[SELECTED_SOURCE] ?: it[DEFAULT_SOURCE] ?: fallbackDefault }

    fun defaultSourceId(context: Context): Flow<String> = context.sourceDataStore.data.map { it[DEFAULT_SOURCE] ?: fallbackDefault }

    suspend fun setSelectedSourceId(context: Context, id: String) {
        context.sourceDataStore.edit { it[SELECTED_SOURCE] = id }
    }

    suspend fun setDefaultSourceId(context: Context, id: String) {
        context.sourceDataStore.edit { it[DEFAULT_SOURCE] = id }
    }

    // Jembatan backup (bug B17): ekspor semua nilai apa adanya; impor hanya
    // menerima kunci yang dikenal object ini (daftar putih).
    suspend fun exportValues(context: Context): Map<String, String> = context.sourceDataStore.data.first().asMap().entries.associate { (key, value) -> key.name to value.toString() }

    suspend fun importValues(context: Context, values: Map<String, String>) {
        context.sourceDataStore.edit { prefs ->
            values[SELECTED_SOURCE.name]?.let { prefs[SELECTED_SOURCE] = it }
            values[DEFAULT_SOURCE.name]?.let { prefs[DEFAULT_SOURCE] = it }
        }
    }
}
