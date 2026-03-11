package com.romankozak.forwardappmobile.features.contexts.di.capabilities

import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.navigation.capability.settings.CapabilitySettingsDescriptor
import com.romankozak.forwardappmobile.core.navigation.capability.settings.CapabilitySettingsEntry
import com.romankozak.forwardappmobile.features.contexts.ui.context_properties.capabilitysettings.inboxsorting.InboxSortingSettingsContent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object InboxSortingCapabilitySettingsModule {
    @Provides
    @IntoSet
    fun provideInboxSortingCapabilitySettingsEntry(): CapabilitySettingsEntry =
        object : CapabilitySettingsEntry {
            override val descriptor =
                CapabilitySettingsDescriptor(
                    id = "inbox_sorting_settings",
                    ownerCapability = CapabilityId("inbox_sorting"),
                    tabTitle = "Inbox Sorting",
                    order = 100,
                )

            @Composable
            override fun Content(contextId: String) {
                InboxSortingSettingsContent(contextId = contextId)
            }
        }
}
