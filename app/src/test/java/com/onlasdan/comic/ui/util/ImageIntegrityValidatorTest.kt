package com.onlasdan.comic.ui.util

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImageIntegrityValidatorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `valid JPEG returns true`() {
        val file = tempFolder.newFile("valid.jpg")
        val content = ByteArray(256)
        content[0] = 0xFF.toByte()
        content[1] = 0xD8.toByte() // SOI
        content[254] = 0xFF.toByte()
        content[255] = 0xD9.toByte() // EOI
        file.writeBytes(content)

        assertTrue(ImageIntegrityValidator.isComplete(file))
        assertTrue(ImageIntegrityValidator.isComplete(content))
    }

    @Test
    fun `valid JPEG with trailing padding returns true`() {
        val file = tempFolder.newFile("padded.jpg")
        val content = ByteArray(300)
        content[0] = 0xFF.toByte()
        content[1] = 0xD8.toByte()
        content[280] = 0xFF.toByte()
        content[281] = 0xD9.toByte()
        // remaining bytes are 0x00 / whitespace padding
        file.writeBytes(content)

        assertTrue(ImageIntegrityValidator.isComplete(file))
        assertTrue(ImageIntegrityValidator.isComplete(content))
    }

    @Test
    fun `truncated JPEG without EOI returns false`() {
        val file = tempFolder.newFile("truncated.jpg")
        val content = ByteArray(200)
        content[0] = 0xFF.toByte()
        content[1] = 0xD8.toByte()
        // No FF D9 anywhere in the tail
        file.writeBytes(content)

        assertFalse(ImageIntegrityValidator.isComplete(file))
        assertFalse(ImageIntegrityValidator.isComplete(content))
    }

    @Test
    fun `valid PNG with IEND chunk returns true`() {
        val file = tempFolder.newFile("valid.png")
        val content = ByteArray(100)
        // PNG magic: 89 50 4E 47 0D 0A 1A 0A
        val magic = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        System.arraycopy(magic, 0, content, 0, magic.size)
        // IEND chunk: "IEND" (49 45 4E 44) followed by CRC
        val iend = "IEND".toByteArray(Charsets.US_ASCII)
        System.arraycopy(iend, 0, content, 90, 4)
        file.writeBytes(content)

        assertTrue(ImageIntegrityValidator.isComplete(file))
        assertTrue(ImageIntegrityValidator.isComplete(content))
    }

    @Test
    fun `truncated PNG without IEND chunk returns false`() {
        val file = tempFolder.newFile("truncated.png")
        val content = ByteArray(100)
        val magic = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        System.arraycopy(magic, 0, content, 0, magic.size)
        file.writeBytes(content)

        assertFalse(ImageIntegrityValidator.isComplete(file))
        assertFalse(ImageIntegrityValidator.isComplete(content))
    }

    @Test
    fun `valid WebP returns true`() {
        val file = tempFolder.newFile("valid.webp")
        val content = ByteArray(40)
        "RIFF".toByteArray().copyInto(content, 0)
        // size: 32 bytes (file length = 32 + 8 = 40)
        content[4] = 32
        content[5] = 0
        content[6] = 0
        content[7] = 0
        "WEBP".toByteArray().copyInto(content, 8)
        file.writeBytes(content)

        assertTrue(ImageIntegrityValidator.isComplete(file))
        assertTrue(ImageIntegrityValidator.isComplete(content))
    }

    @Test
    fun `truncated WebP returns false`() {
        val file = tempFolder.newFile("truncated.webp")
        val content = ByteArray(25) // Less than expected 40 bytes
        "RIFF".toByteArray().copyInto(content, 0)
        content[4] = 32
        content[5] = 0
        content[6] = 0
        content[7] = 0
        "WEBP".toByteArray().copyInto(content, 8)
        file.writeBytes(content)

        assertFalse(ImageIntegrityValidator.isComplete(file))
        assertFalse(ImageIntegrityValidator.isComplete(content))
    }

    @Test
    fun `empty and small files return false`() {
        val empty = tempFolder.newFile("empty.jpg")
        assertFalse(ImageIntegrityValidator.isComplete(empty))
        assertFalse(ImageIntegrityValidator.isComplete(ByteArray(0)))

        val tiny = tempFolder.newFile("tiny.jpg")
        tiny.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte()))
        assertFalse(ImageIntegrityValidator.isComplete(tiny))
        assertFalse(ImageIntegrityValidator.isComplete(byteArrayOf(0xFF.toByte(), 0xD8.toByte())))
    }

    @Test
    fun `HTML error page returns false`() {
        val html = tempFolder.newFile("error.html")
        val content = "<html><body>502 Bad Gateway</body></html>".toByteArray()
        html.writeBytes(content)

        assertFalse(ImageIntegrityValidator.isComplete(html))
        assertFalse(ImageIntegrityValidator.isComplete(content))
    }
}
