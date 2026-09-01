package com.romankozak.forwardappmobile.core.gate

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.capability.CapabilityDescriptor
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.capability.CapabilityRegistry
import com.romankozak.forwardappmobile.core.capability.CapabilitySet
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.DefaultContextController
import com.romankozak.forwardappmobile.core.context.DefaultContextState
import com.romankozak.forwardappmobile.core.context.ViewId
import com.romankozak.forwardappmobile.core.context.ViewSet
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import org.junit.Test

class CapabilityGateTest {
    @Test
    fun `Dashboard ignores legacy config after canonical cutover`() {
        val dashboard = CapabilityId("dashboard")
        val controller =
            DefaultContextController(
                DefaultContextState(
                    id = ContextId("context"),
                    features = CapabilitySet(active = emptySet()),
                    views = ViewSet(emptySet(), ViewId("backlog")),
                    config =
                        ContextConfiguration(
                            id = "cfg",
                            contextId = "context",
                            enableDashboard = true,
                        ),
                ),
            )
        val gate = CapabilityGate(singleCapabilityRegistry(dashboard), controller)

        assertThat(gate.isEnabled(dashboard)).isFalse()

        controller.update {
            DefaultContextState(
                id = it.id,
                features = CapabilitySet(active = setOf(dashboard)),
                views = it.views,
                config = it.config,
            )
        }

        assertThat(gate.isEnabled(dashboard)).isTrue()
    }

    private fun singleCapabilityRegistry(id: CapabilityId) =
        object : CapabilityRegistry {
            private val descriptor =
                object : CapabilityDescriptor {
                    override val id: CapabilityId = id
                    override val label: String = id.raw
                    override val iconRes: Int? = null
                    override val navRoute: String = id.raw
                    override val supportedViews: Set<ViewId> = emptySet()
                }

            override fun all(): Set<CapabilityDescriptor> = setOf(descriptor)

            override fun get(id: CapabilityId): CapabilityDescriptor? =
                descriptor.takeIf { it.id == id }
        }
}
