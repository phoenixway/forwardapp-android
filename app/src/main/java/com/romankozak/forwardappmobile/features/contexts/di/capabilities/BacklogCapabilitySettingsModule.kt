package com.romankozak.forwardappmobile.features.contexts.di.capabilities

import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.navigation.capability.settings.CapabilitySettingsDescriptor
import com.romankozak.forwardappmobile.core.navigation.capability.settings.CapabilitySettingsEntry
import com.romankozak.forwardappmobile.features.contexts.ui.context_properties.capabilitysettings.backlog.BacklogSettingsContent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object BacklogCapabilitySettingsModule {
    @Provides
    @IntoSet
    fun provideBacklogCapabilitySettingsEntry(): CapabilitySettingsEntry =
        object : CapabilitySettingsEntry {
            override val descriptor =
                CapabilitySettingsDescriptor(
                    id = "backlog_settings",
                    ownerCapability = CapabilityId("backlog"),
                    tabTitle = "Backlog",
                    order = 85,
                )

            @Composable
            override fun Content(contextId: String) {
                BacklogSettingsContent(contextId = contextId)
            }
        }
}
