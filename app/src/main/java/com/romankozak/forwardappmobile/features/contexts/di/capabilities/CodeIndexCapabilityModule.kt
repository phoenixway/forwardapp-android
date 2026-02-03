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
object CodeIndexCapabilityModule {

    @Provides
    @IntoSet
    fun provideCodeIndexCapability(): Capability {
        return CodeIndexCapability
    }

    @Provides
    @IntoSet
    fun provideCodeIndexCapabilityDescriptor(): CapabilityDescriptor {
        return CodeIndexCapability.descriptor
    }
}

/**
 * Базова реалізація Capability для Індексу коду.
 */
object CodeIndexCapability : Capability {
    override val descriptor = object : CapabilityDescriptor {
        override val id = CapabilityId("code_index")
        override val label = "Індекс коду"
        override val iconRes: Int? = null
        override val navRoute = "code_index_root"
        override val supportedViews: Set<ViewId> = setOf(ViewId("kanban"))
    }

    override fun register(runtime: com.romankozak.forwardappmobile.core.capability.CapabilityRuntime) {
        // Тут буде реєстрація екранів або сервісів індексування коду
    }
}
