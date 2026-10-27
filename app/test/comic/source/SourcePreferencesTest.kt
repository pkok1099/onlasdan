package comic.source

import android.content.Context
import comic.test.MainDispatcherRule
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenever

class SourcePreferencesTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var testFilesDir: File

    @Before
    fun setUp() {
        testFilesDir = tempFolder.newFolder("datastore_test_${System.currentTimeMillis()}")
        mockContext = mock(Context::class.java)

        whenever(mockContext.applicationContext).thenReturn(mockContext)
        whenever(mockContext.filesDir).thenReturn(testFilesDir)

        resetDataStoreDelegate()
    }

    @After
    fun tearDown() {
        resetDataStoreDelegate()
    }

    private fun resetDataStoreDelegate() {
        try {
            val ktClass = Class.forName("comic.source.SourcePreferencesKt")
            val delegateField = ktClass.getDeclaredField("sourceDataStore\$delegate")
            delegateField.isAccessible = true
            val delegate = delegateField.get(null) ?: return
            val instanceField = delegate::class.java.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            instanceField.set(delegate, null)
        } catch (e: Exception) {
            println("resetDataStoreDelegate exception: ${e.message}")
        }
    }

    @Test
    fun selectedSourceId_emitsFallbackDefaultInitially() = runTest {
        val initial = SourcePreferences.selectedSourceId(mockContext).first()
        assertEquals(SourceRegistry.defaultSource.id, initial)
    }

    @Test
    fun selectedSourceId_emitsSourceSetBySetSelectedSourceId() = runTest {
        SourcePreferences.setSelectedSourceId(mockContext, "doujindesu")
        val afterDoujin = SourcePreferences.selectedSourceId(mockContext).first()
        assertEquals("doujindesu", afterDoujin)

        SourcePreferences.setSelectedSourceId(mockContext, "shinigami")
        val afterShinigami = SourcePreferences.selectedSourceId(mockContext).first()
        assertEquals("shinigami", afterShinigami)

        SourcePreferences.setSelectedSourceId(mockContext, "komikindo")
        val afterKomikindo = SourcePreferences.selectedSourceId(mockContext).first()
        assertEquals("komikindo", afterKomikindo)
    }

    @Test
    fun defaultSourceId_emitsSourceSetBySetDefaultSourceId() = runTest {
        val initialDefault = SourcePreferences.defaultSourceId(mockContext).first()
        assertEquals(SourceRegistry.defaultSource.id, initialDefault)

        SourcePreferences.setDefaultSourceId(mockContext, "doujindesu")
        val afterUpdate = SourcePreferences.defaultSourceId(mockContext).first()
        assertEquals("doujindesu", afterUpdate)
    }
}
