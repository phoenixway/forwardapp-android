package com.romankozak.forwardappmobile.config

import com.romankozak.forwardappmobile.BuildConfig

object FeatureToggles {
    val attachmentsLibraryEnabled: Boolean
        get() = BuildConfig.DEBUG
}
