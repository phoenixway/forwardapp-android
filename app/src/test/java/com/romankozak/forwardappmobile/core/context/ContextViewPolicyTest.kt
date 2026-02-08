package com.romankozak.forwardappmobile.core.context

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import org.junit.Test

class ContextViewPolicyTest {
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
