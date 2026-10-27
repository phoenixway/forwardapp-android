package com.romankozak.forwardappmobile.features.mainscreen

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.features.daymanagement.runtime.presentation.DayManagementRuntimeUiState
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.*
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.*
import com.romankozak.forwardappmobile.features.missions.presentation.TacticsWorkspaceMode
import com.romankozak.forwardappmobile.features.recent.RecentViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class GlobalDockComposerUiTest {
    @get:Rule val ui = createComposeRule()
    private val events = mutableListOf<String>()
    private val global = mockk<BottomPanelGlobalActions>(relaxed = true) {
        every { onNavigateToTracker } returns { events += "journal" }
        every { onNavigateToProjectHierarchy } returns { events += "navigation" }
        every { onNavigateToGlobalSearch } returns { events += "magic" }
        every { onShowCreateActions } returns { events += "global-create" }
        every { onNavigateToEntityLibrary } returns { events += "library" }
        every { featureToggles } returns mapOf(FeatureFlag.AiChat to true, FeatureFlag.AiLifeManagement to true,
            FeatureFlag.AttachmentsLibrary to true, FeatureFlag.ScriptsLibrary to true)
    }
    private val recent = mockk<RecentViewModel>(relaxed = true) {
        every { recentItems } returns MutableStateFlow(emptyList())
    }

    private fun clickMenu(label: String) {
        // Exercise the accessibility click contract; moving modal geometry is a device smoke check.
        ui.onNodeWithText(label).performSemanticsAction(SemanticsActions.OnClick) { it() }
    }

    private fun assertEmptyInput() {
        ui.onNode(hasSetTextAction()).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
    }

    @Test fun `all tabs expose the same five dock actions in stable order`() {
        var route by mutableStateOf(MAIN_SCREEN_DASHBOARD_ROUTE)
        ui.setContent { MaterialTheme { MainScreenDock(route, global, recent) {} } }
        listOf(MAIN_SCREEN_DASHBOARD_ROUTE, MAIN_SCREEN_TODAY_ROUTE, MAIN_SCREEN_TACTICS_ROUTE,
            MAIN_SCREEN_STRATEGIC_ARC_ROUTE, MAIN_SCREEN_CORE_ROUTE, "strategic_management").forEach { tab ->
            ui.runOnIdle { route = tab }
            val descriptions = listOf("Показати журнал життя", "Navigation", "Magicbox", "Створити", "Більше дій")
            val positions = descriptions.map { description ->
                ui.onNodeWithContentDescription(description).assertIsDisplayed().fetchSemanticsNode().boundsInRoot.left
            }
            assertEquals(positions.sorted(), positions)
            descriptions.take(4).forEach { ui.onNodeWithContentDescription(it).performClick() }
        }
        assertEquals(List(6) { listOf("journal", "navigation", "magic", "global-create") }.flatten(), events)
    }

    @Test fun `global overflow retains gated tools and Library dispatch from Today`() {
        ui.setContent { MaterialTheme { MainScreenDock(MAIN_SCREEN_TODAY_ROUTE, global, recent) {} } }
        ui.onNodeWithContentDescription("Більше дій").performClick()
        listOf("Недавні", "Context Markers", "Inbox", "AI Life-Management", "AI-Chat", "AI Insights",
            "Reminders", "Structure presets", "Library", "Attachments", "Scripts", "Import/Export", "About", "Settings")
            .forEach { ui.onNodeWithText(it).assertExists() }
        ui.onNodeWithText("Library").performScrollTo()
        clickMenu("Library")
        ui.mainClock.advanceTimeBy(1_000)
        ui.runOnIdle { assertEquals(listOf("library"), events) }
    }

    @Test fun `route switch closes local modal and restores independent drafts`() {
        var route by mutableStateOf(MAIN_SCREEN_TODAY_ROUTE)
        ui.setContent { MaterialTheme {
            MainScreenDock(route, global, recent) {
                var draft by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
                BottomPanelComposer(draft, { draft = it }, { events += draft.text }, "Draft", compact = true,
                    trailingContent = { ContextualActionsButton("Local", listOf(ContextualAction("noop", "Local action") {})) })
            }
        } }
        ui.onNode(hasSetTextAction()).performTextInput("Today draft")
        ui.onNodeWithContentDescription("Дії: Local").performClick()
        ui.onNodeWithText("Local action").assertIsDisplayed()
        ui.runOnIdle { route = MAIN_SCREEN_TACTICS_ROUTE }
        ui.onNodeWithText("Local action").assertDoesNotExist()
        assertEmptyInput()
        ui.onNode(hasSetTextAction()).performTextInput("Mission draft")
        ui.runOnIdle { route = MAIN_SCREEN_TODAY_ROUTE }
        ui.onNode(hasSetTextAction()).assertTextEquals("Today draft")
        ui.runOnIdle { route = MAIN_SCREEN_TACTICS_ROUTE }
        ui.onNode(hasSetTextAction()).assertTextEquals("Mission draft")
    }

    @Test fun `Today task and focus drafts survive switching and state restoration`() {
        var tab by mutableStateOf(DayManagementTab.DAY_PLAN)
        val restoration = StateRestorationTester(ui)
        restoration.setContent { MaterialTheme {
            var draft by rememberTodayComposerDraft(tab)
            BottomPanelComposer(draft, { draft = it }, {}, "Draft", compact = true)
        } }
        ui.onNode(hasSetTextAction()).performTextInput("Task plan")
        ui.runOnIdle { tab = DayManagementTab.DAY_FOCUSES }
        assertEmptyInput()
        ui.onNode(hasSetTextAction()).performTextInput("Focus")
        restoration.emulateSavedInstanceStateRestore()
        ui.onNode(hasSetTextAction()).assertTextEquals("Focus")
        ui.runOnIdle { tab = DayManagementTab.DAY_PLAN }
        ui.onNode(hasSetTextAction()).assertTextEquals("Task plan")
    }

    @Test fun `Strategic source and view controls dispatch while draft survives view change`() {
        ui.setContent { MaterialTheme {
            var mode by remember { mutableStateOf(StrategicArcTab.QUESTS) }
            var draft by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
            StrategicArcBottomPanelContent(mode, draft, { draft = it }, { events += "quest:${draft.text}" },
                { mode = it }, { events += "source" }, { draft = TextFieldValue("") })
        } }
        ui.onNode(hasSetTextAction()).performTextInput("Quest draft")
        ui.onNodeWithContentDescription("Дії: Стратегічна арка · Квести").performClick()
        clickMenu("Створити Quest на основі сутності")
        ui.runOnIdle { assertEquals(listOf("source"), events) }
        ui.onNodeWithContentDescription("Дії: Стратегічна арка · Квести").performClick()
        clickMenu("Артефакт")
        ui.onNode(hasSetTextAction()).assertDoesNotExist()
        ui.onNodeWithContentDescription("Дії: Стратегічна арка · Артефакт").performClick()
        clickMenu("Квести арки")
        ui.onNode(hasSetTextAction()).assertTextEquals("Quest draft")
        ui.onNodeWithContentDescription("Створити ArcQuest").performClick()
        ui.runOnIdle { assertEquals("quest:Quest draft", events.last()) }
    }

    @Test fun `Tactics disabled paste and selected mode remain explicit in local menu`() {
        ui.setContent { MaterialTheme {
            ContextualActionsButton("Тактики", tacticsComposerActions(TacticsWorkspaceMode.STREAMS, false, false,
                { events += it.name }, { events += "streams" }, { events += "paste" }, { events += "source" }, { events += "links" }))
        } }
        ui.onNodeWithContentDescription("Дії: Тактики").performClick()
        ui.onNodeWithText("Вставити в потік").assertIsNotEnabled()
        ui.onNodeWithContentDescription("Вибрано").assertExists()
        clickMenu("Створити Mission на основі сутності")
        ui.runOnIdle { assertEquals(listOf("source"), events) }
    }

    @Test fun `Today local mode selection delegates exact selected subtab`() {
        var tab by mutableStateOf(DayManagementTab.DAY_PLAN)
        ui.setContent { MaterialTheme {
            ContextualActionsButton("Сьогодні", todayContextualActions(tab, emptyList(), DayManagementRuntimeUiState(),
                { tab = it }, {}, {}))
        } }
        ui.onNodeWithContentDescription("Дії: Сьогодні").performClick()
        clickMenu(DayManagementTab.DAY_FOCUSES.title)
        ui.runOnIdle { assertEquals(DayManagementTab.DAY_FOCUSES, tab) }
    }

    @Test fun `Today quick task supports both IME and send without invoking global creation`() {
        ui.setContent { MaterialTheme {
            var draft by remember { mutableStateOf(TextFieldValue("")) }
            TodayBottomPanelContent(
                currentTab = DayManagementTab.DAY_PLAN, inputValue = draft, allTags = emptyList(),
                contextMarkerNames = emptyList(), activityInputText = "", activityTagSuggestions = emptyList(),
                activityEntityLinks = emptyList(), activityEntityOptions = emptyList(), isActivityOngoing = false,
                journalHoldMenuController = com.romankozak.forwardappmobile.features.common.components.holdmenu2.rememberHoldMenu2(),
                additionalMoreActions = emptyList(), runtimeUiState = DayManagementRuntimeUiState(),
                onInputValueChange = { draft = it }, onActivityTextChange = {}, onActivityTagSuggestionClick = {},
                onActivityEntityLinksChanged = {}, onToggleActivityStartStop = {}, onTimelessRecordClick = {},
                onQuickDoneClick = {}, onBackdatedClick = {}, onDaySummaryClick = {},
                onSubmitInput = { events += "task:${draft.text}" }, onSelectTodayTab = {},
                onStartFinalization = {}, onSleep = {},
            )
        } }
        ui.onNodeWithText(DayManagementTab.DAY_PLAN.title).assertDoesNotExist()
        ui.onNode(hasSetTextAction()).performTextInput("Day task")
        ui.onNode(hasSetTextAction()).performImeAction()
        ui.onNodeWithContentDescription("Створити задачу").performClick()
        ui.runOnIdle { assertEquals(listOf("task:Day task", "task:Day task"), events) }
    }

    @Test fun `Tactics real composer submits draft and retains source link handlers`() {
        ui.setContent { MaterialTheme {
            var draft by remember { mutableStateOf(TextFieldValue("")) }
            TacticsBottomPanelContent(
                inputValue = draft, onValueChange = { draft = it }, onSubmit = { events += "mission:${draft.text}" },
                placeholder = "Нова місія...", autocompleteSuggestions = emptyList(), onSuggestionClick = {},
                onAddMissionFromContext = { events += "source" }, onToggleScopeLinksSheet = { events += "links" },
                selectedMode = TacticsWorkspaceMode.ALL, missionStreams = emptyList(), allMissionStreams = emptyList(),
                selectedMissionStreamId = "general", missionStreamCounts = emptyMap(), iterationStatus = null,
                iterationDurationDays = null, iterationDurationHours = null, activitySlotContexts = emptyList(),
                selectedPlanningContextId = null, projectOptions = emptyList(), canPasteAsMissions = false,
                onModeSelected = {}, onMissionStreamSelected = {}, onPlanningContextSelected = {},
                onOpenMissionStreamsSheet = {}, onBrowseBacklogSource = {}, onPasteMissions = {},
            )
        } }
        ui.onNode(hasSetTextAction()).performTextInput("Mission draft")
        ui.onNodeWithContentDescription("Створити місію").performClick()
        ui.onNodeWithContentDescription("Дії: Тактики · Усі місії").performClick()
        clickMenu("Показати зв’язки")
        ui.runOnIdle { assertEquals(listOf("mission:Mission draft", "links"), events) }
    }

    @Test fun `Android Back dismisses local actions without dispatching a command`() {
        ui.setContent { MaterialTheme {
            ContextualActionsButton("Local", listOf(ContextualAction("action", "Local action") { events += "unexpected" }))
        } }
        ui.onNodeWithContentDescription("Дії: Local").performClick()
        ui.onNodeWithText("Local action").assertExists()
        androidx.test.espresso.Espresso.pressBack()
        ui.onNodeWithText("Local action").assertDoesNotExist()
        ui.runOnIdle { assertEquals(emptyList<String>(), events) }
    }

    @Test fun `unified Arc menu exposes former FAB commands and preserves availability`() {
        ui.setContent { MaterialTheme {
            StrategicArcBottomPanelContent(StrategicArcTab.QUESTS, TextFieldValue(""), {}, {}, {},
                { events += "source" }, {}, additionalActions = strategicArcMenuActions(true, false,
                    { events += "previous" }, { events += "next" }, { events += "beacon" },
                    { events += "group" }, { events += "artifact" }, { events += "links" }))
        } }
        ui.onNodeWithContentDescription("Дії: Стратегічна арка · Квести").performClick()
        ui.onNodeWithText("Попередня арка").assertIsNotEnabled()
        listOf("Наступна арка", "Орієнтир як ArcQuest", "Група орієнтирів як ArcQuest", "Відкрити артефакт", "Показати зв’язки")
            .forEach { ui.onNodeWithText(it).assertExists() }
        clickMenu("Наступна арка")
        ui.runOnIdle { assertEquals(listOf("next"), events) }
        ui.onNodeWithContentDescription("Дії: Стратегічна арка · Квести").performClick()
        clickMenu("Група орієнтирів як ArcQuest")
        ui.runOnIdle { assertEquals(listOf("next", "group"), events) }
    }

    @Test fun `Today calendar opens from local menu cancel is inert and confirm delegates selected date`() {
        val selected = 20_000L * 86_400_000L
        val dates = mutableListOf<Long>()
        ui.setContent { MaterialTheme {
            var calendar by remember { mutableStateOf(false) }
            ContextualActionsButton("Сьогодні", todayContextualActions(DayManagementTab.DAY_PLAN,
                todayDateActions(DayManagementTab.DAY_PLAN, { calendar = true }, {}, {}),
                DayManagementRuntimeUiState(), {}, {}, {}))
            TodayDatePickerDialogHost(calendar, selected, { dates += it }) { calendar = false }
        } }
        ui.onNodeWithContentDescription("Дії: Сьогодні").performClick()
        clickMenu("Вибрати день")
        clickMenu("Скасувати")
        ui.runOnIdle { assertEquals(emptyList<Long>(), dates) }
        ui.onNodeWithContentDescription("Дії: Сьогодні").performClick()
        clickMenu("Вибрати день")
        clickMenu("Обрати")
        ui.runOnIdle { assertEquals(listOf(selected), dates) }
    }
}
