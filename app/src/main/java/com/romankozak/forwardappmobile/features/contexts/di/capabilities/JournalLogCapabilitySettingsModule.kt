package com.romankozak.forwardappmobile.features.contexts.di.capabilities

import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.navigation.capability.settings.CapabilitySettingsDescriptor
import com.romankozak.forwardappmobile.core.navigation.capability.settings.CapabilitySettingsEntry
import com.romankozak.forwardappmobile.features.contexts.ui.context_properties.capabilitysettings.journallog.JournalLogSettingsContent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object JournalLogCapabilitySettingsModule {
    @Provides
    @IntoSet
    fun provideJournalLogCapabilitySettingsEntry(): CapabilitySettingsEntry =
        object : CapabilitySettingsEntry {
            override val descriptor =
                CapabilitySettingsDescriptor(
                    id = "journal_log_settings",
                    ownerCapability = CapabilityId("journal_log"),
                    tabTitle = "Journal Log",
                    order = 110,
                )

            @Composable
            override fun Content(contextId: String) {
                JournalLogSettingsContent(contextId = contextId)
            }
        }
}
