package com.romankozak.forwardappmobile.core.navigation.capability.settings

import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.core.capability.CapabilityId

data class CapabilitySettingsDescriptor(
    val id: String,
    val ownerCapability: CapabilityId,
    val tabTitle: String,
    val order: Int = 0,
)

interface CapabilitySettingsEntry {
    val descriptor: CapabilitySettingsDescriptor

    @Composable
    fun Content(contextId: String)
}

