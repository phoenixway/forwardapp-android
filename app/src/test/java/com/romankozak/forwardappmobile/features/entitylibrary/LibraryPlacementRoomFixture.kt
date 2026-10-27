package com.romankozak.forwardappmobile.features.entitylibrary

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.*
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.toWorkspaceSnapshot
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.hierarchy.*
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.data.orientation.CanonicalAspectRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

internal class LibraryPlacementRoomFixture : AutoCloseable {
    val db = database()
    val store = ViewModelStore()
    val writer = H1StructuralMutationCoordinator(db)
    val command = EntityLibraryPlacementViewModel(writer, SavedStateHandle()).also { store.put("placement", it) }
    val library = EntityLibraryViewModel(com.romankozak.forwardappmobile.data.entitybrowser.canonicalQueryCatalog(db),
        CanonicalHierarchyPlacementPresenceRepository(db.hierarchyPlacementDao()), SavedStateHandle(), Dispatchers.Unconfined)
        .also { store.put("library", it) }

    suspend fun seed(): List<CanonicalEntityRef> {
        val o = db.orientationDao()
        val orientation = CanonicalOrientationRepository(db, o).createOrientation(OrientationKind.INTENT, "Orientation", now = 1)
        val aspect = CanonicalAspectRepository(db, o, HierarchyPlacementLifecycleCoordinator(db)).create("Aspect", now = 2)
        db.workspaceDao().upsert(listOf(WorkspaceEntity("w", "Workspace", null, null, 1, 1, null, false, 1,
            provenance = "STANDALONE", sourceContextId = null)))
        return listOf(CanonicalEntityRef.Orientation(orientation), CanonicalEntityRef.Aspect(aspect), CanonicalEntityRef.Workspace("w"))
    }

    suspend fun add(ref: CanonicalEntityRef, parent: H1StructuralParent) {
        command.begin(ref)
        command.place(parent)
        withTimeout(10_000) { command.state.first { !it.busy } }
        assertThat(command.state.value.error).isNull()
        assertThat(command.state.value.pendingRef).isNull()
    }

    suspend fun awaitLibrary(predicate: (EntityLibraryUiState.Ready) -> Boolean): EntityLibraryUiState.Ready =
        withTimeout(10_000) {
            library.uiState.first { it is EntityLibraryUiState.Ready && predicate(it) } as EntityLibraryUiState.Ready
        }

    suspend fun historicalContainer(): String {
        val o = db.orientationDao()
        o.upsertManagedSubjects(listOf(ManagedSubjectEntity("group", "ORIENTATION", "Historical group", null, 1, 1, null, false, 1)))
        o.upsertOrientations(listOf(OrientationEntity("group", "MAIN_BEACON_GROUP", null, "UNSET")))
        o.upsertLegacyMappings(listOf(LegacySubjectMappingEntity("map", "MAIN_BEACON_GROUP", "legacy-group", "group", 1, "CUT_OVER", 1, 1, null, false, 1)))
        HistoricalGroupContainerCutover(db).materialize()
        return historicalGroupContainerId("group")
    }

    suspend fun graph() = projectH1UnifiedStructuralGraph(
        db.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() }, BigPicturePresentationRepository(db).snapshot())

    suspend fun roundtrip() {
        val o = db.orientationDao()
        val placements = CanonicalHierarchyPlacementSyncStore(db).loadAll()
        val scopes = CanonicalHierarchyPlacementGroupScopeSyncStore(db).loadAll()
        val coverage = GroupScopeCoverageStore(db).snapshot()
        val presentation = BigPicturePresentationRepository(db).snapshot()
        val bundle = SnapshotBundle(workspaces = db.workspaceDao().getAll().map { it.toWorkspaceSnapshot() },
            managedSubjects = o.getAllManagedSubjects(), orientations = o.getAllOrientations(),
            aspects = o.getAllAspects(), hierarchyPlacements = placements,
            hierarchyPlacementGroupScopes = scopes, groupScopeCoverage = coverage, bigPicturePresentation = presentation)
        for (merge in listOf(false, true)) {
            val target = database()
            try {
                target.withTransaction {
                    val dao = target.orientationDao()
                    dao.upsertManagedSubjects(o.getAllManagedSubjects()); dao.upsertOrientations(o.getAllOrientations())
                    dao.upsertAspects(o.getAllAspects()); target.workspaceDao().upsert(db.workspaceDao().getAll())
                    dao.upsertLegacyMappings(o.getAllLegacyMappings()); dao.upsertOrientationRelations(o.getAllOrientationRelations())
                    val h1 = CanonicalHierarchyPlacementSyncStore(target)
                    val gs = CanonicalHierarchyPlacementGroupScopeSyncStore(target)
                    if (merge) {
                        h1.mergeIncoming(placements)
                        gs.mergeIncoming(scopes, coverage)
                        BigPicturePresentationRepository(target).merge(presentation)
                    } else {
                        val decoded = h1.decodeAndValidateForRestore(bundle, placements)
                        val decodedScopes = gs.decodeAndValidateForRestore(bundle, scopes, decoded, true)
                        h1.restoreExactDecoded(decoded)
                        GroupScopeCoverageStore(target).restoreExact(coverage)
                        gs.restoreExactDecoded(decodedScopes, true)
                        BigPicturePresentationRepository(target).restoreExact(presentation)
                    }
                    BigPicturePresentationRepository(target).requireCurrentUnifiedStructuralGraphValid()
                    GroupScopeCoverageStore(target).validateCurrent()
                }
                assertThat(target.hierarchyPlacementDao().getAll().map { it.copy(syncedAt = null) })
                    .containsExactlyElementsIn(db.hierarchyPlacementDao().getAll().map { it.copy(syncedAt = null) })
                assertThat(BigPicturePresentationRepository(target).snapshot().let { snapshot ->
                    snapshot.copy(containers = snapshot.containers.map { it.copy(syncedAt = null) },
                        memberships = snapshot.memberships.map { it.copy(syncedAt = null) })
                }).isEqualTo(presentation.copy(containers = presentation.containers.map { it.copy(syncedAt = null) },
                    memberships = presentation.memberships.map { it.copy(syncedAt = null) }))
            } finally { target.close() }
        }
    }

    /** Exact row snapshots, including versions, of all independent semantic authorities requested by this seam. */
    fun semanticRows(): Map<String, List<List<String?>>> {
        val relevant = listOf("managed_subjects", "orientations", "aspects", "orientation_relations", "aspect_orientation_refs",
            "workspaces", "workspace_bindings", "workspace_direction_entries", "planning_scopes", "planning_commitments",
            "contexts", "main_beacons", "main_beacon_groups", "main_beacon_group_members", "legacy_subject_mappings",
            "hierarchy_placement_group_scopes")
        val sqlite = db.openHelper.readableDatabase
        return relevant.associateWith { table ->
            sqlite.query("SELECT * FROM $table ORDER BY rowid").use { cursor ->
                buildList { while (cursor.moveToNext()) add((0 until cursor.columnCount).map { cursor.getString(it) }) }
            }
        }
    }

    override fun close() { store.clear(); db.close() }

    private fun database() = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java,
    ).allowMainThreadQueries().build()
}
