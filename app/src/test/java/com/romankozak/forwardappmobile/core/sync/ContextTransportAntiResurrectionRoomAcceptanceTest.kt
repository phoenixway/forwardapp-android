package com.romankozak.forwardappmobile.core.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncVersion
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.dao.DayPlanDao
import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationBootstrapper
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceBootstrapper
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceProblemSyncStore
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagTransportStore
import com.romankozak.forwardappmobile.data.workspace.ContextWorkspaceWriteThrough
import com.romankozak.forwardappmobile.data.workspace.SystemContextShellRetirer
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceMaterializer
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagSeed
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.DatabaseInitializer
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureDao
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconDao
import com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.sync.SyncLogicHelper
import com.romankozak.forwardappmobile.sync.datasource.CanonicalWorkspaceProblemSyncPayload
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkClass
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
class ContextTransportAntiResurrectionRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `full restore cannot resurrect Context retired by incoming canonical Workspace`() =
        runBlocking {
            val destination = database()
            try {
                val liveLegacy = ordinaryContext(RETIRED_ID)

                fullBackup(destination).applySnapshotBundle(
                    canonicalBundle(
                        context = liveLegacy,
                        workspace = canonicalOnlyWorkspace(RETIRED_ID),
                    ),
                )

                val persisted = destination.contextDao().getContextById(RETIRED_ID)
                assertTrue(persisted == null || persisted.isDeleted)

                val workspace = destination.workspaceDao().getById(RETIRED_ID)
                assertTrue(workspace != null)
                assertTrue(workspace!!.provenance == WorkspaceProvenance.CANONICAL_ONLY.name)
                assertNull(workspace.sourceContextId)
            } finally {
                destination.close()
            }
        }

    @Test
    fun `merge cannot resurrect Context retired by local canonical Workspace`() =
        runBlocking {
            val database = database()
            try {
                database.workspaceDao().upsert(
                    listOf(canonicalOnlyWorkspace(RETIRED_ID)),
                )

                merge(database).applySnapshotBundle(
                    SnapshotBundle(
                        version = 1,
                        contexts = listOf(ordinaryContext(RETIRED_ID).toSnapshot()),
                    ),
                )

                val persisted = database.contextDao().getContextById(RETIRED_ID)
                assertTrue(persisted == null || persisted.isDeleted)
            } finally {
                database.close()
            }
        }

    @Test
    fun `retired Context tombstone remains valid transport evidence`() =
        runBlocking {
            val destination = database()
            try {
                val tombstone =
                    ordinaryContext(RETIRED_ID).copy(
                        isDeleted = true,
                        version = 7L,
                    )

                fullBackup(destination).applySnapshotBundle(
                    canonicalBundle(
                        context = tombstone,
                        workspace = canonicalOnlyWorkspace(RETIRED_ID),
                    ),
                )

                val persisted = destination.contextDao().getContextById(RETIRED_ID)
                assertTrue(persisted != null)
                assertTrue(persisted!!.isDeleted)
            } finally {
                destination.close()
            }
        }

    @Test
    fun `full export excludes live retired Context but preserves Context-backed transport`() =
        runBlocking {
            val database = database()
            try {
                database.contextDao().insertContexts(
                    listOf(
                        ordinaryContext(RETIRED_ID),
                        ordinaryContext(CONTEXT_BACKED_ID),
                    ),
                )
                database.workspaceDao().upsert(
                    listOf(
                        canonicalOnlyWorkspace(RETIRED_ID),
                        contextBackedWorkspace(CONTEXT_BACKED_ID),
                    ),
                )

                val exported = fullBackup(database).loadFullSnapshotBundle()

                assertFalse(
                    exported.contexts.any {
                        it.id == RETIRED_ID && !it.isDeleted
                    },
                )
                assertTrue(
                    exported.contexts.any {
                        it.id == CONTEXT_BACKED_ID && !it.isDeleted
                    },
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `sync selection and delta exclude live retired Context but keep Context-backed Context`() =
        runBlocking {
            val database = database()
            try {
                database.contextDao().insertContexts(
                    listOf(
                        ordinaryContext(RETIRED_ID),
                        ordinaryContext(CONTEXT_BACKED_ID),
                    ),
                )
                database.workspaceDao().upsert(
                    listOf(
                        canonicalOnlyWorkspace(RETIRED_ID),
                        contextBackedWorkspace(CONTEXT_BACKED_ID),
                    ),
                )

                val sync = sync(database)

                val selection = sync.getUnsyncedSelection()
                assertFalse(selection.contexts.any { it.id == RETIRED_ID })
                assertTrue(selection.contexts.any { it.id == CONTEXT_BACKED_ID })

                val delta = sync.getChangesSince(0L)
                assertFalse(
                    delta.contexts.any {
                        it.id == RETIRED_ID && !it.isDeleted
                    },
                )
                assertTrue(
                    delta.contexts.any {
                        it.id == CONTEXT_BACKED_ID && !it.isDeleted
                    },
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `sync acknowledge cannot rewrite retired live Context`() =
        runBlocking {
            val database = database()
            try {
                database.contextDao().insertContexts(
                    listOf(ordinaryContext(RETIRED_ID)),
                )
                database.workspaceDao().upsert(
                    listOf(canonicalOnlyWorkspace(RETIRED_ID)),
                )

                val sync = sync(database)
                val before = requireNotNull(database.contextDao().getContextById(RETIRED_ID))
                assertNull(before.syncedAt)

                val baseline = sync.getUnsyncedSelection()
                sync.acknowledge(
                    baseline.copy(
                        contexts =
                            listOf(
                                LocalSyncVersion(
                                    id = RETIRED_ID,
                                    version = before.version,
                                ),
                            ),
                    ),
                )

                val after = requireNotNull(database.contextDao().getContextById(RETIRED_ID))
                assertNull(after.syncedAt)
                assertFalse(after.isDeleted)
            } finally {
                database.close()
            }
        }



    private fun canonicalBundle(
        context: ContextEntity,
        workspace: WorkspaceEntity,
    ) = SnapshotBundle(
        version = 2,
        contexts = listOf(context.toSnapshot()),
        managedSubjects = emptyList(),
        orientations = emptyList(),
        aspects = emptyList(),
        orientationAssessments = emptyList(),
        orientationAssessmentRevisions = emptyList(),
        legacySubjectMappings = emptyList(),
        orientationRelations = emptyList(),
        aspectOrientationRefs = emptyList(),
        workspaces = listOf(workspace),
        workspaceBindings = emptyList(),
        workspaceCapabilityInstances = emptyList(),
        savedOrientationViews = emptyList(),
    )

    private fun ordinaryContext(id: String) =
        ContextEntity(
            id = id,
            name = id,
            description = null,
            parentId = null,
            createdAt = 10L,
            updatedAt = 20L,
        )

    private fun canonicalOnlyWorkspace(id: String) =
        workspace(
            id = id,
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
            sourceContextId = null,
        )

    private fun contextBackedWorkspace(id: String) =
        workspace(
            id = id,
            provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
            sourceContextId = id,
        )

    private fun workspace(
        id: String,
        provenance: String,
        sourceContextId: String?,
    ) = WorkspaceEntity(
        id = id,
        nameOverride = id,
        descriptionOverride = null,
        parentWorkspaceId = null,
        roleCode = null,
        workspaceOrder = 0L,
        createdAt = 10L,
        updatedAt = 20L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
        provenance = provenance,
        sourceContextId = sourceContextId,
    )

    private fun initializer(database: AppDatabase): DatabaseInitializer =
        DatabaseInitializer(
            systemWorkspaceMaterializer =
                SystemWorkspaceMaterializer(
                    database,
                    database.contextDao(),
                    database.workspaceDao(),
                ),
            systemWorkspaceTagSeed =
                SystemWorkspaceTagSeed(
                    database,
                    database.contextDao(),
                ),
            systemContextShellRetirer =
                SystemContextShellRetirer(
                    database = database,
                    contextDao = database.contextDao(),
                ),
        )

    private fun fullBackup(database: AppDatabase): FullBackupLocalDataSourceImpl {
        val bootstrapper = workspaceBootstrapper(database)
        val problemStore = mockk<CanonicalWorkspaceProblemSyncStore>(relaxed = true)
        coEvery { problemStore.loadAll() } returns CanonicalWorkspaceProblemSyncPayload()

        return construct(
            type = FullBackupLocalDataSourceImpl::class.java,
            overrides =
                mapOf(
                    AppDatabase::class.java to database,
                    DayPlanDao::class.java to database.dayPlanDao(),
                    DayTaskDao::class.java to database.dayTaskDao(),
                    ContextDao::class.java to database.contextDao(),
                    ContextStructureDao::class.java to database.contextStructureDao(),
                    MainBeaconDao::class.java to database.mainBeaconDao(),
                    TacticalMissionDao::class.java to database.tacticalMissionDao(),
                    CanonicalWorkspaceBootstrapper::class.java to bootstrapper,
                    ContextWorkspaceWriteThrough::class.java to ContextWorkspaceWriteThrough(bootstrapper),
                    DatabaseInitializer::class.java to initializer(database),
                    CanonicalWorkspaceProblemSyncStore::class.java to problemStore,
                    CanonicalWorkspaceTagTransportStore::class.java to
                        CanonicalWorkspaceTagTransportStore(database),
                    SystemWorkspaceTagSeed::class.java to
                        SystemWorkspaceTagSeed(database, database.contextDao()),
                ),
        )
    }

    private fun merge(database: AppDatabase): MergeLocalDataSourceImpl {
        val bootstrapper = workspaceBootstrapper(database)

        return construct(
            type = MergeLocalDataSourceImpl::class.java,
            overrides =
                mapOf(
                    AppDatabase::class.java to database,
                    DayPlanDao::class.java to database.dayPlanDao(),
                    DayTaskDao::class.java to database.dayTaskDao(),
                    ContextDao::class.java to database.contextDao(),
                    ContextStructureDao::class.java to database.contextStructureDao(),
                    MainBeaconDao::class.java to database.mainBeaconDao(),
                    TacticalMissionDao::class.java to database.tacticalMissionDao(),
                    CanonicalWorkspaceBootstrapper::class.java to bootstrapper,
                    ContextWorkspaceWriteThrough::class.java to ContextWorkspaceWriteThrough(bootstrapper),
                ),
        )
    }

    private fun sync(database: AppDatabase): SyncLocalDataSourceImpl =
        construct(
            type = SyncLocalDataSourceImpl::class.java,
            overrides =
                mapOf(
                    AppDatabase::class.java to database,
                    ContextDao::class.java to database.contextDao(),
                    ActivityRecordDao::class.java to database.activityRecordDao(),
                    SyncLogicHelper::class.java to SyncLogicHelper(),
                ),
        )

    private fun workspaceBootstrapper(database: AppDatabase) =
        CanonicalWorkspaceBootstrapper(
            database = database,
            workspaceDao = database.workspaceDao(),
            orientationDao = database.orientationDao(),
            contextDao = database.contextDao(),
            contextStructureDao = database.contextStructureDao(),
        )

    private fun <T : Any> construct(
        type: Class<T>,
        overrides: Map<Class<*>, Any>,
    ): T {
        val constructor = type.declaredConstructors.single()
        constructor.isAccessible = true
        val arguments =
            constructor.parameterTypes.map { parameterType ->
                overrides[parameterType] ?: relaxedMock(parameterType)
            }.toTypedArray()

        @Suppress("UNCHECKED_CAST")
        return constructor.newInstance(*arguments) as T
    }

    @Suppress("UNCHECKED_CAST")
    private fun relaxedMock(type: Class<*>): Any =
        mockkClass(type.kotlin as KClass<Any>, relaxed = true)

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private companion object {
        const val RETIRED_ID = "transport-retired-context"
        const val CONTEXT_BACKED_ID = "transport-context-backed"
    }
}
