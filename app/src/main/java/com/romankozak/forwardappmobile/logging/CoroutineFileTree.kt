package com.romankozak.forwardappmobile.logging

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CoroutineFileTree(
    private val logDir: File,
    private val fileName: String = "app.log",
    private val maxSizeBytes: Long = 1_000_000, // 1 MB
    private val maxBackups: Int = 3
) : Timber.Tree() {

    private val formatter =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private val channel = Channel<String>(capacity = Channel.BUFFERED)

    private var logFile: File

    init {
        if (!logDir.exists()) logDir.mkdirs()
        logFile = File(logDir, fileName)

        scope.launch {
            for (line in channel) {
                rotateIfNeeded()
                logFile.appendText(line)
            }
        }
    }

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?
    ) {
        val time = formatter.format(Date())
        val record = buildString {
            append("$time [${priorityToChar(priority)}]")
            append(" [${tag ?: "NO_TAG"}] ")
            append(message)
            append('\n')

            if (t != null) {
                append(Log.getStackTraceString(t))
                append('\n')
            }
        }

        // ❗ НЕ блокує викликаючий потік
        channel.trySend(record)
    }

    private fun rotateIfNeeded() {
        if (!logFile.exists()) return
        if (logFile.length() < maxSizeBytes) return

        for (i in maxBackups downTo 1) {
            val src = File(logDir, "$fileName.$i")
            val dst = File(logDir, "$fileName.${i + 1}")
            if (src.exists()) src.renameTo(dst)
        }

        logFile.renameTo(File(logDir, "$fileName.1"))
        logFile = File(logDir, fileName)
    }

    private fun priorityToChar(priority: Int): Char =
        when (priority) {
            Log.VERBOSE -> 'V'
            Log.DEBUG -> 'D'
            Log.INFO -> 'I'
            Log.WARN -> 'W'
            Log.ERROR -> 'E'
            Log.ASSERT -> 'A'
            else -> '?'
        }

    fun shutdown() {
        channel.close()
        scope.cancel()
    }
}
