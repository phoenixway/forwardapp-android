package com.romankozak.forwardappmobile.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.logicalProjectId
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DayTaskDaoWorkspaceProjectRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `typed routing stores exact reserved project in Workspace branch and preserves one logical id`() =
        runBlocking {
            val database = database()
            try {
                val dao = database.dayTaskDao()

                database.dayPlanDao().insert(dayPlan())
                database.contextDao().insertContexts(
                    listOf(
                        context(ORDINARY_ID),
                        context(HISTORICAL_SYS_PREFIX_ID),
                        context(RETIRED_ID).copy(isDeleted = true),
                    ),
                )
                database.workspaceDao().upsert(
                    listOf(
                        canonicalWorkspace(SYSTEM_ID),
                        standaloneWorkspace(STANDALONE_ID),
                        canonicalWorkspace(RETIRED_ID),
                        canonicalWorkspace(ORDINARY_ID),
                        canonicalWorkspace(HISTORICAL_SYS_PREFIX_ID).copy(
                            provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
                            sourceContextId = HISTORICAL_SYS_PREFIX_ID,
                        ),
                    ),
                )

                dao.insertTasks(
                    listOf(
                        task("system-task", SYSTEM_ID),
                        task("ordinary-task", ORDINARY_ID),
                        task("historical-task", HISTORICAL_SYS_PREFIX_ID),
                        task("standalone-task", STANDALONE_ID),
                        task("retired-task", RETIRED_ID),
                    ),
                )

                val stored = dao.getAllTasksSync().associateBy { it.id }

                val systemTask = requireNotNull(stored["system-task"])
                assertNull(systemTask.projectId)
                assertEquals(SYSTEM_ID, systemTask.projectWorkspaceId)
                assertEquals(SYSTEM_ID, systemTask.logicalProjectId)

                val ordinaryTask = requireNotNull(stored["ordinary-task"])
                assertEquals(ORDINARY_ID, ordinaryTask.projectId)
                assertNull(ordinaryTask.projectWorkspaceId)
                assertEquals(ORDINARY_ID, ordinaryTask.logicalProjectId)

                val historicalTask = requireNotNull(stored["historical-task"])
                assertEquals(HISTORICAL_SYS_PREFIX_ID, historicalTask.projectId)
                assertNull(historicalTask.projectWorkspaceId)
                assertEquals(HISTORICAL_SYS_PREFIX_ID, historicalTask.logicalProjectId)

                val standaloneTask = requireNotNull(stored["standalone-task"])
                assertNull(standaloneTask.projectId)
                assertEquals(STANDALONE_ID, standaloneTask.projectWorkspaceId)
                assertEquals(STANDALONE_ID, standaloneTask.logicalProjectId)

                val retiredTask = requireNotNull(stored["retired-task"])
                assertNull(retiredTask.projectId)
                assertEquals(RETIRED_ID, retiredTask.projectWorkspaceId)
                assertEquals(RETIRED_ID, retiredTask.logicalProjectId)

                assertEquals(
                    listOf("system-task"),
                    dao.getTasksForProject(SYSTEM_ID).first().map { it.id },
                )
                assertEquals(
                    listOf("ordinary-task"),
                    dao.getTasksForProject(ORDINARY_ID).first().map { it.id },
                )
                assertEquals(
                    listOf("historical-task"),
                    dao.getTasksForProject(HISTORICAL_SYS_PREFIX_ID).first().map { it.id },
                )
                assertEquals(
                    listOf("standalone-task"),
                    dao.getTasksForProject(STANDALONE_ID).first().map { it.id },
                )
                assertEquals(
                    listOf("retired-task"),
                    dao.getTasksForProject(RETIRED_ID).first().map { it.id },
                )

                dao.update(
                    systemTask.copy(
                        title = "Updated",
                        projectId = null,
                        projectWorkspaceId = SYSTEM_ID,
                    ),
                )

                val updated = requireNotNull(dao.getTaskById("system-task"))
                assertEquals("Updated", updated.title)
                assertNull(updated.projectId)
                assertEquals(SYSTEM_ID, updated.projectWorkspaceId)
                assertEquals(SYSTEM_ID, updated.logicalProjectId)

                dao.update(
                    retiredTask.copy(
                        title = "Updated retired",
                        projectId = RETIRED_ID,
                        projectWorkspaceId = null,
                    ),
                )
                val updatedRetired = requireNotNull(dao.getTaskById("retired-task"))
                assertNull(updatedRetired.projectId)
                assertEquals(RETIRED_ID, updatedRetired.projectWorkspaceId)
            } finally {
                database.close()
            }
        }

    @Test
    fun `reserved project routing fails closed without live canonical-only same-id Workspace`() =
        runBlocking {
            listOf(
                "missing" to null,
                "deleted" to canonicalWorkspace(SYSTEM_ID).copy(isDeleted = true),
                "context-backed" to
                    canonicalWorkspace(SYSTEM_ID).copy(
                        provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
                        sourceContextId = SYSTEM_ID,
                    ),
                "malformed-source" to
                    canonicalWorkspace(SYSTEM_ID).copy(
                        sourceContextId = SYSTEM_ID,
                    ),
            ).forEach { (case, workspace) ->
                val database = database()
                try {
                    database.dayPlanDao().insert(dayPlan())
                    workspace?.let {
                        database.workspaceDao().upsert(listOf(it))
                    }

                    val failure =
                        runCatching {
                            database.dayTaskDao().insert(
                                task("system-task", SYSTEM_ID),
                            )
                        }.exceptionOrNull()

                    assertTrue("$case must fail closed", failure is IllegalArgumentException)
                    assertTrue(database.dayTaskDao().getAllTasksSync().isEmpty())
                } finally {
                    database.close()
                }
            }
        }

    @Test
    fun `non-System routing rejects unadmitted Workspace owners`() =
        runBlocking {
            listOf(
                canonicalWorkspace(ARBITRARY_CANONICAL_ONLY_ID),
                standaloneWorkspace(DELETED_STANDALONE_ID).copy(isDeleted = true),
                standaloneWorkspace(MALFORMED_STANDALONE_ID).copy(sourceContextId = ORDINARY_ID),
            ).forEach { workspace ->
                val database = database()
                try {
                    database.dayPlanDao().insert(dayPlan())
                    database.workspaceDao().upsert(listOf(workspace))

                    val failure = runCatching {
                        database.dayTaskDao().insert(task("task-${workspace.id}", workspace.id))
                    }.exceptionOrNull()

                    assertTrue("${workspace.id} must fail closed", failure is IllegalArgumentException)
                    assertTrue(database.dayTaskDao().getAllTasksSync().isEmpty())
                } finally {
                    database.close()
                }
            }
        }

    @Test
    fun `conflicting physical project branches are rejected`() =
        runBlocking {
            val database = database()
            try {
                database.dayPlanDao().insert(dayPlan())
                database.contextDao().insertContexts(listOf(context(ORDINARY_ID)))
                database.workspaceDao().upsert(listOf(canonicalWorkspace(SYSTEM_ID)))

                val failure =
                    runCatching {
                        database.dayTaskDao().insert(
                            task("conflicting-task", ORDINARY_ID).copy(
                                projectWorkspaceId = SYSTEM_ID,
                            ),
                        )
                    }.exceptionOrNull()

                assertTrue(failure is IllegalArgumentException)
                assertTrue(database.dayTaskDao().getAllTasksSync().isEmpty())
            } finally {
                database.close()
            }
        }

    private fun dayPlan() =
        DayPlan(
            id = DAY_PLAN_ID,
            date = 1L,
            createdAt = 1L,
            updatedAt = 1L,
            version = 1L,
        )

    private fun task(
        id: String,
        projectId: String?,
    ) =
        DayTask(
            id = id,
            dayPlanId = DAY_PLAN_ID,
            title = id,
            projectId = projectId,
            createdAt = 1L,
            updatedAt = 1L,
            version = 1L,
        )

    private fun context(id: String) =
        ContextEntity(
            id = id,
            name = id,
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = 1L,
        )

    private fun canonicalWorkspace(id: String) =
        WorkspaceEntity(
            id = id,
            nameOverride = id,
            descriptionOverride = null,
            parentWorkspaceId = null,
            roleCode = null,
            workspaceOrder = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
            sourceContextId = null,
        )

    private fun standaloneWorkspace(id: String) =
        canonicalWorkspace(id).copy(provenance = WorkspaceProvenance.STANDALONE.name)

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private companion object {
        const val DAY_PLAN_ID = "day-task-routing-plan"
        val SYSTEM_ID: String = SystemContexts.TODAY.raw
        const val ORDINARY_ID = "ordinary-context"
        const val HISTORICAL_SYS_PREFIX_ID = "sys_strategic-beacons"
        const val STANDALONE_ID = "standalone-workspace"
        const val RETIRED_ID = "retired-context-owner"
        const val ARBITRARY_CANONICAL_ONLY_ID = "arbitrary-canonical-only"
        const val DELETED_STANDALONE_ID = "deleted-standalone"
        const val MALFORMED_STANDALONE_ID = "malformed-standalone"
    }
}
