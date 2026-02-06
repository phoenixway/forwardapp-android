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

/**
 * Timber Tree, який асинхронно пише логи у файл у external storage.
 */
class CoroutineFileTree(
    logsDir: File,
    private val maxFileSizeBytes: Long = 5 * 1024 * 1024 // 5 MB
) : Timber.Tree() {

    private val logChannel = Channel<String>(Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentLogFile: File

    init {
        if (!logsDir.exists()) logsDir.mkdirs()
        currentLogFile = File(logsDir, "app.log")

        scope.launch {
            for (line in logChannel) {
                try {
                    checkRotate()
                    currentLogFile.appendText(line + "\n")
                } catch (t: Throwable) {
                    Log.e("CoroutineFileTree", "Error writing log", t)
                }
            }
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val lvl = when (priority) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "A"
            else -> "?"
        }

        val logLine = "$ts $lvl/${tag ?: "App"}: $message" +
                (t?.let { "\n${Log.getStackTraceString(it)}" } ?: "")

        // Відправляємо в корутину
        scope.launch { logChannel.send(logLine) }
    }

    private fun checkRotate() {
        if (currentLogFile.length() > maxFileSizeBytes) {
            val oldFile = File(currentLogFile.parent, "app-${System.currentTimeMillis()}.log")
            currentLogFile.renameTo(oldFile)
            currentLogFile = File(currentLogFile.parent, "app.log")
        }
    }
}
