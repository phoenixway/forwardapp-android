package com.romankozak.forwardappmobile.core.context

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import org.junit.Test

class ContextViewPolicyTest {
    @Test
    fun `relations capability exposes its own view without enabling legacy connections`() {
        val views = ContextViewPolicy.availableViews(setOf(CapabilityId("relations")))
        assertThat(views).containsExactly(ContextViewMode.RELATIONS)
        assertThat(com.romankozak.forwardappmobile.features.contexts.di.capabilities.RelationsCapability.descriptor.label)
            .isEqualTo("Semantics")
        assertThat(com.romankozak.forwardappmobile.features.contexts.di.capabilities.RelationsCapability.descriptor.supportedViews)
            .containsExactly(ViewId("relations"))
    }

    @Test
    fun `disabling relations removes view and resolves focused view to surviving dashboard`() {
        val views = ContextViewPolicy.availableViews(setOf(CapabilityId("dashboard")))
        assertThat(views).doesNotContain(ContextViewMode.RELATIONS)
        assertThat(ContextViewPolicy.resolveView(views, ContextViewMode.RELATIONS, ContextViewMode.RELATIONS))
            .isEqualTo(ContextViewMode.DASHBOARD)
    }

    @Test
    fun `availableViews respects capabilities`() {
        val views = ContextViewPolicy.availableViews(setOf(CapabilityId("inbox")))
        assertThat(views).contains(ContextViewMode.INBOX)
        assertThat(views).doesNotContain(ContextViewMode.BACKLOG)
    }

    @Test
    fun `resolveView picks preferred when available`() {
        val available = listOf(ContextViewMode.INBOX, ContextViewMode.BACKLOG)
        val resolved = ContextViewPolicy.resolveView(available, ContextViewMode.INBOX, ContextViewMode.BACKLOG)
        assertThat(resolved).isEqualTo(ContextViewMode.INBOX)
    }
}
