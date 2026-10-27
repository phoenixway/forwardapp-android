package com.romankozak.forwardappmobile.features.mainscreen

import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.features.planning.planningNavigationTarget
import org.junit.Assert.*
import org.junit.Test

class StrategicQuestReferencesTest {
    private val orientation = CanonicalEntitySummary(CanonicalEntityRef.Orientation("same"), "Health")
    private val aspect = CanonicalEntitySummary(CanonicalEntityRef.Aspect("same"), "Medicine")
    private val workspace = CanonicalEntitySummary(CanonicalEntityRef.Workspace("ws"), "Clinic")
    private val catalog = listOf(orientation, aspect, workspace)

    private fun row(target: String = "ORIENTATION", id: String = "same", role: String = "SOURCE", quest: String = "q") =
        PlanningReferenceEntity("$quest:$target:$id:$role", "ARC_QUEST", quest, target, id, role, 1, 1, null, 1, false)

    @Test fun `source and related retain typed identities and deterministic order`() {
        val rows = listOf(row(), row("ASPECT", role = "RELATED"), row("WORKSPACE", "ws", "RELATED"))
        val projected = projectStrategicQuestReferences(rows, catalog).getValue("q")
        assertEquals(3, projected.size)
        assertEquals(setOf(orientation.ref, aspect.ref, workspace.ref), projected.map { it.entity.ref }.toSet())
        assertEquals(projected, projectStrategicQuestReferences(rows.reversed(), catalog.reversed()).getValue("q"))
    }

    @Test fun `only live resolvable quest references are presented`() {
        val rows = listOf(row().copy(isDeleted = true), row(id = "missing"),
            row().copy(ownerKind = "TACTICAL_MISSION"), row().copy(role = "UNKNOWN"), row(target = "UNKNOWN"))
        assertTrue(projectStrategicQuestReferences(rows, catalog).isEmpty())
    }

    @Test fun `owners stay isolated and empty state has no badges`() {
        val result = projectStrategicQuestReferences(listOf(row(), row(quest = "other")), catalog)
        assertEquals(setOf("q", "other"), result.keys)
        assertEquals(1, result.getValue("q").size)
        assertTrue(projectStrategicQuestReferences(emptyList(), catalog).isEmpty())
    }

    @Test fun `rename removal and target unavailability update derived badges`() {
        val rows = listOf(row())
        assertEquals("Renamed", projectStrategicQuestReferences(rows, listOf(orientation.copy(title = "Renamed")))
            .getValue("q").single().entity.title)
        assertTrue(projectStrategicQuestReferences(rows, emptyList()).isEmpty())
        assertTrue(projectStrategicQuestReferences(rows.map { it.copy(isDeleted = true) }, catalog).isEmpty())
    }

    @Test fun `badge includes relation class and title with exact typed navigation`() {
        val links = projectStrategicQuestReferences(listOf(row(), row("WORKSPACE", "ws", "RELATED")), catalog).getValue("q")
        assertTrue(links.single { it.row.role == "SOURCE" }.questBadgeLabel().contains("На основі: Health · Орієнтир"))
        assertTrue(links.single { it.row.role == "RELATED" }.questBadgeLabel().contains("Пов’язано: Clinic · Робочий простір"))
        assertEquals(NavTarget.SemanticEntityEditor("same"), orientation.ref.planningNavigationTarget())
        assertEquals(NavTarget.SemanticEntityEditor("same"), aspect.ref.planningNavigationTarget())
        assertEquals(NavTarget.Workspace("ws"), workspace.ref.planningNavigationTarget())
    }
}
