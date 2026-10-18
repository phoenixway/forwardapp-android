package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.data.hierarchy.container.BigPicturePresentationRepository
import com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralMutationCoordinator
import com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralParent
import com.romankozak.forwardappmobile.data.hierarchy.container.H1TypedNode
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
            val service = HierarchyOccurrenceCommandService(repository, H1StructuralMutationCoordinator(db))

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
            val service = HierarchyOccurrenceCommandService(repository, H1StructuralMutationCoordinator(db))

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
            val service = HierarchyOccurrenceCommandService(repository, H1StructuralMutationCoordinator(db))

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
            val service = HierarchyOccurrenceCommandService(repository, H1StructuralMutationCoordinator(db))

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

    @Test
    fun `mixed typed siblings reject legacy reorder while typed reorder remains valid`() = runBlocking {
        val db = database()
        try {
            listOf("mixed-a", "mixed-b").forEach { subject(db, it) }
            val repository = CanonicalHierarchyPlacementRepository(db)
            val coordinator = H1StructuralMutationCoordinator(db)
            val service = HierarchyOccurrenceCommandService(repository, coordinator)

            val a = repository.createPrimaryAppearance(target("mixed-a"), now = 10L)
            val b = repository.createPrimaryAppearance(target("mixed-b"), now = 11L)
            val container = coordinator.createContainer(
                title = "Mixed sibling",
                destination = H1StructuralParent.Root,
                now = 20L,
            )

            val failure = runCatching {
                service.reorderSiblings(
                    HierarchyOccurrenceCommand.ReorderSiblings(
                        parentPlacementId = null,
                        orderedPlacementIds = listOf(b, a),
                    ),
                    now = 21L,
                )
            }.exceptionOrNull()

            assertTrue(failure is HierarchySiblingSetMismatchException)
            assertEquals(listOf(a, b), repository.getLiveChildren(null).map { it.id })

            coordinator.reorderChildren(
                parent = H1StructuralParent.Root,
                orderedChildren = listOf(
                    H1TypedNode.Container(container),
                    H1TypedNode.Occurrence(b.value),
                    H1TypedNode.Occurrence(a.value),
                ),
                now = 22L,
            )

            assertEquals(
                listOf(b, a),
                repository.getLiveChildren(null).map { it.id },
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun `legacy relative move positions preserve exact occurrence identity`() = runBlocking {
        val db = database()
        try {
            listOf("relative-a", "relative-b", "relative-c").forEach { subject(db, it) }
            val repository = CanonicalHierarchyPlacementRepository(db)
            val service = HierarchyOccurrenceCommandService(
                repository,
                H1StructuralMutationCoordinator(db),
            )

            val a = repository.createPrimaryAppearance(target("relative-a"), now = 10L)
            val b = repository.createPrimaryAppearance(target("relative-b"), now = 11L)
            val c = repository.createPrimaryAppearance(target("relative-c"), now = 12L)

            service.move(
                HierarchyOccurrenceCommand.Move(
                    placementId = a,
                    newParentPlacementId = null,
                    position = HierarchyPlacementPosition.AFTER(c),
                ),
                now = 20L,
            )
            assertEquals(listOf(b, c, a), repository.getLiveChildren(null).map { it.id })

            service.move(
                HierarchyOccurrenceCommand.Move(
                    placementId = a,
                    newParentPlacementId = null,
                    position = HierarchyPlacementPosition.BEFORE(b),
                ),
                now = 21L,
            )
            assertEquals(listOf(a, b, c), repository.getLiveChildren(null).map { it.id })
            assertEquals(a, service.occurrence(a)?.placementId)
        } finally {
            db.close()
        }
    }


    @Test
    fun `new occurrence inside generic container is materialized atomically`() = runBlocking {
        val db = database()
        try {
            subject(db, "created-in-container")
            val repository = CanonicalHierarchyPlacementRepository(db)
            val coordinator = H1StructuralMutationCoordinator(db)

            val containerId = coordinator.createContainer(
                title = "Typed creation destination",
                destination = H1StructuralParent.Root,
                now = 10L,
            )

            val occurrenceId = coordinator.createOccurrence(
                target = target("created-in-container"),
                placementKind = PlacementKind.PRIMARY,
                destination = H1StructuralParent.Node(
                    H1TypedNode.Container(containerId),
                ),
                now = 20L,
            )

            val occurrence = requireNotNull(repository.getPlacement(occurrenceId))
            assertEquals(occurrenceId, occurrence.id)
            assertEquals(null, occurrence.parentPlacementId)
            assertEquals(PlacementKind.PRIMARY, occurrence.placementKind)
            assertEquals(1L, occurrence.version)

            val presentation = BigPicturePresentationRepository(db).snapshot()
            val membership = presentation.memberships.single {
                it.placementId == occurrenceId.value
            }

            assertEquals(containerId, membership.containerId)
            assertEquals(false, membership.isDeleted)
            assertEquals(1L, membership.version)

            val rejection = runCatching {
                coordinator.createOccurrence(
                    target = target("created-in-container"),
                    placementKind = PlacementKind.LINK,
                    destination = H1StructuralParent.Root,
                    id = occurrenceId,
                    now = 30L,
                )
            }.exceptionOrNull()

            assertTrue(rejection is IllegalArgumentException)
            assertEquals(
                occurrence,
                repository.getPlacement(occurrenceId),
            )
            assertEquals(
                presentation,
                BigPicturePresentationRepository(db).snapshot(),
            )
        } finally {
            db.close()
        }
    }


    @Test
    fun `legacy create positions retain exact H1 occurrence ordering`() =
        runBlocking {
            val db = database()
            try {
                val names = listOf(
                    "cc-create-a",
                    "cc-create-b",
                    "cc-create-between",
                    "cc-create-after",
                    "cc-create-first",
                    "cc-create-rejected",
                )
                names.forEach { subject(db, it) }

                val repository = CanonicalHierarchyPlacementRepository(db)
                val coordinator = H1StructuralMutationCoordinator(db)
                val service = HierarchyOccurrenceCommandService(
                    repository,
                    coordinator,
                )

                suspend fun create(
                    name: String,
                    position: HierarchyPlacementPosition =
                        HierarchyPlacementPosition.LAST,
                ) = service.createAppearance(
                    HierarchyOccurrenceCommand.CreateAppearance(
                        target = target(name),
                        parentPlacementId = null,
                        placementKind = PlacementKind.LINK,
                        position = position,
                    ),
                    now = 20L,
                )

                val a = create("cc-create-a")
                val b = create("cc-create-b")
                val between = create(
                    "cc-create-between",
                    HierarchyPlacementPosition.BEFORE(b),
                )
                val after = create(
                    "cc-create-after",
                    HierarchyPlacementPosition.AFTER(a),
                )
                val first = create(
                    "cc-create-first",
                    HierarchyPlacementPosition.FIRST,
                )

                assertEquals(
                    listOf(first, a, after, between, b),
                    repository.getLiveChildren(null).map { it.id },
                )

                for (id in listOf(first, a, after, between, b)) {
                    assertEquals(id, service.occurrence(id)?.placementId)
                }

                coordinator.createContainer(
                    title = "Typed barrier",
                    destination = H1StructuralParent.Root,
                    now = 30L,
                    id = "cc2c-create-barrier",
                )

                val before =
                    repository.getLiveChildren(null).map { it.id }

                val failure = runCatching {
                    create("cc-create-rejected")
                }.exceptionOrNull()

                assertTrue(
                    failure is HierarchySiblingSetMismatchException,
                )
                assertEquals(
                    before,
                    repository.getLiveChildren(null).map { it.id },
                )
            } finally {
                db.close()
            }
        }

    @Test
    fun `facade single remove rejects typed descendants and subtree retires them`() =
        runBlocking {
            val db = database()
            try {
                listOf(
                    "cc-subtree-root",
                    "cc-subtree-child",
                    "cc-subtree-outside",
                ).forEach { subject(db, it) }

                val repository = CanonicalHierarchyPlacementRepository(db)
                val coordinator = H1StructuralMutationCoordinator(db)
                val service = HierarchyOccurrenceCommandService(
                    repository,
                    coordinator,
                )

                suspend fun create(
                    name: String,
                    parent: PlacementId? = null,
                ) = service.createAppearance(
                    HierarchyOccurrenceCommand.CreateAppearance(
                        target = target(name),
                        parentPlacementId = parent,
                        placementKind = PlacementKind.LINK,
                    ),
                    now = 10L,
                )

                val root = create("cc-subtree-root")
                val child = create("cc-subtree-child", root)
                val outside = create("cc-subtree-outside")

                coordinator.createContainer(
                    title = "Nested typed child",
                    destination = H1StructuralParent.Node(
                        H1TypedNode.Occurrence(root.value),
                    ),
                    now = 20L,
                    id = "cc2c-subtree-container",
                )
                coordinator.moveNode(
                    node = H1TypedNode.Occurrence(child.value),
                    destination = H1StructuralParent.Node(
                        H1TypedNode.Container("cc2c-subtree-container"),
                    ),
                    now = 21L,
                )

                val failure = runCatching {
                    service.removeOccurrence(
                        HierarchyOccurrenceCommand.RemoveOccurrence(root),
                        now = 30L,
                    )
                }.exceptionOrNull()

                assertTrue(
                    failure is HierarchyChildPolicyRejectedException,
                )
                assertEquals(root, service.occurrence(root)?.placementId)

                val retired = service.removeOccurrenceSubtree(
                    HierarchyOccurrenceCommand.RemoveOccurrenceSubtree(root),
                    now = 40L,
                )

                assertEquals(listOf(root, child), retired)
                assertEquals(null, service.occurrence(root))
                assertEquals(null, service.occurrence(child))
                assertEquals(outside, service.occurrence(outside)?.placementId)

                val presentation =
                    BigPicturePresentationRepository(db).snapshot()
                assertTrue(
                    presentation.containers.single {
                        it.id == "cc2c-subtree-container"
                    }.isDeleted,
                )
                assertTrue(
                    presentation.memberships.single {
                        it.placementId == child.value
                    }.isDeleted,
                )
            } finally {
                db.close()
            }
        }


    @Test
    fun `legacy batch creation commits all or none`() = runBlocking {
        val db = database()
        try {
            listOf(
                "cc-batch-root",
                "cc-batch-a",
                "cc-batch-b",
                "cc-batch-late",
            ).forEach { subject(db, it) }

            val repository = CanonicalHierarchyPlacementRepository(db)
            val service = HierarchyOccurrenceCommandService(
                repository,
                H1StructuralMutationCoordinator(db),
            )

            val root = service.createAppearance(
                HierarchyOccurrenceCommand.CreateAppearance(
                    target = target("cc-batch-root"),
                    parentPlacementId = null,
                    placementKind = PlacementKind.PRIMARY,
                ),
                now = 10L,
            )

            fun command(
                name: String,
                parent: PlacementId?,
            ) = HierarchyOccurrenceCommand.CreateAppearance(
                target = target(name),
                parentPlacementId = parent,
                placementKind = PlacementKind.LINK,
            )

            val created = service.createAppearances(
                listOf(
                    command("cc-batch-a", root),
                    command("cc-batch-b", root),
                ),
                now = 20L,
            )

            assertEquals(2, created.size)
            assertEquals(
                created,
                repository.getLiveChildren(root).map { it.id },
            )

            val beforePlacements =
                db.hierarchyPlacementDao().getAll()
            val beforePresentation =
                BigPicturePresentationRepository(db).snapshot()

            val failure = runCatching {
                service.createAppearances(
                    listOf(
                        command("cc-batch-late", root),
                        command(
                            "cc-batch-b",
                            PlacementId("missing-batch-parent"),
                        ),
                    ),
                    now = 30L,
                )
            }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertEquals(
                beforePlacements,
                db.hierarchyPlacementDao().getAll(),
            )
            assertEquals(
                beforePresentation,
                BigPicturePresentationRepository(db).snapshot(),
            )
            assertEquals(
                created,
                repository.getLiveChildren(root).map { it.id },
            )
        } finally {
            db.close()
        }
    }


    @Test
    fun `H1 batch retirement rolls back earlier occurrences on late rejection`() =
        runBlocking {
            val db = database()
            try {
                listOf(
                    "retirement-root",
                    "retirement-leaf",
                    "retirement-parent",
                    "retirement-child",
                ).forEach { subject(db, it) }

                val repository = CanonicalHierarchyPlacementRepository(db)
                val service = HierarchyOccurrenceCommandService(
                    repository,
                    H1StructuralMutationCoordinator(db),
                )

                fun create(
                    id: String,
                    parent: PlacementId?,
                    kind: PlacementKind,
                ) = HierarchyOccurrenceCommand.CreateAppearance(
                    target = target(id),
                    parentPlacementId = parent,
                    placementKind = kind,
                )

                val root = service.createAppearance(
                    create("retirement-root", null, PlacementKind.PRIMARY),
                    now = 10L,
                )
                val leaf = service.createAppearance(
                    create("retirement-leaf", root, PlacementKind.LINK),
                    now = 11L,
                )
                val parent = service.createAppearance(
                    create("retirement-parent", root, PlacementKind.LINK),
                    now = 12L,
                )
                val child = service.createAppearance(
                    create("retirement-child", parent, PlacementKind.LINK),
                    now = 13L,
                )

                val placementsBefore = db.hierarchyPlacementDao().getAll()
                val presentationBefore =
                    BigPicturePresentationRepository(db).snapshot()

                val failure = runCatching {
                    H1StructuralMutationCoordinator(db).removeOccurrences(
                        listOf(leaf, parent),
                        now = 20L,
                    )
                }.exceptionOrNull()

                assertTrue(failure is HierarchyChildPolicyRejectedException)
                assertEquals(
                    placementsBefore,
                    db.hierarchyPlacementDao().getAll(),
                )
                assertEquals(
                    presentationBefore,
                    BigPicturePresentationRepository(db).snapshot(),
                )
                assertEquals(
                    listOf(leaf, parent),
                    repository.getLiveChildren(root).map { it.id },
                )
                assertEquals(
                    listOf(child),
                    repository.getLiveChildren(parent).map { it.id },
                )
            } finally {
                db.close()
            }
        }

}
