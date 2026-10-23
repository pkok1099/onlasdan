package com.onlasdan.comic.ui.source

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object AppErrorHandler {
    fun toUserMessage(throwable: Throwable?): String = when (throwable) {
        is UnknownHostException -> "Tidak dapat terhubung ke server. Periksa koneksi internet Anda."

        is SocketTimeoutException -> "Waktu koneksi habis. Silakan coba lagi."

        is IOException -> "Terjadi kesalahan jaringan. Silakan coba lagi."

        null -> "Terjadi kesalahan yang tidak diketahui."

        else -> {
            val msg = throwable.message.orEmpty().lowercase()
            when {
                "timeout" in msg -> "Waktu koneksi habis. Silakan coba lagi."
                "unable to resolve" in msg || "no address" in msg -> "Tidak dapat terhubung ke server. Periksa koneksi Anda."
                "404" in msg || "not found" in msg -> "Konten tidak ditemukan atau telah dihapus."
                "500" in msg || "502" in msg || "503" in msg -> "Server sumber sedang mengalami gangguan. Coba lagi nanti."
                else -> "Gagal memuat data. Silakan coba lagi."
            }
        }
    }

    fun toUserMessage(errorMessage: String?): String {
        if (errorMessage.isNullOrBlank()) return "Gagal memuat data. Silakan coba lagi."
        val lower = errorMessage.lowercase()
        return when {
            "timeout" in lower -> "Waktu koneksi habis. Silakan coba lagi."
            "unable to resolve" in lower || "connection" in lower -> "Koneksi internet bermasalah. Periksa jaringan Anda."
            "404" in lower || "not found" in lower -> "Konten tidak ditemukan."
            "500" in lower || "502" in lower || "503" in lower -> "Server sumber sedang mengalami gangguan. Coba lagi nanti."
            else -> "Gagal memuat data. Silakan coba lagi."
        }
    }
}
