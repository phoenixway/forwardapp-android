package com.romankozak.forwardappmobile.features.globalsearch

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalAttachmentSearchResult
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MagicBoxAttachmentResultsUiTest {
    @get:Rule val ui = createComposeRule()
    private val note = GlobalSearchResultItem.AttachmentItem(GlobalAttachmentSearchResult(
        "same-id", "document-id", "NOTE_DOCUMENT", null, "Treatment notes", null, null, updatedAt = 1))
    private val workspace = CanonicalEntitySummary(CanonicalEntityRef.Workspace("same-id"), "Treatment Workspace")

    @Test fun `plain query displays canonical and attachment identities together with separate exact clicks`() {
        var entity: CanonicalEntityRef? = null
        var attachment: GlobalSearchResultItem.AttachmentItem? = null
        ui.setContent { MaterialTheme { MagicBoxCombinedResultList(listOf(workspace), listOf(note),
            { entity = it }, { attachment = it }, true) } }
        ui.onNodeWithText("Treatment Workspace").performClick()
        ui.onNodeWithText("Treatment notes").performScrollTo().performClick()
        ui.onNodeWithText("Нотатка").assertIsDisplayed()
        ui.runOnIdle { assertThat(entity).isEqualTo(workspace.ref); assertThat(attachment).isEqualTo(note) }
    }

    @Test fun `attachment only match is visible instead of empty canonical inventory message`() {
        ui.setContent { MaterialTheme { MagicBoxCombinedResultList(emptyList(), listOf(note), {}, {}, true) } }
        ui.onNodeWithText("Treatment notes").assertIsDisplayed()
        ui.onNodeWithText("Нічого не знайдено").assertDoesNotExist()
    }

    @Test fun `structured mode or excluded attachment filter never renders attachment rows`() {
        ui.setContent { MaterialTheme { MagicBoxCombinedResultList(listOf(workspace), listOf(note), {}, {}, false) } }
        ui.onNodeWithText("Treatment notes").assertDoesNotExist()
        ui.onNodeWithText("Treatment Workspace").assertIsDisplayed()
    }

    @Test fun `read error is visible without hiding canonical results`() {
        ui.setContent { MaterialTheme { MagicBoxCombinedResultList(listOf(workspace), emptyList(), {}, {}, true,
            attachmentError = "Read unavailable") } }
        ui.onNodeWithText("Read unavailable").assertIsDisplayed()
        ui.onNodeWithText("Treatment Workspace").assertIsDisplayed()
    }

    @Test fun `result type narrows mixed search and orientation subtype can be cleared`() {
        val input = "Treatment"
        val goal = CanonicalEntitySummary(
            CanonicalEntityRef.Orientation("goal"),
            "Treatment goal",
            orientationKind = OrientationKind.GOAL,
        )
        val project = CanonicalEntitySummary(
            CanonicalEntityRef.Orientation("project"),
            "Treatment project",
            orientationKind = OrientationKind.PROJECT,
        )
        val aspect = CanonicalEntitySummary(CanonicalEntityRef.Aspect("aspect"), "Treatment aspect")
        ui.setContent {
            MaterialTheme {
                MagicBoxCanonicalResultContent(
                    input = input,
                    state = MagicBoxCanonicalState(input = input, results = listOf(goal, project, aspect, workspace)),
                    attachmentState = MagicBoxAttachmentState(input = input, results = listOf(note)),
                    attachmentsAvailable = true,
                    onLegacyResults = {},
                    onSelectSaved = {},
                    onEntity = {},
                    onAttachment = {},
                )
            }
        }

        ui.onNodeWithText("Усі типи").performClick()
        ui.onNodeWithText("Вкладення").performClick()
        ui.onNodeWithText("Treatment notes").assertIsDisplayed()
        ui.onNodeWithText("Treatment goal").assertDoesNotExist()
        ui.onNodeWithText("Усі підтипи").assertDoesNotExist()

        ui.onNodeWithText("Вкладення").performClick()
        ui.onNodeWithText("Аспекти").performClick()
        ui.onNodeWithText("Treatment aspect").assertIsDisplayed()
        ui.onNodeWithText("Treatment notes").assertDoesNotExist()

        ui.onNodeWithText("Аспекти").performClick()
        ui.onNodeWithText("Орієнтири").performClick()
        ui.onNodeWithText("Усі підтипи").performClick()
        ui.onNodeWithText("Ціль").performClick()
        ui.onNodeWithText("Treatment goal").assertIsDisplayed()
        ui.onNodeWithText("Treatment project").assertDoesNotExist()
        ui.onNodeWithText("Treatment aspect").assertDoesNotExist()

        ui.onNodeWithText("Ціль").performClick()
        ui.onNodeWithText("Проєкт").performScrollTo().performClick()
        ui.onNodeWithText("Treatment project").assertIsDisplayed()
        ui.onNodeWithText("Treatment goal").assertDoesNotExist()

        ui.onNodeWithText("Проєкт").performClick()
        ui.onNodeWithText("Усі підтипи").performScrollTo().performClick()
        ui.onNodeWithText("Treatment goal").assertIsDisplayed()
        ui.onNodeWithText("Treatment project").assertIsDisplayed()
    }
}
