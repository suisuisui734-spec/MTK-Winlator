package com.mtk.winlator.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs
import java.io.File

/** Lightweight, read-only device diagnostics. */
object DeviceDiagnostics {
    data class MemorySnapshot(val totalBytes: Long, val availableBytes: Long, val lowMemory: Boolean)

    fun memory(context: Context): MemorySnapshot? = runCatching {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        MemorySnapshot(info.totalMem, info.availMem, info.lowMemory)
    }.getOrNull()

    fun internalStorage(context: Context): Pair<Long, Long>? = runCatching {
        val stat = StatFs(context.filesDir.absolutePath)
        stat.availableBytes to stat.totalBytes
    }.getOrNull()

    fun formatBytes(bytes: Long): String {
        if (bytes < 0) return "Unknown"
        val gib = bytes / (1024.0 * 1024.0 * 1024.0)
        val mib = bytes / (1024.0 * 1024.0)
        return if (gib >= 1.0) String.format("%.2f GB", gib) else String.format("%.0f MB", mib)
    }

    fun is64BitCapable(): Boolean = android.os.Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
}
