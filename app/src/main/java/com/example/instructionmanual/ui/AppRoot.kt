package com.example.instructionmanual.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.instructionmanual.data.Device

@Composable
fun AppRoot(viewModel: AppViewModel = viewModel()) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val visibleDevices by viewModel.visibleDevices.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val current by viewModel.current.collectAsStateWithLifecycle()
    val canGoBack by viewModel.canGoBack.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.category.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val imageDir = remember { viewModel.imageDir() }

    LaunchedEffect(message) {
        val text = message
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.consumeMessage()
        }
    }

    BackHandler(enabled = canGoBack) { viewModel.back() }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val screen = current) {
            is Screen.List -> DeviceListScreen(
                devices = visibleDevices,
                categories = categories,
                query = query,
                selectedCategory = selectedCategory,
                sort = sort,
                totalCount = devices.size,
                onQueryChange = viewModel::setQuery,
                onCategoryChange = viewModel::setCategory,
                onSortChange = viewModel::setSort,
                onOpenDevice = { viewModel.navigate(Screen.Detail(it)) },
                onAddDevice = { viewModel.navigate(Screen.Edit(null)) },
                onOpenSettings = { viewModel.navigate(Screen.Settings) },
                imageDir = imageDir,
            )

            is Screen.Detail -> {
                val device = devices.firstOrNull { it.id == screen.deviceId }
                if (device == null) {
                    LaunchedEffect(screen.deviceId) { viewModel.goHome() }
                } else {
                    DeviceDetailScreen(
                        device = device,
                        imageDir = imageDir,
                        onBack = { viewModel.back() },
                        onEdit = { viewModel.navigate(Screen.Edit(device.id)) },
                        onDelete = { viewModel.deleteDevice(device.id) },
                        onOpenViewer = { index -> viewModel.navigate(Screen.Viewer(device.id, index)) },
                        onPickImage = { viewModel.addImageFromUri(device.id, it) },
                        onCaptureImage = { viewModel.addImageFromFile(device.id, it) },
                        onDeleteImage = { viewModel.deleteImage(device.id, it.id) },
                        onRenameImage = { image, caption ->
                            viewModel.updateCaption(device.id, image.id, caption)
                        },
                    )
                }
            }

            is Screen.Edit -> {
                val existing: Device? = screen.deviceId?.let { id ->
                    devices.firstOrNull { it.id == id }
                }
                DeviceEditScreen(
                    existing = existing,
                    knownCategories = categories,
                    onBack = { viewModel.back() },
                    onSave = { device -> viewModel.saveDevice(device) { viewModel.back() } },
                )
            }

            is Screen.Viewer -> {
                val device = devices.firstOrNull { it.id == screen.deviceId }
                if (device == null || device.images.isEmpty()) {
                    LaunchedEffect(screen.deviceId) { viewModel.back() }
                } else {
                    ImageViewerScreen(
                        title = device.name,
                        images = device.images,
                        imageDir = imageDir,
                        startIndex = screen.index,
                        onBack = { viewModel.back() },
                    )
                }
            }

            is Screen.Settings -> SettingsScreen(
                deviceCount = devices.size,
                imageCount = devices.sumOf { it.images.size },
                usedBytes = remember(devices) { viewModel.usedBytes() },
                onBack = { viewModel.back() },
                onExport = { viewModel.exportBackup(it) },
                onImport = { uri, mode -> viewModel.importBackup(uri, mode) },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}
