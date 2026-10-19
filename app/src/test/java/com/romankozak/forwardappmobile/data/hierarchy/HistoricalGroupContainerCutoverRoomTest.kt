package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.*
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.*
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.*
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.*
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.BigPictureUiState
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoricalGroupContainerCutoverRoomTest {
    private fun database() = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java,
    ).allowMainThreadQueries().build()

    private val groupId = historicalGroupContainerId("g")
    private val destination get() = H1StructuralParent.Node(H1TypedNode.Container(groupId))

    @Test fun `materialization preserves exact appearances kinds descendants and provenance idempotently`() = runBlocking {
        val db = database()
        try {
            seed(db)
            val relations = db.orientationDao().getAllOrientationRelations()
            HistoricalGroupContainerCutover(db).materialize()
            val once = BigPicturePresentationRepository(db).snapshot()
            assertThat(once.containers.single().id).isEqualTo(groupId)
            assertThat(once.containers.single().title).isEqualTo("Historical group")
            assertThat(once.memberships.map { it.placementId }).containsExactly("a", "link")
            assertThat(db.hierarchyPlacementDao().getById("link")!!.placementKind).isEqualTo("LINK")
            assertThat(db.hierarchyPlacementDao().getById("child")!!.parentPlacementId).isEqualTo("a")
            assertThat(db.hierarchyPlacementGroupScopeDao().getAll().all { it.isDeleted }).isTrue()
            assertThat(GroupScopeCoverageStore(db).snapshot().records.filter { it.placementId != "child" }.map { it.mode })
                .containsExactly("HISTORICAL", "HISTORICAL")
            assertThat(db.orientationDao().getAllOrientationRelations()).isEqualTo(relations)
            HistoricalGroupContainerCutover(db).materialize()
            assertThat(BigPicturePresentationRepository(db).snapshot()).isEqualTo(once)
            assertConvergence(db, groupId)
        } finally { db.close() }
    }

    @Test fun `LINK paste nested section mixed reorder and moves use ordinary container identity`() = runBlocking {
        val db = database()
        try {
            seed(db)
            HistoricalGroupContainerCutover(db).materialize()
            val writer = H1StructuralMutationCoordinator(db)
            val link = writer.addAppearance(HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, "s"), destination, id = PlacementId("extra"))
            assertThat(db.hierarchyPlacementDao().getById(link.value)!!.placementKind).isEqualTo("LINK")
            val nested = writer.createContainer("WIP", destination, id = "nested")
            val children = listOf(H1TypedNode.Container(nested), H1TypedNode.Occurrence("extra"),
                H1TypedNode.Occurrence("link"), H1TypedNode.Occurrence("a"))
            writer.reorderChildren(destination, children)
            assertThat(graph(db).childrenOf(H1TypedNode.Container(groupId)).map { it.node }).containsExactlyElementsIn(children).inOrder()
            assertConvergence(db, groupId)
            val generic = writer.createContainer("Other", H1StructuralParent.Root, id = "other")
            writer.moveNode(H1TypedNode.Occurrence("extra"), H1StructuralParent.Node(H1TypedNode.Container(generic)))
            assertConvergence(db, generic)
            writer.moveNode(H1TypedNode.Occurrence("extra"), destination)
            assertConvergence(db, groupId)
        } finally { db.close() }
    }

    @Test fun `deletion promotes children retains semantic group and tombstone prevents rematerialization`() = runBlocking {
        val db = database()
        try {
            seed(db)
            val cutover = HistoricalGroupContainerCutover(db)
            cutover.materialize()
            H1StructuralMutationCoordinator(db).deleteContainer(groupId)
            cutover.materialize()
            assertThat(BigPicturePresentationRepository(db).snapshot().containers.single().isDeleted).isTrue()
            assertThat(db.orientationDao().getManagedSubject("g")!!.isDeleted).isFalse()
            assertThat(db.hierarchyPlacementDao().getById("a")!!.isDeleted).isFalse()
            assertThat(graph(db).parentOf(H1TypedNode.Occurrence("a"))).isNull()
        } finally { db.close() }
    }

    @Test fun `NoGroup remains uncontained without synthetic parent and Workspace is an ordinary root`() = runBlocking {
        val db = database()
        try {
            seed(db)
            db.hierarchyPlacementGroupScopeDao().getAll().forEach {
                db.hierarchyPlacementGroupScopeDao().upsert(it.copy(groupSubjectId = null))
            }
            db.orientationDao().upsertOrientationRelations(db.orientationDao().getAllOrientationRelations().map { it.copy(isDeleted = true) })
            HistoricalGroupContainerCutover(db).materialize()
            assertThat(graph(db).parentOf(H1TypedNode.Occurrence("a"))).isNull()
            val read = read(db)
            assertThat(read.rootEntries().filterIsInstance<BigPictureReadEntry.Occurrence>().map { it.node.placement.id.value })
                .containsExactly("a", "link")
            assertThat(read.allScopes.map { it.title }).doesNotContain("NoBeacon")
            assertThat(read.allScopes.map { it.title }).doesNotContain("Без групи")
        } finally { db.close() }
    }

    @Test fun `invalid provenance rolls back materialization without structural or sidecar changes`() = runBlocking {
        val db = database()
        try {
            seed(db)
            db.orientationDao().upsertOrientationRelations(db.orientationDao().getAllOrientationRelations().map { it.copy(isDeleted = true) })
            val before = db.hierarchyPlacementDao().getAll()
            val scopes = db.hierarchyPlacementGroupScopeDao().getAll()
            val failure = runCatching { HistoricalGroupContainerCutover(db).materialize() }
            assertThat(failure.isFailure).isTrue()
            assertThat(BigPicturePresentationRepository(db).snapshot().containers).isEmpty()
            assertThat(db.hierarchyPlacementDao().getAll()).isEqualTo(before)
            assertThat(db.hierarchyPlacementGroupScopeDao().getAll()).isEqualTo(scopes)
            assertThat(GroupScopeCoverageStore(db).snapshot().records).isEmpty()
        } finally { db.close() }
    }

    @Test fun `old and new snapshots restore or merge canonically once and stale replay preserves deletion`() = runBlocking {
        val source = database()
        try {
            seed(source)
            GroupScopeCoverageStore(source).ensureLegacy(source.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() })
            val old = bundle(source)
            HistoricalGroupContainerCutover(source).materialize()
            val current = bundle(source)
            for (payload in listOf(old, current, old.copy(groupScopeCoverage = null, bigPicturePresentation = null))) for (merge in listOf(false, true)) {
                val target = database()
                try {
                    admit(target, payload, merge)
                    assertConvergence(target, groupId)
                    assertThat(graph(target).childrenOf(H1TypedNode.Container(groupId)).map { it.node })
                        .containsExactly(H1TypedNode.Occurrence("a"), H1TypedNode.Occurrence("link")).inOrder()
                    admit(target, payload, true)
                    assertThat(BigPicturePresentationRepository(target).snapshot().containers.size).isEqualTo(1)
                    H1StructuralMutationCoordinator(target).deleteContainer(groupId)
                    admit(target, payload, true)
                    assertThat(BigPicturePresentationRepository(target).snapshot().containers.single().isDeleted).isTrue()
                } finally { target.close() }
            }
        } finally { source.close() }
    }

    @Test fun `selective old payload materializes only selected exact occurrences`() = runBlocking {
        val source = database()
        val target = database()
        try {
            seed(source)
            GroupScopeCoverageStore(source).ensureLegacy(source.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() })
            val b = bundle(source)
            val selected = setOf("a", "child")
            admit(target, b.copy(
                hierarchyPlacements = b.hierarchyPlacements!!.filter { it.id in selected },
                hierarchyPlacementGroupScopes = b.hierarchyPlacementGroupScopes!!.filter { it.placementId in selected },
                groupScopeCoverage = b.groupScopeCoverage!!.let { it.copy(records = it.records.filter { r -> r.placementId in selected }) },
            ), merge = true, selective = true)
            assertThat(target.hierarchyPlacementDao().getById("link")).isNull()
            assertThat(graph(target).childrenOf(H1TypedNode.Container(groupId)).map { it.node })
                .containsExactly(H1TypedNode.Occurrence("a"))
            assertConvergence(target, groupId)
        } finally { source.close(); target.close() }
    }

    @Test fun `late membership failure rolls back container all moves and sidecars together`() = runBlocking {
        val db = database()
        try {
            seed(db)
            val placements = db.hierarchyPlacementDao().getAll()
            val scopes = db.hierarchyPlacementGroupScopeDao().getAll()
            db.openHelper.writableDatabase.execSQL("""
                CREATE TRIGGER reject_late_member BEFORE INSERT ON big_picture_memberships
                WHEN NEW.placementId = 'link' BEGIN SELECT RAISE(ABORT, 'test late failure'); END
            """.trimIndent())
            assertThat(runCatching { HistoricalGroupContainerCutover(db).materialize() }.isFailure).isTrue()
            assertThat(db.hierarchyPlacementDao().getAll()).isEqualTo(placements)
            assertThat(db.hierarchyPlacementGroupScopeDao().getAll()).isEqualTo(scopes)
            assertThat(BigPicturePresentationRepository(db).snapshot()).isEqualTo(BigPicturePresentationSnapshot())
            assertThat(GroupScopeCoverageStore(db).snapshot().records).isEmpty()
        } finally { db.close() }
    }

    @Test fun `materialized groups retain CoreLevel provenance without restoring Big Picture wrappers`() = runBlocking {
        val db = database()
        try {
            seed(db)
            HistoricalGroupContainerCutover(db).materialize()
            val o = db.orientationDao()
            val read = CanonicalV2HierarchyReadSnapshotAssembler().assemble(
                placements = db.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() },
                admittedWorkspacePresentations = emptyList(), managedSubjects = o.getAllManagedSubjects(),
                legacySubjectMappings = o.getAllLegacyMappings(), relations = o.getAllOrientationRelations(),
                groupScopes = db.hierarchyPlacementGroupScopeDao().getAll(),
                legacyGroups = listOf(com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup("legacy-g", "Historical group")),
                presentationProvenance = CanonicalV2HierarchyPresentationProvenance(),
                applicability = db.groupScopeApplicabilityDao().all(),
                materializedHistoricalGroupSubjects = setOf("g"),
            )
            assertThat(CanonicalV2HierarchyConsumerReadiness().coreLevelOccurrences(read).map { it.placementId.value })
                .containsExactly("a", "link", "child")
            assertThat(CanonicalV2HierarchyConsumerReadiness().coreLevelOccurrences(read).map { it.groupPresentationId }.toSet())
                .containsExactly("legacy-g")
            assertConvergence(db, groupId)
        } finally { db.close() }
    }

    @Test fun `cutover changes are dirty and stale ACK cannot acknowledge subsequent container mutation`() = runBlocking {
        val db = database()
        try {
            seed(db)
            HistoricalGroupContainerCutover(db).materialize()
            val repo = BigPicturePresentationRepository(db)
            val sent = repo.dirty()
            assertThat(sent.containers).hasSize(1)
            assertThat(sent.memberships).hasSize(2)
            H1StructuralMutationCoordinator(db).deleteContainer(groupId)
            repo.markSynced(BigPicturePresentationAck(containers = sent.containers.map { BigPictureRecordVersion(it.id, it.version) },
                memberships = sent.memberships.map { BigPictureRecordVersion(it.placementId, it.version) }), 100)
            assertThat(repo.dirty().containers.single().isDeleted).isTrue()
            assertThat(repo.dirty().memberships).hasSize(2)
            assertThat(repo.changedSince(1).containers).hasSize(1)
        } finally { db.close() }
    }

    @Test fun `mixed semantic classes and Workspace may be added without semantic side effects`() = runBlocking {
        val db = database()
        try {
            seed(db)
            val o = db.orientationDao()
            val aspect = com.romankozak.forwardappmobile.data.orientation.CanonicalAspectRepository(db, o,
                HierarchyPlacementLifecycleCoordinator(db)).create("Aspect", now = 2)
            db.workspaceDao().upsert(listOf(WorkspaceEntity("w", "Workspace", null, null, 1, 1, null, false, 1,
                provenance = "CANONICAL_ONLY", sourceContextId = null)))
            val subjects = o.getAllManagedSubjects()
            val relations = o.getAllOrientationRelations()
            val refs = o.getAllAspectOrientationRefs()
            val bindings = o.getAllWorkspaceBindings()
            HistoricalGroupContainerCutover(db).materialize()
            val writer = H1StructuralMutationCoordinator(db)
            writer.addAppearance(HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, aspect), destination)
            writer.addAppearance(HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "w"), destination)
            assertConvergence(db, groupId)
            assertThat(o.getAllManagedSubjects()).isEqualTo(subjects)
            assertThat(o.getAllOrientationRelations()).isEqualTo(relations)
            assertThat(o.getAllAspectOrientationRefs()).isEqualTo(refs)
            assertThat(o.getAllWorkspaceBindings()).isEqualTo(bindings)
        } finally { db.close() }
    }

    private suspend fun read(db: AppDatabase): BigPictureReadComposition = BigPictureReadComposition(
        projectBigPicture(db.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() }, emptyList()),
        emptyList(), BigPicturePresentationRepository(db).snapshot(),
    )
    private suspend fun graph(db: AppDatabase) = read(db).structuralGraph
    private suspend fun assertConvergence(db: AppDatabase, id: String) {
        val read = read(db)
        val index = BigPictureStructuralIndex(read)
        val section = "presentation:$id"
        val visible = read.sectionEntries(section).map { it.structuralRef(index) }
        assertThat(visible).containsExactlyElementsIn(read.structuralGraph.childrenOf(H1TypedNode.Container(id)).map { it.node }).inOrder()
        assertThat(index.children(H1StructuralParent.Node(H1TypedNode.Container(id))).map { it.ref }).containsExactlyElementsIn(visible).inOrder()
        assertThat(index.sections[section]).isEqualTo(H1TypedNode.Container(id))
        assertThat(BigPictureUiState.Ready(emptyList(), null, emptyList(), activeScope = read.section(section),
            structure = index).structuralDestination()).isEqualTo(H1StructuralParent.Node(H1TypedNode.Container(id)))
        assertThat(read.rootEntries().map { it.structuralRef(index) })
            .containsExactlyElementsIn(read.structuralGraph.childrenOf(null).map { it.node }).inOrder()
    }

    private suspend fun bundle(db: AppDatabase): SnapshotBundle = SnapshotBundle(
        managedSubjects = db.orientationDao().getAllManagedSubjects(), orientations = db.orientationDao().getAllOrientations(),
        aspects = db.orientationDao().getAllAspects(), legacySubjectMappings = db.orientationDao().getAllLegacyMappings(),
        orientationRelations = db.orientationDao().getAllOrientationRelations(),
        hierarchyPlacements = CanonicalHierarchyPlacementSyncStore(db).loadAll(),
        hierarchyPlacementGroupScopes = CanonicalHierarchyPlacementGroupScopeSyncStore(db).loadAll(),
        groupScopeCoverage = GroupScopeCoverageStore(db).snapshot(),
        bigPicturePresentation = BigPicturePresentationRepository(db).snapshot(),
    )

    private suspend fun admit(db: AppDatabase, b: SnapshotBundle, merge: Boolean, selective: Boolean = false) = db.withTransaction {
        val o = db.orientationDao()
        o.upsertManagedSubjects(b.managedSubjects.orEmpty()); o.upsertOrientations(b.orientations.orEmpty())
        o.upsertAspects(b.aspects.orEmpty()); o.upsertLegacyMappings(b.legacySubjectMappings.orEmpty())
        o.upsertOrientationRelations(b.orientationRelations.orEmpty())
        val h1 = CanonicalHierarchyPlacementSyncStore(db)
        val gs = CanonicalHierarchyPlacementGroupScopeSyncStore(db)
        if (merge) {
            h1.mergeIncoming(b.hierarchyPlacements)
            if (selective) gs.mergeIncomingSelective(b.hierarchyPlacementGroupScopes,
                b.hierarchyPlacements!!.mapTo(hashSetOf()) { it.id }, b.groupScopeCoverage)
            else gs.mergeIncoming(b.hierarchyPlacementGroupScopes, b.groupScopeCoverage)
            BigPicturePresentationRepository(db).merge(b.bigPicturePresentation ?: BigPicturePresentationSnapshot())
        } else {
            val placements = h1.decodeAndValidateForRestore(b, b.hierarchyPlacements!!)
            val scopes = gs.decodeAndValidateForRestore(b, b.hierarchyPlacementGroupScopes!!, placements, true)
            h1.restoreExactDecoded(placements)
            GroupScopeCoverageStore(db).restoreExact(GroupScopeCoverageStore(db).decodeForRestore(b, placements, scopes))
            gs.restoreExactDecoded(scopes, true)
            BigPicturePresentationRepository(db).restoreExact(b.bigPicturePresentation ?: BigPicturePresentationSnapshot())
        }
        HistoricalGroupContainerCutover(db).materialize()
        GroupScopeCoverageStore(db).validateCurrent()
    }

    private suspend fun seed(db: AppDatabase) {
        val o = db.orientationDao()
        o.upsertManagedSubjects(listOf("g", "s", "child-subject").map {
            ManagedSubjectEntity(it, "ORIENTATION", if (it == "g") "Historical group" else it, null, 1, 1, null, false, 1)
        })
        o.upsertOrientations(listOf("g", "s", "child-subject").map {
            OrientationEntity(it, if (it == "g") "MAIN_BEACON_GROUP" else "MAIN_BEACON", null, "UNSET")
        })
        o.upsertLegacyMappings(listOf(LegacySubjectMappingEntity("mapping", "MAIN_BEACON_GROUP", "legacy-g", "g", 1, "CUT_OVER", 1, 1, null, false, 1)))
        o.upsertOrientationRelations(listOf(OrientationRelationEntity("part", "s", "g", "PART_OF", 0, 1, 1, null, false, 1)))
        db.hierarchyPlacementDao().upsertAll(listOf(
            HierarchyPlacementEntity("a", "GENERAL", "MANAGED_SUBJECT", "s", null, "PRIMARY", 0, 1, 1, null, false, 1),
            HierarchyPlacementEntity("link", "GENERAL", "MANAGED_SUBJECT", "s", null, "LINK", 1, 1, 1, null, false, 1),
            HierarchyPlacementEntity("child", "GENERAL", "MANAGED_SUBJECT", "child-subject", "a", "PRIMARY", 0, 1, 1, null, false, 1),
        ))
        db.hierarchyPlacementGroupScopeDao().upsertAll(listOf("a", "link").map {
            HierarchyPlacementGroupScopeEntity(it, "GENERAL", "g", 1, 1, null, false, 1)
        })
    }
}
