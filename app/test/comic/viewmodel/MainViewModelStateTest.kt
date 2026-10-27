package comic.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import comic.source.SourcePreferencesKt
import comic.source.SourceRegistry
import comic.test.MainDispatcherRule
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenever

class MainViewModelStateTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockApplication: Application
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var testFilesDir: File

    @Before
    fun setUp() {
        testFilesDir = tempFolder.newFolder("vm_datastore_test_${System.nanoTime()}")
        mockApplication = mock(Application::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)

        whenever(mockApplication.applicationContext).thenReturn(mockApplication)
        whenever(mockApplication.filesDir).thenReturn(testFilesDir)
        whenever(mockApplication.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        whenever(mockPrefs.getString(anyString(), org.mockito.ArgumentMatchers.nullable(String::class.java))).thenReturn(null)
        whenever(mockPrefs.edit()).thenReturn(mockEditor)
        whenever(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)

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
        } catch (_: Exception) {
            // Ignored during cleanup
        }
    }

    @Test
    fun savedStateHandle_retrievesPreconfiguredSource() {
        val handle = SavedStateHandle(mapOf("selected_source" to "komiku"))
        val source = handle.get<String>("selected_source") ?: SourceRegistry.defaultSource.id
        assertEquals("komiku", source)
    }

    @Test
    fun savedStateHandle_emptyHandle_fallsBackToDefaultSource() {
        val handle = SavedStateHandle()
        val source = handle.get<String>("selected_source") ?: SourceRegistry.defaultSource.id
        assertEquals(SourceRegistry.defaultSource.id, source)
    }

    @Test
    fun savedStateHandle_stateMutation_updatesStoredValue() {
        val handle = SavedStateHandle()
        handle["selected_source"] = "shinigami"
        assertEquals("shinigami", handle.get<String>("selected_source"))

        handle["selected_source"] = "doujindesu"
        assertEquals("doujindesu", handle.get<String>("selected_source"))
    }

    @Test
    fun defaultSource_resolution_isConsistentWithRegistry() {
        val defaultId = SourceRegistry.defaultSource.id
        assertNotNull(defaultId)
        val resolved = SourceRegistry.get(defaultId)
        assertEquals(defaultId, resolved.id)
    }

    @Test
    fun selectSource_updatesSelectedSourceFlowAndSavedStateHandle() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = MainViewModel(mockApplication, savedStateHandle)

        // Verify initial fallback default
        val initialSource = viewModel.selectedSource.value
        assertEquals(SourceRegistry.defaultSource.id, initialSource)

        // Select DoujinDesu
        viewModel.selectSource("doujindesu")
        assertEquals("doujindesu", savedStateHandle.get<String>("selected_source"))
        withContext(Dispatchers.Default) {
            withTimeout(30_000) {
                viewModel.selectedSource.first { it == "doujindesu" }
            }
        }
        assertEquals("doujindesu", viewModel.selectedSource.value)

        // Select Shinigami
        viewModel.selectSource("shinigami")
        assertEquals("shinigami", savedStateHandle.get<String>("selected_source"))
        withContext(Dispatchers.Default) {
            withTimeout(30_000) {
                viewModel.selectedSource.first { it == "shinigami" }
            }
        }
        assertEquals("shinigami", viewModel.selectedSource.value)
    }

    @Test
    fun setDefaultSource_updatesDefaultSourceFlow() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = MainViewModel(mockApplication, savedStateHandle)

        val initialDefault = viewModel.defaultSource.value
        assertEquals(SourceRegistry.defaultSource.id, initialDefault)

        viewModel.setDefaultSource("doujindesu")
        withContext(Dispatchers.Default) {
            withTimeout(30_000) {
                viewModel.defaultSource.first { it == "doujindesu" }
            }
        }
        assertEquals("doujindesu", viewModel.defaultSource.value)
    }
}
