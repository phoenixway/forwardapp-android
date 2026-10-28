package com.romankozak.forwardappmobile.features.planning

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.romankozak.forwardappmobile.data.planning.*
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class OrientationPlanningScreenUiTest {
    @get:Rule val ui = createComposeRule()
    private val f = CompositionUiFixture
    private val model = mockk<PlanningCompositionViewModel>(relaxed = true)
    private val entity = f.item("subject").entity
    private val frame = MutableStateFlow(PlanningCommitmentCompositionProjection(emptyList(), emptyList(), listOf(entity)))
    private fun item(key: String, kind: PlanningScopeKind) = f.item(key, kind).let {
        it.copy(commitment = it.commitment.copy(orientationId = entity.ref.id), entity = entity)
    }
    private fun show() {
        every { model.projection } returns frame
        every { model.busy } returns MutableStateFlow(false)
        every { model.error } returns MutableStateFlow(null)
        every { model.embodiments } returns MutableStateFlow(emptyMap())
        ui.setContent { MaterialTheme { OrientationPlanningScreen(entity.ref.id, {}, model) } }
    }
    @Test fun `empty Orientation has useful Add to planning state`() {
        show()
        ui.onNodeWithText("Not currently planned").assertIsDisplayed()
        ui.onNodeWithText("Add to planning").assertIsDisplayed()
        verify(exactly = 0) { model.plan(any(), any()) }
    }
    @Test fun `participation groups use actual horizon labels`() {
        val items = PlanningScopeKind.entries.map { item(it.name, it) }
        frame.value = frame.value.copy(participants = items)
        show()
        ui.onNodeWithText("Strategic").assertIsDisplayed()
        ui.onNodeWithText("Tactical").performScrollTo().assertIsDisplayed()
        ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Day"))
        ui.onNodeWithText("Day").assertIsDisplayed()
    }
    @Test fun `multiple scopes of one horizon remain individually navigable`() {
        val a = item("first", PlanningScopeKind.TACTICAL_CYCLE)
        val b = item("second", PlanningScopeKind.TACTICAL_CYCLE)
        frame.value = frame.value.copy(participants = listOf(a, b))
        show()
        ui.onNodeWithText(a.contextLabel).assertIsDisplayed()
        ui.onNodeWithText(b.contextLabel).performClick()
        verify { model.open(b) }
    }
    @Test fun `reactive new participation replaces empty state`() {
        show()
        val a = item("new", PlanningScopeKind.STRATEGIC_ARC)
        ui.runOnIdle { frame.value = frame.value.copy(participants = listOf(a)) }
        ui.onNodeWithText("Not currently planned").assertDoesNotExist()
        ui.onNodeWithText(a.contextLabel).assertIsDisplayed()
    }
    @Test fun `explicit Add dialog cancel never dispatches planning write`() {
        show()
        ui.onNodeWithText("Add to planning").performClick()
        ui.onNodeWithText("Скасувати").performClick()
        verify(exactly = 0) { model.plan(any(), any()) }
    }
    @Test fun `explicit current horizon selection preserves exact Orientation`() {
        show()
        ui.onNodeWithText("Add to planning").performClick()
        ui.onNodeWithText("Current Tactical Cycle · Mission").performClick()
        verify(exactly = 1) { model.plan(entity.ref.id, PlanningScopeKind.TACTICAL_CYCLE) }
    }
    @Test fun `removed Orientation becomes unavailable not recreated`() {
        show()
        ui.runOnIdle { frame.value = frame.value.copy(orientations = emptyList()) }
        ui.onNodeWithText("Add to planning").assertDoesNotExist()
        ui.onNodeWithText("Orientation недоступний або завантажується").assertExists()
        verify(exactly = 0) { model.plan(any(), any()) }
    }
}
