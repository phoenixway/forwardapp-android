package com.romankozak.forwardappmobile.features.globalsearch

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass
import com.romankozak.forwardappmobile.features.semanticcreation.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MagicBoxSharedActionsUiTest {
    @get:Rule val ui = createComposeRule()
    private fun semantic() = mockk<SemanticEntityCreationViewModel>(relaxed = true).also {
        every { it.uiState } returns MutableStateFlow(SemanticEntityCreationUiState(name = "Health", entityClass = SemanticEntityClass.ASPECT))
        every { it.events } returns MutableSharedFlow()
    }

    @Test fun `shared creation menu includes semantic Workspace document reminder without executing creation`() {
        val model = semantic()
        ui.setContent { MaterialTheme {
            GlobalCreateActionsHost(true, {}, { error("No create") }, { error("No create") }, {}, {}, {},
                remember { SnackbarHostState() }, semanticEntityCreationViewModel = model)
        } }
        listOf("Семантична сутність", "Новий контекст", "Новий документ", "Нагадування").forEach {
            ui.onNodeWithText(it).assertExists()
        }
        verify(exactly = 0) { model.create() }
    }

    @Test fun `shared host handles Workspace shortcut once and cancellation never creates`() {
        var creates = 0
        var consumed = 0
        ui.setContent { MaterialTheme {
            var request by remember { mutableStateOf<GlobalCreateRequest?>(GlobalCreateRequest.WORKSPACE) }
            GlobalCreateActionsHost(false, {}, { creates++ }, { null }, {}, {}, {},
                remember { SnackbarHostState() }, request, { consumed++; request = null }, semantic())
        } }
        ui.onNodeWithText("Скасувати").performClick()
        ui.runOnIdle { assertThat(creates).isEqualTo(0); assertThat(consumed).isEqualTo(1) }
    }

    @Test fun `scrollable dock puts Recents immediately after search and retains create at far end`() {
        var creates = 0
        var recents = false
        var modeChanges = 0
        ui.setContent { MaterialTheme {
            Box(Modifier.width(220.dp)) {
                ModeIconDock(OmniboxMode.DataSearch, rememberModePalette(OmniboxMode.DataSearch),
                    { creates++ }, { recents = true }, false, { modeChanges++ }, Modifier.testTag("dock"))
            }
        } }
        val search = ui.onNodeWithContentDescription("Пошук даних").fetchSemanticsNode().boundsInRoot
        val recent = ui.onNodeWithContentDescription("Недавні").fetchSemanticsNode().boundsInRoot
        ui.runOnIdle { assertThat(recent.left).isGreaterThan(search.left) }
        ui.onNodeWithContentDescription("Недавні").performClick()
        ui.onNodeWithTag("dock").performTouchInput { swipeLeft() }
        ui.onNodeWithContentDescription("Створити").performScrollTo().performClick()
        ui.runOnIdle { assertThat(recents).isTrue(); assertThat(creates).isEqualTo(1); assertThat(modeChanges).isEqualTo(0) }
    }

    @Test fun `Recents selection does not mark search mode as selected`() {
        ui.setContent { MaterialTheme {
            ModeIconDock(OmniboxMode.DataSearch, rememberModePalette(OmniboxMode.DataSearch), {}, {}, true, {})
        } }
        ui.onNodeWithContentDescription("Недавні").assertIsSelected()
        ui.onNodeWithContentDescription("Пошук даних").assertIsNotSelected()
    }
}
