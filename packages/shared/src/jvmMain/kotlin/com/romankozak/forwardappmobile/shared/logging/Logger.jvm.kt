package com.romankozak.forwardappmobile.shared.logging

actual fun logMessage(
    level: LogLevel,
    tag: String,
    message: String,
    throwable: Throwable?
) {
    val prefix = "[$level][$tag]"
    if (throwable != null) {
        System.err.println("$prefix $message\n${throwable.stackTraceToString()}")
    } else {
        println("$prefix $message")
    }
}

actual fun logd(tag: String, message: String) {
    println("[DEBUG][$tag] $message")
}

actual fun logError(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        System.err.println("[ERROR][$tag] $message\n${throwable.stackTraceToString()}")
    } else {
        System.err.println("[ERROR][$tag] $message")
    }
}
