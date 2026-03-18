package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.data.logic.ContextMarkerHandler
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.settings.settings.models.PlanningSettings
import com.romankozak.forwardappmobile.ui.dialogs.UiContextMarker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class SettingsUseCase
    @Inject
    constructor(
        private val settingsRepo: SettingsRepository,
        private val contextMarkerHandler: ContextMarkerHandler,
    ) {
        fun saveSettings(
            scope: CoroutineScope,
            settings: PlanningSettings,
        ) {
            scope.launch {
                settingsRepo.saveShowPlanningModes(settings.showModes)
                settingsRepo.saveDailyTag(settings.dailyTag.trim())
                settingsRepo.saveMediumTag(settings.mediumTag.trim())
                settingsRepo.saveLongTag(settings.longTag.trim())
                settingsRepo.saveObsidianVaultName(settings.vaultName.trim())
            }
        }

        fun saveAllContextMarkers(
            scope: CoroutineScope,
            updatedContextMarkers: List<UiContextMarker>,
        ) {
            scope.launch {
                val customContextMarkers = updatedContextMarkers.filter { !it.isReserved }
                val reservedContextMarkers = updatedContextMarkers.filter { it.isReserved }

                settingsRepo.saveCustomContextMarkers(customContextMarkers)
                settingsRepo.saveReservedContextMarkers(reservedContextMarkers)

                contextMarkerHandler.initialize()
            }
        }
    }
