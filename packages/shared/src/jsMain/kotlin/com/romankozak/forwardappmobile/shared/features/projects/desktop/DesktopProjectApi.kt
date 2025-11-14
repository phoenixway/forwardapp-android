@file:OptIn(ExperimentalJsExport::class)

package com.romankozak.forwardappmobile.shared.features.projects.desktop

import app.cash.sqldelight.driver.sqljs.initSqlDriver
import com.benasher44.uuid.uuid4
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.createForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.projects.core.data.repository.ProjectRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.repository.ProjectRepository
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.Promise
import kotlin.system.getTimeMillis
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.promise
import kotlinx.coroutines.withContext

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
    private val repository: ProjectRepository,
    private val dispatcher: CoroutineDispatcher,
) {

    fun listProjects(): Promise<Array<DesktopProject>> =
        GlobalScope.promise {
            repository.getAllProjects()
                .first()
                .map { it.toDesktopProject() }
                .toTypedArray()
        }

    fun createProject(
        name: String,
        description: String?,
        parentId: String?,
    ): Promise<DesktopProject> =
        GlobalScope.promise {
            withContext(dispatcher) {
                val projects = repository.getAllProjects().first()
                val order = (projects.maxOfOrNull { it.goalOrder } ?: -1L) + 1L
                val now = currentTimestamp()
                val project =
                    Project(
                        id = uuid4().toString(),
                        name = name,
                        description = description,
                        parentId = parentId,
                        createdAt = now,
                        updatedAt = now,
                        goalOrder = order,
                    )
                repository.upsertProject(project)
                project.toDesktopProject()
            }
        }

    fun updateProject(
        id: String,
        name: String,
        description: String?,
    ): Promise<DesktopProject> =
        GlobalScope.promise {
            withContext(dispatcher) {
                val existing = repository.getProjectById(id).first()
                    ?: error("Project $id not found")
                val updated =
                    existing.copy(
                        name = name,
                        description = description,
                        updatedAt = currentTimestamp(),
                    )
                repository.upsertProject(updated)
                updated.toDesktopProject()
            }
        }

    fun deleteProject(id: String): Promise<Unit> =
        GlobalScope.promise {
            repository.deleteProject(id)
        }

    fun toggleProjectExpanded(id: String): Promise<DesktopProject?> =
        GlobalScope.promise {
            withContext(dispatcher) {
                val existing = repository.getProjectById(id).first() ?: return@withContext null
                val updated =
                    existing.copy(
                        isExpanded = !existing.isExpanded,
                        updatedAt = currentTimestamp(),
                    )
                repository.upsertProject(updated)
                updated.toDesktopProject()
            }
        }
}

@JsExport
fun createDesktopProjectApi(): Promise<DesktopProjectApi> =
    GlobalScope.promise {
        val driver = initSqlDriver(schema = ForwardAppDatabase.Schema).await()
        val database = createForwardAppDatabase(driver)
        DesktopProjectApi(
            repository = ProjectRepositoryImpl(database, Dispatchers.Default),
            dispatcher = Dispatchers.Default,
        )
    }

private fun Project.toDesktopProject(): DesktopProject =
    DesktopProject(
        id = id,
        name = name,
        description = description,
        parentId = parentId,
        goalOrder = goalOrder.toDouble(),
        createdAt = (createdAt).toDouble(),
        updatedAt = (updatedAt ?: createdAt).toDouble(),
        isExpanded = isExpanded,
    )

private fun currentTimestamp(): Long = getTimeMillis()
