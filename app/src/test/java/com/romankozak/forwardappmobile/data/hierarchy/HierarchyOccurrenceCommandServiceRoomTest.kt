package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HierarchyOccurrenceCommandServiceRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `same target sibling occurrences reorder independently by PlacementId`() = runBlocking {
        val db = database()
        try {
            listOf("same", "other").forEach { subject(db, it) }
            val repository = CanonicalHierarchyPlacementRepository(db)
            val service = HierarchyOccurrenceCommandService(repository)

            val primary =
                repository.createPrimaryAppearance(
                    target = target("same"),
                    now = 10L,
                )
            val link =
                repository.createLinkAppearance(
                    target = target("same"),
                    now = 11L,
                )
            val other =
                repository.createPrimaryAppearance(
                    target = target("other"),
                    now = 12L,
                )

            service.reorderSiblings(
                command =
                    HierarchyOccurrenceCommand.ReorderSiblings(
                        parentPlacementId = null,
                        orderedPlacementIds = listOf(link, other, primary),
                    ),
                now = 20L,
            )

            val reordered = repository.getLiveChildren(parentPlacementId = null)

            assertEquals(
                listOf(link, other, primary),
                reordered.map { it.id },
            )
            assertEquals(
                listOf(0L, 1L, 2L),
                reordered.map { it.siblingOrder },
            )
            assertEquals(
                listOf("same", "other", "same"),
                reordered.map { it.target.id },
            )
            assertEquals(
                listOf(
                    PlacementKind.LINK,
                    PlacementKind.PRIMARY,
                    PlacementKind.PRIMARY,
                ),
                reordered.map { it.placementKind },
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun `incomplete or duplicate sibling occurrence set is rejected without mutation`() = runBlocking {
        val db = database()
        try {
            listOf("a", "b", "c").forEach { subject(db, it) }
            val repository = CanonicalHierarchyPlacementRepository(db)
            val service = HierarchyOccurrenceCommandService(repository)

            val a = repository.createPrimaryAppearance(target("a"), now = 10L)
            val b = repository.createPrimaryAppearance(target("b"), now = 11L)
            val c = repository.createPrimaryAppearance(target("c"), now = 12L)
            val before = repository.getLiveChildren(null).map { it.id }

            val incompleteFailure =
                runCatching {
                    service.reorderSiblings(
                        HierarchyOccurrenceCommand.ReorderSiblings(
                            parentPlacementId = null,
                            orderedPlacementIds = listOf(c, a),
                        ),
                        now = 20L,
                    )
                }.exceptionOrNull()

            assertTrue(incompleteFailure is HierarchySiblingSetMismatchException)
            assertEquals(before, repository.getLiveChildren(null).map { it.id })

            val duplicateFailure =
                runCatching {
                    service.reorderSiblings(
                        HierarchyOccurrenceCommand.ReorderSiblings(
                            parentPlacementId = null,
                            orderedPlacementIds = listOf(a, a, b),
                        ),
                        now = 21L,
                    )
                }.exceptionOrNull()

            assertTrue(duplicateFailure is HierarchySiblingSetMismatchException)
            assertEquals(before, repository.getLiveChildren(null).map { it.id })
        } finally {
            db.close()
        }
    }

    @Test
    fun `LINK owned child is addressed by parent occurrence and can move independently`() = runBlocking {
        val db = database()
        try {
            listOf("owner", "child", "destination").forEach { subject(db, it) }
            val repository = CanonicalHierarchyPlacementRepository(db)
            val service = HierarchyOccurrenceCommandService(repository)

            repository.createPrimaryAppearance(target("owner"), now = 10L)
            val ownerLink =
                repository.createLinkAppearance(
                    target = target("owner"),
                    now = 11L,
                )
            val child =
                repository.createPrimaryAppearance(
                    target = target("child"),
                    parentPlacementId = ownerLink,
                    now = 12L,
                )
            val destination =
                repository.createPrimaryAppearance(
                    target = target("destination"),
                    now = 13L,
                )

            assertEquals(
                ownerLink,
                service.occurrence(child)?.parentPlacementId,
            )

            service.move(
                HierarchyOccurrenceCommand.Move(
                    placementId = child,
                    newParentPlacementId = destination,
                ),
                now = 20L,
            )

            assertEquals(
                destination,
                service.occurrence(child)?.parentPlacementId,
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun `create remove and restore commands operate on concrete occurrence identity`() = runBlocking {
        val db = database()
        try {
            subject(db, "target")
            val repository = CanonicalHierarchyPlacementRepository(db)
            val service = HierarchyOccurrenceCommandService(repository)

            val placementId =
                service.createAppearance(
                    HierarchyOccurrenceCommand.CreateAppearance(
                        target = target("target"),
                        parentPlacementId = null,
                        placementKind = PlacementKind.LINK,
                    ),
                    now = 10L,
                )

            assertEquals(
                PlacementKind.LINK,
                service.occurrence(placementId)?.placementKind,
            )

            service.removeOccurrence(
                HierarchyOccurrenceCommand.RemoveOccurrence(placementId),
                now = 11L,
            )
            assertEquals(null, service.occurrence(placementId))

            service.restoreOccurrence(
                HierarchyOccurrenceCommand.RestoreOccurrence(placementId),
                now = 12L,
            )
            assertEquals(
                placementId,
                service.occurrence(placementId)?.placementId,
            )
        } finally {
            db.close()
        }
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java,
        ).allowMainThreadQueries()
            .build()

    private suspend fun subject(
        db: AppDatabase,
        id: String,
    ) {
        db.orientationDao().upsertManagedSubjects(
            listOf(
                ManagedSubjectEntity(
                    id = id,
                    subjectType = ManagedSubjectType.ASPECT.name,
                    title = id,
                    description = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
    }

    private fun target(id: String) =
        HierarchyTargetRef(
            type = HierarchyTargetType.MANAGED_SUBJECT,
            id = id,
        )
}
