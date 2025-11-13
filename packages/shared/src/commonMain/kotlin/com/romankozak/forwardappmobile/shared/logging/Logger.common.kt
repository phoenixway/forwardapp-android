package com.romankozak.forwardappmobile.shared.logging

enum class LogLevel { ERROR, WARN, INFO, DEBUG }

expect fun logMessage(
    level: LogLevel,
    tag: String,
    message: String,
    throwable: Throwable? = null,
)

