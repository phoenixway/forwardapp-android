@file:OptIn(ExperimentalJsExport::class)

package com.romankozak.forwardappmobile.shared.features.desktop

import com.benasher44.uuid.uuid4
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.Promise
import kotlinx.datetime.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.promise
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@JsExport
data class DesktopProject(
    val id: String,
    val name: String,
    val description: String?,
    val parentId: String?,
    val goalOrder: Double,
    val createdAt: Double,
    val updatedAt: Double,
    val isExpanded: Boolean,
)

@JsExport
class DesktopProjectApi internal constructor(
    private val state: MutableStateFlow<List<Project>>,
    private val mutex: Mutex,
) {

    fun listProjects(): Promise<Array<DesktopProject>> =
        GlobalScope.promise(context = Dispatchers.Default) {
            state.value
                .sortedWith { a, b ->
                    when {
                        a.goalOrder == b.goalOrder -> (a.createdAt - b.createdAt).toInt()
                        else -> (a.goalOrder - b.goalOrder).toInt()
                    }
                }
                .map { it.toDesktopProject() }
                .toTypedArray()
        }

    fun createProject(
        name: String,
        description: String?,
        parentId: String?,
    ): Promise<DesktopProject> =
        GlobalScope.promise(context = Dispatchers.Default) {
            mutex.withLock {
                val current = state.value
                val nextOrder = (current.maxOfOrNull { it.goalOrder } ?: -1L) + 1L
                val now = timestamp()
                val newProject =
                    Project(
                        id = uuid4().toString(),
                        name = name,
                        description = description,
                        parentId = parentId,
                        createdAt = now,
                        updatedAt = now,
                        goalOrder = nextOrder,
                    )
                state.value = current + newProject
                newProject.toDesktopProject()
            }
        }

    fun updateProject(
        id: String,
        name: String,
        description: String?,
    ): Promise<DesktopProject> =
        GlobalScope.promise(context = Dispatchers.Default) {
            mutex.withLock {
                var updatedProject: Project? = null
                state.update { projects ->
                    projects.map { project ->
                        if (project.id == id) {
                            val next =
                                project.copy(
                                    name = name,
                                    description = description,
                                    updatedAt = timestamp(),
                                )
                            updatedProject = next
                            next
                        } else {
                            project
                        }
                    }
                }
                updatedProject?.toDesktopProject()
                    ?: error("Project with id $id not found")
            }
        }

    fun deleteProject(id: String): Promise<Unit> =
        GlobalScope.promise(context = Dispatchers.Default) {
            mutex.withLock {
                val idsToRemove = collectDescendants(id, state.value).plus(id)
                state.value = state.value.filterNot { it.id in idsToRemove }
            }
        }

    fun toggleProjectExpanded(id: String): Promise<DesktopProject?> =
        GlobalScope.promise(context = Dispatchers.Default) {
            mutex.withLock {
                var updated: Project? = null
                state.update { projects ->
                    projects.map { project ->
                        if (project.id == id) {
                            val next =
                                project.copy(
                                    isExpanded = !project.isExpanded,
                                    updatedAt = timestamp(),
                                )
                            updated = next
                            next
                        } else project
                    }
                }
                updated?.toDesktopProject()
            }
        }
}

@JsExport
fun createDesktopProjectApi(): Promise<DesktopProjectApi> =
    GlobalScope.promise(context = Dispatchers.Default) {
        DesktopProjectApi(
            state = MutableStateFlow(emptyList()),
            mutex = Mutex(),
        )
    }

private fun Project.toDesktopProject(): DesktopProject =
    DesktopProject(
        id = id,
        name = name.ifBlank { "Без назви" },
        description = description,
        parentId = parentId,
        goalOrder = goalOrder.toDouble(),
        createdAt = createdAt.toDouble(),
        updatedAt = (updatedAt ?: createdAt).toDouble(),
        isExpanded = isExpanded,
    )

private fun collectDescendants(rootId: String, projects: List<Project>): Set<String> {
    val childrenMap = projects.groupBy { it.parentId }
    val result = mutableSetOf<String>()

    fun visit(id: String) {
        val children = childrenMap[id] ?: emptyList()
        for (child in children) {
            if (result.add(child.id)) {
                visit(child.id)
            }
        }
    }

    visit(rootId)
    return result
}

private fun timestamp(): Long = Clock.System.now().toEpochMilliseconds()
