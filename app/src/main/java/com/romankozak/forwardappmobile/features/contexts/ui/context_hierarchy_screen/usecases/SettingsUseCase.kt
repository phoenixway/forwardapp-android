package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.ui.dialogs.UiContext
import com.romankozak.forwardappmobile.features.settings.settings.models.PlanningSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class SettingsUseCase @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val contextHandler: ContextHandler,
) {

    fun saveSettings(scope: CoroutineScope, settings: PlanningSettings) {
        scope.launch {
            settingsRepo.saveShowPlanningModes(settings.showModes)
            settingsRepo.saveDailyTag(settings.dailyTag.trim())
            settingsRepo.saveMediumTag(settings.mediumTag.trim())
            settingsRepo.saveLongTag(settings.longTag.trim())
            settingsRepo.saveObsidianVaultName(settings.vaultName.trim())
        }
    }

    fun saveAllContexts(scope: CoroutineScope, updatedContexts: List<UiContext>) {
        scope.launch {
            val customContexts = updatedContexts.filter { !it.isReserved }
            val reservedContexts = updatedContexts.filter { it.isReserved }

            settingsRepo.saveCustomContexts(customContexts)
            settingsRepo.saveReservedContexts(reservedContexts)

            contextHandler.initialize()
        }
    }
}
