package com.romankozak.forwardappmobile.features.contexts.di.capabilities

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.core.navigation.capability.actions.CapabilityViewActionDescriptor
import com.romankozak.forwardappmobile.core.navigation.capability.actions.CapabilityViewActionEntry
import com.romankozak.forwardappmobile.core.navigation.capability.actions.CapabilityViewActionIds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object BacklogCapabilityViewActionsModule {
    @Provides
    @IntoSet
    fun provideBacklogImportMarkdownAction(): CapabilityViewActionEntry =
        object : CapabilityViewActionEntry {
            override val descriptor =
                CapabilityViewActionDescriptor(
                    id = CapabilityViewActionIds.BACKLOG_IMPORT_MARKDOWN,
                    ownerCapability = CapabilityId("backlog"),
                    viewMode = ContextViewMode.BACKLOG,
                    title = "Імпорт з Markdown",
                    description = "Вставити markdown у беклог",
                    order = 10,
                )
        }

    @Provides
    @IntoSet
    fun provideBacklogExportMarkdownAction(): CapabilityViewActionEntry =
        object : CapabilityViewActionEntry {
            override val descriptor =
                CapabilityViewActionDescriptor(
                    id = CapabilityViewActionIds.BACKLOG_EXPORT_MARKDOWN,
                    ownerCapability = CapabilityId("backlog"),
                    viewMode = ContextViewMode.BACKLOG,
                    title = "Експорт у Markdown",
                    description = "Скопіювати беклог як markdown у буфер",
                    order = 20,
                )
        }
}
