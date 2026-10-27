package com.romankozak.forwardappmobile.features.workspace

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class WorkspaceSendToDialogUiTest {
    @get:Rule val ui = createComposeRule()

    @Test fun `grid exposes three labeled destinations and delegates exact selected target`() {
        val events = mutableListOf<WorkspaceSendTarget>()
        ui.setContent { MaterialTheme {
            WorkspaceSendToDialogContent(WorkspaceSendToState("w", "Workspace", loading = false), { events += it }, {})
        } }
        ui.onNodeWithText("Send to…").assertExists()
        WorkspaceSendTarget.entries.forEach { target ->
            ui.onNodeWithText(target.label).performScrollTo().assertIsDisplayed().performClick()
        }
        ui.runOnIdle { assertEquals(WorkspaceSendTarget.entries, events) }
    }

    @Test fun `cancel creates nothing and completed state disallows further sending`() {
        var closed = 0
        val events = mutableListOf<WorkspaceSendTarget>()
        ui.setContent { MaterialTheme {
            WorkspaceSendToDialogContent(WorkspaceSendToState("w", "Workspace", loading = false, completed = WorkspaceSendTarget.ARC_QUEST),
                { events += it }, { closed++ })
        } }
        ui.onNodeWithText("Створено: Квест арки").assertExists()
        ui.onNodeWithText("Завдання дня").assertIsNotEnabled()
        ui.onNodeWithText("Готово").performClick()
        ui.runOnIdle { assertEquals(1, closed); assertEquals(emptyList<WorkspaceSendTarget>(), events) }
    }

    @Test fun `loading leaves destinations disabled and Close remains safe`() {
        var closed = false
        ui.setContent { MaterialTheme {
            WorkspaceSendToDialogContent(WorkspaceSendToState(), {}, { closed = true })
        } }
        ui.onNodeWithText("Тактична місія").assertIsNotEnabled()
        ui.onNodeWithText("Закрити").performClick()
        ui.runOnIdle { assertEquals(true, closed) }
    }
}
