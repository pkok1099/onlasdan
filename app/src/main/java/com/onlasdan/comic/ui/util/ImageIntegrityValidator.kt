package com.onlasdan.comic.ui.util

import java.io.File
import java.io.RandomAccessFile

/**
 * Validasi integritas file gambar (JPEG, PNG, WebP) untuk mencegah
 * rendering atau penyimpanan gambar terpotong secara diam-diam.
 */
object ImageIntegrityValidator {

    private const val MIN_IMAGE_SIZE = 32L
    private const val TAIL_SCAN_BYTES = 256

    fun isComplete(file: File): Boolean {
        if (!file.exists() || !file.isFile) return false
        val len = file.length()
        if (len < MIN_IMAGE_SIZE) return false

        return try {
            RandomAccessFile(file, "r").use { raf ->
                val header = ByteArray(16)
                if (raf.read(header) < 16) return false
                checkIntegrity(header, len) { offset, bytes ->
                    raf.seek(offset)
                    raf.readFully(bytes)
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    fun isComplete(bytes: ByteArray): Boolean {
        val len = bytes.size.toLong()
        if (len < MIN_IMAGE_SIZE) return false
        val headerSize = minOf(bytes.size, 16)
        val header = bytes.copyOfRange(0, headerSize)
        return checkIntegrity(header, len) { offset, out ->
            System.arraycopy(bytes, offset.toInt(), out, 0, out.size)
        }
    }

    private inline fun checkIntegrity(
        header: ByteArray,
        totalLen: Long,
        readBytesAt: (offset: Long, out: ByteArray) -> Unit,
    ): Boolean = when {
        isJpeg(header) -> verifyJpeg(totalLen, readBytesAt)
        isPng(header) -> verifyPng(totalLen, readBytesAt)
        isWebp(header) -> verifyWebp(header, totalLen)
        isGif(header) -> totalLen >= MIN_IMAGE_SIZE
        else -> verifyGenericBinary(header, totalLen)
    }

    private fun isJpeg(header: ByteArray): Boolean = header.size >= 2 && header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte()

    private inline fun verifyJpeg(
        totalLen: Long,
        readBytesAt: (offset: Long, out: ByteArray) -> Unit,
    ): Boolean {
        val tailSize = minOf(totalLen, TAIL_SCAN_BYTES.toLong()).toInt()
        val tail = ByteArray(tailSize)
        readBytesAt(totalLen - tailSize, tail)
        for (i in tailSize - 2 downTo 0) {
            if (tail[i] == 0xFF.toByte() && tail[i + 1] == 0xD9.toByte()) {
                return true
            }
        }
        return false
    }

    private val pngIend = byteArrayOf('I'.code.toByte(), 'E'.code.toByte(), 'N'.code.toByte(), 'D'.code.toByte())

    private fun isPng(header: ByteArray): Boolean = header.size >= 8 &&
        header[0] == 0x89.toByte() && header[1] == 0x50.toByte() &&
        header[2] == 0x4E.toByte() && header[3] == 0x47.toByte() &&
        header[4] == 0x0D.toByte() && header[5] == 0x0A.toByte() &&
        header[6] == 0x1A.toByte() && header[7] == 0x0A.toByte()

    private inline fun verifyPng(
        totalLen: Long,
        readBytesAt: (offset: Long, out: ByteArray) -> Unit,
    ): Boolean {
        if (totalLen < 12) return false
        val scanSize = minOf(totalLen, 64L).toInt()
        val tail = ByteArray(scanSize)
        readBytesAt(totalLen - scanSize, tail)
        for (i in 0..scanSize - 4) {
            if (matchesAt(tail, i, pngIend)) return true
        }
        return false
    }

    private fun matchesAt(source: ByteArray, offset: Int, target: ByteArray): Boolean {
        for (j in target.indices) {
            if (source[offset + j] != target[j]) return false
        }
        return true
    }

    private fun isWebp(header: ByteArray): Boolean = header.size >= 12 &&
        header[0] == 'R'.code.toByte() && header[1] == 'I'.code.toByte() &&
        header[2] == 'F'.code.toByte() && header[3] == 'F'.code.toByte() &&
        header[8] == 'W'.code.toByte() && header[9] == 'E'.code.toByte() &&
        header[10] == 'B'.code.toByte() && header[11] == 'P'.code.toByte()

    private fun verifyWebp(header: ByteArray, totalLen: Long): Boolean {
        val riffSize = (header[4].toInt() and 0xFF) or
            ((header[5].toInt() and 0xFF) shl 8) or
            ((header[6].toInt() and 0xFF) shl 16) or
            ((header[7].toInt() and 0xFF) shl 24)
        val expectedSize = (riffSize.toLong() and 0xFFFFFFFFL) + 8L
        return totalLen >= expectedSize
    }

    private fun isGif(header: ByteArray): Boolean = header.size >= 3 && header[0] == 'G'.code.toByte() && header[1] == 'I'.code.toByte() && header[2] == 'F'.code.toByte()

    private fun verifyGenericBinary(header: ByteArray, totalLen: Long): Boolean {
        val isText = header[0] == '<'.code.toByte() || header[0] == '{'.code.toByte()
        return !isText && totalLen > 512
    }
}
