package com.romankozak.forwardappmobile.core.gate

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.capability.CapabilityDescriptor
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.capability.CapabilityRegistry
import com.romankozak.forwardappmobile.core.capability.CapabilitySet
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.DefaultContextController
import com.romankozak.forwardappmobile.core.context.DefaultContextState
import com.romankozak.forwardappmobile.core.context.SystemContexts
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

    @Test
    fun `canonical System override prevents legacy Inbox resurrection`() {
        val inbox = CapabilityId("inbox")
        val controller =
            DefaultContextController(
                DefaultContextState(
                    id = ContextId(SystemContexts.INBOX.raw),
                    features = CapabilitySet(active = emptySet()),
                    views = ViewSet(emptySet(), ViewId("backlog")),
                    config = ContextConfiguration(id = "cfg", contextId = SystemContexts.INBOX.raw, enableInbox = true),
                    canonicalCapabilityOverrides = setOf(inbox),
                ),
            )

        assertThat(CapabilityGate(singleCapabilityRegistry(inbox), controller).isEnabled(inbox)).isFalse()
    }

    @Test
    fun `canonical System override prevents preset Backlog resurrection`() {
        val backlog = CapabilityId("backlog")
        val controller =
            DefaultContextController(
                DefaultContextState(
                    id = ContextId(SystemContexts.INBOX.raw),
                    features = CapabilitySet(active = emptySet()),
                    views = ViewSet(emptySet(), ViewId("backlog")),
                    config =
                        ContextConfiguration(
                            id = "cfg",
                            contextId = SystemContexts.INBOX.raw,
                            basePresetCode = "management",
                            enableBacklog = true,
                        ),
                    canonicalCapabilityOverrides = setOf(backlog),
                ),
            )

        assertThat(CapabilityGate(singleCapabilityRegistry(backlog), controller).isEnabled(backlog)).isFalse()
    }

    @Test
    fun `promoted System gate ignores role defaults but keeps registered experimental extension`() {
        val backlog = CapabilityId("backlog")
        val residual = CapabilityId("unrelated")
        val controller =
            DefaultContextController(
                DefaultContextState(
                    id = ContextId(SystemContexts.INBOX.raw),
                    features = CapabilitySet(active = setOf(residual)),
                    views = ViewSet(emptySet(), ViewId("dashboard")),
                    config =
                        ContextConfiguration(
                            id = "cfg",
                            contextId = SystemContexts.INBOX.raw,
                            basePresetCode = "management",
                            applyMode = "ADDITIVE",
                            experimentalCapabilityIds = listOf(residual),
                        ),
                    suppressPresetCapabilityDerivation = true,
                ),
            )
        val gate = CapabilityGate(capabilityRegistry(listOf(backlog, residual)), controller)

        assertThat(gate.isEnabled(backlog)).isFalse()
        assertThat(gate.isEnabled(residual)).isTrue()
    }

    private fun singleCapabilityRegistry(id: CapabilityId) =
        capabilityRegistry(listOf(id))

    private fun capabilityRegistry(ids: Collection<CapabilityId>) =
        object : CapabilityRegistry {
            private val descriptors =
                ids.associateWith { capabilityId ->
                    object : CapabilityDescriptor {
                        override val id: CapabilityId = capabilityId
                        override val label: String = capabilityId.raw
                        override val iconRes: Int? = null
                        override val navRoute: String = capabilityId.raw
                        override val supportedViews: Set<ViewId> = emptySet()
                    }
                }

            override fun all(): Set<CapabilityDescriptor> = descriptors.values.toSet()

            override fun get(id: CapabilityId): CapabilityDescriptor? = descriptors[id]
        }
}
