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
object KeyProblemsCapabilityModule {
    @Provides
    @IntoSet
    fun provideKeyProblemsCapability(): Capability {
        return KeyProblemsCapability
    }

    @Provides
    @IntoSet
    fun provideKeyProblemsCapabilityDescriptor(): CapabilityDescriptor {
        return KeyProblemsCapability.descriptor
    }
}

object KeyProblemsCapability : Capability {
    override val descriptor =
        object : CapabilityDescriptor {
            override val id = CapabilityId("key_problems")
            override val label = "Ключові проблеми"
            override val iconRes: Int? = null
            override val navRoute = "key_problems"
            override val supportedViews: Set<ViewId> = setOf(ViewId("key_problems"))
        }

    override fun register(runtime: com.romankozak.forwardappmobile.core.capability.CapabilityRuntime) {
        // UI вбудовано в ContextScreen, окрема runtime-реєстрація не потрібна.
    }
}
