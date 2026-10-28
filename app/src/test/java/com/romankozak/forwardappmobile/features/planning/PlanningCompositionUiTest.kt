package com.romankozak.forwardappmobile.features.planning

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.data.planning.*
import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PlanningCompositionUiTest {
    @get:Rule val ui = createComposeRule()
    private val f = CompositionUiFixture
    private val owner = PlanningReferenceOwner(PlanningReferenceOwnerKind.DAY_TASK, "task")
    private var opened: String? = null
    private var context: String? = null
    private fun ownedFrame(vararg edges: Pair<String, String>): PlanningCommitmentCompositionProjection {
        val frame = f.frame(*edges)
        return frame.copy(participants = frame.participants.map {
            if (it.commitment.id.value == "a") it.copy(commitment = it.commitment.copy(
                provenance = PlanningProvenance(PlanningProvenanceKind.MANUAL, "DAY_TASK", "task"))) else it
        })
    }
    private fun preview(frame: PlanningCommitmentCompositionProjection, compact: Boolean = true) {
        ui.setContent { MaterialTheme { PlanningCompositionPreview(owner,
            PlanningCompositionListUi(frame, { opened = it.commitment.id.value }, { context = it.commitment.id.value }), compact) } }
    }
    @Test fun `Day compact relation opens exact Mission`() {
        preview(ownedFrame("a" to "b"))
        ui.onNodeWithText("↑ Part of · Title b").performClick()
        ui.runOnIdle { assertEquals("b", opened); assertNull(context) }
    }
    @Test fun `multiple parents compact indicator opens current context`() {
        preview(ownedFrame("a" to "b", "a" to "c"))
        ui.onNodeWithText("↑ Part of · Title b +1").performClick()
        ui.runOnIdle { assertEquals("a", context); assertNull(opened) }
    }
    @Test fun `noncanonical Day item renders no composition affordance`() {
        preview(f.frame("a" to "b"))
        ui.onNodeWithText("Part of", substring = true).assertDoesNotExist()
        ui.onNodeWithText("Components", substring = true).assertDoesNotExist()
    }
    @Test fun `empty canonical Day item renders no decorative blocks`() {
        preview(ownedFrame("a" to "b").copy(relations = emptyList()))
        ui.onNodeWithText("Part of", substring = true).assertDoesNotExist()
        ui.onNodeWithText("Components", substring = true).assertDoesNotExist()
    }
    @Test fun `Day preview does not display children or technical identities`() {
        preview(ownedFrame("a" to "b", "c" to "a"))
        ui.onNodeWithText("Title c").assertDoesNotExist()
        ui.onNodeWithText("COMPONENT_OF", substring = true).assertDoesNotExist()
    }
    @Test fun `Strategic list stays collapsed and count opens planning context`() {
        preview(ownedFrame("b" to "a", "c" to "a"), compact = false)
        ui.onNodeWithText("Components · 2").assertIsDisplayed()
        ui.onNodeWithText("Title c").assertDoesNotExist()
        ui.onNodeWithText("Components · 2").performClick()
        ui.runOnIdle { assertEquals("a", context); assertNull(opened) }
    }
    @Test fun `component preview gives direct access to add and reorder context`() {
        preview(ownedFrame("b" to "a"), compact = false)
        ui.onNodeWithText("Components · 1").performClick()
        ui.runOnIdle { assertEquals("a", context) }
    }
    @Test fun `relation updates and title changes recompose without refresh`() {
        val frame = mutableStateOf(ownedFrame("a" to "b"))
        ui.setContent { MaterialTheme { PlanningCompositionPreview(owner,
            PlanningCompositionListUi(frame.value, {}, {}), compact = true) } }
        ui.onNodeWithText("↑ Part of · Title b").assertExists()
        ui.runOnIdle { frame.value = frame.value.copy(participants = frame.value.participants.map {
            if (it.commitment.id.value == "b") it.copy(entity = it.entity.copy(title = "Renamed")) else it
        }) }
        ui.onNodeWithText("↑ Part of · Renamed").assertExists()
        ui.runOnIdle { frame.value = frame.value.copy(relations = emptyList()) }
        ui.onNodeWithText("Part of", substring = true).assertDoesNotExist()
    }
    @Test fun `row menu removes relation via supplied action only`() {
        var removed = 0
        ui.setContent { MaterialTheme { PlanningCompositionRow(f.item("a"), {}, remove = { removed++ }) } }
        ui.onNodeWithContentDescription("Relation actions").performClick()
        ui.onNodeWithText("Remove relation").performSemanticsAction(SemanticsActions.OnClick) { it() }
        ui.runOnIdle { assertEquals(1, removed) }
    }
    @Test fun `relation reorder is an explicit row menu operation`() {
        var moved = 0
        ui.setContent { MaterialTheme { PlanningCompositionRow(f.item("a"), {}, remove = {}, up = { moved++ }) } }
        ui.onNodeWithContentDescription("Relation actions").performClick()
        ui.onNodeWithText("Move up").performSemanticsAction(SemanticsActions.OnClick) { it() }
        ui.runOnIdle { assertEquals(1, moved) }
    }
    @Test fun `trace opens exact participation and closes dialog`() {
        var dismissed = false
        val frame = f.frame("a" to "b", "b" to "c")
        ui.setContent { MaterialTheme { PlanningTraceDialog(frame, f.item("a"), true,
            { dismissed = true }, { opened = it.commitment.id.value }) } }
        ui.onNodeWithText("Title c").performClick()
        ui.runOnIdle { assertTrue(dismissed); assertEquals("c", opened) }
    }
    @Test fun `trace cancel does not navigate or mutate`() {
        var dismissed = false
        ui.setContent { MaterialTheme { PlanningTraceDialog(f.frame("a" to "b"), f.item("a"), true,
            { dismissed = true }, { opened = it.commitment.id.value }) } }
        ui.onNodeWithText("Закрити").performClick()
        ui.runOnIdle { assertTrue(dismissed); assertNull(opened) }
    }
}
