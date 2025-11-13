package com.romankozak.forwardappmobile.shared.logging

import kotlin.js.console

actual fun logMessage(
    level: LogLevel,
    tag: String,
    message: String,
    throwable: Throwable?,
) {
    val payload =
        buildString {
            append('[')
            append(level.name)
            append("][")
            append(tag)
            append("] ")
            append(message)
            if (throwable != null) {
                append('\n')
                append(throwable.stackTraceToString())
            }
        }

    when (level) {
        LogLevel.ERROR -> console.error(payload)
        LogLevel.WARN -> console.warn(payload)
        LogLevel.INFO -> console.info(payload)
        LogLevel.DEBUG -> console.log(payload)
    }
}

// Convenience actuals mapped to logMessage
actual fun logd(tag: String, message: String) =
    logMessage(LogLevel.DEBUG, tag, message, null)

actual fun logError(tag: String, message: String, throwable: Throwable?) =
    logMessage(LogLevel.ERROR, tag, message, throwable)
