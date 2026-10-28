package com.romankozak.forwardappmobile.features.planning

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.features.entitybrowser.*
import org.junit.Assert.*
import org.junit.Test

class WorkspacePlanningProjectionTest {
    private val workspace = CanonicalEntitySummary(CanonicalEntityRef.Workspace("ws"), "Workspace")
    private val orientation = CanonicalEntitySummary(CanonicalEntityRef.Orientation("o"), "Meaning")
    private val aspect = CanonicalEntitySummary(CanonicalEntityRef.Aspect("a"), "Facet")
    private fun binding(id: String, subject: String, kind: String = "EMBODIES", ws: String = "ws") =
        WorkspaceBindingEntity(id, ws, subject, kind, false, 0, 1, 1, null, false, 1)
    @Test fun `unique EMBODIES Orientation is exact choice`() {
        val choices = workspacePlanningChoices("ws", listOf(binding("b", "o")), listOf(workspace, orientation))
        assertEquals(listOf(orientation.ref), choices.map { it.ref })
        assertEquals(listOf("EMBODIES"), choices.single().bindingTypes)
    }
    @Test fun `Aspect only Workspace has no fake Planning`() {
        assertTrue(workspacePlanningChoices("ws", listOf(binding("b", "a")), listOf(workspace, aspect)).isEmpty())
    }
    @Test fun `multiple exact Orientation bindings retain relation kinds`() {
        val second = CanonicalEntitySummary(CanonicalEntityRef.Orientation("o2"), "Another")
        val choices = workspacePlanningChoices("ws", listOf(binding("b", "o"), binding("c", "o2", "SUPPORTS")), listOf(workspace, orientation, second))
        assertEquals(listOf("o", "o2"), choices.map { it.ref!!.id })
        assertEquals(listOf("SUPPORTS"), choices.last().bindingTypes)
    }
    @Test fun `multiple bindings to one Orientation produce single choice`() {
        val choices = workspacePlanningChoices("ws", listOf(binding("b", "o", "MONITORS"), binding("c", "o", "SUPPORTS")), listOf(workspace, orientation))
        assertEquals(1, choices.size); assertEquals(setOf("MONITORS", "SUPPORTS"), choices.single().bindingTypes.toSet())
    }
    @Test fun `unrelated Workspace binding ignored`() {
        assertTrue(workspacePlanningChoices("ws", listOf(binding("b", "o", ws = "other")), listOf(workspace, orientation)).isEmpty())
    }
    @Test fun `tombstoned binding excluded`() {
        assertTrue(workspacePlanningChoices("ws", listOf(binding("b", "o").copy(isDeleted = true)), listOf(workspace, orientation)).isEmpty())
    }
    @Test fun `missing target excluded`() {
        assertTrue(workspacePlanningChoices("ws", listOf(binding("b", "o")), listOf(workspace)).isEmpty())
    }
    @Test fun `deleted or unavailable Workspace excluded`() {
        assertTrue(workspacePlanningChoices("ws", listOf(binding("b", "o")), listOf(orientation)).isEmpty())
    }
    @Test fun `title equality creates no relationship`() {
        assertTrue(workspacePlanningChoices("ws", emptyList(), listOf(workspace, orientation.copy(title = workspace.title))).isEmpty())
    }
    @Test fun `raw ID equality creates no relationship`() {
        assertTrue(workspacePlanningChoices("ws", emptyList(), listOf(workspace, orientation.copy(ref = CanonicalEntityRef.Orientation("ws")))).isEmpty())
    }
    @Test fun `cross class subject ambiguity fails closed`() {
        assertTrue(workspacePlanningChoices("ws", listOf(binding("b", "o")),
            listOf(workspace, orientation, aspect.copy(ref = CanonicalEntityRef.Aspect("o")))).isEmpty())
    }
    @Test fun `unknown binding type not offered`() {
        assertTrue(workspacePlanningChoices("ws", listOf(binding("b", "o", "UNKNOWN")), listOf(workspace, orientation)).isEmpty())
    }
}
