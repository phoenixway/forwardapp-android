package com.romankozak.forwardappmobile.ui.shared

import androidx.lifecycle.ViewModel

class NavigationResultViewModel : ViewModel() {
    private val results = mutableMapOf<String, Any?>()

    fun <T> setResult(
        key: String,
        value: T,
    ) {
        results[key] = value
    }

    fun <T> consumeResult(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return results.remove(key) as? T
    }
}
