// ClearAndNavigateHomeUseCase.kt - Спрощена версія без SearchAndNavigationManager
package com.romankozak.forwardappmobile.ui.navigation

import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClearAndNavigateHomeUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

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

    // Альтернативна версія з окремими callbacks
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