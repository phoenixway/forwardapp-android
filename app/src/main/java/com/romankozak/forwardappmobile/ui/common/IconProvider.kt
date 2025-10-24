package com.romankozak.forwardappmobile.ui.common

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IconProvider @Inject constructor(
    private val remoteConfigManager: RemoteConfigManager
) {
    fun getIconMappings(): Map<String, List<String>> {
        return remoteConfigManager.getIconMappings()
    }
}
