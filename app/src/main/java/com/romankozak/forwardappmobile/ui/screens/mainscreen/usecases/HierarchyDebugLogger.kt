package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

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
