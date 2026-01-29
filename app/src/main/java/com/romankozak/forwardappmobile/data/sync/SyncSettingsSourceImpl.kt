package com.romankozak.forwardappmobile.data.sync

import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.sync.datasource.SyncSettingsSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncSettingsSourceImpl @Inject constructor(
    private val settingsRepository: SettingsRepository
) : SyncSettingsSource {

    // Ми просто перенаправляємо потік з репозиторію налаштувань
    override val wifiSyncPortFlow: Flow<Int> = settingsRepository.wifiSyncPortFlow
}