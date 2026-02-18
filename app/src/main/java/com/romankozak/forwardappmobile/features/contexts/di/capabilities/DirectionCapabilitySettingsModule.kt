package com.romankozak.forwardappmobile.features.contexts.di.capabilities

import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.navigation.capability.settings.CapabilitySettingsDescriptor
import com.romankozak.forwardappmobile.core.navigation.capability.settings.CapabilitySettingsEntry
import com.romankozak.forwardappmobile.features.contexts.ui.context_properties.capabilitysettings.direction.DirectionSettingsContent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object DirectionCapabilitySettingsModule {
    @Provides
    @IntoSet
    fun provideDirectionCapabilitySettingsEntry(): CapabilitySettingsEntry =
        object : CapabilitySettingsEntry {
            override val descriptor =
                CapabilitySettingsDescriptor(
                    id = "direction_settings",
                    ownerCapability = CapabilityId("direction"),
                    tabTitle = "Direction",
                    order = 90,
                )

            @Composable
            override fun Content(contextId: String) {
                DirectionSettingsContent(contextId = contextId)
            }
        }
}
