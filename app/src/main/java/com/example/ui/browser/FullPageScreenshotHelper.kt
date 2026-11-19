package com.example.ui.browser

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.WebView
import androidx.core.content.FileProvider
import com.example.adblock.AdBlockEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

data class CapturedScreenshot(
    val bitmap: Bitmap,
    val pageUrl: String,
    val pageTitle: String,
    val wasTruncated: Boolean
)

object FullPageScreenshotHelper {

    private const val MAX_OUTPUT_HEIGHT_PX = 12_000
    private const val MAX_TOTAL_PIXELS = 16_000_000L
    private const val MAX_VIEWPORT_SLICES = 18

    suspend fun captureFullPage(
        webView: WebView,
        pageUrl: String,
        pageTitle: String,
        onProgress: (currentStep: Int, totalSteps: Int) -> Unit = { _, _ -> }
    ): CapturedScreenshot? = withContext(Dispatchers.Main) {
        val viewWidth = webView.width
        val viewHeight = webView.height
        if (viewWidth <= 0 || viewHeight <= 0) return@withContext null

        val originalScrollX = webView.scrollX
        val originalScrollY = webView.scrollY

        val rawContentHeight = (webView.contentHeight * webView.scale)
            .roundToInt()
            .coerceAtLeast(viewHeight)

        val maxAllowedHeight = min(MAX_OUTPUT_HEIGHT_PX, viewHeight * MAX_VIEWPORT_SLICES)
        val captureContentHeight = min(rawContentHeight, maxAllowedHeight)
        val wasTruncated = rawContentHeight > captureContentHeight

        // Compute safe output scale to avoid OOM on ultra-tall or high-DPI pages
        val estimatedPixels = viewWidth.toLong() * captureContentHeight.toLong()
        val downscale = if (estimatedPixels > MAX_TOTAL_PIXELS) {
            kotlin.math.sqrt(MAX_TOTAL_PIXELS.toDouble() / estimatedPixels.toDouble()).toFloat()
        } else {
            1f
        }

        val outWidth = (viewWidth * downscale).roundToInt().coerceAtLeast(1)
        val outHeight = (captureContentHeight * downscale).roundToInt().coerceAtLeast(1)

        val fullBitmap = runCatching {
            Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        }.getOrNull() ?: return@withContext null

        val fullCanvas = Canvas(fullBitmap)
        fullCanvas.drawColor(Color.WHITE)

        val sliceBitmap = runCatching {
            Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
        }.getOrNull()

        if (sliceBitmap == null) {
            fullBitmap.recycle()
            return@withContext null
        }

        val sliceCanvas = Canvas(sliceBitmap)
        val totalSteps = ceil(captureContentHeight.toDouble() / viewHeight.toDouble())
            .toInt()
            .coerceAtLeast(1)

        try {
            var yOffset = 0
            var step = 0
            while (yOffset < captureContentHeight) {
                step++
                onProgress(step, totalSteps)

                // Clamp scroll target to max scrollable Y so final slice aligns cleanly
                val maxScrollY = (captureContentHeight - viewHeight).coerceAtLeast(0)
                val targetScrollY = min(yOffset, maxScrollY)
                webView.scrollTo(0, targetScrollY)
                // Allow Chromium compositor to rasterize scrolled content
                delay(90L)

                val actualScrollY = webView.scrollY
                sliceCanvas.drawColor(Color.WHITE)
                val saveSlice = sliceCanvas.save()
                sliceCanvas.translate(0f, -actualScrollY.toFloat())
                webView.draw(sliceCanvas)
                sliceCanvas.restoreToCount(saveSlice)

                val remainingHeight = captureContentHeight - yOffset
                val sliceHeightToCopy = min(viewHeight, remainingHeight)
                val srcTop = (yOffset - actualScrollY).coerceIn(0, (viewHeight - 1).coerceAtLeast(0))
                val srcBottom = (srcTop + sliceHeightToCopy).coerceAtMost(viewHeight)

                if (srcBottom > srcTop) {
                    val srcRect = android.graphics.Rect(0, srcTop, viewWidth, srcBottom)
                    val dstTop = (yOffset * downscale).roundToInt()
                    val dstBottom = ((yOffset + (srcBottom - srcTop)) * downscale)
                        .roundToInt()
                        .coerceAtMost(outHeight)
                    val dstRect = android.graphics.Rect(0, dstTop, outWidth, dstBottom)
                    fullCanvas.drawBitmap(sliceBitmap, srcRect, dstRect, null)
                }

                yOffset += viewHeight
            }

            CapturedScreenshot(
                bitmap = fullBitmap,
                pageUrl = pageUrl,
                pageTitle = pageTitle,
                wasTruncated = wasTruncated
            )
        } catch (t: Throwable) {
            fullBitmap.recycle()
            null
        } finally {
            sliceBitmap.recycle()
            webView.scrollTo(originalScrollX, originalScrollY)
        }
    }

    suspend fun saveToGallery(
        context: Context,
        screenshot: CapturedScreenshot
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = buildFileName(screenshot.pageUrl)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/AegisBrowser"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("保存先の作成に失敗しました")
                resolver.openOutputStream(uri)?.use { stream ->
                    check(screenshot.bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                        "PNGエンコードに失敗しました"
                    }
                } ?: error("出力ストリームを開けませんでした")

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                "Pictures/AegisBrowser/$fileName"
            } else {
                val dir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "AegisBrowser"
                ).apply { mkdirs() }
                val file = File(dir, fileName)
                FileOutputStream(file).use { stream ->
                    check(screenshot.bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                        "PNGエンコードに失敗しました"
                    }
                }
                file.absolutePath
            }
        }
    }

    suspend fun createShareIntent(
        context: Context,
        screenshot: CapturedScreenshot
    ): Result<Intent> = withContext(Dispatchers.IO) {
        runCatching {
            val cacheDir = File(context.cacheDir, "screenshots").apply { mkdirs() }
            val outFile = File(cacheDir, buildFileName(screenshot.pageUrl))
            FileOutputStream(outFile).use { stream ->
                check(screenshot.bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    "PNGエンコードに失敗しました"
                }
            }
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, screenshot.pageTitle.ifBlank { screenshot.pageUrl })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    private fun buildFileName(pageUrl: String): String {
        val host = AdBlockEngine.extractHost(pageUrl)
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(28)
            .ifBlank { "page" }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "aegis_${host}_${stamp}.png"
    }
}
