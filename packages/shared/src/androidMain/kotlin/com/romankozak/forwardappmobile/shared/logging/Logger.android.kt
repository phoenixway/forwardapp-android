package com.romankozak.forwardappmobile.shared.logging

import android.util.Log

actual fun logMessage(
    level: LogLevel,
    tag: String,
    message: String,
    throwable: Throwable?
) {
    when (level) {
        LogLevel.ERROR -> Log.e(tag, message, throwable)
        LogLevel.WARN -> Log.w(tag, message, throwable)
        LogLevel.INFO -> Log.i(tag, message, throwable)
        LogLevel.DEBUG -> Log.d(tag, message, throwable)
    }
}

actual fun logd(tag: String, message: String) {
    Log.d(tag, message)
}

actual fun logError(tag: String, message: String, throwable: Throwable?) {
    Log.e(tag, message, throwable)
}
