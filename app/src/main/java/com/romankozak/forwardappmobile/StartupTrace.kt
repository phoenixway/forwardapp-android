package com.romankozak.forwardappmobile

import android.os.SystemClock
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/** Temporary debug-only timing markers for cold-start diagnosis. */
internal object StartupTrace {
    private const val TAG = "ForwardStartup"
    private val processBaselineMillis = SystemClock.elapsedRealtime()
    private val emittedOnce = ConcurrentHashMap.newKeySet<String>()

    fun mark(event: String) {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, "+${SystemClock.elapsedRealtime() - processBaselineMillis}ms $event")
    }

    fun markOnce(event: String) {
        if (!BuildConfig.DEBUG) return
        if (emittedOnce.add(event)) mark(event)
    }

    fun <T> measureSync(
        event: String,
        block: () -> T,
    ): T {
        if (!BuildConfig.DEBUG) return block()
        val startedAt = SystemClock.elapsedRealtime()
        mark("$event.begin")
        return try {
            block()
        } finally {
            val duration = SystemClock.elapsedRealtime() - startedAt
            mark("$event.end duration=${duration}ms")
        }
    }

    suspend fun <T> measure(
        event: String,
        block: suspend () -> T,
    ): T {
        if (!BuildConfig.DEBUG) return block()
        val startedAt = SystemClock.elapsedRealtime()
        mark("$event.begin")
        return try {
            block()
        } finally {
            val duration = SystemClock.elapsedRealtime() - startedAt
            mark("$event.end duration=${duration}ms")
        }
    }
}
