package com.example.ui.browser

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Safely imports a user-selected image from the zero-permission Android Photo Picker
 * or Storage Access Framework (`OpenDocument`) into app-private internal storage.
 *
 * Copies the URI stream once into a temporary file, decodes using [ImageDecoder] (API 28+,
 * supporting JPEG/PNG/WebP/GIF/HEIC/AVIF with automatic EXIF orientation and software allocation)
 * with a [BitmapFactory] + [ExifInterface] fallback, normalizes to an opaque ARGB_8888 bitmap,
 * and saves a verified JPEG into `filesDir/home_wallpapers/`.
 */
object HomeBackgroundStorage {

    private const val WALLPAPER_DIR_NAME = "home_wallpapers"
    private const val MAX_EDGE_PX = 2048
    private const val JPEG_QUALITY = 92

    fun getWallpaperDir(context: Context): File {
        return File(context.filesDir, WALLPAPER_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Reads, decodes, EXIF-orients, downscales if needed, and saves the image from [uri]
     * into internal storage, pruning older wallpaper files only after the new file is verified.
     * Returns the absolute file path.
     */
    fun savePickedImage(context: Context, uri: Uri): String {
        val resolver = context.contentResolver
        val tempFile = copyUriToTempFile(context, resolver, uri)

        try {
            val decoded = decodeBitmapFromFile(tempFile, MAX_EDGE_PX)
                ?: decodeBitmapDirectWithImageDecoder(resolver, uri, MAX_EDGE_PX)
                ?: throw IllegalStateException("対応していない画像形式か、ファイルを読み込めませんでした")

            val safeBitmap = ensureSoftwareOpaqueArgb8888(decoded)
            val finalBitmap = scaleDownIfNeeded(safeBitmap, MAX_EDGE_PX)

            val dir = getWallpaperDir(context)
            val outputFile = File(dir, "bg_${System.currentTimeMillis()}.jpg")
            try {
                FileOutputStream(outputFile).use { fos ->
                    if (!finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)) {
                        throw IllegalStateException("画像の保存に失敗しました")
                    }
                    fos.flush()
                }
                if (!outputFile.exists() || outputFile.length() <= 0L) {
                    runCatching { outputFile.delete() }
                    throw IllegalStateException("保存した画像ファイルが空です")
                }
            } finally {
                if (finalBitmap !== safeBitmap && !finalBitmap.isRecycled) finalBitmap.recycle()
                if (safeBitmap !== decoded && !safeBitmap.isRecycled) safeBitmap.recycle()
                if (!decoded.isRecycled) decoded.recycle()
            }

            // Remove older wallpaper files only after the new file is safely written
            dir.listFiles()?.forEach { file ->
                if (file.absolutePath != outputFile.absolutePath) {
                    runCatching { file.delete() }
                }
            }

            return outputFile.absolutePath
        } finally {
            runCatching { tempFile.delete() }
        }
    }

    /**
     * Copies the picked [uri] once into a local cache temp file so one-shot / Photo Picker /
     * cloud streams never fail due to multiple [ContentResolver.openInputStream] calls.
     */
    private fun copyUriToTempFile(
        context: Context,
        resolver: ContentResolver,
        uri: Uri
    ): File {
        val tempFile = File.createTempFile("bg_import_", ".img", context.cacheDir)
        var copiedBytes = 0L

        val copiedViaStream = runCatching {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    copiedBytes = input.copyTo(output, bufferSize = 32 * 1024)
                    output.flush()
                }
                true
            } ?: false
        }.getOrDefault(false)

        if (!copiedViaStream || copiedBytes <= 0L) {
            val copiedViaFd = runCatching {
                resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    FileInputStream(pfd.fileDescriptor).use { input ->
                        FileOutputStream(tempFile).use { output ->
                            copiedBytes = input.copyTo(output, bufferSize = 32 * 1024)
                            output.flush()
                        }
                    }
                    true
                } ?: false
            }.getOrDefault(false)

            if (!copiedViaFd || copiedBytes <= 0L) {
                runCatching { tempFile.delete() }
                throw IllegalStateException("画像ファイルを開けませんでした")
            }
        }

        return tempFile
    }

    private fun decodeBitmapFromFile(file: File, maxEdgePx: Int): Bitmap? {
        if (!file.exists() || file.length() <= 0L) return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val imageDecoderBitmap = runCatching {
                val source = ImageDecoder.createSource(file)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val srcW = info.size.width
                    val srcH = info.size.height
                    val longest = max(srcW, srcH)
                    if (longest > maxEdgePx && srcW > 0 && srcH > 0) {
                        val ratio = maxEdgePx.toFloat() / longest.toFloat()
                        val targetW = (srcW * ratio).roundToInt().coerceAtLeast(1)
                        val targetH = (srcH * ratio).roundToInt().coerceAtLeast(1)
                        decoder.setTargetSize(targetW, targetH)
                    }
                }
            }.getOrNull()
            if (imageDecoderBitmap != null) {
                return imageDecoderBitmap
            }
        }

        return decodeWithBitmapFactory(file, maxEdgePx)
    }

    private fun decodeBitmapDirectWithImageDecoder(
        resolver: ContentResolver,
        uri: Uri,
        maxEdgePx: Int
    ): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return runCatching {
            val source = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val srcW = info.size.width
                val srcH = info.size.height
                val longest = max(srcW, srcH)
                if (longest > maxEdgePx && srcW > 0 && srcH > 0) {
                    val ratio = maxEdgePx.toFloat() / longest.toFloat()
                    val targetW = (srcW * ratio).roundToInt().coerceAtLeast(1)
                    val targetH = (srcH * ratio).roundToInt().coerceAtLeast(1)
                    decoder.setTargetSize(targetW, targetH)
                }
            }
        }.getOrNull()
    }

    private fun decodeWithBitmapFactory(file: File, maxEdgePx: Int): Bitmap? {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxEdgePx)
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
                ?: return@runCatching null

            val orientation = runCatching {
                ExifInterface(file.absolutePath).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

            val oriented = applyExifOrientation(decoded, orientation)
            if (oriented !== decoded && !decoded.isRecycled) {
                decoded.recycle()
            }
            oriented
        }.getOrNull()
    }

    /**
     * Converts hardware, HDR (`RGBA_F16`), or alpha-channel bitmaps into an opaque software
     * `ARGB_8888` bitmap so JPEG compression and Compose drawing succeed on every device.
     */
    private fun ensureSoftwareOpaqueArgb8888(source: Bitmap): Bitmap {
        val isHardware = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            source.config == Bitmap.Config.HARDWARE
        if (!isHardware && source.config == Bitmap.Config.ARGB_8888 && !source.hasAlpha()) {
            return source
        }
        val softwareSource = if (isHardware) {
            source.copy(Bitmap.Config.ARGB_8888, false) ?: source
        } else {
            source
        }
        val output = Bitmap.createBitmap(
            softwareSource.width.coerceAtLeast(1),
            softwareSource.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(softwareSource, 0f, 0f, null)
        if (softwareSource !== source && !softwareSource.isRecycled) {
            softwareSource.recycle()
        }
        return output
    }

    /**
     * Loads a stored wallpaper file as a memory-safe software [Bitmap].
     */
    fun loadBitmap(path: String, maxEdgePx: Int = MAX_EDGE_PX): Bitmap? {
        if (path.isBlank()) return null
        val file = File(path)
        if (!file.exists() || !file.isFile || file.length() <= 0L) return null

        val factoryBitmap = runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            val opts = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxEdgePx)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        }.getOrNull()

        if (factoryBitmap != null) return factoryBitmap

        return decodeBitmapFromFile(file, maxEdgePx)
    }

    fun deleteAllWallpapers(context: Context) {
        val dir = File(context.filesDir, WALLPAPER_DIR_NAME)
        if (dir.exists()) {
            dir.listFiles()?.forEach { runCatching { it.delete() } }
        }
    }

    internal fun calculateInSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var inSampleSize = 1
        val longest = max(width, height)
        while ((longest / inSampleSize) > maxEdge * 2) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrDefault(bitmap)
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxEdge) return bitmap
        val ratio = maxEdge.toFloat() / longest.toFloat()
        val targetW = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
        return runCatching {
            Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        }.getOrDefault(bitmap)
    }
}
