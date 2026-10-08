package com.example.instructionmanual.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.instructionmanual.data.BackupManager
import com.example.instructionmanual.data.Device
import com.example.instructionmanual.data.ImportMode
import com.example.instructionmanual.data.ManualRepository
import com.example.instructionmanual.data.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface Screen {
    data object List : Screen
    data class Detail(val deviceId: String) : Screen
    data class Edit(val deviceId: String?) : Screen
    data class Viewer(val deviceId: String, val index: Int) : Screen
    data object Settings : Screen
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ManualRepository(app)

    val devices: StateFlow<List<Device>> = repo.devices
    val loading: StateFlow<Boolean> = repo.loading

    private val _backStack = MutableStateFlow<List<Screen>>(listOf(Screen.List))
    val current: StateFlow<Screen> = _backStack
        .map { it.last() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Screen.List)

    val canGoBack: StateFlow<Boolean> = _backStack
        .map { it.size > 1 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _category = MutableStateFlow<String?>(null)
    val category: StateFlow<String?> = _category.asStateFlow()

    private val _sort = MutableStateFlow(SortOrder.RecentUpdated)
    val sort: StateFlow<SortOrder> = _sort.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val visibleDevices: StateFlow<List<Device>> =
        combine(repo.devices, _query, _category, _sort) { list, q, cat, order ->
            val filtered = list.asSequence()
                .filter { cat == null || it.category == cat }
                .filter { it.matches(q) }
            when (order) {
                SortOrder.RecentUpdated -> filtered.sortedByDescending { it.updatedAt }
                SortOrder.RecentCreated -> filtered.sortedByDescending { it.createdAt }
                SortOrder.Name -> filtered.sortedWith(compareBy({ it.name.lowercase() }, { it.updatedAt }))
                SortOrder.Category -> filtered.sortedWith(
                    compareBy({ it.category }, { it.name.lowercase() })
                )
            }.toList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 已有数据中出现过的分类，用于筛选条。 */
    val categories: StateFlow<List<String>> = repo.devices
        .map { list -> list.map { it.category }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { repo.load() }
    }

    fun device(id: String): Device? = repo.devices.value.firstOrNull { it.id == id }

    fun imageFile(device: Device, fileName: String): File = File(repo.imageDir, fileName)

    // ---- 导航 ----

    fun navigate(screen: Screen) {
        _backStack.value = _backStack.value + screen
    }

    /** 返回上一层，返回 false 表示已在栈底。 */
    fun back(): Boolean {
        val stack = _backStack.value
        if (stack.size <= 1) return false
        _backStack.value = stack.dropLast(1)
        return true
    }

    fun goHome() {
        _backStack.value = listOf(Screen.List)
    }

    // ---- 列表筛选 ----

    fun setQuery(value: String) { _query.value = value }
    fun setCategory(value: String?) { _category.value = value }
    fun setSort(value: SortOrder) { _sort.value = value }

    // ---- 设备增删改 ----

    fun saveDevice(device: Device, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repo.saveDevice(device)
            onDone()
        }
    }

    fun deleteDevice(deviceId: String) {
        viewModelScope.launch {
            repo.deleteDevice(deviceId)
            back()
            _message.value = "已删除设备"
        }
    }

    // ---- 图片 ----

    fun addImageFromUri(deviceId: String, uri: Uri) {
        viewModelScope.launch {
            val ok = repo.addImageFromUri(deviceId, uri)
            _message.value = if (ok) "已添加说明书" else "图片导入失败"
        }
    }

    fun addImageFromFile(deviceId: String, file: File) {
        viewModelScope.launch {
            val ok = repo.addImageFromFile(deviceId, file)
            _message.value = if (ok) "已添加说明书" else "图片导入失败"
            file.delete()
        }
    }

    fun deleteImage(deviceId: String, imageId: String) {
        viewModelScope.launch {
            repo.deleteImage(deviceId, imageId)
            _message.value = "已删除图片"
        }
    }

    fun updateCaption(deviceId: String, imageId: String, caption: String) {
        viewModelScope.launch { repo.updateImageCaption(deviceId, imageId, caption) }
    }

    fun moveImage(deviceId: String, imageId: String, delta: Int) {
        viewModelScope.launch { repo.moveImage(deviceId, imageId, delta) }
    }

    // ---- 备份 ----

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _message.value = "正在导出…"
            val result = BackupManager.export(
                getApplication(),
                uri,
                repo.devices.value,
                repo.imageDir,
            )
            _message.value = result.fold(
                onSuccess = { "已导出 ${repo.devices.value.size} 台设备、$it 张图片" },
                onFailure = { "导出失败：${it.message ?: "未知错误"}" },
            )
        }
    }

    fun importBackup(uri: Uri, mode: ImportMode) {
        viewModelScope.launch {
            _message.value = "正在导入…"
            val result = BackupManager.read(getApplication(), uri, repo.imageDir)
            result.fold(
                onSuccess = { incoming ->
                    if (mode == ImportMode.Replace) repo.replaceAll(incoming) else repo.mergeAll(incoming)
                    _message.value = "已导入 ${incoming.size} 台设备"
                },
                onFailure = { _message.value = "导入失败：${it.message ?: "未知错误"}" },
            )
        }
    }

    fun usedBytes(): Long = repo.usedBytes()

    fun imageDir(): File = repo.imageDir

    fun consumeMessage() { _message.value = null }
}
