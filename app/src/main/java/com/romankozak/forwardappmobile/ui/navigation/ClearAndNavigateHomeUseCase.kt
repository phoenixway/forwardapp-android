// ClearAndNavigateHomeUseCase.kt - Розширена версія для закриття пошуку
package com.romankozak.forwardappmobile.ui.navigation

import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainSubState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningMode
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.navigation.SearchAndNavigationManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.state.PlanningModeManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClearAndNavigateHomeUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    // Оригінальний метод залишається для зворотної сумісності
    suspend operator fun invoke(
        currentProjects: List<Project>,
        onComplete: () -> Unit
    ) {
        // Collapse projects in background
        withContext(ioDispatcher) {
            val expandedProjects = currentProjects.filter { it.isExpanded }
            if (expandedProjects.isNotEmpty()) {
                val collapsedProjects = expandedProjects.map { it.copy(isExpanded = false) }
                projectRepository.updateProjects(collapsedProjects)
            }
        }

        // Execute completion callback on Main thread
        withContext(Dispatchers.Main.immediate) {
            onComplete()
        }
    }

    // НОВИЙ: Спеціальний метод для закриття пошуку
    suspend fun closeSearchAndNavigateHome(
        currentProjects: List<Project>,
        subStateStack: MutableStateFlow<List<MainSubState>>,
        searchAndNavigationManager: SearchAndNavigationManager,
        planningModeManager: PlanningModeManager,
        enhancedNavigationManager: EnhancedNavigationManager?,
        uiEventChannel: Channel<ProjectUiEvent>
    ) {
        // 1. Миттєво очищуємо UI стан
        withContext(Dispatchers.Main.immediate) {
            // Очищаємо стек підстанів до базового стану
            subStateStack.value = listOf(MainSubState.Hierarchy)

            // Очищаємо всі стани пошуку та навігації
            searchAndNavigationManager.clearAllSearchState()
            searchAndNavigationManager.clearNavigation()
        }

        // 2. Згортаємо проєкти у фоні
        withContext(ioDispatcher) {
            val expandedProjects = currentProjects.filter { it.isExpanded }
            if (expandedProjects.isNotEmpty()) {
                val collapsedProjects = expandedProjects.map { it.copy(isExpanded = false) }
                projectRepository.updateProjects(collapsedProjects)
            }
        }

        // 3. Завершуємо на Main thread
        withContext(Dispatchers.Main.immediate) {
            // Скидаємо режим планування та стани розширення
            planningModeManager.changeMode(PlanningMode.All)
            planningModeManager.resetExpansionStates()

            // Навігуємо додому
            enhancedNavigationManager?.navigateHome()

            // Прокручуємо до початку списку
            uiEventChannel.trySend(ProjectUiEvent.ScrollToIndex(0))
        }
    }

    // Альтернативний метод з окремими callbacks (залишається для зворотної сумісності)
    suspend operator fun invoke(
        currentProjects: List<Project>,
        onSubStateCleared: () -> Unit,
        onNavigationCleared: () -> Unit,
        onNavigateHome: () -> Unit,
        onScrollToTop: () -> Unit
    ) {
        // 1. Clear UI state immediately
        withContext(Dispatchers.Main.immediate) {
            onSubStateCleared()
            onNavigationCleared()
        }

        // 2. Collapse projects in background
        withContext(ioDispatcher) {
            val expandedProjects = currentProjects.filter { it.isExpanded }
            if (expandedProjects.isNotEmpty()) {
                val collapsedProjects = expandedProjects.map { it.copy(isExpanded = false) }
                projectRepository.updateProjects(collapsedProjects)
            }
        }

        // 3. Navigate and scroll on Main thread
        withContext(Dispatchers.Main.immediate) {
            onNavigateHome()
            onScrollToTop()
        }
    }
}