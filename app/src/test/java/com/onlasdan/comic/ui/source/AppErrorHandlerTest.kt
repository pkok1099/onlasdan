package com.onlasdan.comic.ui.source

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertTrue
import org.junit.Test

class AppErrorHandlerTest {

    @Test
    fun toUserMessage_unknownHostException_returnsNetworkAdvice() {
        val message = AppErrorHandler.toUserMessage(UnknownHostException("No address"))
        assertTrue(message.contains("Tidak dapat terhubung ke server"))
    }

    @Test
    fun toUserMessage_socketTimeoutException_returnsTimeoutAdvice() {
        val message = AppErrorHandler.toUserMessage(SocketTimeoutException("Read timed out"))
        assertTrue(message.contains("Waktu koneksi habis"))
    }

    @Test
    fun toUserMessage_ioException_returnsGenericNetworkAdvice() {
        val message = AppErrorHandler.toUserMessage(IOException("Connection reset"))
        assertTrue(message.contains("Terjadi kesalahan jaringan"))
    }

    @Test
    fun toUserMessage_nullThrowable_returnsUnknownError() {
        val message = AppErrorHandler.toUserMessage(null as Throwable?)
        assertTrue(message.contains("Terjadi kesalahan yang tidak diketahui"))
    }

    @Test
    fun toUserMessage_http404String_returnsNotFound() {
        val message = AppErrorHandler.toUserMessage(Exception("HTTP 404 Not Found"))
        assertTrue(message.contains("tidak ditemukan"))

        val strMessage = AppErrorHandler.toUserMessage("404 Not Found")
        assertTrue(strMessage.contains("tidak ditemukan"))
    }

    @Test
    fun toUserMessage_http500String_returnsServerDisturbance() {
        val message = AppErrorHandler.toUserMessage(Exception("503 Service Unavailable"))
        assertTrue(message.contains("Server sumber sedang mengalami gangguan"))

        val strMessage = AppErrorHandler.toUserMessage("502 Bad Gateway")
        assertTrue(strMessage.contains("Server sumber sedang mengalami gangguan"))
    }

    @Test
    fun toUserMessage_emptyOrNullString_returnsDefaultError() {
        val message = AppErrorHandler.toUserMessage("")
        assertTrue(message.contains("Gagal memuat data"))

        val nullMessage = AppErrorHandler.toUserMessage(null as String?)
        assertTrue(nullMessage.contains("Gagal memuat data"))
    }
}
