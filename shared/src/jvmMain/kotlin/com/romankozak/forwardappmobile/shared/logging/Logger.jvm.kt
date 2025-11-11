package com.romankozak.forwardappmobile.shared.logging

actual fun logd(tag: String, message: String) {
    println("DEBUG [$tag]: $message")
}

actual fun logError(tag: String, message: String, throwable: Throwable?) {
    System.err.println("ERROR [$tag]: $message")
    throwable?.printStackTrace(System.err)
}

