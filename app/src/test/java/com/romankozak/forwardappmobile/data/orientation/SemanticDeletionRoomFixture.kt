package com.romankozak.forwardappmobile.data.orientation

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.toWorkspaceSnapshot
import com.romankozak.forwardappmobile.data.hierarchy.*
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.*

internal class SemanticDeletionRoomFixture(private val queryCallback: androidx.room.RoomDatabase.QueryCallback? = null) : AutoCloseable {
    val db = database()
    val dao = db.orientationDao()
    val hierarchy = HierarchyPlacementLifecycleCoordinator(db)
    val orientations = CanonicalOrientationRepository(db, dao)
    val aspects = CanonicalAspectRepository(db, dao, hierarchy)
    val graph = CanonicalOrientationGraphRepository(db, dao, db.workspaceDao())
    val links = CanonicalAspectLinksRepository(db, dao, db.contextDao())
    val bindings = CanonicalWorkspaceBindingRepository(db, dao, db.workspaceDao())
    val h1 = H1StructuralMutationCoordinator(db)
    val command = CanonicalSemanticEntityLifecycleCoordinator(db, orientations, aspects, graph, links, bindings, hierarchy)

    suspend fun orientation(kind: OrientationKind = OrientationKind.INTENT) = orientations.createOrientation(kind, "Meaning", now = 10)
    suspend fun aspect() = aspects.create("Health", now = 10)
    suspend fun workspace() {
        db.workspaceDao().upsert(listOf(WorkspaceEntity("workspace", "Workspace", null, null, 1, 1, null, false, 1, "STANDALONE", null)))
    }
    suspend fun appearance(id: String, destination: H1StructuralParent = H1StructuralParent.Root): PlacementId =
        h1.addAppearance(HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, id), destination, now = 20)

    suspend fun delete(id: String, type: ManagedSubjectType = ManagedSubjectType.ORIENTATION, version: Long = 1) =
        command.tombstone(id, type, version, now = 100)

    suspend fun snapshot(): SnapshotBundle = SnapshotBundle(
        managedSubjects = dao.getAllManagedSubjects(), orientations = dao.getAllOrientations(), aspects = dao.getAllAspects(),
        orientationAssessments = dao.getAllAssessments(), orientationAssessmentRevisions = dao.getAllAssessmentRevisions(),
        orientationScoringOverrides = dao.getAllScoringOverrides(), legacySubjectMappings = dao.getAllLegacyMappings(),
        orientationRelations = dao.getAllOrientationRelations(), aspectOrientationRefs = dao.getAllAspectOrientationRefs(),
        workspaces = db.workspaceDao().getAll().map { it.toWorkspaceSnapshot() }, workspaceBindings = dao.getAllWorkspaceBindings(),
        workspaceCapabilityInstances = dao.getAllWorkspaceCapabilities(), savedOrientationViews = dao.getAllSavedViews(),
        hierarchyPlacements = CanonicalHierarchyPlacementSyncStore(db).loadAll(),
        hierarchyPlacementGroupScopes = CanonicalHierarchyPlacementGroupScopeSyncStore(db).loadAll(),
        hierarchyPlacementLinkedAppearances = CanonicalHierarchyPlacementLinkedAppearanceSyncStore(db).loadAll(),
        groupScopeCoverage = GroupScopeCoverageStore(db).snapshot(), bigPicturePresentation = BigPicturePresentationRepository(db).snapshot(),
    )

    suspend fun admit(bundle: SnapshotBundle, merge: Boolean) = db.withTransaction {
        dao.storeCanonicalPayload(bundle, merge, db.workspaceDao(), validateEmbeddedWorkspaceTopology = false)
        val placements = requireNotNull(bundle.hierarchyPlacements)
        val scopes = requireNotNull(bundle.hierarchyPlacementGroupScopes)
        val coverage = requireNotNull(bundle.groupScopeCoverage)
        val presentation = requireNotNull(bundle.bigPicturePresentation)
        val store = CanonicalHierarchyPlacementSyncStore(db)
        val groups = CanonicalHierarchyPlacementGroupScopeSyncStore(db)
        if (merge) {
            store.mergeIncoming(placements)
            groups.mergeIncoming(scopes, coverage)
            BigPicturePresentationRepository(db).merge(presentation)
        } else {
            val decoded = store.decodeAndValidateForRestore(bundle, placements)
            val decodedGroups = groups.decodeAndValidateForRestore(bundle, scopes, decoded, true)
            store.restoreExactDecoded(decoded)
            GroupScopeCoverageStore(db).restoreExact(coverage)
            groups.restoreExactDecoded(decodedGroups, true)
            BigPicturePresentationRepository(db).restoreExact(presentation)
        }
    }

    fun rows(): Map<String, List<List<String?>>> {
        val sqlite = db.openHelper.readableDatabase
        val tables = sqlite.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }
        return tables.associateWith { table -> sqlite.query("SELECT * FROM `$table` ORDER BY rowid").use { c ->
            buildList { while (c.moveToNext()) add((0 until c.columnCount).map { c.getString(it) }) }
        } }
    }
    override fun close() { db.close() }
    private fun database() = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries().apply { queryCallback?.let { setQueryCallback(it, java.util.concurrent.Executor { task -> task.run() }) } }.build()
}
