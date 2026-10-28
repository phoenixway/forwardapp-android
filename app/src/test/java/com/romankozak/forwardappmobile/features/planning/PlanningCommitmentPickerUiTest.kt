package com.romankozak.forwardappmobile.features.planning

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.romankozak.forwardappmobile.data.planning.PlanningParticipantSummary
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PlanningCommitmentPickerUiTest {
    @get:Rule val ui = createComposeRule()
    private var selected: PlanningCommitmentId? = null
    private var dismissed = false
    private fun participant(id: String, kind: PlanningScopeKind, title: String) = PlanningParticipantSummary(
        PlanningCommitment(PlanningCommitmentId(id), PlanningScopeId("scope-$id"), "same-orientation", PlanningRoleCode.FOCUS,
            0, null, PlanningCommitmentStatus.ACTIVE, PlanningProvenance(PlanningProvenanceKind.MANUAL), 1, 1, null, 1, false),
        PlanningScope(PlanningScopeId("scope-$id"), kind, PlanningScopeLifecycle.ACTIVE, title, null, null, 1, 1, null, 1, false),
        CanonicalEntitySummary(CanonicalEntityRef.Orientation("same-orientation"), "Same title", orientationKind = OrientationKind.PROJECT))
    private val day = participant("day", PlanningScopeKind.DAY, "Today")
    private val tactical = participant("mission", PlanningScopeKind.TACTICAL_CYCLE, "Cycle 7")
    private fun show() = ui.setContent { MaterialTheme {
        PlanningCommitmentPickerDialog(listOf(day, tactical), { dismissed = true }, { selected = it.commitment.id })
    } }
    @Test fun `same Orientation in two scopes selects exact commitment not semantic ID`() {
        show()
        ui.onNodeWithText("PROJECT · ${tactical.contextLabel}").performClick()
        ui.runOnIdle { assertEquals(PlanningCommitmentId("mission"), selected) }
    }
    @Test fun `search includes actual scope context`() {
        show()
        ui.onNodeWithText("Пошук").performTextInput("Cycle 7")
        ui.onNodeWithText("PROJECT · ${tactical.contextLabel}").assertIsDisplayed()
        ui.onNodeWithText("PROJECT · ${day.contextLabel}").assertDoesNotExist()
    }
    @Test fun `cancel selects nothing`() {
        show(); ui.onNodeWithText("Скасувати").performClick()
        ui.runOnIdle { assertTrue(dismissed); assertNull(selected) }
    }
}
