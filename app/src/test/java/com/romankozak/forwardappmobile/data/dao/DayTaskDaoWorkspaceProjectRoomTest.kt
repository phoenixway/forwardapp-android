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
                        canonicalWorkspace(ORDINARY_ID).copy(
                            provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
                            sourceContextId = ORDINARY_ID,
                        ),
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
                assertNull(ordinaryTask.projectId)
                assertEquals(ORDINARY_ID, ordinaryTask.projectWorkspaceId)
                assertEquals(ORDINARY_ID, ordinaryTask.logicalProjectId)

                val historicalTask = requireNotNull(stored["historical-task"])
                assertNull(historicalTask.projectId)
                assertEquals(HISTORICAL_SYS_PREFIX_ID, historicalTask.projectWorkspaceId)
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

    @Test
    fun `compatibility project input converges on insert and update`() =
        runBlocking {
            val database = database()
            try {
                val dao = database.dayTaskDao()
                database.dayPlanDao().insert(dayPlan())
                database.contextDao().insertContexts(
                    listOf(context(ORDINARY_ID)),
                )
                database.workspaceDao().upsert(
                    listOf(
                        canonicalWorkspace(ORDINARY_ID).copy(
                            provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
                            sourceContextId = ORDINARY_ID,
                        ),
                    ),
                )

                dao.insert(task("compatibility-input", ORDINARY_ID))

                val stored = requireNotNull(
                    dao.getTaskById("compatibility-input"),
                )
                assertNull(stored.projectId)
                assertEquals(ORDINARY_ID, stored.projectWorkspaceId)
                assertEquals(ORDINARY_ID, stored.logicalProjectId)

                dao.update(
                    stored.copy(
                        title = "Updated",
                        projectId = ORDINARY_ID,
                        projectWorkspaceId = null,
                    ),
                )

                val updated = requireNotNull(
                    dao.getTaskById("compatibility-input"),
                )
                assertEquals("Updated", updated.title)
                assertNull(updated.projectId)
                assertEquals(ORDINARY_ID, updated.projectWorkspaceId)

                dao.insert(
                    task("equivalent-dual", ORDINARY_ID).copy(
                        projectWorkspaceId = ORDINARY_ID,
                    ),
                )

                val equivalent = requireNotNull(
                    dao.getTaskById("equivalent-dual"),
                )
                assertNull(equivalent.projectId)
                assertEquals(ORDINARY_ID, equivalent.projectWorkspaceId)
            } finally {
                database.close()
            }
        }

    @Test
    fun `contextless canonical Workspace and no-project DayTask remain valid`() =
        runBlocking {
            val database = database()
            try {
                val dao = database.dayTaskDao()
                database.dayPlanDao().insert(dayPlan())
                database.workspaceDao().upsert(
                    listOf(canonicalWorkspace(ARBITRARY_CANONICAL_ONLY_ID)),
                )

                dao.insertTasks(
                    listOf(
                        task("contextless-canonical", ARBITRARY_CANONICAL_ONLY_ID),
                        task("without-project", null),
                    ),
                )

                val canonical = requireNotNull(
                    dao.getTaskById("contextless-canonical"),
                )
                assertNull(canonical.projectId)
                assertEquals(
                    ARBITRARY_CANONICAL_ONLY_ID,
                    canonical.projectWorkspaceId,
                )

                val withoutProject = requireNotNull(
                    dao.getTaskById("without-project"),
                )
                assertNull(withoutProject.projectId)
                assertNull(withoutProject.projectWorkspaceId)
                assertNull(withoutProject.logicalProjectId)
            } finally {
                database.close()
            }
        }

    @Test
    fun `unproven live and retired Context mappings fail closed`() =
        runBlocking {
            val cases = listOf(
                "unproven-live-mapping" to (
                    context(ORDINARY_ID) to canonicalWorkspace(ORDINARY_ID)
                ),
                "missing-workspace" to (
                    context(ORDINARY_ID) to null
                ),
                "deleted-workspace" to (
                    context(ORDINARY_ID) to
                        canonicalWorkspace(ORDINARY_ID).copy(isDeleted = true)
                ),
                "unproven-retired-mapping" to (
                    context(ORDINARY_ID).copy(isDeleted = true) to
                        canonicalWorkspace(ORDINARY_ID).copy(
                            provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
                            sourceContextId = ORDINARY_ID,
                        )
                ),
            )

            cases.forEach { (name, owners) ->
                val (ownerContext, ownerWorkspace) = owners
                val database = database()
                try {
                    database.dayPlanDao().insert(dayPlan())
                    database.contextDao().insertContexts(
                        listOf(ownerContext),
                    )
                    ownerWorkspace?.let { workspace ->
                        database.workspaceDao().upsert(listOf(workspace))
                    }

                    val failure = runCatching {
                        database.dayTaskDao().insert(
                            task("invalid-$name", ORDINARY_ID),
                        )
                    }.exceptionOrNull()

                    assertTrue(
                        "$name must fail closed: $failure",
                        failure is IllegalArgumentException,
                    )
                    assertTrue(
                        database.dayTaskDao().getAllTasksSync().isEmpty(),
                    )
                } finally {
                    database.close()
                }
            }
        }

    @Test
    fun `mixed batch rejects invalid owner without inserting valid task`() =
        runBlocking {
            val database = database()
            try {
                database.dayPlanDao().insert(dayPlan())
                database.contextDao().insertContexts(
                    listOf(context(ORDINARY_ID)),
                )

                val failure = runCatching {
                    database.dayTaskDao().insertTasks(
                        listOf(
                            task("valid-no-project", null),
                            task("invalid-owner", ORDINARY_ID),
                        ),
                    )
                }.exceptionOrNull()

                assertTrue(failure is IllegalArgumentException)
                assertTrue(
                    database.dayTaskDao().getAllTasksSync().isEmpty(),
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `sync ACK changes only metadata for canonical Workspace owner`() =
        runBlocking {
            val database = database()
            try {
                val dao = database.dayTaskDao()
                database.dayPlanDao().insert(dayPlan())
                database.contextDao().insertContexts(
                    listOf(context(ORDINARY_ID)),
                )
                database.workspaceDao().upsert(
                    listOf(
                        canonicalWorkspace(ORDINARY_ID).copy(
                            provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
                            sourceContextId = ORDINARY_ID,
                        ),
                    ),
                )

                dao.insert(task("ack-canonical", ORDINARY_ID))
                val before = dao.getAllTasksSync().single()

                assertNull(before.projectId)
                assertEquals(ORDINARY_ID, before.projectWorkspaceId)
                assertNull(before.syncedAt)

                assertEquals(
                    1,
                    dao.markSyncedIfVersionMatches(
                        taskId = before.id,
                        expectedVersion = before.version,
                        syncedAt = 1234L,
                    ),
                )

                val acknowledged = dao.getAllTasksSync().single()
                assertEquals(before.copy(syncedAt = 1234L), acknowledged)
                assertNull(acknowledged.projectId)
                assertEquals(
                    ORDINARY_ID,
                    acknowledged.projectWorkspaceId,
                )

                assertEquals(
                    0,
                    dao.markSyncedIfVersionMatches(
                        taskId = before.id,
                        expectedVersion = before.version + 1L,
                        syncedAt = 5678L,
                    ),
                )
                assertEquals(
                    acknowledged,
                    dao.getAllTasksSync().single(),
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `sync ACK preserves canonicalized compatibility owner`() =
        runBlocking {
            val database = database()
            try {
                val dao = database.dayTaskDao()
                database.dayPlanDao().insert(dayPlan())
                database.contextDao().insertContexts(
                    listOf(context(ORDINARY_ID)),
                )
                database.workspaceDao().upsert(
                    listOf(
                        canonicalWorkspace(ORDINARY_ID).copy(
                            provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
                            sourceContextId = ORDINARY_ID,
                        ),
                    ),
                )

                dao.insert(task("ack-compatibility", ORDINARY_ID))

                val before = dao.getAllTasksSync().single()
                assertNull(before.projectId)
                assertEquals(ORDINARY_ID, before.projectWorkspaceId)
                assertNull(before.syncedAt)

                assertEquals(
                    1,
                    dao.markSyncedIfVersionMatches(
                        taskId = before.id,
                        expectedVersion = before.version,
                        syncedAt = 2345L,
                    ),
                )

                val acknowledged = dao.getAllTasksSync().single()
                assertEquals(before.copy(syncedAt = 2345L), acknowledged)
                assertNull(acknowledged.projectId)
                assertEquals(ORDINARY_ID, acknowledged.projectWorkspaceId)

                assertEquals(
                    0,
                    dao.markSyncedIfVersionMatches(
                        taskId = before.id,
                        expectedVersion = before.version + 1L,
                        syncedAt = 6789L,
                    ),
                )
                assertEquals(acknowledged, dao.getAllTasksSync().single())
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
            roleCode = null,
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
