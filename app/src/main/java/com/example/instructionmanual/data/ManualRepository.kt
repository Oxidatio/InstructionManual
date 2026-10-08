package com.example.instructionmanual.data

import android.content.Context
import android.net.Uri
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 设备说明书仓库：数据以 JSON 文件形式保存在 App 私有目录，图片存放在 images/ 子目录。
 * 不依赖网络与服务器。
 */
class ManualRepository(private val context: Context) {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val dataFile = File(context.filesDir, DATA_FILE)
    val imageDir: File = File(context.filesDir, IMAGE_DIR).apply { mkdirs() }

    private val mutex = Mutex()

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun imageFile(image: ManualImage): File = File(imageDir, image.fileName)

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val loaded = runCatching {
                if (!dataFile.exists()) {
                    emptyList()
                } else {
                    val type = object : TypeToken<List<Device>>() {}.type
                    gson.fromJson<List<Device>>(dataFile.readText(), type) ?: emptyList()
                }
            }.getOrElse { emptyList() }

            // 丢掉图片文件已丢失的条目
            _devices.value = loaded.map { device ->
                device.copy(images = device.images.filter { imageFile(it).exists() })
            }
            _loading.value = false
        }
    }

    suspend fun saveDevice(device: Device) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val updated = device.copy(updatedAt = System.currentTimeMillis())
            val list = _devices.value.toMutableList()
            val index = list.indexOfFirst { it.id == updated.id }
            if (index >= 0) list[index] = updated else list.add(0, updated)
            commit(list)
        }
    }

    suspend fun deleteDevice(deviceId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val device = _devices.value.firstOrNull { it.id == deviceId } ?: return@withLock
            device.images.forEach { imageFile(it).delete() }
            commit(_devices.value.filterNot { it.id == deviceId })
        }
    }

    /** 从相册导入一张说明书图片。 */
    suspend fun addImageFromUri(deviceId: String, uri: Uri, caption: String = ""): Boolean =
        withContext(Dispatchers.IO) {
            val stored = ImageStore.storeFromUri(context, uri, imageDir) ?: return@withContext false
            attachImage(deviceId, stored, caption)
        }

    /** 从相机临时文件导入一张说明书图片。 */
    suspend fun addImageFromFile(deviceId: String, source: File, caption: String = ""): Boolean =
        withContext(Dispatchers.IO) {
            val stored = ImageStore.storeFromFile(source, imageDir) ?: return@withContext false
            attachImage(deviceId, stored, caption)
        }

    private suspend fun attachImage(deviceId: String, stored: StoredImage, caption: String): Boolean =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val list = _devices.value.toMutableList()
                val index = list.indexOfFirst { it.id == deviceId }
                if (index < 0) {
                    File(imageDir, stored.fileName).delete()
                    return@withLock false
                }
                val device = list[index]
                val image = ManualImage(
                    fileName = stored.fileName,
                    caption = caption,
                    width = stored.width,
                    height = stored.height,
                )
                list[index] = device.copy(
                    images = device.images + image,
                    updatedAt = System.currentTimeMillis(),
                )
                commit(list)
                true
            }
        }

    suspend fun updateImageCaption(deviceId: String, imageId: String, caption: String) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                mutateDevice(deviceId) { device ->
                    device.copy(
                        images = device.images.map {
                            if (it.id == imageId) it.copy(caption = caption) else it
                        },
                        updatedAt = System.currentTimeMillis(),
                    )
                }
            }
        }

    suspend fun deleteImage(deviceId: String, imageId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            var removed: ManualImage? = null
            mutateDevice(deviceId) { device ->
                removed = device.images.firstOrNull { it.id == imageId }
                device.copy(
                    images = device.images.filterNot { it.id == imageId },
                    updatedAt = System.currentTimeMillis(),
                )
            }
            removed?.let { imageFile(it).delete() }
        }
    }

    /** 把图片在说明书列表中的顺序前移/后移一位。 */
    suspend fun moveImage(deviceId: String, imageId: String, delta: Int) = withContext(Dispatchers.IO) {
        mutex.withLock {
            mutateDevice(deviceId) { device ->
                val images = device.images.toMutableList()
                val from = images.indexOfFirst { it.id == imageId }
                val to = from + delta
                if (from < 0 || to < 0 || to >= images.size) {
                    device
                } else {
                    images.add(to, images.removeAt(from))
                    device.copy(images = images, updatedAt = System.currentTimeMillis())
                }
            }
        }
    }

    /** 覆盖全部数据（导入备份用）。 */
    suspend fun replaceAll(newDevices: List<Device>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val keep = newDevices.flatMap { it.images }.map { it.fileName }.toSet()
            imageDir.listFiles()?.forEach { file ->
                if (file.name !in keep) file.delete()
            }
            commit(newDevices)
        }
    }

    /** 合并导入的数据（同 id 保留较新的）。 */
    suspend fun mergeAll(newDevices: List<Device>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val byId = _devices.value.associateBy { it.id }.toMutableMap()
            newDevices.forEach { incoming ->
                val existing = byId[incoming.id]
                if (existing == null || incoming.updatedAt > existing.updatedAt) {
                    byId[incoming.id] = incoming
                }
            }
            commit(byId.values.sortedByDescending { it.updatedAt })
        }
    }

    private inline fun mutateDevice(deviceId: String, transform: (Device) -> Device) {
        val list = _devices.value.toMutableList()
        val index = list.indexOfFirst { it.id == deviceId }
        if (index < 0) return
        list[index] = transform(list[index])
        commit(list)
    }

    private fun commit(list: List<Device>) {
        _devices.value = list
        dataFile.writeText(gson.toJson(list))
    }

    /** 统计占用空间（字节）。 */
    fun usedBytes(): Long {
        val images = imageDir.listFiles()?.sumOf { it.length() } ?: 0L
        return images + (if (dataFile.exists()) dataFile.length() else 0L)
    }

    fun imageCount(): Int = _devices.value.sumOf { it.images.size }

    companion object {
        private const val DATA_FILE = "devices.json"
        private const val IMAGE_DIR = "images"
    }
}
