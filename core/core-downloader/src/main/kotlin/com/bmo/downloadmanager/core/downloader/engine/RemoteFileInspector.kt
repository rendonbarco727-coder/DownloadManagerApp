package com.bmo.downloadmanager.core.downloader.engine

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

/**
 * Hace el HEAD request inicial para decidir cómo segmentar la descarga.
 *
 * Algunos servidores no responden bien a HEAD (lo ignoran, devuelven 405, o
 * no informan Content-Length aunque sí lo hagan en GET). Por eso inspect()
 * tiene un fallback: si HEAD falla o no trae Content-Length, se reintenta con
 * un GET de solo el primer byte (Range: bytes=0-0) para inferir soporte de
 * rangos a partir del código de respuesta 206, sin descargar el archivo
 * completo.
 */
class RemoteFileInspector @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {

    @Throws(IOException::class)
    fun inspect(
        url: String,
        refererHeader: String?,
        userAgentHeader: String?,
    ): RemoteFileInfo {
        val headInfo = runCatching { headRequest(url, refererHeader, userAgentHeader) }.getOrNull()

        if (headInfo != null && headInfo.contentLength > 0) {
            return headInfo
        }

        return rangeProbeRequest(url, refererHeader, userAgentHeader)
    }

    private fun headRequest(
        url: String,
        refererHeader: String?,
        userAgentHeader: String?,
    ): RemoteFileInfo {
        val request = buildRequest(url, refererHeader, userAgentHeader) { builder ->
            builder.head()
        }

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HEAD request falló con código ${response.code} para $url")
            }
            val contentLength = response.header("Content-Length")?.toLongOrNull() ?: -1L
            val acceptsRanges = response.header("Accept-Ranges")?.contains("bytes", ignoreCase = true) == true
            val mimeType = response.header("Content-Type")

            return RemoteFileInfo(
                contentLength = contentLength,
                acceptsRanges = acceptsRanges,
                mimeType = mimeType,
            )
        }
    }

    /**
     * Fallback cuando HEAD no informa el tamaño. Pide solo el primer byte;
     * si el servidor responde 206 Partial Content con Content-Range, se
     * extrae el tamaño total de ahí y se confirma soporte de rangos.
     */
    private fun rangeProbeRequest(
        url: String,
        refererHeader: String?,
        userAgentHeader: String?,
    ): RemoteFileInfo {
        val request = buildRequest(url, refererHeader, userAgentHeader) { builder ->
            builder.get().addHeader("Range", "bytes=0-0")
        }

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 206) {
                throw IOException("Range probe falló con código ${response.code} para $url")
            }

            val mimeType = response.header("Content-Type")
            val contentRange = response.header("Content-Range") // formato: "bytes 0-0/12345"
            val total = contentRange?.substringAfter('/')?.toLongOrNull() ?: -1L
            val acceptsRanges = response.code == 206

            return RemoteFileInfo(
                contentLength = total,
                acceptsRanges = acceptsRanges,
                mimeType = mimeType,
            )
        }
    }

    private inline fun buildRequest(
        url: String,
        refererHeader: String?,
        userAgentHeader: String?,
        configure: (Request.Builder) -> Request.Builder,
    ): Request {
        var builder = Request.Builder().url(url)
        refererHeader?.let { builder = builder.addHeader("Referer", it) }
        userAgentHeader?.let { builder = builder.addHeader("User-Agent", it) }
        builder = configure(builder)
        return builder.build()
    }
}
