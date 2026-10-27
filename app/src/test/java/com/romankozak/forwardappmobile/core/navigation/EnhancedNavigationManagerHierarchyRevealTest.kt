package com.romankozak.forwardappmobile.core.navigation

import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.core.navigation.routes.NavigationRoutes
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnhancedNavigationManagerHierarchyRevealTest {
    @Test
    fun historyKeepsExactAppearanceRatherThanCollapsingWorkspaceIdentity() = runTest {
        val manager = EnhancedNavigationManager(SavedStateHandle(), this)
        val commands = mutableListOf<NavigationCommand>()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.navigationCommandFlow.collect { commands += it }
        }
        val target = NavTarget.ContextHierarchy(projectIdToReveal = "w", placementIdToReveal = "link")
        manager.navigate(target, recordInHistory = true)
        manager.navigate(NavTarget.ContextHierarchy(projectIdToReveal = "w", placementIdToReveal = "primary"), recordInHistory = true)
        advanceUntilIdle()
        val index = manager.history.value.indexOfFirst { it.id == "link" }
        assertTrue(index >= 0)
        assertTrue(manager.history.value.any { it.id == "primary" })
        manager.navigateToHistoryEntry(index)
        advanceUntilIdle()
        assertEquals(NavTargetRouter.routeOf(target), (commands.last() as NavigationCommand.Navigate).route)
        collector.cancel()
    }

    @Test
    fun historyReplaysHierarchyRevealWithStableProjectIdRoute() =
        runTest {
            val manager = EnhancedNavigationManager(SavedStateHandle(), this)
            val commands = mutableListOf<NavigationCommand>()
            val collector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    manager.navigationCommandFlow.collect { commands += it }
                }
            val projectId = "sys_personal-management"

            manager.navigate(
                target = NavTarget.ContextHierarchy(projectIdToReveal = projectId),
                recordInHistory = true,
                historyTitle = "Personal management",
            )
            manager.navigate(target = NavTarget.GlobalSearch("next"), recordInHistory = true)
            advanceUntilIdle()

            val hierarchyEntryIndex =
                manager.history.value.indexOfFirst { entry -> entry.id == projectId }
            assertTrue(hierarchyEntryIndex >= 0)

            val hierarchyEntry = manager.history.value[hierarchyEntryIndex]
            assertEquals(projectId, hierarchyEntry.id)
            assertEquals(NavigationRoutes.goalLists(projectId), hierarchyEntry.route)

            manager.navigateToHistoryEntry(hierarchyEntryIndex)
            advanceUntilIdle()

            val replay = commands.last() as NavigationCommand.Navigate
            assertEquals(NavigationRoutes.goalLists(projectId), replay.route)
            assertTrue(manager.currentEntry.value?.id == projectId)
            collector.cancel()
        }
}
