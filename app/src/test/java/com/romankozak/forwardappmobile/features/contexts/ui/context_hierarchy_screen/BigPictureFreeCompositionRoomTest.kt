package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.hierarchy.*
import com.romankozak.forwardappmobile.data.orientation.CanonicalAspectRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.*
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BigPictureFreeCompositionRoomTest {
    @Test fun `mixed nesting reading moving and removing occurrences never authors semantic relations`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val subjects = db.orientationDao()
            val o = CanonicalOrientationRepository(db, subjects)
                .createOrientation(OrientationKind.INTENT, "Purpose", now = 1)
            val a = CanonicalAspectRepository(db, subjects, HierarchyPlacementLifecycleCoordinator(db))
                .create("Health", now = 2)
            db.workspaceDao().upsert(listOf(WorkspaceEntity(
                id = "w", nameOverride = "Workspace", descriptionOverride = null, roleCode = null,
                createdAt = 1, updatedAt = 1, syncedAt = null, isDeleted = false, version = 1,
                provenance = WorkspaceProvenance.CANONICAL_ONLY.name, sourceContextId = null,
            )))
            val identitiesBefore = subjects.getAllManagedSubjects()
            val workspacesBefore = db.workspaceDao().getAll()
            val placements = CanonicalHierarchyPlacementRepository(db)
            fun target(id: String, workspace: Boolean = false) = HierarchyTargetRef(
                if (workspace) HierarchyTargetType.WORKSPACE else HierarchyTargetType.MANAGED_SUBJECT, id,
            )
            val orientation = placements.createPrimaryAppearance(target(o), now = 10)
            val aspect = placements.createPrimaryAppearance(target(a), parentPlacementId = orientation, now = 11)
            val workspace = placements.createPrimaryAppearance(target("w", true), parentPlacementId = aspect, now = 12)
            val link = placements.createLinkAppearance(target(o), parentPlacementId = workspace, now = 13)

            val structure = BigPictureStructureRepository(db).observe().first()
            val catalog = CanonicalEntityCatalogRepository(db.canonicalEntityCatalogDao()).observeCatalog().first()
            val nodes = projectBigPicture(structure.placements, catalog)
            assertThat(nodes.map { it.placement.id }).containsExactly(orientation, aspect, workspace, link).inOrder()
            assertThat(bigPictureRootOccurrences(nodes, bigPictureScopes(structure, nodes, catalog))
                .map { it.placement.id }).containsExactly(orientation)
            assertThat(nodes.first().entity?.ref).isEqualTo(nodes.last().entity?.ref)

            placements.movePlacement(link, newParentPlacementId = aspect, now = 20)
            placements.reorderSiblings(aspect, listOf(link, workspace), now = 21)
            placements.removePlacement(link, now = 22)

            assertThat(subjects.getAllManagedSubjects()).containsExactlyElementsIn(identitiesBefore)
            assertThat(db.workspaceDao().getAll()).containsExactlyElementsIn(workspacesBefore)
            assertThat(subjects.getAllOrientationRelations()).isEmpty()
            assertThat(subjects.getAllAspectOrientationRefs()).isEmpty()
            assertThat(subjects.getAllWorkspaceBindings()).isEmpty()
            assertThat(db.planningDao().getAllScopes()).isEmpty()
            assertThat(db.planningDao().getAllCommitments()).isEmpty()
            assertThat(db.contextDao().getAllRaw()).isEmpty()
            assertThat(db.hierarchyPlacementGroupScopeDao().getAll()).isEmpty()
        } finally {
            db.close()
        }
    }
}
