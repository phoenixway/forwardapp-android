package com.romankozak.forwardappmobile.features.contexts.ui.context_screen

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ContextViewPolicy
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.workspace.WorkspaceLinkedSemanticEntity
import com.romankozak.forwardappmobile.features.workspace.dashboardSemanticLinksLabel
import java.util.Locale
import org.junit.Test

class WorkspaceDashboardPresentationTest {
    @Test fun `compact strip retains every currently available capability view`() {
        val enabled = ContextViewMode.entries.map { CapabilityId(it.name.lowercase(Locale.ROOT)) }.toSet()
        assertThat(workspaceDashboardTabs(enabled, emptyMap()).map { it.mode })
            .containsExactlyElementsIn(ContextViewPolicy.availableViews(enabled))
    }

    @Test fun `compact tabs begin with Dashboard Connections Backlog and preserve counts`() {
        val tabs = workspaceDashboardTabs(setOf(CapabilityId("backlog"), CapabilityId("connections"), CapabilityId("dashboard")),
            mapOf(ContextViewMode.CONNECTIONS to 1, ContextViewMode.BACKLOG to 0))
        assertThat(tabs.map { it.mode }).containsExactly(ContextViewMode.DASHBOARD, ContextViewMode.CONNECTIONS, ContextViewMode.BACKLOG).inOrder()
        assertThat(tabs.map { it.count }).containsExactly(null, 1, 0).inOrder()
    }

    @Test fun `tabs contain only enabled views including Semantics and empty capabilities are safe`() {
        val tabs = workspaceDashboardTabs(setOf(CapabilityId("dashboard"), CapabilityId("relations")), emptyMap())
        assertThat(tabs.map { it.label }).containsExactly("Dashboard", "Semantics").inOrder()
        assertThat(workspaceDashboardTabs(emptySet(), emptyMap())).isEmpty()
    }

    @Test fun `single link preview identifies exact semantic title type and actual relation`() {
        val link = WorkspaceLinkedSemanticEntity(CanonicalEntityRef.Aspect("exact"), "new-home", listOf("EMBODIES"), true)
        assertThat(dashboardSemanticLinksLabel(listOf(link))).isEqualTo("Links to  Aspect: new-home (embodies)")
        assertThat(link.navigationTarget()?.subjectId).isEqualTo("exact")
    }

    @Test fun `multiple links do not silently choose a primary semantic target`() {
        val a = WorkspaceLinkedSemanticEntity(CanonicalEntityRef.Aspect("a"), "Same", listOf("SUPPORTS"), true)
        val o = WorkspaceLinkedSemanticEntity(CanonicalEntityRef.Orientation("o"), "Same", listOf("MONITORS"), true)
        assertThat(dashboardSemanticLinksLabel(listOf(a, o))).isEqualTo("Links to 2 semantic entities")
        assertThat(dashboardSemanticLinksLabel(emptyList())).isEqualTo("Links to 0 semantic entities")
    }

    @Test fun `relation labels are deterministic across locale and unavailable targets remain safe`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val link = WorkspaceLinkedSemanticEntity(null, "Unavailable", listOf("REALIZES"), false)
            assertThat(dashboardSemanticLinksLabel(listOf(link))).isEqualTo("Links to  Unavailable (realizes)")
            assertThat(link.navigationTarget()).isNull()
        } finally { Locale.setDefault(previous) }
    }
}
