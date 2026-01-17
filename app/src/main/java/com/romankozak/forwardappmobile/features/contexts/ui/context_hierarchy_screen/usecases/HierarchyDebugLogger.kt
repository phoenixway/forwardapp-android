package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import android.util.Log
import com.romankozak.forwardappmobile.BuildConfig

internal object HierarchyDebugLogger {
  private const val TAG = "HierarchyDebug"

  inline fun d(message: () -> String) {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, message())
    }
  }

  fun e(message: String, throwable: Throwable? = null) {
    Log.e(TAG, message, throwable)
  }
}
