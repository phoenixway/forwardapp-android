package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.FilterState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningMode
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningSettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenHierarchyInstrumentedTest {

  private lateinit var scope: CoroutineScope
  private lateinit var builder: HierarchyStateBuilder
  private lateinit var expansionState: MutableStateFlow<MainScreenStateUseCase.ExpansionState>

  @Before
  fun setUp() {
    scope = CoroutineScope(Dispatchers.Main)
    builder = HierarchyStateBuilder(HierarchyUseCase())
    expansionState =
      MutableStateFlow(MainScreenStateUseCase.ExpansionState(emptySet(), emptySet(), emptySet()))
  }

  @After
  fun tearDown() {
    scope.cancel()
  }

  @Test
  fun hierarchyAppearsOnceFilterIsReady() = runBlocking {
    val filterStates = MutableStateFlow(baseFilterState())
    val hierarchyState =
      builder.buildHierarchyState(
        scope = scope,
        filterStates = filterStates,
        expansionStates = expansionState,
      )

    val projects = listOf(project("root"), project("child", parentId = "root"))
    filterStates.value = readyState(projects)

    val hierarchy =
      withTimeout(1_000) {
        hierarchyState.first { it.topLevelProjects.isNotEmpty() }
      }

    assertEquals(1, hierarchy.topLevelProjects.size)
    assertEquals("root", hierarchy.topLevelProjects.first().id)
  }

  @Test
  fun hierarchyReusesLastSnapshotWhenFlatListBecomesEmpty() = runBlocking {
    val filterStates = MutableStateFlow(baseFilterState())
    val hierarchyState =
      builder.buildHierarchyState(
        scope = scope,
        filterStates = filterStates,
        expansionStates = expansionState,
      )

    val projects = listOf(project("root"), project("child", parentId = "root"))
    filterStates.value = readyState(projects)
    val populated =
      withTimeout(1_000) {
        hierarchyState.first { it.topLevelProjects.isNotEmpty() }
      }

    filterStates.value = readyState(emptyList())
    // Очікуємо, що кешована ієрархія залишиться доступною
    val fallback =
      withTimeout(1_000) {
        hierarchyState.first { it.topLevelProjects.isNotEmpty() }
      }

    assertEquals(populated.topLevelProjects, fallback.topLevelProjects)
    assertEquals(populated.childMap, fallback.childMap)
  }

  @Test
  fun hierarchyNotEmittedBeforeReady() = runBlocking {
    val filterStates = MutableStateFlow(baseFilterState(flatList = listOf(project("orphan"))))
    val hierarchyState =
      builder.buildHierarchyState(
        scope = scope,
        filterStates = filterStates,
        expansionStates = expansionState,
      )

    // Поточне значення має бути порожнім, бо isReady = false
    assertEquals(0, hierarchyState.value.topLevelProjects.size)
    assertEquals(0, hierarchyState.value.childMap.size)

    val emission =
      withTimeoutOrNull(250) {
        hierarchyState.first { it.topLevelProjects.isNotEmpty() }
      }
    assertEquals(null, emission)
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

  private fun readyState(flatList: List<Project>): FilterState = baseFilterState(flatList).copy(isReady = true)

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
