package com.romankozak.forwardappmobile.features.contexts.di.capabilities

import com.romankozak.forwardappmobile.core.capability.*
import com.romankozak.forwardappmobile.core.context.ViewId
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object RelationsCapabilityModule {
    @Provides @IntoSet fun capability(): Capability = RelationsCapability
    @Provides @IntoSet fun descriptor(): CapabilityDescriptor = RelationsCapability.descriptor
}

object RelationsCapability : Capability {
    override val descriptor = object : CapabilityDescriptor {
        override val id = CapabilityId("relations")
        override val label = "Semantics"
        override val iconRes: Int? = null
        override val navRoute = "relations"
        override val supportedViews: Set<ViewId> = setOf(ViewId("relations"))
    }
    override fun register(runtime: CapabilityRuntime) = Unit
}
