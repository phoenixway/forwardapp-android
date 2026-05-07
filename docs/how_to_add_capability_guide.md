# How to Add a New Capability (Verified Guide)

Based on a direct analysis of the codebase, here is the verified, step-by-step guide to add a new "Journal Log" capability. The `DirectionCapability` was used as the reference.

The process involves two main steps: creating a `Capability` object and then providing it to the application using a Dagger Hilt module.

### Step 1: Create the Capability Object

First, you need to define your new capability by creating an `object` that implements the `Capability` interface. This object will contain a `descriptor` that holds the ID, display name (label), and navigation route.

1.  Create a new Kotlin file, for example: `app/src/main/java/com/romankozak/forwardappmobile/features/contexts/data/models/capabilities/journal/JournalLogCapability.kt`.
2.  Add the following code, adjusting the `id`, `label`, and `navRoute` as needed. Note that there isn't a central `WellKnownCapabilityId` file to edit; you just define the ID as a string within a `CapabilityId` wrapper.

```kotlin
package com.romankozak.forwardappmobile.features.contexts.data.models.capabilities.journal

import com.romankozak.forwardappmobile.core.capability.Capability
import com.romankozak.forwardappmobile.core.capability.CapabilityDescriptor
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.capability.CapabilityRuntime
import com.romankozak.forwardappmobile.core.navigation.models.ViewId

// Example based on DirectionCapability.kt
object JournalLogCapability : Capability {

    override val descriptor = object : CapabilityDescriptor {
        override val id = CapabilityId("journal_log") // Unique ID for the capability
        override val label: String = "Journal Log" // Display name in the UI
        override val iconRes: Int? = null // Optional icon resource
        override val navRoute: String = "journal_log_root" // Navigation route
        override val supportedViews: Set<ViewId> = setOf(ViewId("journal_log_main"))
    }

    override fun register(runtime: CapabilityRuntime) {
        // This can be left empty for now.
        // It's used for more advanced registration if needed.
    }
}
```

### Step 2: Create the Dagger Hilt Module

To make the application aware of your new capability, you must provide it to the dependency graph using a Dagger Hilt module. This is done using multibindings (`@IntoSet`).

1.  Create a new Kotlin file for the module, for example: `app/src/main/java/com/romankozak/forwardappmobile/features/contexts/di/capabilities/JournalLogCapabilityModule.kt`.
2.  Add the following module code. It provides both the `Capability` object and its `CapabilityDescriptor` to the application.

```kotlin
package com.romankozak.forwardappmobile.features.contexts.di.capabilities

import com.romankozak.forwardappmobile.core.capability.Capability
import com.romankozak.forwardappmobile.core.capability.CapabilityDescriptor
import com.romankozak.forwardappmobile.features.contexts.data.models.capabilities.journal.JournalLogCapability
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object JournalLogCapabilityModule {

    @Provides
    @IntoSet
    fun provideJournalLogCapability(): Capability {
        return JournalLogCapability
    }

    @Provides
    @IntoSet
    fun provideJournalLogCapabilityDescriptor(): CapabilityDescriptor {
        return JournalLogCapability.descriptor
    }
}
```

After rebuilding the project, the "Journal Log" capability should now appear in the UI where capabilities are listed.
