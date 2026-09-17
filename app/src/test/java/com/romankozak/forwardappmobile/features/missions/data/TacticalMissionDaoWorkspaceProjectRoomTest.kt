package com.romankozak.forwardappmobile.features.missions.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.NO_DEADLINE
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.logicalProjectId
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
class TacticalMissionDaoWorkspaceProjectRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `typed routing stores exact reserved project in Workspace branch and preserves one logical project id`() =
        runBlocking {
            val database = database()
            try {
                val dao = database.tacticalMissionDao()

                database.contextDao().insertContexts(
                    listOf(
                        context(ORDINARY_ID),
                        context(HISTORICAL_SYS_PREFIX_ID),
                    ),
                )
                database.workspaceDao().upsert(
                    listOf(canonicalWorkspace(SYSTEM_ID)),
                )

                dao.insertMissions(
                    listOf(
                        mission(1L, SYSTEM_ID),
                        mission(2L, ORDINARY_ID),
                        mission(3L, HISTORICAL_SYS_PREFIX_ID),
                    ),
                )

                val stored = dao.getAllMissionsSync().associateBy { it.id }

                val systemMission = requireNotNull(stored[1L])
                assertNull(systemMission.projectId)
                assertEquals(SYSTEM_ID, systemMission.projectWorkspaceId)
                assertEquals(SYSTEM_ID, systemMission.logicalProjectId)

                val ordinaryMission = requireNotNull(stored[2L])
                assertEquals(ORDINARY_ID, ordinaryMission.projectId)
                assertNull(ordinaryMission.projectWorkspaceId)
                assertEquals(ORDINARY_ID, ordinaryMission.logicalProjectId)

                val historicalMission = requireNotNull(stored[3L])
                assertEquals(HISTORICAL_SYS_PREFIX_ID, historicalMission.projectId)
                assertNull(historicalMission.projectWorkspaceId)
                assertEquals(HISTORICAL_SYS_PREFIX_ID, historicalMission.logicalProjectId)

                assertEquals(
                    listOf(1L),
                    dao.getMissionsForProject(SYSTEM_ID).first().map { it.id },
                )
                assertEquals(
                    listOf(2L),
                    dao.getMissionsForProject(ORDINARY_ID).first().map { it.id },
                )
                assertEquals(
                    listOf(3L),
                    dao.getMissionsForProject(HISTORICAL_SYS_PREFIX_ID).first().map { it.id },
                )

                dao.updateMission(
                    systemMission.copy(
                        title = "Updated",
                        projectId = null,
                        projectWorkspaceId = SYSTEM_ID,
                    ),
                )

                val updated = requireNotNull(dao.getMissionById(1L))
                assertEquals("Updated", updated.title)
                assertNull(updated.projectId)
                assertEquals(SYSTEM_ID, updated.projectWorkspaceId)
                assertEquals(SYSTEM_ID, updated.logicalProjectId)
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
                    workspace?.let {
                        database.workspaceDao().upsert(listOf(it))
                    }

                    val failure =
                        runCatching {
                            database.tacticalMissionDao().insertMission(
                                mission(1L, SYSTEM_ID),
                            )
                        }.exceptionOrNull()

                    assertTrue("$case must fail closed", failure is IllegalArgumentException)
                    assertTrue(database.tacticalMissionDao().getAllMissionsSync().isEmpty())
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
                database.contextDao().insertContexts(listOf(context(ORDINARY_ID)))
                database.workspaceDao().upsert(listOf(canonicalWorkspace(SYSTEM_ID)))

                val failure =
                    runCatching {
                        database.tacticalMissionDao().insertMission(
                            mission(1L, ORDINARY_ID).copy(
                                projectWorkspaceId = SYSTEM_ID,
                            ),
                        )
                    }.exceptionOrNull()

                assertTrue(failure is IllegalArgumentException)
                assertTrue(database.tacticalMissionDao().getAllMissionsSync().isEmpty())
            } finally {
                database.close()
            }
        }

    private fun mission(
        id: Long,
        projectId: String?,
    ) =
        TacticalMission(
            id = id,
            title = "Mission $id",
            description = null,
            deadline = NO_DEADLINE,
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

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private companion object {
        val SYSTEM_ID: String = SystemContexts.TODAY.raw
        const val ORDINARY_ID = "ordinary-context"
        const val HISTORICAL_SYS_PREFIX_ID = "sys_strategic-beacons"
    }
}
