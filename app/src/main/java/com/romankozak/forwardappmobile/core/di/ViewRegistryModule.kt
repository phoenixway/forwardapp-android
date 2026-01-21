package com.romankozak.forwardappmobile.core.di

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ViewId
import com.romankozak.forwardappmobile.core.navigation.capability.InMemoryViewRegistry
import com.romankozak.forwardappmobile.core.navigation.capability.ScreenId
import com.romankozak.forwardappmobile.core.navigation.capability.ViewDescriptor
import com.romankozak.forwardappmobile.core.navigation.capability.ViewRegistry
import com.romankozak.forwardappmobile.core.navigation.routes.KANBAN_ROUTE
import com.romankozak.forwardappmobile.core.navigation.routes.VET_CASE_HISTORY_ROUTE
import com.romankozak.forwardappmobile.core.navigation.routes.VET_CASE_SUMMARY_ROUTE
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ViewRegistryModule {

    @Provides
    @Singleton
    fun provideViewRegistry(): ViewRegistry {
        val descriptors = setOf(
            ViewDescriptor(
                id = ViewId("kanban"),
                ownerCapability = CapabilityId("code_index"),
                screenId = ScreenId(KANBAN_ROUTE),
                label = "Kanban Board"
            ),
            ViewDescriptor(
                id = ViewId("summary"),
                ownerCapability = CapabilityId("treatment_plan"),
                screenId = ScreenId(VET_CASE_SUMMARY_ROUTE),
                label = "Case Summary"
            ),
            ViewDescriptor(
                id = ViewId("history"),
                ownerCapability = CapabilityId("treatment_plan"),
                screenId = ScreenId(VET_CASE_HISTORY_ROUTE),
                label = "Case History"
            )
        )
        return InMemoryViewRegistry(descriptors)
    }
}
