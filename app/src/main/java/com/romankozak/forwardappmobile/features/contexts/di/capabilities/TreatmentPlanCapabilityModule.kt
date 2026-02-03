package com.romankozak.forwardappmobile.features.contexts.di.capabilities

import com.romankozak.forwardappmobile.core.capability.Capability
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
    fun provideTreatmentPlanCapability(): Capability {
        return TreatmentPlanCapability
    }

    @Provides
    @IntoSet
    fun provideTreatmentPlanCapabilityDescriptor(): CapabilityDescriptor {
        return TreatmentPlanCapability.descriptor
    }
}

/**
 * Базова реалізація Capability для Плану лікування.
 */
object TreatmentPlanCapability : Capability {
    override val descriptor = object : CapabilityDescriptor {
        override val id = CapabilityId("treatment_plan")
        override val label = "План лікування"
        override val iconRes: Int? = null
        override val navRoute = "treatment_plan_root"
        override val supportedViews: Set<ViewId> = setOf(ViewId("summary"), ViewId("history"))
    }

    override fun register(runtime: com.romankozak.forwardappmobile.core.capability.CapabilityRuntime) {
        // Реєстрація екранів огляду пацієнта та історії лікування
    }
}
