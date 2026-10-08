package com.example.instructionmanual.data

import android.content.Context
import android.net.Uri
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** 备份包格式：devices.json + images/<文件名>。 */
object BackupManager {

    private const val ENTRY_DEVICES = "devices.json"
    private const val ENTRY_IMAGES = "images/"
    private const val BUFFER = 64 * 1024

    private val gson = GsonBuilder().setPrettyPrinting().create()

    /** 导出为 zip。返回导出的图片数量。 */
    suspend fun export(
        context: Context,
        target: Uri,
        devices: List<Device>,
        imageDir: File,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val stream = context.contentResolver.openOutputStream(target)
                ?: error("无法写入所选位置")
            var count = 0
            ZipOutputStream(BufferedOutputStream(stream, BUFFER)).use { zip ->
                zip.putNextEntry(ZipEntry(ENTRY_DEVICES))
                zip.write(gson.toJson(devices).toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                val keep = devices.flatMap { it.images }.map { it.fileName }.toSet()
                keep.forEach { name ->
                    val file = File(imageDir, name)
                    if (!file.exists()) return@forEach
                    zip.putNextEntry(ZipEntry("$ENTRY_IMAGES$name"))
                    file.inputStream().use { it.copyTo(zip, BUFFER) }
                    zip.closeEntry()
                    count++
                }
            }
            count
        }
    }

    /** 从 zip 读取设备列表，并把图片解压到 [imageDir]。 */
    suspend fun read(
        context: Context,
        source: Uri,
        imageDir: File,
    ): Result<List<Device>> = withContext(Dispatchers.IO) {
        runCatching {
            imageDir.mkdirs()
            val stream = context.contentResolver.openInputStream(source)
                ?: error("无法读取所选文件")
            var devices: List<Device>? = null

            ZipInputStream(BufferedInputStream(stream, BUFFER)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    when {
                        entry.name == ENTRY_DEVICES -> {
                            val text = zip.readBytes().toString(Charsets.UTF_8)
                            val type = object : TypeToken<List<Device>>() {}.type
                            devices = gson.fromJson<List<Device>>(text, type) ?: emptyList()
                        }

                        entry.name.startsWith(ENTRY_IMAGES) && !entry.isDirectory -> {
                            val name = entry.name.removePrefix(ENTRY_IMAGES)
                            if (name.isNotBlank() && !name.contains('/') && !name.contains('\\')) {
                                val out = File(imageDir, name)
                                out.outputStream().use { zip.copyTo(it, BUFFER) }
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }
            devices ?: error("备份包里没有 devices.json，文件可能已损坏")
        }
    }
}
