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
object DirectionCapabilityViewActionsModule {
    @Provides
    @IntoSet
    fun provideDirectionCopyLinkedBacklogsAction(): CapabilityViewActionEntry =
        object : CapabilityViewActionEntry {
            override val descriptor =
                CapabilityViewActionDescriptor(
                    id = CapabilityViewActionIds.DIRECTION_COPY_LINKED_BACKLOGS_AS_LINKS,
                    ownerCapability = CapabilityId("direction"),
                    viewMode = ContextViewMode.DIRECTION,
                    title = "Зібрати беклоги в поточний",
                    description = "Скопіювати цілі з пов'язаних контекстів як посилання",
                    order = 10,
                )
        }
}
