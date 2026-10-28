package com.romankozak.forwardappmobile.features.mainscreen

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceEntity
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.features.planning.PlanningReferencePresentation
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StrategicArcQuestCardUiTest {
    @get:Rule val ui = createComposeRule()
    private val quest = ArcQuestEntity(id = "quest", arcKey = "2026-10", title = "Quest title", description = "Useful details", linkedContextId = "legacy")
    private var opens = 0
    private var missions = 0
    private var deletes = 0
    private var carries = 0
    private var openedContext: String? = null
    private var openedReference: CanonicalEntityRef? = null

    @Composable private fun Card(value: ArcQuestEntity = quest, references: List<PlanningReferencePresentation> = emptyList(), historical: Boolean = true, dragging: Boolean = false, width: Dp = 300.dp) {
        MaterialTheme {
            Column(Modifier.width(width)) {
                ArcQuestCard(value, if (value.linkedContextId == null) null else "Legacy link",
                    onQuestClick = { opens++ }, onOpenContext = { openedContext = it },
                    onCreateMission = { missions++ }, onEditQuest = { _, _ -> }, onDeleteQuest = { deletes++ },
                    onCarryToCurrentArc = if (historical) ({ carries++ }) else null,
                    dragHandleModifier = Modifier, references = references,
                    onOpenReference = { openedReference = it }, isDragging = dragging)
            }
        }
    }

    private fun openMenu() {
        ui.onNodeWithContentDescription("ArcQuest actions").performSemanticsAction(SemanticsActions.OnClick) { it() }
    }
    private fun menuClick(text: String) {
        ui.onNodeWithText(text).performSemanticsAction(SemanticsActions.OnClick) { it() }
        ui.waitForIdle()
    }

    @Test fun `transparent quest row uses theme foreground instead of inherited dark text`() {
        val colors = darkColorScheme()
        ui.setContent {
            MaterialTheme(colorScheme = colors) {
                CompositionLocalProvider(LocalContentColor provides Color.Black) {
                    ArcQuestCard(quest, null, {}, {}, {}, { _, _ -> }, {}, null, Modifier)
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        ui.onNodeWithText("Quest title").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(colors.onSurface, layouts.single().layoutInput.style.color)
    }

    @Test fun `lightweight quest row keeps title description drag affordance and primary click`() {
        ui.setContent { Card() }
        ui.onNodeWithText("Quest title").assertIsDisplayed().performClick()
        ui.onNodeWithText("Useful details").assertIsDisplayed()
        ui.onNodeWithContentDescription("ArcQuest").assertIsDisplayed()
        ui.runOnIdle { assertEquals(1, opens) }
    }

    @Test fun `source and RELATED badges wrap on narrow row and retain exact typed callbacks`() {
        val orientation = CanonicalEntitySummary(CanonicalEntityRef.Orientation("same"), "Meaning")
        val aspect = CanonicalEntitySummary(CanonicalEntityRef.Aspect("same"), "Facet")
        fun ref(kind: String, role: String, entity: CanonicalEntitySummary) = PlanningReferencePresentation(
            PlanningReferenceEntity("$kind:$role", "ARC_QUEST", "quest", kind, "same", role, 1, 1, null, 1, false), entity)
        ui.setContent { Card(references = listOf(ref("ORIENTATION", "SOURCE", orientation), ref("ASPECT", "RELATED", aspect)), width = 140.dp) }
        val source = ui.onNodeWithText("На основі: Meaning · Орієнтир")
        val related = ui.onNodeWithText("Пов’язано: Facet · Аспект")
        source.assertIsDisplayed().performClick()
        ui.runOnIdle { assertEquals(orientation.ref, openedReference); assertEquals(0, opens) }
        related.assertIsDisplayed().performClick()
        ui.runOnIdle { assertEquals(aspect.ref, openedReference); assertEquals(0, opens) }
        assertTrue(related.fetchSemanticsNode().boundsInRoot.top > source.fetchSemanticsNode().boundsInRoot.top)
    }

    @Test fun `compact menu preserves existing quest commands`() {
        ui.setContent { Card() }
        openMenu()
        listOf("Редагувати", "Відкрити контекст", "Add to Tactical", "В поточну арку", "Видалити").forEach {
            ui.onNodeWithText(it).assertIsDisplayed()
        }
        menuClick("Add to Tactical")
        openMenu(); menuClick("В поточну арку")
        openMenu(); menuClick("Відкрити контекст")
        openMenu(); menuClick("Видалити")
        ui.runOnIdle { assertEquals(1, missions); assertEquals(1, carries); assertEquals("legacy", openedContext); assertEquals(1, deletes); assertEquals(0, opens) }
    }

    @Test fun `dragging and minimal current quest retain controls without historical actions`() {
        ui.setContent { Card(quest.copy(description = null, linkedContextId = null), historical = false, dragging = true) }
        ui.onNodeWithText("Quest title").assertIsDisplayed()
        ui.onNodeWithContentDescription("ArcQuest").assertIsDisplayed()
        openMenu()
        ui.onNodeWithText("В поточну арку").assertDoesNotExist()
        ui.onNodeWithText("Відкрити контекст").assertDoesNotExist()
        ui.onNodeWithText("Add to Tactical").assertIsDisplayed()
        ui.onNodeWithText("Видалити").assertIsDisplayed()
    }
}
