package com.romankozak.forwardappmobile.logging

import android.util.Log
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class CoroutineFileTree(
    private val logsDir: File,
    private val maxFileSizeBytes: Long = 5 * 1024 * 1024, // 5 MB
    private val maxLogFiles: Int = 6,
) : Timber.Tree() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val channel = Channel<String>(Channel.UNLIMITED)

    private var currentFile: File
    private val currentFileName = "app_log.txt"

    init {
        if (!logsDir.exists()) logsDir.mkdirs()
        currentFile = File(logsDir, currentFileName)
        migrateLegacyLogIfNeeded()
        rotateOnStartupIfNeeded()

        scope.launch {
            for (line in channel) {
                try {
                    rotateIfNeeded()
                    currentFile.appendText(line)
                } catch (t: Throwable) {
                    Log.e("CoroutineFileTree", "File logging failed", t)
                }
            }
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val time = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS",
            Locale.US
        ).format(Date())

        val level = when (priority) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "A"
            else -> "?"
        }

        val sb = StringBuilder()
        sb.append(time)
            .append(" ")
            .append(level)
            .append("/")
            .append(tag ?: "App")
            .append(": ")
            .append(message)
            .append("\n")

        if (t != null) {
            sb.append(Log.getStackTraceString(t))
                .append("\n")
        }

        scope.launch {
            channel.send(sb.toString())
        }
    }

    private fun rotateIfNeeded() {
        if (currentFile.length() <= maxFileSizeBytes) return

        val rotated = File(logsDir, "app-${timestampForFilename()}.log")
        currentFile.renameTo(rotated)
        currentFile = File(logsDir, currentFileName)
        cleanupOldLogs()
    }

    private fun rotateOnStartupIfNeeded() {
        if (!currentFile.exists() || currentFile.length() == 0L) return
        val rotated = File(logsDir, "app-startup-${timestampForFilename()}.log")
        currentFile.renameTo(rotated)
        currentFile = File(logsDir, currentFileName)
        cleanupOldLogs()
    }

    private fun migrateLegacyLogIfNeeded() {
        val legacy = File(logsDir, "app.log")
        if (currentFile.exists()) return
        if (legacy.exists()) {
            legacy.renameTo(currentFile)
        }
    }

    private fun cleanupOldLogs() {
        val files =
            logsDir.listFiles()
                ?.filter { it.isFile }
                ?.filterNot { it.name == currentFileName }
                ?.sortedByDescending { it.lastModified() }
                .orEmpty()

        val allowed = (maxLogFiles - 1).coerceAtLeast(0)
        if (files.size <= allowed) return

        files.drop(allowed).forEach { it.delete() }
    }

    private fun timestampForFilename(): String {
        return SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
    }
}
