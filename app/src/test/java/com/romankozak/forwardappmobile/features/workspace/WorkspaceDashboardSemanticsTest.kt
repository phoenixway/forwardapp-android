package com.romankozak.forwardappmobile.features.workspace

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.NavTargetRouter
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import org.junit.Test

class WorkspaceDashboardSemanticsTest {
    @Test fun `dashboard row preserves canonical title class and every admitted binding kind`() {
        val links = projectWorkspaceSemanticLinks(
            WorkspaceBindingType.entries.map { binding("orientation", it.name) } + binding("aspect", "SUPPORTS"),
            listOf(CanonicalEntitySummary(CanonicalEntityRef.Orientation("orientation"), "Exact canonical title"),
                CanonicalEntitySummary(CanonicalEntityRef.Aspect("aspect"), "Canonical facet")))
        val orientation = links.single { it.ref is CanonicalEntityRef.Orientation }
        val aspect = links.single { it.ref is CanonicalEntityRef.Aspect }
        assertThat(orientation.title).isEqualTo("Exact canonical title")
        WorkspaceBindingType.entries.forEach { assertThat(orientation.dashboardDescription()).contains(it.name) }
        assertThat(orientation.dashboardDescription()).contains("Орієнтир")
        assertThat(aspect.dashboardDescription()).contains("Аспект")
        assertThat(aspect.navigationTarget()).isEqualTo(NavTarget.SemanticEntityEditor("aspect"))
        assertThat(orientation.navigationTarget()).isEqualTo(NavTarget.SemanticEntityEditor("orientation"))
    }

    @Test fun `mixed equal-title rows and relation labels sort deterministically independently of input order`() {
        val catalog = listOf(CanonicalEntitySummary(CanonicalEntityRef.Orientation("o-z"), "Same"),
            CanonicalEntitySummary(CanonicalEntityRef.Orientation("o-a"), "Same"),
            CanonicalEntitySummary(CanonicalEntityRef.Aspect("a"), "Same"))
        val bindings = listOf(binding("o-z", "SUPPORTS"), binding("a", "REALIZES"),
            binding("o-a", "MONITORS"), binding("o-a", "SUPPORTS"))
        val rows = projectWorkspaceSemanticLinks(bindings, catalog)
        assertThat(projectWorkspaceSemanticLinks(bindings.reversed(), catalog.reversed())).isEqualTo(rows)
        assertThat(rows.map { it.ref!!.id }).containsExactly("a", "o-a", "o-z").inOrder()
        assertThat(rows.single { it.ref!!.id == "o-a" }.bindingTypes).containsExactly("MONITORS", "SUPPORTS").inOrder()
    }

    @Test fun `dashboard ordering does not depend on device default locale`() {
        val previous = Locale.getDefault()
        try {
            val bindings = listOf(binding("i", "SUPPORTS"), binding("j", "MONITORS"))
            val catalog = listOf(CanonicalEntitySummary(CanonicalEntityRef.Orientation("i"), "INTENT"),
                CanonicalEntitySummary(CanonicalEntityRef.Aspect("j"), "Journal"))
            Locale.setDefault(Locale.US)
            val normal = projectWorkspaceSemanticLinks(bindings, catalog)
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertThat(projectWorkspaceSemanticLinks(bindings, catalog)).isEqualTo(normal)
        } finally { Locale.setDefault(previous) }
    }

    @Test fun `unsupported unavailable and malformed targets never expose semantic editor navigation`() {
        val links = projectWorkspaceSemanticLinks(listOf(binding("workspace", "SUPPORTS"),
            binding("missing", "MONITORS"), binding("orientation", "UNKNOWN")),
            listOf(CanonicalEntitySummary(CanonicalEntityRef.Workspace("workspace"), "Not semantic"),
                CanonicalEntitySummary(CanonicalEntityRef.Orientation("orientation"), "Meaning")))
        links.forEach { assertThat(it.navigationTarget()).isNull() }
        assertThat(links.first { it.ref == null }.dashboardDescription()).contains("Недоступна сутність")
        assertThat(projectWorkspaceSemanticLinks(emptyList(), emptyList())).isEmpty()
        assertThat(projectWorkspaceSemanticLinks(listOf(binding("missing", "SUPPORTS", deleted = true)), emptyList())).isEmpty()
    }

    @Test fun `dashboard navigation uses exact canonical id never title or Workspace identity`() {
        val id = "canonical/subject with spaces"
        val row = projectWorkspaceSemanticLinks(listOf(binding(id, "SUPPORTS")),
            listOf(CanonicalEntitySummary(CanonicalEntityRef.Orientation(id), "A completely different title"))).single()
        assertThat(row.navigationTarget()).isEqualTo(NavTarget.SemanticEntityEditor(id))
        assertThat(NavTargetRouter.routeOf(row.navigationTarget()!!)).contains("canonical%2Fsubject+with+spaces")
    }

    private fun binding(id: String, type: String, deleted: Boolean = false) = mockk<WorkspaceBindingEntity> {
        every { subjectId } returns id
        every { bindingType } returns type
        every { isDeleted } returns deleted
    }
}
