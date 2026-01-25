package com.romankozak.forwardappmobile.features.contexts.di.capabilities

import com.romankozak.forwardappmobile.core.capability.CapabilityDescriptor
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ViewId
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object TreatmentPlanCapabilityModule {
    @Provides
    @IntoSet
    fun provideTreatmentPlanCapability(): CapabilityDescriptor {
        return object : CapabilityDescriptor {
            override val id = CapabilityId("treatment_plan")
            override val label = "План лікування"
            override val iconRes: Int? = null
            override val navRoute = "treatment_plan_root" // Placeholder
            override val supportedViews: Set<ViewId>
                get() = setOf(ViewId("summary"), ViewId("history"))
        }
    }
}
