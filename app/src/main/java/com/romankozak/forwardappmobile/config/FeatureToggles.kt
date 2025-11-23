package com.romankozak.forwardappmobile.config

import com.romankozak.forwardappmobile.BuildConfig

object FeatureToggles {
    private val defaults: Map<FeatureFlag, Boolean> = mapOf(
        FeatureFlag.AttachmentsLibrary to BuildConfig.DEBUG,
        FeatureFlag.AllowSystemProjectMoves to false,
        FeatureFlag.PlanningModes to BuildConfig.DEBUG,
        FeatureFlag.WifiSync to BuildConfig.DEBUG,
        FeatureFlag.StrategicManagement to BuildConfig.DEBUG,
    )

    @Volatile
    private var overrides: Map<FeatureFlag, Boolean> = defaults

    fun isEnabled(flag: FeatureFlag): Boolean = overrides[flag] ?: defaults[flag] ?: false

    fun update(flag: FeatureFlag, enabled: Boolean) {
        overrides = overrides + (flag to enabled)
    }

    fun updateAll(flags: Map<FeatureFlag, Boolean>) {
        overrides = defaults + flags
    }
}
