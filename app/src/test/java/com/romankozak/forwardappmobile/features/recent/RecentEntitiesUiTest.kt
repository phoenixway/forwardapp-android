package com.romankozak.forwardappmobile.features.recent

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.*
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RecentEntitiesUiTest {
    @get:Rule val ui = createComposeRule()
    @Test fun `inline recent list preserves typed same UUID and opens exact entity without a dialog`() {
        var target: NavTarget? = null
        val rows = listOf(RecentItem("semantic:same", RecentItemType.SEMANTIC_ENTITY, 3, "Health", "same"),
            RecentItem("same", RecentItemType.PROJECT, 2, "Treatment Workspace", "same"),
            RecentItem("doc", RecentItemType.NOTE_DOCUMENT, 1, "Notes", "same"))
        ui.setContent { MaterialTheme { RecentEntitiesList(rows) { target = it } } }
        ui.onAllNodes(isDialog()).assertCountEquals(0)
        ui.onNodeWithText("Health").performClick()
        ui.runOnIdle { assertThat(target).isEqualTo(NavTarget.SemanticEntityEditor("same")) }
        ui.onNodeWithText("Treatment Workspace").performClick()
        ui.runOnIdle { assertThat(target).isEqualTo(NavTarget.Workspace("same")) }
        ui.onNodeWithText("Notes").performScrollTo().performClick()
        ui.runOnIdle { assertThat(target).isEqualTo(NavTarget.NoteDocument("same")) }
    }

    @Test fun `empty history is valid inline content`() {
        ui.setContent { MaterialTheme { RecentEntitiesList(emptyList()) {} } }
        ui.onNodeWithText("Ще немає відкритих сутностей").assertIsDisplayed()
        ui.onAllNodes(isDialog()).assertCountEquals(0)
    }
}
