package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context as AndroidContext
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextParentLink
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconParentLink
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagRepository
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalV1HierarchySnapshotReaderRoomTest {
    private val context: AndroidContext = ApplicationProvider.getApplicationContext()

    @Test
    fun `live V1 capture preserves workspace and beacon additional parent occurrences and materializes atomically`() =
        runBlocking {
            val db = database()
            try {
                context(db, "wa", null, 0)
                context(db, "wb", null, 1)
                context(db, "shared", "wa", 0)

                workspace(db, "wa", null, 0)
                workspace(db, "wb", null, 1)
                workspace(db, "shared", "wa", 0)

                db.contextParentLinkDao().insertAll(
                    listOf(
                        ContextParentLink(
                            parentContextId = "wb",
                            childContextId = "shared",
                            order = 0,
                            createdAt = 1L,
                            updatedAt = 1L,
                        ),
                    ),
                )

                beaconWithCutOverTarget(db, "b1", "s1", null, 0)
                beaconWithCutOverTarget(db, "b2", "s2", null, 1)
                beaconWithCutOverTarget(db, "child", "s-child", "b1", 0)
                db.mainBeaconDao().insertParentLinks(
                    listOf(
                        MainBeaconParentLink(
                            parentBeaconId = "b2",
                            childBeaconId = "child",
                            order = 0,
                            createdAt = 1L,
                            updatedAt = 1L,
                        ),
                    ),
                )

                val reader = reader(db)
                val snapshot = reader.capture()

                val shared =
                    snapshot.occurrences.filter {
                        it.target.type == HierarchyTargetType.WORKSPACE &&
                            it.target.id == "shared"
                    }
                assertEquals(2, shared.size)
                assertEquals(1, shared.count { it.placementKind == PlacementKind.PRIMARY })
                assertEquals(1, shared.count { it.placementKind == PlacementKind.LINK })

                val beaconChild =
                    snapshot.occurrences.filter {
                        it.target.type == HierarchyTargetType.MANAGED_SUBJECT &&
                            it.target.id == "s-child"
                    }
                assertEquals(2, beaconChild.size)
                assertEquals(1, beaconChild.count { it.placementKind == PlacementKind.PRIMARY })
                assertEquals(1, beaconChild.count { it.placementKind == PlacementKind.LINK })
                assertTrue(
                    beaconChild.any {
                        it.sourceAuthority ==
                            CanonicalV1HierarchySourceAuthority.MAIN_BEACON_PARENT_LINK
                    },
                )

                val migration =
                    CanonicalV1HierarchyMigration(
                        database = db,
                        snapshotReader = reader,
                        materializer = CanonicalV1HierarchyMaterializer(db),
                    )

                val first = migration.materializeFromCurrentV1(now = 100L)
                val rowsAfterFirst = db.hierarchyPlacementDao().getAll()
                val second = migration.materializeFromCurrentV1(now = 200L)
                val rowsAfterSecond = db.hierarchyPlacementDao().getAll()

                assertEquals(
                    CanonicalV1HierarchyMaterializationOutcome.CREATED,
                    first.outcome,
                )
                assertEquals(snapshot.occurrences.size, first.occurrenceCount)
                assertEquals(
                    CanonicalV1HierarchyMaterializationOutcome.EXACT_RERUN,
                    second.outcome,
                )
                assertEquals(rowsAfterFirst, rowsAfterSecond)
                assertEquals(setOf(100L), rowsAfterSecond.map { it.updatedAt }.toSet())
            } finally {
                db.close()
            }
        }

    @Test
    fun `visible beacon without live CUT_OVER subject fails closed before V2 write`() =
        runBlocking {
            val db = database()
            try {
                db.mainBeaconDao().insertBeacon(
                    MainBeacon(
                        id = "unmapped",
                        title = "Legacy only",
                        createdAt = 1L,
                        updatedAt = 1L,
                    ),
                )

                val reader = reader(db)
                val migration =
                    CanonicalV1HierarchyMigration(
                        database = db,
                        snapshotReader = reader,
                        materializer = CanonicalV1HierarchyMaterializer(db),
                    )

                val failure =
                    runCatching {
                        migration.materializeFromCurrentV1(now = 100L)
                    }.exceptionOrNull()

                assertTrue(failure is CanonicalV1HierarchySnapshotCaptureException)
                assertTrue(
                    failure?.message?.contains("no live CUT_OVER ManagedSubject target") == true,
                )
                assertTrue(db.hierarchyPlacementDao().getAll().isEmpty())
            } finally {
                db.close()
            }
        }

    private fun reader(db: AppDatabase): CanonicalV1HierarchySnapshotReader {
        val canonicalTags = CanonicalWorkspaceTagRepository(db)
        val tagAuthority =
            SystemWorkspaceTagAuthority(
                workspaceDao = db.workspaceDao(),
                workspaceTagRefDao = db.workspaceTagRefDao(),
                systemWorkspaceTagSeedStateDao = db.systemWorkspaceTagSeedStateDao(),
                canonicalWorkspaceTagRepository = canonicalTags,
            )
        val projector =
            SystemWorkspacePresentationContextProjector(
                workspaceDao = db.workspaceDao(),
                systemWorkspaceTagAuthority = tagAuthority,
                canonicalWorkspaceTagRepository = canonicalTags,
                contextDao = db.contextDao(),
            )

        return CanonicalV1HierarchySnapshotReader(
            database = db,
            presentationProjector = projector,
            builder = CanonicalV1HierarchySnapshotBuilder(),
        )
    }

    private suspend fun context(
        db: AppDatabase,
        id: String,
        parentId: String?,
        order: Long,
    ) {
        db.contextDao().insertContexts(
            listOf(
                Context(
                    id = id,
                    name = id,
                    description = null,
                    parentId = parentId,
                    createdAt = 1L,
                    updatedAt = 1L,
                    isDeleted = false,
                    version = 1L,
                    order = order,
                ),
            ),
        )
    }

    private suspend fun workspace(
        db: AppDatabase,
        id: String,
        parentId: String?,
        order: Long,
    ) {
        db.workspaceDao().upsert(
            listOf(
                WorkspaceEntity(
                    id = id,
                    nameOverride = id,
                    descriptionOverride = null,
                    parentWorkspaceId = parentId,
                    roleCode = null,
                    workspaceOrder = order,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                    provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
                    sourceContextId = id,
                ),
            ),
        )
    }

    private suspend fun beaconWithCutOverTarget(
        db: AppDatabase,
        beaconId: String,
        subjectId: String,
        parentBeaconId: String?,
        order: Long,
    ) {
        db.orientationDao().upsertManagedSubjects(
            listOf(
                ManagedSubjectEntity(
                    id = subjectId,
                    subjectType = "ASPECT",
                    title = "canonical-$beaconId",
                    description = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
        db.orientationDao().upsertLegacyMappings(
            listOf(
                LegacySubjectMappingEntity(
                    id = "mapping-$beaconId",
                    sourceType = LegacyOrientationSourceType.MAIN_BEACON.name,
                    sourceId = beaconId,
                    subjectId = subjectId,
                    migrationVersion = 1,
                    state = LegacySubjectMappingState.CUT_OVER.name,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
        db.mainBeaconDao().insertBeacon(
            MainBeacon(
                id = beaconId,
                title = "legacy-$beaconId",
                parentBeaconId = parentBeaconId,
                order = order,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
