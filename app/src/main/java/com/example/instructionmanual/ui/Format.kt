package com.example.instructionmanual.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

fun formatDate(millis: Long): String = dateFormat.format(Date(millis))

fun formatSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> String.format(Locale.US, "%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0)
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
    bytes >= 1024L -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}
