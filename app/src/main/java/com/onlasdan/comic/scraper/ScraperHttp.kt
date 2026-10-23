package com.onlasdan.comic.scraper

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Satu-satunya OkHttpClient aplikasi: satu connection pool + satu dispatcher
 * untuk semua scraper/API/unduhan. Varian unduhan hanya menaikkan read timeout.
 */
object HttpClients {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val downloadClient: OkHttpClient = client.newBuilder()
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
}

/**
 * Parse angka JSON secara defensif: nilai bertipe string ("12") tetap terbaca,
 * tipe lain/null/rusak -> null. Satu field rusak tidak boleh membunuh seluruh
 * respons (bug A13).
 */
internal fun JsonElement?.asDoubleOrNull(): Double? {
    val prim = (this as? JsonPrimitive)?.takeIf { it.isNumber || it.isString } ?: return null
    return runCatching { prim.asDouble }.getOrNull()
}

internal class ScraperHttp {
    private val client = HttpClients.client

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        val reqBuilder = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36",
            )
            .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")

        headers.forEach { (k, v) -> reqBuilder.header(k, v) }

        client.newCall(reqBuilder.build()).execute().use { res ->
            if (!res.isSuccessful) throw RuntimeException("HTTP ${res.code} for $url")
            return@use res.body?.string().orEmpty()
        }
    }
}
