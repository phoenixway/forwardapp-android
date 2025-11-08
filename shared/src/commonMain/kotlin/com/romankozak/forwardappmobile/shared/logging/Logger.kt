package com.romankozak.forwardappmobile.shared.logging

expect fun logd(tag: String, message: String)
expect fun logError(tag: String, message: String, throwable: Throwable? = null)