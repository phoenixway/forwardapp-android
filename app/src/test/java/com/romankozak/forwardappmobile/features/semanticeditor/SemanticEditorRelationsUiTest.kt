package com.romankozak.forwardappmobile.features.semanticeditor

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SemanticEditorRelationsUiTest {
    @get:Rule val ui = createComposeRule()
    private fun model(type: SemanticEntityClass = SemanticEntityClass.ASPECT) =
        SemanticEntityEditorModel("current", type, "Здоров’я", "Здоров’я", null, 1)

    @Test fun `Aspect rows show each title once with short relation and count`() {
        val model = model().copy(orientationAssociations = listOf(
            OrientationAssociationItem("edge", "goal", "Медичний потенціал", AspectOrientationRelationType.BELONGS_TO, true)))
        ui.setContent { MaterialTheme { Column { SemanticAssociationsSection(model, {}, {}, { _, _ -> }, {}) } } }
        ui.onAllNodesWithText("Медичний потенціал").assertCountEquals(1)
        ui.onNodeWithText("Належить до цього аспекту · основний аспект").assertIsDisplayed()
        ui.onNodeWithText("1").assertIsDisplayed()
    }

    @Test fun `relation opens exact endpoint and menu removal does not open it`() {
        var opened: String? = null
        var removed: String? = null
        val model = model().copy(orientationAssociations = listOf(
            OrientationAssociationItem("edge", "goal", "Потенціал", AspectOrientationRelationType.RELEVANT_TO, false)))
        ui.setContent { MaterialTheme { Column { SemanticAssociationsSection(model, {}, { removed = it }, { _, _ -> }, { opened = it }) } } }
        ui.onNodeWithText("Потенціал").performClick()
        ui.runOnIdle { assertEquals("goal", opened); opened = null }
        ui.onNodeWithContentDescription("Дії зв’язку: Потенціал").performClick()
        ui.onNodeWithText("Прибрати зв’язок").performClick()
        ui.runOnIdle { assertEquals("edge", removed); assertNull(opened) }
    }

    @Test fun `Workspace row preserves exact Workspace callback and primary meaning`() {
        var opened: WorkspaceBindingItem? = null
        val binding = WorkspaceBindingItem("binding", "workspace", "Кабінет", WorkspaceBindingType.EMBODIES, true)
        ui.setContent { MaterialTheme { WorkspaceBindingsSection(model().copy(workspaceBindings = listOf(binding)), {}, {}, { opened = it }) } }
        ui.onNodeWithText("Втілює цю сутність · основний").assertIsDisplayed()
        ui.onNodeWithText("Кабінет").performClick()
        ui.runOnIdle { assertEquals(binding, opened) }
    }

    @Test fun `busy relation cannot remove or open`() {
        var changed = false
        ui.setContent { MaterialTheme { RelationRow("Title", "Caption", false, { changed = true }, onOpen = { changed = true }) } }
        ui.onNodeWithContentDescription("Дії зв’язку: Title").assertIsNotEnabled()
        ui.onNodeWithText("Title").performClick()
        ui.runOnIdle { assertFalse(changed) }
    }

    @Test fun `read only graph remains openable without remove action`() {
        var opened = false
        ui.setContent { MaterialTheme { RelationRow("Title", "Частина цього орієнтира", true, {}, false, { opened = true }) } }
        ui.onNodeWithText("Лише перегляд").assertIsDisplayed()
        ui.onNodeWithContentDescription("Дії зв’язку: Title").assertDoesNotExist()
        ui.onNodeWithText("Title").performClick()
        ui.runOnIdle { assertTrue(opened) }
    }

    @Test fun `parent can still be changed opened and made root`() {
        var opened: String? = null
        var root = false
        var choose = false
        ui.setContent { MaterialTheme { AspectParentSection(model().copy(aspectParentId = "parent",
            aspectTitles = mapOf("parent" to "Життя")), { choose = true }, { root = true }, { opened = it }) } }
        ui.onNodeWithText("Змінити").performClick()
        ui.onNodeWithText("Життя").performClick()
        ui.onNodeWithText("Зробити кореневим").performClick()
        ui.runOnIdle { assertTrue(choose); assertTrue(root); assertEquals("parent", opened) }
    }

    @Test fun `children use exact semantic navigation without misleading read only badge`() {
        var opened: String? = null
        ui.setContent { MaterialTheme { AspectChildrenSection(model().copy(aspectChildren = listOf(
            AspectChildItem("child", "Лікування"))), { opened = it }) } }
        ui.onNodeWithText("Лише перегляд").assertDoesNotExist()
        ui.onNodeWithText("Лікування").performClick()
        ui.runOnIdle { assertEquals("child", opened) }
    }

    @Test fun `section add action keeps handler and disabled state`() {
        var added = false
        ui.setContent { MaterialTheme { RelationSectionHeader("Робочі простори", true, { added = true }, 0) } }
        ui.onNodeWithText("Додати").assertIsNotEnabled()
        ui.runOnIdle { assertFalse(added) }
    }
}
