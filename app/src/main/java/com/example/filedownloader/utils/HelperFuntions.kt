package com.example.filedownloader.utils

import kotlin.math.floor
import kotlin.text.*


fun formatBytes(bytes: Long): String {
    val kilobyte = 1024
    val megabyte = kilobyte * 1024
    val gigabyte = megabyte * 1024

    return when {
        bytes >= gigabyte -> String.format("%.1f GB", bytes.toDouble() / gigabyte)
        bytes >= megabyte -> String.format("%.1f MB", bytes.toDouble() / megabyte)
        bytes >= kilobyte -> String.format("%.1f KB", bytes.toDouble() / kilobyte)
        else -> "$bytes B"
    }
}

fun formatSpeed(bytesPerSecond: Double): String {
    val kilobyte = 1024
    val megabyte = kilobyte * 1024
    val gigabyte = megabyte * 1024

    return when {
        bytesPerSecond >= gigabyte -> String.format("%.1f GB/s", bytesPerSecond / gigabyte)
        bytesPerSecond >= megabyte -> String.format("%.1f MB/s", bytesPerSecond / megabyte)
        bytesPerSecond >= kilobyte -> String.format("%.1f KB/s", bytesPerSecond / kilobyte)
        else -> String.format("%.1f B/s", bytesPerSecond)
    }
}

fun getFloor(value: Float): Int {
    return floor(value).toInt();
}