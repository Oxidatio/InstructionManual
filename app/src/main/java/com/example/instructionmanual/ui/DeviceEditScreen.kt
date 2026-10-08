package com.example.instructionmanual.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.instructionmanual.data.DEFAULT_CATEGORY
import com.example.instructionmanual.data.Device
import com.example.instructionmanual.data.PRESET_CATEGORIES
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceEditScreen(
    existing: Device?,
    knownCategories: List<String>,
    onBack: () -> Unit,
    onSave: (Device) -> Unit,
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var brand by remember { mutableStateOf(existing?.brand.orEmpty()) }
    var model by remember { mutableStateOf(existing?.model.orEmpty()) }
    var category by remember { mutableStateOf(existing?.category ?: DEFAULT_CATEGORY) }
    var serial by remember { mutableStateOf(existing?.serialNumber.orEmpty()) }
    var purchaseDate by remember { mutableStateOf(existing?.purchaseDate.orEmpty()) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var nameError by remember { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }

    val categoryOptions = remember(knownCategories) {
        (PRESET_CATEGORIES + knownCategories).distinct()
    }

    fun submit() {
        if (name.isBlank()) {
            nameError = true
            return
        }
        val base = existing ?: Device(name = name.trim())
        onSave(
            base.copy(
                name = name.trim(),
                brand = brand.trim(),
                model = model.trim(),
                category = category.trim().ifBlank { DEFAULT_CATEGORY },
                serialNumber = serial.trim(),
                purchaseDate = purchaseDate.trim(),
                notes = notes.trim(),
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existing == null) "添加设备" else "编辑设备",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { submit() }) {
                        Icon(Icons.Default.Check, contentDescription = "保存")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (nameError) nameError = it.isNotBlank()
                },
                label = { Text("设备名称 *") },
                placeholder = { Text("例如：客厅路由器") },
                singleLine = true,
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("请填写设备名称") }
                } else null,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("品牌") },
                    placeholder = { Text("小米") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("型号") },
                    placeholder = { Text("AX3000") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Box {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("分类") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { categoryMenu = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "选择分类")
                        }
                    },
                )
                DropdownMenu(
                    expanded = categoryMenu,
                    onDismissRequest = { categoryMenu = false },
                ) {
                    categoryOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                category = option
                                categoryMenu = false
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = serial,
                onValueChange = { serial = it },
                label = { Text("序列号 / SN") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = purchaseDate,
                onValueChange = { purchaseDate = it },
                label = { Text("购买日期") },
                placeholder = { Text("2024-06-01") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                purchaseDate = String.format(
                                    "%04d-%02d-%02d", year, month + 1, day,
                                )
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH),
                        ).show()
                    }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "选择日期")
                    }
                },
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("备注") },
                placeholder = { Text("购买渠道、保修期、放置位置…") },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))

            androidx.compose.material3.Button(
                onClick = { submit() },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text("保存")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
