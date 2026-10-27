package comic.util

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

class DatabaseBackupStoreTest {

    @Test
    fun isBackupEnabled_readsFromSharedPreferences() {
        val mockContext = mock(Context::class.java)
        val mockPrefs = mock(SharedPreferences::class.java)
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.getBoolean(eq("db_backup_enabled"), eq(false))).thenReturn(true)

        val enabled = DatabaseBackupStore.isBackupEnabled(mockContext)
        assertTrue(enabled)
    }

    @Test
    fun exportAndRestoreSettings_preservesValidEntries() {
        val mockContext = mock(Context::class.java)
        val mockPrefs = mock(SharedPreferences::class.java)
        val mockEditor = mock(SharedPreferences.Editor::class.java)

        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), any())).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), any())).thenReturn(mockEditor)

        val entries = mapOf<String, Any>(
            "comic_read_mode" to "WEBTOON",
            "comic_grid_columns" to 4,
            "comic_hd_mode" to true,
        )
        `when`(mockPrefs.all).thenReturn(entries)

        val jsonString = SettingsBackupStore.exportSettingsToJson(mockContext)
        assertTrue(jsonString.contains("comic_read_mode"))
        assertTrue(jsonString.contains("WEBTOON"))
        assertTrue(jsonString.contains("comic_grid_columns"))
        assertTrue(jsonString.contains("comic_hd_mode"))

        SettingsBackupStore.restoreSettingsFromJson(mockContext, jsonString)
        verify(mockEditor).putString("comic_read_mode", "WEBTOON")
        verify(mockEditor).putInt("comic_grid_columns", 4)
        verify(mockEditor).putBoolean("comic_hd_mode", true)
        verify(mockEditor).apply()
    }

    @Test
    fun exportAndRestoreSettings_includesDataStoreSection() {
        val mockContext = mock(Context::class.java)
        val mockPrefs = mock(SharedPreferences::class.java)
        val mockEditor = mock(SharedPreferences.Editor::class.java)

        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockPrefs.all).thenReturn(emptyMap())

        val jsonString = SettingsBackupStore.exportSettingsToJson(
            mockContext,
            mapOf("ui.animation_mode" to "MINIMAL", "src.selected_source" to "shinigami"),
        )
        assertTrue(jsonString.contains("__datastore__"))

        val restored = SettingsBackupStore.restoreSettingsFromJson(mockContext, jsonString)
        assertEquals("MINIMAL", restored["ui.animation_mode"])
        assertEquals("shinigami", restored["src.selected_source"])
    }
}
