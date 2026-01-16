package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.FilterState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningMode
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningSettingsState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HierarchyStateBuilderTest {

  @Test
  fun emitsReadyFilterWithProjects() = runTest {
    val filterStates = MutableStateFlow(baseFilterState())
    val builder = HierarchyStateBuilder(HierarchyUseCase())
    val readyFlow = builder.prepareReadyFilterState(filterStates)

    val awaited =
      backgroundScope.async {
        readyFlow.first()
      }

    val projects = listOf(project("root"), project("child", parentId = "root"))
    filterStates.value = readyState(projects)
    advanceUntilIdle()

    val result = awaited.await()
    assertEquals(projects, result.flatList)
  }

  @Test
  fun keepsLastProjectsWhenNextReadyIsEmpty() = runTest {
    val filterStates = MutableStateFlow(baseFilterState())
    val builder = HierarchyStateBuilder(HierarchyUseCase())
    val readyFlow = builder.prepareReadyFilterState(filterStates)

    val projects = listOf(project("root"), project("child", parentId = "root"))
    val firstEmission =
      backgroundScope.async {
        readyFlow.first()
      }
    filterStates.value = readyState(projects)
    advanceUntilIdle()
    firstEmission.await()

    filterStates.value = readyState(emptyList())
    advanceUntilIdle()

    val fallback = readyFlow.first()
    assertEquals(projects, fallback.flatList)
  }

  @Test
  fun ignoresNonReadyUpdates() = runTest {
    val filterStates = MutableStateFlow(baseFilterState())
    val builder = HierarchyStateBuilder(HierarchyUseCase())
    val readyFlow = builder.prepareReadyFilterState(filterStates)

    val waiter =
      backgroundScope.async {
        readyFlow.first()
      }

    filterStates.value = baseFilterState(flatList = listOf(project("root")))
    advanceUntilIdle()

    assertFalse(waiter.isCompleted)
  }

  private fun baseFilterState(
    flatList: List<Project> = emptyList(),
  ): FilterState =
    FilterState(
      flatList = flatList,
      query = "",
      searchActive = false,
      mode = PlanningMode.All,
      settings = PlanningSettingsState(),
      isReady = false,
    )

  private fun readyState(
    flatList: List<Project>,
  ): FilterState = baseFilterState(flatList).copy(isReady = true)

  private fun project(
    id: String,
    parentId: String? = null,
    order: Long = 0,
  ) =
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
