package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainSubState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.state.PlanningModeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanningUseCaseTest {

    @Test
    fun marksReadyAfterReceivingProjects() = runTest {
        val projectsFlow = MutableStateFlow<List<Project>>(emptyList())
        val useCase = planningUseCase()

        useCase.initialize(backgroundScope, projectsFlow)
        advanceTimeBy(150)
        advanceUntilIdle()

        assertFalse(useCase.isReadyForFiltering.value)
        assertTrue(useCase.filterStateFlow.value.flatList.isEmpty())

        val project = project("root")
        projectsFlow.value = listOf(project)
        advanceTimeBy(1)
        advanceUntilIdle()

        println("ready after timeout = ${'$'}{useCase.isReadyForFiltering.value}")
        assertTrue(useCase.isReadyForFiltering.value)
        assertEquals(listOf(project), useCase.filterStateFlow.value.flatList)
    }

    @Test
    fun marksReadyAfterTimeoutEvenIfEmpty() = runTest {
        val projectsFlow = MutableStateFlow<List<Project>>(emptyList())
        val useCase = planningUseCase()

        useCase.initialize(backgroundScope, projectsFlow)
        advanceTimeBy(100)
        advanceUntilIdle()

        assertFalse(useCase.isReadyForFiltering.value)

        advanceTimeBy(500)
        advanceUntilIdle()
        assertFalse(useCase.isReadyForFiltering.value)

        val project = project("delayed")
        projectsFlow.value = listOf(project)
        advanceTimeBy(1)
        advanceUntilIdle()

        assertTrue(useCase.isReadyForFiltering.value)
        assertEquals(listOf(project), useCase.filterStateFlow.value.flatList)
    }

    private fun planningUseCase(): PlanningUseCase =
        PlanningUseCase(
            planningModeManager = PlanningModeManager(),
            searchAdapter = FakeSearchAdapter(),
            settingsProvider = FakeSettingsProvider(),
        )

    private class FakeSearchAdapter : PlanningSearchAdapter {
        private val queryFlow = MutableStateFlow(TextFieldValue(""))
        private val subStateFlow = MutableStateFlow<List<MainSubState>>(listOf(MainSubState.Hierarchy))
        private var active = false

        override val searchQuery: StateFlow<TextFieldValue> = queryFlow.asStateFlow()
        override val subStateStack: StateFlow<List<MainSubState>> = subStateFlow.asStateFlow()

        override fun isSearchActive(): Boolean = active

        override fun popToSubState(targetState: MainSubState) {
            subStateFlow.value = listOf(targetState)
            active = targetState is MainSubState.LocalSearch
        }

        override fun onToggleSearch(isActive: Boolean) {
            active = isActive
            if (!isActive) {
                subStateFlow.value = listOf(MainSubState.Hierarchy)
            }
        }
    }

    private class FakeSettingsProvider : PlanningSettingsProvider {
        override val showPlanningModesFlow = MutableStateFlow(false)
        override val dailyTagFlow = MutableStateFlow("daily")
        override val mediumTagFlow = MutableStateFlow("medium")
        override val longTagFlow = MutableStateFlow("long")
    }

    private fun project(
        id: String,
        parentId: String? = null,
        order: Long = 0,
    ): Project =
        Project(
            id = id,
            name = id,
            description = null,
            parentId = parentId,
            createdAt = 0L,
            updatedAt = 0L,
            tags = null,
            isExpanded = false,
            order = order,
        )
}
