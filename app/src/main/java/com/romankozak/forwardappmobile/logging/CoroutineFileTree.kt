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
    private val maxFileSizeBytes: Long = 5 * 1024 * 1024 // 5 MB
) : Timber.Tree() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val channel = Channel<String>(Channel.UNLIMITED)

    private var currentFile: File

    init {
        if (!logsDir.exists()) logsDir.mkdirs()
        currentFile = File(logsDir, "app.log")

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

        val rotated = File(
            logsDir,
            "app-${System.currentTimeMillis()}.log"
        )
        currentFile.renameTo(rotated)
        currentFile = File(logsDir, "app.log")
    }
}
