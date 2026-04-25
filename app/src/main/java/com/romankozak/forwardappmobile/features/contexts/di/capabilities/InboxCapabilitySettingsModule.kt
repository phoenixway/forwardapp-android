package com.romankozak.forwardappmobile.features.contexts.di.capabilities

import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.navigation.capability.settings.CapabilitySettingsDescriptor
import com.romankozak.forwardappmobile.core.navigation.capability.settings.CapabilitySettingsEntry
import com.romankozak.forwardappmobile.features.contexts.ui.context_properties.capabilitysettings.inbox.InboxSettingsContent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object InboxCapabilitySettingsModule {
    @Provides
    @IntoSet
    fun provideInboxCapabilitySettingsEntry(): CapabilitySettingsEntry =
        object : CapabilitySettingsEntry {
            override val descriptor =
                CapabilitySettingsDescriptor(
                    id = "inbox_settings",
                    ownerCapability = CapabilityId("inbox"),
                    tabTitle = "Inbox",
                    order = 80,
                )

            @Composable
            override fun Content(contextId: String) {
                InboxSettingsContent(contextId = contextId)
            }
        }
}
