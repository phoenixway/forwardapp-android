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
                        context(RETIRED_ID).copy(isDeleted = true),
                    ),
                )
                database.workspaceDao().upsert(
                    listOf(
                        canonicalWorkspace(SYSTEM_ID),
                        standaloneWorkspace(STANDALONE_ID),
                        contextBackedWorkspace(ORDINARY_ID),
                        canonicalWorkspace(RETIRED_ID),
                        contextBackedWorkspace(HISTORICAL_SYS_PREFIX_ID),
                    ),
                )

                dao.insertMissions(
                    listOf(
                        mission(1L, SYSTEM_ID),
                        mission(2L, ORDINARY_ID),
                        mission(3L, HISTORICAL_SYS_PREFIX_ID),
                        mission(4L, STANDALONE_ID),
                        mission(5L, RETIRED_ID),
                    ),
                )

                val stored = dao.getAllMissionsSync().associateBy { it.id }

                val systemMission = requireNotNull(stored[1L])
                assertNull(systemMission.projectId)
                assertEquals(SYSTEM_ID, systemMission.projectWorkspaceId)
                assertEquals(SYSTEM_ID, systemMission.logicalProjectId)

                val ordinaryMission = requireNotNull(stored[2L])
                assertNull(ordinaryMission.projectId)
                assertEquals(ORDINARY_ID, ordinaryMission.projectWorkspaceId)
                assertEquals(ORDINARY_ID, ordinaryMission.logicalProjectId)

                val historicalMission = requireNotNull(stored[3L])
                assertNull(historicalMission.projectId)
                assertEquals(HISTORICAL_SYS_PREFIX_ID, historicalMission.projectWorkspaceId)
                assertEquals(HISTORICAL_SYS_PREFIX_ID, historicalMission.logicalProjectId)

                val standaloneMission = requireNotNull(stored[4L])
                assertNull(standaloneMission.projectId)
                assertEquals(STANDALONE_ID, standaloneMission.projectWorkspaceId)
                assertEquals(STANDALONE_ID, standaloneMission.logicalProjectId)

                val retiredMission = requireNotNull(stored[5L])
                assertNull(retiredMission.projectId)
                assertEquals(RETIRED_ID, retiredMission.projectWorkspaceId)
                assertEquals(RETIRED_ID, retiredMission.logicalProjectId)

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
                assertEquals(
                    listOf(4L),
                    dao.getMissionsForProject(STANDALONE_ID).first().map { it.id },
                )
                assertEquals(
                    listOf(5L),
                    dao.getMissionsForProject(RETIRED_ID).first().map { it.id },
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

                dao.updateMission(
                    standaloneMission.copy(
                        title = "Updated standalone",
                        projectId = STANDALONE_ID,
                        projectWorkspaceId = null,
                    ),
                )

                val updatedStandalone = requireNotNull(dao.getMissionById(4L))
                assertEquals("Updated standalone", updatedStandalone.title)
                assertNull(updatedStandalone.projectId)
                assertEquals(STANDALONE_ID, updatedStandalone.projectWorkspaceId)
                assertEquals(STANDALONE_ID, updatedStandalone.logicalProjectId)

                dao.updateMission(
                    retiredMission.copy(
                        projectId = RETIRED_ID,
                        projectWorkspaceId = null,
                    ),
                )
                val updatedRetired = requireNotNull(dao.getMissionById(5L))
                assertNull(updatedRetired.projectId)
                assertEquals(RETIRED_ID, updatedRetired.projectWorkspaceId)
            } finally {
                database.close()
            }
        }

    @Test
    fun `invalid non-System Workspace liveness and provenance fail closed`() =
        runBlocking {
            listOf(
                "deleted-standalone" to standaloneWorkspace(DELETED_STANDALONE_ID).copy(isDeleted = true),
                "malformed-standalone-source" to
                    standaloneWorkspace(MALFORMED_STANDALONE_ID).copy(
                        sourceContextId = ORDINARY_ID,
                    ),
            ).forEach { (case, workspace) ->
                val database = database()
                try {
                    database.workspaceDao().upsert(listOf(workspace))

                    val failure =
                        runCatching {
                            database.tacticalMissionDao().insertMission(
                                mission(1L, workspace.id),
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

    @Test
    fun `Context only and equivalent dual owners converge to validated Workspace`() =
        runBlocking {
            val db = database()
            try {
                val dao = db.tacticalMissionDao()
                db.contextDao().insertContexts(listOf(context(ORDINARY_ID)))
                db.workspaceDao().upsert(listOf(contextBackedWorkspace(ORDINARY_ID)))

                dao.insertMission(mission(61L, ORDINARY_ID))
                val first = requireNotNull(dao.getMissionById(61L))
                assertNull(first.projectId)
                assertEquals(ORDINARY_ID, first.projectWorkspaceId)

                dao.updateMission(
                    first.copy(
                        projectId = ORDINARY_ID,
                        projectWorkspaceId = ORDINARY_ID,
                        title = "Equivalent update",
                    ),
                )
                val updated = requireNotNull(dao.getMissionById(61L))
                assertNull(updated.projectId)
                assertEquals(ORDINARY_ID, updated.projectWorkspaceId)
                assertEquals("Equivalent update", updated.title)

                // A historical raw Context-only fixture must not be returned
                // by the ordinary canonical Workspace project reader.
                dao.insertMissionRaw(mission(62L, ORDINARY_ID))
                assertEquals(
                    listOf(61L),
                    dao.getMissionsForProject(ORDINARY_ID).first().map { it.id },
                )
            } finally {
                db.close()
            }
        }

    @Test
    fun `missing or deleted Workspace rejects Context owned mission`() =
        runBlocking {
            listOf(
                "missing" to null,
                "deleted" to contextBackedWorkspace(ORDINARY_ID).copy(isDeleted = true),
                "unproven" to canonicalWorkspace(ORDINARY_ID),
            ).forEach { (label, workspace) ->
                val db = database()
                try {
                    db.contextDao().insertContexts(listOf(context(ORDINARY_ID)))
                    workspace?.let { db.workspaceDao().upsert(listOf(it)) }

                    val failure =
                        runCatching {
                            db.tacticalMissionDao().insertMission(
                                mission(71L, ORDINARY_ID),
                            )
                        }.exceptionOrNull()

                    assertTrue("$label must fail closed", failure is IllegalArgumentException)
                    assertTrue(db.tacticalMissionDao().getAllMissionsSync().isEmpty())
                } finally {
                    db.close()
                }
            }
        }

    @Test
    fun `Contextless canonical only Workspace and no project remain valid`() =
        runBlocking {
            val db = database()
            try {
                val dao = db.tacticalMissionDao()
                db.workspaceDao().upsert(
                    listOf(canonicalWorkspace(ARBITRARY_CANONICAL_ONLY_ID)),
                )

                dao.insertMission(mission(81L, ARBITRARY_CANONICAL_ONLY_ID))
                dao.insertMission(mission(82L, null))

                val workspaceOwned = requireNotNull(dao.getMissionById(81L))
                assertNull(workspaceOwned.projectId)
                assertEquals(
                    ARBITRARY_CANONICAL_ONLY_ID,
                    workspaceOwned.projectWorkspaceId,
                )

                val ownerless = requireNotNull(dao.getMissionById(82L))
                assertNull(ownerless.projectId)
                assertNull(ownerless.projectWorkspaceId)
            } finally {
                db.close()
            }
        }

    @Test
    fun `version guarded mission ACK changes metadata only`() =
        runBlocking {
            val db = database()
            try {
                val dao = db.tacticalMissionDao()
                db.workspaceDao().upsert(listOf(standaloneWorkspace(STANDALONE_ID)))
                dao.insertMission(
                    mission(91L, STANDALONE_ID).copy(
                        missionStreamId = "stream-a",
                        orderInSlot = 7L,
                    ),
                )

                val before = requireNotNull(dao.getMissionById(91L))

                assertEquals(
                    0,
                    dao.markMissionSyncedIfVersionMatches(
                        missionId = 91L,
                        expectedVersion = before.version + 1L,
                        syncedAt = 100L,
                    ),
                )
                assertEquals(before, dao.getMissionById(91L))

                assertEquals(
                    1,
                    dao.markMissionSyncedIfVersionMatches(
                        missionId = 91L,
                        expectedVersion = before.version,
                        syncedAt = 200L,
                    ),
                )
                val after = requireNotNull(dao.getMissionById(91L))
                assertEquals(before.copy(syncedAt = 200L), after)
                assertNull(after.projectId)
                assertEquals(STANDALONE_ID, after.projectWorkspaceId)
                assertEquals("stream-a", after.missionStreamId)
                assertEquals(7L, after.orderInSlot)
            } finally {
                db.close()
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
            roleCode = null,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
            sourceContextId = null,
        )

    private fun contextBackedWorkspace(id: String) =
        canonicalWorkspace(id).copy(
            provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
            sourceContextId = id,
        )

    private fun standaloneWorkspace(id: String) =
        canonicalWorkspace(id).copy(
            provenance = WorkspaceProvenance.STANDALONE.name,
        )

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private companion object {
        val SYSTEM_ID: String = SystemContexts.TODAY.raw
        const val ORDINARY_ID = "ordinary-context"
        const val HISTORICAL_SYS_PREFIX_ID = "sys_strategic-beacons"
        const val STANDALONE_ID = "standalone-workspace"
        const val RETIRED_ID = "retired-context-owner"
        const val ARBITRARY_CANONICAL_ONLY_ID = "canonical-only-workspace"
        const val DELETED_STANDALONE_ID = "deleted-standalone-workspace"
        const val MALFORMED_STANDALONE_ID = "malformed-standalone-workspace"
    }
}
