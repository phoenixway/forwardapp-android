package com.romankozak.forwardappmobile.shared.logging

import android.util.Log

actual fun logd(tag: String, message: String) {
    Log.d(tag, message)
}

actual fun logError(tag: String, message: String, throwable: Throwable?) {
    Log.e(tag, message, throwable)
}

