package com.romankozak.forwardappmobile.shared.logging

/**
 * Simple cross-platform logging levels.
 */
enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

/**
 * Platform-specific logger entry point.
 *
 * @param level importance/severity of the log entry
 * @param tag short identifier of the component producing the log
 * @param message message payload to record
 * @param throwable optional error to attach for diagnostics
 */
expect fun logMessage(
    level: LogLevel,
    tag: String,
    message: String,
    throwable: Throwable? = null,
)

inline fun logDebug(tag: String, message: String) {
    logMessage(LogLevel.DEBUG, tag, message)
}

inline fun logInfo(tag: String, message: String) {
    logMessage(LogLevel.INFO, tag, message)
}

inline fun logWarn(tag: String, message: String, throwable: Throwable? = null) {
    logMessage(LogLevel.WARN, tag, message, throwable)
}

inline fun logError(tag: String, message: String, throwable: Throwable? = null) {
    logMessage(LogLevel.ERROR, tag, message, throwable)
}
