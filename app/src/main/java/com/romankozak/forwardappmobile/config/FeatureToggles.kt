package com.romankozak.forwardappmobile.config

import com.romankozak.forwardappmobile.BuildConfig

object FeatureToggles {
    private val experimentalFlags = setOf(
        FeatureFlag.AttachmentsLibrary,
        FeatureFlag.ScriptsLibrary,
        FeatureFlag.PlanningModes,
        FeatureFlag.WifiSync,
        FeatureFlag.StrategicManagement,
        FeatureFlag.AiChat,
        FeatureFlag.AiInsights,
        FeatureFlag.AiLifeManagement,
    )

    private val defaults: Map<FeatureFlag, Boolean> = mapOf(
        FeatureFlag.AttachmentsLibrary to BuildConfig.IS_EXPERIMENTAL_BUILD,
        FeatureFlag.ScriptsLibrary to BuildConfig.IS_EXPERIMENTAL_BUILD,
        FeatureFlag.AllowSystemProjectMoves to false,
        FeatureFlag.PlanningModes to BuildConfig.IS_EXPERIMENTAL_BUILD,
        FeatureFlag.WifiSync to BuildConfig.IS_EXPERIMENTAL_BUILD,
        FeatureFlag.StrategicManagement to BuildConfig.IS_EXPERIMENTAL_BUILD,
        FeatureFlag.AiChat to BuildConfig.IS_EXPERIMENTAL_BUILD,
        FeatureFlag.AiInsights to BuildConfig.IS_EXPERIMENTAL_BUILD,
        FeatureFlag.AiLifeManagement to BuildConfig.IS_EXPERIMENTAL_BUILD,
    )

    @Volatile
    private var overrides: Map<FeatureFlag, Boolean> = defaults

    fun isEnabled(flag: FeatureFlag): Boolean {
        if (!BuildConfig.IS_EXPERIMENTAL_BUILD && experimentalFlags.contains(flag)) {
            return false
        }
        return overrides[flag] ?: defaults[flag] ?: false
    }

    fun update(flag: FeatureFlag, enabled: Boolean) {
        overrides = overrides + (flag to enabled)
    }

    fun updateAll(flags: Map<FeatureFlag, Boolean>) {
        overrides = defaults + flags
    }
}
