package com.example.instructionmanual.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID
import kotlin.math.max

/** 图片入库结果。 */
data class StoredImage(val fileName: String, val width: Int, val height: Int)

/**
 * 把外部图片（相机 / 相册）规范化后存入 App 私有目录：
 * 按 EXIF 摆正方向、限制最长边、重新编码为 JPEG。
 */
object ImageStore {

    private const val MAX_DIMEN = 2400
    private const val JPEG_QUALITY = 88

    /** 从相册等 content Uri 导入。 */
    fun storeFromUri(context: Context, uri: Uri, destDir: File): StoredImage? {
        val temp = File.createTempFile("import_", ".tmp", context.cacheDir)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            store(temp, destDir)
        } catch (e: Exception) {
            null
        } finally {
            temp.delete()
        }
    }

    /** 从相机写入的临时文件导入。 */
    fun storeFromFile(source: File, destDir: File): StoredImage? =
        try {
            store(source, destDir)
        } catch (e: Exception) {
            null
        }

    private fun store(source: File, destDir: File): StoredImage? {
        if (!source.exists() || source.length() == 0L) return null
        destDir.mkdirs()

        val orientation = readOrientation(source)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMEN)
        }
        val decoded = BitmapFactory.decodeFile(source.absolutePath, options) ?: return null

        var working = applyOrientation(decoded, orientation)
        if (working !== decoded) decoded.recycle()

        val scaled = scaleDown(working, MAX_DIMEN)
        if (scaled !== working) working.recycle()
        working = scaled

        val fileName = "${UUID.randomUUID()}.jpg"
        val target = File(destDir, fileName)
        val result = StoredImage(fileName, working.width, working.height)
        val ok = target.outputStream().use { out ->
            working.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        working.recycle()
        if (!ok) {
            target.delete()
            return null
        }
        return result
    }

    /** 用 inSampleSize 只能按 2 的幂缩小，这里再精确缩放到目标最长边。 */
    private fun scaleDown(bitmap: Bitmap, maxDimen: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDimen) return bitmap
        val ratio = maxDimen.toFloat() / longest
        val target = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
        return target
    }

    private fun readOrientation(file: File): Int =
        try {
            ExifInterface(file.absolutePath)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f); matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f); matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimen: Int): Int {
        var sample = 1
        var longest = max(width, height)
        while (longest / 2 >= maxDimen) {
            longest /= 2
            sample *= 2
        }
        return sample
    }
}
