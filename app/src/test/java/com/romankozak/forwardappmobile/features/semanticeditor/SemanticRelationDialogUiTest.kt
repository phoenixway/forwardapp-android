package com.romankozak.forwardappmobile.features.semanticeditor

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.data.workspace.EmbodyingWorkspaceRequest
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SemanticRelationDialogUiTest {
    @get:Rule val ui = createComposeRule()
    private var submitted = false
    private var direction: OrientationRelationEditDirection? = null
    private var relation: OrientationRelationType? = null
    private var binding: WorkspaceBindingType? = null
    private var aspect: AspectOrientationRelationType? = null
    private fun show(pending: PendingSemanticRelation = PendingSemanticRelation.Orientation("other"), busy: Boolean = false) {
        ui.setContent { MaterialTheme {
            SemanticRelationTypeDialog(pending, busy, {}, { type, _ -> aspect = type; submitted = true },
                { binding = it; submitted = true }, { type, way -> relation = type; direction = way; submitted = true },
                currentTitle = "Лікування", relatedTitles = mapOf(
                    CanonicalEntityRef.Orientation("other") to "Ліки",
                    CanonicalEntityRef.Workspace("workspace") to "Кабінет",
                    CanonicalEntityRef.Aspect("aspect") to "Здоров’я"))
        } }
    }
    @Test fun `outgoing named sentence submits original outgoing direction`() {
        show(); ui.onNodeWithText("Підтримує").performClick()
        ui.onNodeWithText("«Лікування» підтримує «Ліки»").performScrollTo().performClick()
        ui.onNodeWithText("Додати").performClick()
        ui.runOnIdle { assertEquals(OrientationRelationType.SUPPORTS, relation); assertEquals(OrientationRelationEditDirection.OUTGOING, direction) }
    }
    @Test fun `incoming named sentence submits original incoming direction`() {
        show(); ui.onNodeWithText("Залежить від").performClick()
        ui.onNodeWithText("«Ліки» залежить від «Лікування»").performScrollTo().performClick()
        ui.onNodeWithText("Додати").performClick()
        ui.runOnIdle { assertEquals(OrientationRelationEditDirection.INCOMING, direction) }
    }
    @Test fun `no direction is silently selected`() {
        show(); ui.onNodeWithText("Підтримує").performClick()
        ui.onNodeWithText("Додати").assertIsNotEnabled()
        ui.onNodeWithText("Вихідний", substring = true).assertDoesNotExist()
        ui.onNodeWithText("Вхідний", substring = true).assertDoesNotExist()
    }
    @Test fun `mutual conflict needs no direction and hides technical authority copy`() {
        show(); ui.onNodeWithText("Конфліктує з").performScrollTo().performClick()
        ui.onNodeWithText("Додати").performClick()
        ui.runOnIdle { assertEquals(OrientationRelationType.CONFLICTS_WITH, relation); assertNull(direction) }
        ui.onNodeWithText("canonical authority", substring = true).assertDoesNotExist()
    }
    @Test fun `Workspace binding preview identifies Workspace as actor`() {
        show(PendingSemanticRelation.Workspace("workspace"))
        ui.onNodeWithText("Втілює").performClick()
        ui.onNodeWithText("Робочий простір «Кабінет» втілює «Лікування»").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Додати").performClick()
        ui.runOnIdle { assertEquals(WorkspaceBindingType.EMBODIES, binding) }
    }
    @Test fun `Aspect membership preview names both exact subjects`() {
        show(PendingSemanticRelation.Aspect("aspect"))
        ui.onNodeWithText("Належить до").performClick()
        ui.onNodeWithText("«Лікування» належить до аспекту «Здоров’я»").assertIsDisplayed()
        ui.onNodeWithText("Додати").performClick()
        ui.runOnIdle { assertEquals(AspectOrientationRelationType.BELONGS_TO, aspect) }
    }
    @Test fun `busy dialog cannot confirm`() {
        show(busy = true)
        ui.onNodeWithText("Додати").assertIsNotEnabled()
        ui.runOnIdle { assertFalse(submitted) }
    }
    @Test fun `cancel creates no relation`() {
        show(); ui.onNodeWithText("Підтримує").performClick()
        ui.onNodeWithText("Скасувати").performClick()
        ui.runOnIdle { assertFalse(submitted) }
    }
    @Test fun `new Workspace wording explains separate semantic identity`() {
        var created = false
        ui.setContent { MaterialTheme {
            EmbodyingWorkspaceDialogContent(EmbodyingWorkspaceState(request = EmbodyingWorkspaceRequest(
                "subject", ManagedSubjectType.ORIENTATION, 1, "Кабінет", null)), {}, {}, { created = true }, {})
        } }
        ui.onNodeWithText("Створити основний робочий простір").assertIsDisplayed()
        ui.onNodeWithText("Сутність залишається окремою", substring = true).assertExists()
        ui.onNodeWithText("Скасувати").performClick()
        ui.runOnIdle { assertFalse(created) }
    }
}
