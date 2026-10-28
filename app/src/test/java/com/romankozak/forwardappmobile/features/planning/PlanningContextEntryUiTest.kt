package com.romankozak.forwardappmobile.features.planning

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.workspace.WorkspaceLinkedSemanticEntity
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PlanningContextEntryUiTest {
    @get:Rule val ui = createComposeRule()
    private var destination: NavTarget? = null
    private fun choice(id: String, kind: String = "EMBODIES") = WorkspaceLinkedSemanticEntity(CanonicalEntityRef.Orientation(id), "Title $id", listOf(kind), true)
    @Test fun `semantic Orientation exposes secondary Planning action`() {
        var open = false
        ui.setContent { MaterialTheme { SemanticPlanningEntry(CanonicalEntityRef.Orientation("o")) { open = true } } }
        ui.onNodeWithText("Planning").performClick(); ui.runOnIdle { assertTrue(open) }
    }
    @Test fun `Aspect exposes no Orientation Planning controls`() {
        ui.setContent { MaterialTheme { SemanticPlanningEntry(CanonicalEntityRef.Aspect("a")) {} } }
        ui.onNodeWithText("Planning").assertDoesNotExist()
    }
    @Test fun `unique Workspace semantic binding opens exact Orientation planning`() {
        ui.setContent { MaterialTheme { WorkspacePlanningEntryContent(listOf(choice("o"))) { destination = it } } }
        ui.onNodeWithText("Planning").performClick()
        ui.runOnIdle { assertEquals(NavTarget.OrientationPlanning("o"), destination) }
    }
    @Test fun `multiple bindings show chooser without guessing owner`() {
        ui.setContent { MaterialTheme { WorkspacePlanningEntryContent(listOf(choice("o"), choice("b", "SUPPORTS"))) { destination = it } } }
        ui.onNodeWithText("Planning").performClick()
        ui.onNodeWithText("SUPPORTS").assertIsDisplayed()
        ui.runOnIdle { assertNull(destination) }
        ui.onNodeWithText("Title b").performClick()
        ui.runOnIdle { assertEquals(NavTarget.OrientationPlanning("b"), destination) }
    }
    @Test fun `empty Workspace choices omit action`() {
        ui.setContent { MaterialTheme { WorkspacePlanningEntryContent(emptyList()) { destination = it } } }
        ui.onNodeWithText("Planning").assertDoesNotExist()
    }
    @Test fun `chooser cancellation does not navigate`() {
        ui.setContent { MaterialTheme { WorkspacePlanningEntryContent(listOf(choice("o"), choice("b"))) { destination = it } } }
        ui.onNodeWithText("Planning").performClick(); ui.onNodeWithText("Скасувати").performClick()
        ui.runOnIdle { assertNull(destination) }
    }
    @Test fun `Add to planning selects explicit horizon and role`() {
        var selected: PlanningScopeKind? = null
        ui.setContent { MaterialTheme { AddOrientationToPlanningDialog({}, { selected = it }) } }
        ui.onNodeWithText("Today · Task").performClick()
        ui.runOnIdle { assertEquals(PlanningScopeKind.DAY, selected) }
    }
    @Test fun `Add to planning cancellation submits nothing`() {
        var selected: PlanningScopeKind? = null
        ui.setContent { MaterialTheme { AddOrientationToPlanningDialog({}, { selected = it }) } }
        ui.onNodeWithText("Скасувати").performClick(); ui.runOnIdle { assertNull(selected) }
    }
    @Test fun `backlog badge opens exact parent participation`() {
        val item = CompositionUiFixture.item("mission", PlanningScopeKind.TACTICAL_CYCLE)
        ui.setContent { MaterialTheme { BacklogPlanningBadges(listOf(PlanningContextBadge(item, "Tactical · Mission"))) {
            destination = it.planningDestination()
        } } }
        ui.onNodeWithText("Tactical · Mission").performClick()
        ui.runOnIdle { assertEquals(NavTarget.PlanningParticipation("mission"), destination) }
    }
    @Test fun `unplanned backlog has no decorative badges`() {
        ui.setContent { MaterialTheme { BacklogPlanningBadges(emptyList()) {} } }
        ui.onNodeWithText("Tactical", substring = true).assertDoesNotExist()
    }
}
