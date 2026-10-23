package com.onlasdan.comic.ui.util

import android.content.Context
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class AppCacheStoreTest {

    @Test
    fun formatSize_returnsZeroB_forZeroOrNegativeBytes() {
        assertEquals("0 B", AppCacheStore.formatSize(0L))
        assertEquals("0 B", AppCacheStore.formatSize(-500L))
    }

    @Test
    fun formatSize_returnsAccurateUnits() {
        assertEquals("512 B", AppCacheStore.formatSize(512L))
        assertEquals("1.0 KB", AppCacheStore.formatSize(1024L))
        assertEquals("1.5 KB", AppCacheStore.formatSize(1536L))
        assertEquals("1.0 MB", AppCacheStore.formatSize(1024L * 1024L))
        assertEquals("25.0 MB", AppCacheStore.formatSize(25L * 1024L * 1024L))
        assertEquals("2.00 GB", AppCacheStore.formatSize(2L * 1024L * 1024L * 1024L))
    }

    @Test
    fun calculateTotalCacheBytes_sumsDirectorySizesCorrectly() {
        val tempParent = File.createTempFile("cache_test", "").apply {
            delete()
            mkdirs()
        }

        try {
            val internalDir = File(tempParent, "internal").apply { mkdirs() }
            val codeDir = File(tempParent, "code").apply { mkdirs() }
            val externalDir = File(tempParent, "external").apply { mkdirs() }

            File(internalDir, "file1.bin").writeBytes(ByteArray(100))
            File(codeDir, "file2.bin").writeBytes(ByteArray(250))
            File(externalDir, "file3.bin").writeBytes(ByteArray(400))

            val mockContext = mock(Context::class.java)
            `when`(mockContext.cacheDir).thenReturn(internalDir)
            `when`(mockContext.codeCacheDir).thenReturn(codeDir)
            `when`(mockContext.externalCacheDir).thenReturn(externalDir)

            val totalBytes = AppCacheStore.calculateTotalCacheBytes(mockContext)
            assertEquals(750L, totalBytes)
            assertEquals("750 B", AppCacheStore.formatSize(totalBytes))
        } finally {
            tempParent.deleteRecursively()
        }
    }
}
