package com.example.ui.browser

import com.example.data.local.BrowserExtensionEntity
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ChromeWebStoreDownloader {

    const val CHROME_WEB_STORE_URL = "https://chromewebstore.google.com/?hl=ja"
    private const val CHROME_PROD_VERSION = "131.0.6778.204"
    private const val DESKTOP_CHROME_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$CHROME_PROD_VERSION Safari/537.36"
    private const val MAX_REDIRECTS = 6
    private const val STREAM_BUFFER_SIZE = 32 * 1024
    private const val ZIP_SCAN_LIMIT_BYTES = 256 * 1024

    private val RAW_ID_REGEX = Regex("^[a-p]{32}$")
    private val STORE_PATH_ID_REGEX = Regex(
        """(?:/webstore)?/detail/(?:[^/?#]+/)?([a-p]{32})(?:[/?#]|$)""",
        RegexOption.IGNORE_CASE
    )
    private val CRX_QUERY_ID_REGEX = Regex(
        """id(?:=|%3d)([a-p]{32})""",
        RegexOption.IGNORE_CASE
    )

    fun extractExtensionId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val lower = trimmed.lowercase()
        if (RAW_ID_REGEX.matches(lower)) {
            return lower
        }

        val pathMatch = STORE_PATH_ID_REGEX.find(trimmed)
        if (pathMatch != null) {
            return pathMatch.groupValues[1].lowercase()
        }

        val queryMatch = CRX_QUERY_ID_REGEX.find(trimmed)
        if (queryMatch != null) {
            return queryMatch.groupValues[1].lowercase()
        }

        return null
    }

    fun isChromeWebStoreDetailUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val lower = url.lowercase()
        val isStoreHost = lower.contains("chromewebstore.google.com/") ||
            lower.contains("chrome.google.com/webstore/")
        return isStoreHost && extractExtensionId(url) != null
    }

    fun cleanStorePageTitle(rawTitle: String, fallbackId: String): String {
        val cleaned = rawTitle
            .replace(" - Chrome ウェブストア", "")
            .replace(" - Chrome Web Store", "")
            .replace(" - Google Chrome", "")
            .trim()
        return cleaned.ifBlank { "Chrome 拡張機能 (${fallbackId.take(8)}…)" }
    }

    fun buildCrxDownloadUrl(extensionId: String): String {
        val cleanId = extensionId.trim().lowercase()
        return "https://clients2.google.com/service/update2/crx" +
            "?response=redirect" +
            "&prodversion=$CHROME_PROD_VERSION" +
            "&acceptformat=crx2,crx3" +
            "&x=id%3D${cleanId}%26uc"
    }

    fun buildStoreDetailUrl(extensionId: String): String {
        return "https://chromewebstore.google.com/detail/${extensionId.trim().lowercase()}?hl=ja"
    }

    /**
     * Strips CRX2 or CRX3 binary headers and returns the embedded ZIP archive bytes.
     * If [rawBytes] already begins with a standard ZIP header (`PK\x03\x04`), returns it as-is.
     */
    fun extractZipBytesFromCrx(rawBytes: ByteArray): ByteArray {
        val offset = computeZipOffsetFromHeader(rawBytes, rawBytes.size.toLong()).toInt()
        return if (offset == 0) rawBytes else rawBytes.copyOfRange(offset, rawBytes.size)
    }

    /**
     * Determines the byte offset where the embedded ZIP archive begins in a CRX2/CRX3 or ZIP file
     * without loading the entire file into memory.
     */
    fun findZipOffsetInCrxFile(file: File): Long {
        val fileLength = file.length()
        if (fileLength < 4L) {
            throw IllegalArgumentException("拡張機能ファイルのデータが破損しています")
        }

        val headerWindowSize = minOf(fileLength, ZIP_SCAN_LIMIT_BYTES.toLong()).toInt()
        val probe = ByteArray(minOf(fileLength, 16L).toInt())
        RandomAccessFile(file, "r").use { raf ->
            raf.readFully(probe)
            if (isZipSignature(probe, 0)) {
                return 0L
            }

            val isCrxMagic = probe[0] == 'C'.code.toByte() &&
                probe[1] == 'r'.code.toByte() &&
                probe[2] == '2'.code.toByte() &&
                probe[3] == '4'.code.toByte()

            if (!isCrxMagic) {
                throw IllegalArgumentException("有効な CRX または ZIP 形式の拡張機能ファイルではありません")
            }
            if (probe.size < 12) {
                throw IllegalArgumentException("CRX ヘッダーが不完全です")
            }

            val version = readLittleEndianInt(probe, 4)
            val candidateOffset: Long? = when (version) {
                2 -> {
                    if (probe.size < 16) {
                        throw IllegalArgumentException("CRX2 ヘッダーが不完全です")
                    }
                    val pubKeyLen = readLittleEndianUInt(probe, 8)
                    val sigLen = readLittleEndianUInt(probe, 12)
                    16L + pubKeyLen + sigLen
                }
                3 -> {
                    val headerSize = readLittleEndianUInt(probe, 8)
                    12L + headerSize
                }
                else -> null
            }

            if (candidateOffset != null && candidateOffset in 0L until (fileLength - 4L)) {
                val sigBuf = ByteArray(4)
                raf.seek(candidateOffset)
                raf.readFully(sigBuf)
                if (isZipSignature(sigBuf, 0)) {
                    return candidateOffset
                }
            }

            // Fallback scan within the first 256 KB
            val scanBuf = ByteArray(headerWindowSize)
            raf.seek(0L)
            raf.readFully(scanBuf)
            val found = findZipHeaderOffset(scanBuf)
                ?: throw IllegalArgumentException("CRX 内に拡張機能アーカイブ（ZIP）が見つかりませんでした")
            return found.toLong()
        }
    }

    /**
     * Opens a streaming [InputStream] positioned directly at the embedded ZIP payload of [file].
     * Supports arbitrarily large (25MB+) CRX/ZIP archives with minimal heap usage.
     */
    fun openZipStreamFromCrxFile(file: File): InputStream {
        val zipOffset = findZipOffsetInCrxFile(file)
        val fis = FileInputStream(file)
        try {
            if (zipOffset > 0L) {
                fis.channel.position(zipOffset)
            }
            return BufferedInputStream(fis, STREAM_BUFFER_SIZE)
        } catch (e: Exception) {
            runCatching { fis.close() }
            throw e
        }
    }

    /**
     * Streams a Chrome Web Store CRX package directly to [cacheDir], supporting large (>25MB)
     * extensions without loading the entire package into RAM.
     */
    fun downloadExtension(
        extensionId: String,
        cacheDir: File,
        fallbackName: String = "",
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): BrowserExtensionEntity {
        val cleanId = extractExtensionId(extensionId)
            ?: throw IllegalArgumentException("有効な Chrome ウェブストアのURLまたは32文字の拡張機能IDを入力してください")

        val downloadUrl = buildCrxDownloadUrl(cleanId)
        val tempFile = File.createTempFile("aegis_cws_${cleanId.take(8)}_", ".crx", cacheDir)
        try {
            downloadToFileFollowingRedirects(
                startUrl = downloadUrl,
                targetFile = tempFile,
                onProgress = onProgress
            )
            return ExtensionEngine.parseCrxOrZipFile(
                file = tempFile,
                fallbackName = fallbackName.ifBlank { "Chrome 拡張機能 (${cleanId.take(8)})" },
                customId = "cws_$cleanId",
                iconKey = "chrome_store"
            )
        } finally {
            runCatching { tempFile.delete() }
        }
    }

    private fun downloadToFileFollowingRedirects(
        startUrl: String,
        targetFile: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ) {
        var currentUrl = startUrl
        for (hop in 0..MAX_REDIRECTS) {
            val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "GET"
                connectTimeout = 20_000
                readTimeout = 120_000
                setRequestProperty("User-Agent", DESKTOP_CHROME_UA)
                setRequestProperty("Accept", "*/*")
            }

            try {
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                        ?: throw IOException("リダイレクト先が取得できませんでした (HTTP $code)")
                    currentUrl = URL(URL(currentUrl), location).toString()
                    continue
                }
                if (code == HttpURLConnection.HTTP_NO_CONTENT) {
                    throw IOException("Chrome ウェブストアからこの拡張機能を取得できませんでした（非公開または対象外）")
                }
                if (code != HttpURLConnection.HTTP_OK) {
                    throw IOException("ダウンロードに失敗しました (HTTP $code)")
                }

                val contentLength = connection.contentLengthLong
                val parentUsable = targetFile.parentFile?.usableSpace ?: Long.MAX_VALUE
                if (contentLength > 0L && contentLength > parentUsable - (16L * 1024L * 1024L)) {
                    throw IOException("端末の空き容量が不足しているためダウンロードできません")
                }

                var downloaded = 0L
                var lastReportBytes = 0L
                connection.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(STREAM_BUFFER_SIZE)
                        var read = input.read(buffer)
                        while (read >= 0) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (downloaded - lastReportBytes >= 256 * 1024) {
                                lastReportBytes = downloaded
                                onProgress(downloaded, contentLength)
                            }
                            read = input.read(buffer)
                        }
                        output.flush()
                    }
                }
                onProgress(downloaded, contentLength)

                if (downloaded <= 0L) {
                    throw IOException("空の拡張機能ファイルが返されました")
                }
                return
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("リダイレクト回数が上限を超えました")
    }

    private fun computeZipOffsetFromHeader(rawBytes: ByteArray, totalSize: Long): Long {
        if (rawBytes.size < 4) {
            throw IllegalArgumentException("拡張機能ファイルのデータが破損しています")
        }
        if (isZipSignature(rawBytes, 0)) {
            return 0L
        }
        val isCrxMagic = rawBytes[0] == 'C'.code.toByte() &&
            rawBytes[1] == 'r'.code.toByte() &&
            rawBytes[2] == '2'.code.toByte() &&
            rawBytes[3] == '4'.code.toByte()
        if (!isCrxMagic) {
            throw IllegalArgumentException("有効な CRX または ZIP 形式の拡張機能ファイルではありません")
        }
        if (rawBytes.size < 12) {
            throw IllegalArgumentException("CRX ヘッダーが不完全です")
        }
        val version = readLittleEndianInt(rawBytes, 4)
        val zipOffset = when (version) {
            2 -> {
                if (rawBytes.size < 16) {
                    throw IllegalArgumentException("CRX2 ヘッダーが不完全です")
                }
                16L + readLittleEndianUInt(rawBytes, 8) + readLittleEndianUInt(rawBytes, 12)
            }
            3 -> 12L + readLittleEndianUInt(rawBytes, 8)
            else -> {
                findZipHeaderOffset(rawBytes)?.toLong()
                    ?: throw IllegalArgumentException("未対応の CRX バージョンです (v$version)")
            }
        }
        return if (zipOffset in 0L until (totalSize - 4L) &&
            zipOffset <= Int.MAX_VALUE &&
            isZipSignature(rawBytes, zipOffset.toInt())
        ) {
            zipOffset
        } else {
            findZipHeaderOffset(rawBytes)?.toLong()
                ?: throw IllegalArgumentException("CRX 内に拡張機能アーカイブ（ZIP）が見つかりませんでした")
        }
    }

    private fun readLittleEndianInt(bytes: ByteArray, offset: Int): Int {
        return ByteBuffer.wrap(bytes, offset, 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .int
    }

    private fun readLittleEndianUInt(bytes: ByteArray, offset: Int): Long {
        return readLittleEndianInt(bytes, offset).toLong() and 0xFFFF_FFFFL
    }

    private fun isZipSignature(bytes: ByteArray, offset: Int): Boolean {
        if (offset < 0 || offset + 3 >= bytes.size) return false
        return bytes[offset] == 0x50.toByte() &&
            bytes[offset + 1] == 0x4B.toByte() &&
            bytes[offset + 2] == 0x03.toByte() &&
            bytes[offset + 3] == 0x04.toByte()
    }

    private fun findZipHeaderOffset(bytes: ByteArray): Int? {
        val limit = minOf(bytes.size - 4, ZIP_SCAN_LIMIT_BYTES)
        for (i in 0..limit) {
            if (isZipSignature(bytes, i)) return i
        }
        return null
    }
}
