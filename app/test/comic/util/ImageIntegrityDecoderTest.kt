package comic.util

import android.content.Context
import coil.decode.ImageSource
import coil.request.Options
import java.io.File
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mock

class ImageIntegrityDecoderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `decode throws IOException and deletes corrupted truncated file`() = runTest {
        val corruptedFile = tempFolder.newFile("corrupted.jpg")
        val content = ByteArray(200)
        content[0] = 0xFF.toByte()
        content[1] = 0xD8.toByte() // SOI but missing EOI
        corruptedFile.writeBytes(content)

        val imageSource = ImageSource(corruptedFile.toOkioPath())
        val mockContext = mock(Context::class.java)
        val options = Options(mockContext)

        val decoder = ImageIntegrityDecoder(imageSource, options)

        var thrown = false
        try {
            decoder.decode()
        } catch (_: IOException) {
            thrown = true
        }

        assertTrue("Expected IOException for truncated file", thrown)
        assertFalse("Corrupted file should be deleted", corruptedFile.exists())
    }
}
