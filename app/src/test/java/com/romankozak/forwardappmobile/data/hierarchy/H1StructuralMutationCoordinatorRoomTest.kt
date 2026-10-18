package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Production H1 typed structural command acceptance on schema 199.
 *
 * These tests exercise H1StructuralMutationCoordinator, not the earlier
 * test-only physical promotion algorithm.
 */
@RunWith(RobolectricTestRunner::class)
class H1StructuralMutationCoordinatorRoomTest {
    private fun database() =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

    private data class Fixture(
        val a: PlacementId,
        val d: PlacementId,
        val b: PlacementId,
        val c: PlacementId,
        val deep: PlacementId,
    )

    private fun o(id: PlacementId) = H1TypedNode.Occurrence(id.value)
    private fun c(id: String) = H1TypedNode.Container(id)
    private fun under(node: H1TypedNode) = H1StructuralParent.Node(node)

    private suspend fun graph(db: AppDatabase) =
        projectH1UnifiedStructuralGraph(
            db.hierarchyPlacementDao().getAll().map {
                it.toHierarchyPlacementStrict()
            },
            BigPicturePresentationRepository(db).snapshot(),
        )

    private suspend fun fixture(db: AppDatabase): Fixture {
        val names = listOf("a", "d", "b", "c", "deep")
        db.workspaceDao().upsert(names.map { name ->
            WorkspaceEntity(
                name, name, null, null, 1, 1, null, false, 1,
                provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                sourceContextId = null,
            )
        })

        val h1 = CanonicalHierarchyPlacementRepository(db)

        suspend fun occurrence(
            name: String,
            parent: PlacementId?,
            now: Long,
        ) = h1.createPrimaryAppearance(
            HierarchyTargetRef(HierarchyTargetType.WORKSPACE, name),
            parentPlacementId = parent,
            now = now,
        )

        val a = occurrence("a", null, 1)
        val d = occurrence("d", null, 2)
        val b = occurrence("b", a, 3)
        val c = occurrence("c", a, 4)
        val deep = occurrence("deep", b, 5)

        graph(db)
        return Fixture(a, d, b, c, deep)
    }

    @Test
    fun `occurrence moves across container occurrence and root destinations`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val writer = H1StructuralMutationCoordinator(db)

                val x = writer.createContainer(
                    "X", under(o(f.a)), now = 20, id = "x",
                )
                val y = writer.createContainer(
                    "Y", under(o(f.d)), now = 21, id = "y",
                )

                writer.moveNode(o(f.b), under(c(x)), now = 30)

                assertThat(graph(db).parentOf(o(f.b))).isEqualTo(c(x))
                assertThat(db.hierarchyPlacementDao()
                    .getById(f.b.value)!!.parentPlacementId)
                    .isEqualTo(f.a.value)

                val firstMembership = BigPicturePresentationRepository(db)
                    .snapshot().memberships.single { it.placementId == f.b.value }

                assertThat(firstMembership.containerId).isEqualTo(x)
                assertThat(firstMembership.isDeleted).isFalse()

                val beforeCrossMove =
                    db.hierarchyPlacementDao().getById(f.b.value)!!

                writer.moveNode(o(f.b), under(c(y)), now = 40)

                assertThat(graph(db).parentOf(o(f.b))).isEqualTo(c(y))
                val moved = db.hierarchyPlacementDao()
                    .getById(f.b.value)!!

                assertThat(moved.parentPlacementId).isEqualTo(f.d.value)
                assertThat(moved.version)
                    .isEqualTo(beforeCrossMove.version + 1)
                assertThat(moved.updatedAt)
                    .isGreaterThan(beforeCrossMove.updatedAt)
                assertThat(moved.syncedAt).isNull()

                val crossMembership = BigPicturePresentationRepository(db)
                    .snapshot().memberships.single { it.placementId == f.b.value }

                assertThat(crossMembership.containerId).isEqualTo(y)
                assertThat(crossMembership.version)
                    .isEqualTo(firstMembership.version + 1)
                assertThat(crossMembership.syncedAt).isNull()

                writer.moveNode(o(f.b), under(o(f.a)), now = 50)

                assertThat(graph(db).parentOf(o(f.b))).isEqualTo(o(f.a))
                assertThat(BigPicturePresentationRepository(db)
                    .snapshot().memberships.single {
                        it.placementId == f.b.value
                    }.isDeleted).isTrue()

                writer.moveNode(
                    o(f.b), H1StructuralParent.Root, now = 60,
                )

                assertThat(graph(db).parentOf(o(f.b))).isNull()
                assertThat(db.hierarchyPlacementDao()
                    .getById(f.b.value)!!.parentPlacementId).isNull()

                // Its own occurrence descendants retain their direct ancestry.
                assertThat(graph(db).parentOf(o(f.deep)))
                    .isEqualTo(o(f.b))
            } finally {
                db.close()
            }
        }

    @Test
    fun `moving a container reanchors its entire dependent subtree`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val writer = H1StructuralMutationCoordinator(db)

                writer.createContainer(
                    "X", under(o(f.a)), now = 20, id = "x",
                )
                writer.createContainer(
                    "Inner", under(c("x")), now = 21, id = "inner",
                )
                writer.moveNode(
                    o(f.b), under(c("inner")), now = 30,
                )
                writer.moveNode(
                    o(f.c), under(o(f.b)), now = 31,
                )

                writer.moveNode(
                    c("x"), under(o(f.d)), now = 50,
                )

                val result = graph(db)
                assertThat(result.parentOf(c("x"))).isEqualTo(o(f.d))
                assertThat(result.parentOf(c("inner"))).isEqualTo(c("x"))
                assertThat(result.parentOf(o(f.b))).isEqualTo(c("inner"))
                assertThat(result.parentOf(o(f.c))).isEqualTo(o(f.b))

                val presentation = BigPicturePresentationRepository(db)
                    .snapshot()
                val containers = presentation.containers.associateBy { it.id }

                assertThat(containers.getValue("x").anchorPlacementId)
                    .isEqualTo(f.d.value)
                assertThat(containers.getValue("inner").anchorPlacementId)
                    .isEqualTo(f.d.value)
                assertThat(containers.getValue("inner").parentContainerId)
                    .isEqualTo("x")

                assertThat(db.hierarchyPlacementDao()
                    .getById(f.b.value)!!.parentPlacementId)
                    .isEqualTo(f.d.value)
                assertThat(db.hierarchyPlacementDao()
                    .getById(f.c.value)!!.parentPlacementId)
                    .isEqualTo(f.b.value)
                assertThat(db.hierarchyPlacementDao()
                    .getById(f.deep.value)!!.parentPlacementId)
                    .isEqualTo(f.b.value)

                assertThat(presentation.memberships.single {
                    it.placementId == f.b.value
                }.containerId).isEqualTo("inner")
            } finally {
                db.close()
            }
        }

    @Test
    fun `complete mixed reorder normalizes both physical tables and is idempotent`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val writer = H1StructuralMutationCoordinator(db)

                writer.createContainer(
                    "X", under(o(f.a)), now = 20, id = "x",
                )
                writer.createContainer(
                    "Y", under(o(f.a)), now = 21, id = "y",
                )

                val order = listOf(
                    o(f.c), c("y"), o(f.b), c("x"),
                )

                writer.reorderChildren(under(o(f.a)), order, now = 30)

                assertThat(graph(db).childrenOf(o(f.a)).map { it.node })
                    .containsExactlyElementsIn(order).inOrder()

                assertThat(db.hierarchyPlacementDao()
                    .getById(f.c.value)!!.siblingOrder).isEqualTo(0L)
                assertThat(db.hierarchyPlacementDao()
                    .getById(f.b.value)!!.siblingOrder).isEqualTo(2L)

                val presentation = BigPicturePresentationRepository(db)
                    .snapshot()
                val containers = presentation.containers.associateBy { it.id }

                assertThat(containers.getValue("y").siblingOrder)
                    .isEqualTo(1L)
                assertThat(containers.getValue("x").siblingOrder)
                    .isEqualTo(3L)

                val beforeRepeat = db.hierarchyPlacementDao().getAll()
                val presentationBeforeRepeat = presentation

                writer.reorderChildren(under(o(f.a)), order, now = 40)

                assertThat(db.hierarchyPlacementDao().getAll())
                    .containsExactlyElementsIn(beforeRepeat)
                assertThat(BigPicturePresentationRepository(db).snapshot())
                    .isEqualTo(presentationBeforeRepeat)

                val rejected = runCatching {
                    writer.reorderChildren(
                        under(o(f.a)),
                        listOf(o(f.c), c("y")),
                        now = 50,
                    )
                }.exceptionOrNull()

                assertThat(rejected).isNotNull()
                assertThat(db.hierarchyPlacementDao().getAll())
                    .containsExactlyElementsIn(beforeRepeat)
                assertThat(BigPicturePresentationRepository(db).snapshot())
                    .isEqualTo(presentationBeforeRepeat)
            } finally {
                db.close()
            }
        }

    @Test
    fun `deleting nested container promotes all direct mixed children in place`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val writer = H1StructuralMutationCoordinator(db)

                writer.createContainer(
                    "Outer", under(o(f.a)), now = 20, id = "outer",
                )
                writer.createContainer(
                    "Target", under(c("outer")), now = 21, id = "target",
                )
                writer.createContainer(
                    "Nested", under(c("target")), now = 22, id = "nested",
                )

                writer.moveNode(o(f.b), under(c("target")), now = 30)
                writer.moveNode(o(f.c), under(c("target")), now = 31)
                writer.moveNode(o(f.deep), under(c("nested")), now = 32)
                writer.moveNode(o(f.d), under(c("outer")), now = 33)

                writer.reorderChildren(
                    under(c("target")),
                    listOf(o(f.b), c("nested"), o(f.c)),
                    now = 40,
                )

                writer.deleteContainer("target", now = 50)

                val result = graph(db)
                assertThat(result.childrenOf(c("outer")).map { it.node })
                    .containsExactly(
                        o(f.b), c("nested"), o(f.c), o(f.d),
                    ).inOrder()

                assertThat(result.parentOf(c("nested")))
                    .isEqualTo(c("outer"))
                assertThat(result.parentOf(o(f.deep)))
                    .isEqualTo(c("nested"))
                assertThat(result.edges.map { it.node })
                    .doesNotContain(c("target"))

                val presentation = BigPicturePresentationRepository(db)
                    .snapshot()

                assertThat(presentation.containers.single {
                    it.id == "target"
                }.isDeleted).isTrue()

                assertThat(presentation.containers.single {
                    it.id == "nested"
                }.parentContainerId).isEqualTo("outer")

                for (id in listOf(f.b.value, f.c.value)) {
                    val member = presentation.memberships.single {
                        it.placementId == id
                    }
                    assertThat(member.containerId).isEqualTo("outer")
                    assertThat(member.isDeleted).isFalse()
                    assertThat(member.syncedAt).isNull()
                }

                assertThat(presentation.memberships.single {
                    it.placementId == f.deep.value
                }.containerId).isEqualTo("nested")

                for (id in listOf(f.b.value, f.c.value, f.deep.value)) {
                    assertThat(db.hierarchyPlacementDao()
                        .getById(id)!!.isDeleted).isFalse()
                }
            } finally {
                db.close()
            }
        }

    @Test
    fun `deleting occurrence anchored container promotes without deleting entities`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val writer = H1StructuralMutationCoordinator(db)

                writer.createContainer(
                    "Direct", under(o(f.a)), now = 20, id = "direct",
                )
                writer.moveNode(o(f.b), under(c("direct")), now = 30)
                writer.deleteContainer("direct", now = 40)

                val result = graph(db)
                assertThat(result.parentOf(o(f.b))).isEqualTo(o(f.a))
                assertThat(result.edges.map { it.node })
                    .doesNotContain(c("direct"))

                val member = BigPicturePresentationRepository(db)
                    .snapshot().memberships.single {
                        it.placementId == f.b.value
                    }
                assertThat(member.isDeleted).isTrue()
                assertThat(member.syncedAt).isNull()

                assertThat(db.hierarchyPlacementDao()
                    .getById(f.b.value)!!.isDeleted).isFalse()
                assertThat(db.workspaceDao().getById("b")!!.isDeleted)
                    .isFalse()
            } finally {
                db.close()
            }
        }

    @Test
    fun `composite cycle and unavailable destination reject without mutation`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val writer = H1StructuralMutationCoordinator(db)

                writer.createContainer(
                    "X", under(o(f.a)), now = 20, id = "x",
                )

                val beforePlacements = db.hierarchyPlacementDao().getAll()
                val beforePresentation = BigPicturePresentationRepository(db)
                    .snapshot()

                val cycle = runCatching {
                    writer.moveNode(
                        o(f.a), under(c("x")), now = 30,
                    )
                }.exceptionOrNull()

                assertThat(cycle).isNotNull()

                val missing = runCatching {
                    writer.moveNode(
                        o(f.b), under(c("missing")), now = 31,
                    )
                }.exceptionOrNull()

                assertThat(missing).isNotNull()

                assertThat(db.hierarchyPlacementDao().getAll())
                    .containsExactlyElementsIn(beforePlacements)
                assertThat(BigPicturePresentationRepository(db).snapshot())
                    .isEqualTo(beforePresentation)

                writer.deleteContainer("x", now = 40)
                val afterDelete = db.hierarchyPlacementDao().getAll()
                val presentationAfterDelete = BigPicturePresentationRepository(db)
                    .snapshot()

                val tombstoned = runCatching {
                    writer.moveNode(
                        o(f.b), under(c("x")), now = 50,
                    )
                }.exceptionOrNull()

                assertThat(tombstoned).isNotNull()
                assertThat(db.hierarchyPlacementDao().getAll())
                    .containsExactlyElementsIn(afterDelete)
                assertThat(BigPicturePresentationRepository(db).snapshot())
                    .isEqualTo(presentationAfterDelete)
            } finally {
                db.close()
            }
        }

    @Test
    fun `failure after physical placement write rolls back all structural tables`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val writer = H1StructuralMutationCoordinator(db)

                writer.createContainer(
                    "X", under(o(f.a)), now = 20, id = "x",
                )
                writer.moveNode(o(f.b), under(c("x")), now = 30)

                val beforePlacements = db.hierarchyPlacementDao().getAll()
                val beforePresentation = BigPicturePresentationRepository(db)
                    .snapshot()
                val beforeGraph = graph(db).edges

                /*
                 * Fail only when the placement has already acquired the
                 * destination ancestry. The trigger proves the production
                 * coordinator wrote placements before the forced failure.
                 */
                val condition =
                    "(SELECT parentPlacementId FROM hierarchy_placements " +
                        "WHERE id = '${f.b.value}') = '${f.d.value}'"

                for (event in listOf("UPDATE", "INSERT")) {
                    db.openHelper.writableDatabase.execSQL(
                        """
                        CREATE TRIGGER fail_h1_${event.lowercase()}
                        BEFORE $event ON big_picture_containers
                        WHEN NEW.id = 'x' AND $condition
                        BEGIN
                            SELECT RAISE(ABORT, 'H1 injected after placement write');
                        END
                        """.trimIndent(),
                    )
                }

                val failure = runCatching {
                    writer.moveNode(
                        c("x"), under(o(f.d)), now = 50,
                    )
                }.exceptionOrNull()

                assertThat(failure).isNotNull()

                assertThat(db.hierarchyPlacementDao().getAll())
                    .containsExactlyElementsIn(beforePlacements)
                assertThat(BigPicturePresentationRepository(db).snapshot())
                    .isEqualTo(beforePresentation)
                assertThat(graph(db).edges)
                    .containsExactlyElementsIn(beforeGraph)
            } finally {
                db.close()
            }
        }

    @Test
    fun `single typed occurrence retirement preserves shared container and provenance`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val writer = H1StructuralMutationCoordinator(db)

                writer.createContainer(
                    "Shared section",
                    under(o(f.a)),
                    now = 20,
                    id = "retirement-section",
                )
                writer.moveNode(
                    o(f.c),
                    under(c("retirement-section")),
                    now = 21,
                )

                HierarchyPlacementLinkedAppearanceMutationCoordinator(db)
                    .setLinkedAppearance(f.c, now = 22)

                writer.removeOccurrence(f.c, now = 30)

                val placement = db.hierarchyPlacementDao()
                    .getById(f.c.value)!!

                assertThat(placement.isDeleted).isTrue()
                assertThat(placement.version).isGreaterThan(1L)

                val presentation = BigPicturePresentationRepository(db).snapshot()
                assertThat(presentation.containers.single {
                    it.id == "retirement-section"
                }.isDeleted).isFalse()

                assertThat(presentation.memberships.single {
                    it.placementId == f.c.value
                }.isDeleted).isTrue()

                assertThat(
                    db.hierarchyPlacementLinkedAppearanceDao()
                        .getByPlacementId(f.c.value)!!.isDeleted
                ).isTrue()

                assertThat(graph(db).edges.map { it.node })
                    .doesNotContain(o(f.c))

                assertThat(db.workspaceDao().getById("c")!!.isDeleted)
                    .isFalse()
                assertThat(db.hierarchyPlacementDao()
                    .getById(f.b.value)!!.isDeleted).isFalse()
            } finally {
                db.close()
            }
        }

    @Test
    fun `single retirement rejects live typed container child without mutation`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val writer = H1StructuralMutationCoordinator(db)

                writer.createContainer(
                    "Protected child",
                    under(o(f.c)),
                    now = 20,
                    id = "protected-child",
                )

                val beforePlacement = db.hierarchyPlacementDao()
                    .getById(f.c.value)

                val beforePresentation =
                    BigPicturePresentationRepository(db).snapshot()

                val failure = runCatching {
                    writer.removeOccurrence(f.c, now = 30)
                }.exceptionOrNull()

                assertThat(failure).isInstanceOf(
                    HierarchyChildPolicyRejectedException::class.java,
                )

                assertThat(db.hierarchyPlacementDao()
                    .getById(f.c.value)).isEqualTo(beforePlacement)

                assertThat(BigPicturePresentationRepository(db).snapshot())
                    .isEqualTo(beforePresentation)

                assertThat(graph(db).parentOf(c("protected-child")))
                    .isEqualTo(o(f.c))
            } finally {
                db.close()
            }
        }


    @Test
    fun `retirement timestamp correlates provenance and permits exact restore`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val writer = H1StructuralMutationCoordinator(db)
                val linked =
                    HierarchyPlacementLinkedAppearanceMutationCoordinator(db)

                linked.setLinkedAppearance(f.c, now = 100L)

                // Deliberately stale event time: the sidecar has a newer
                // timestamp than both H1 and the requested retirement time.
                writer.removeOccurrence(f.c, now = 10L)

                val retiredPlacement =
                    db.hierarchyPlacementDao().getById(f.c.value)!!
                val retiredLinked =
                    db.hierarchyPlacementLinkedAppearanceDao()
                        .getByPlacementId(f.c.value)!!

                assertThat(retiredPlacement.isDeleted).isTrue()
                assertThat(retiredLinked.isDeleted).isTrue()
                assertThat(retiredPlacement.updatedAt).isGreaterThan(100L)
                assertThat(retiredLinked.updatedAt)
                    .isEqualTo(retiredPlacement.updatedAt)

                CanonicalHierarchyPlacementRepository(db)
                    .restorePlacement(f.c, now = 200L)

                val restoredPlacement =
                    db.hierarchyPlacementDao().getById(f.c.value)!!
                val restoredLinked =
                    db.hierarchyPlacementLinkedAppearanceDao()
                        .getByPlacementId(f.c.value)!!

                assertThat(restoredPlacement.isDeleted).isFalse()
                assertThat(restoredLinked.isDeleted).isFalse()
                assertThat(restoredPlacement.id).isEqualTo(f.c.value)
                assertThat(db.workspaceDao().getById("c")!!.isDeleted)
                    .isFalse()
            } finally {
                db.close()
            }
        }


    @Test
    fun `typed subtree retirement includes nested containers and exact occurrences`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val writer = H1StructuralMutationCoordinator(db)

                writer.createContainer(
                    "Section",
                    under(o(f.a)),
                    now = 20,
                    id = "retired-section",
                )
                writer.createContainer(
                    "Nested",
                    under(c("retired-section")),
                    now = 21,
                    id = "retired-nested",
                )

                writer.moveNode(
                    o(f.c),
                    under(c("retired-nested")),
                    now = 22,
                )
                writer.moveNode(
                    o(f.deep),
                    under(c("retired-nested")),
                    now = 23,
                )

                HierarchyPlacementLinkedAppearanceMutationCoordinator(db)
                    .setLinkedAppearance(f.c, now = 90)

                val removed = writer.removeOccurrenceSubtree(
                    f.a,
                    now = 30,
                )

                assertThat(removed)
                    .containsExactly(f.a, f.b, f.c, f.deep)
                    .inOrder()

                val dao = db.hierarchyPlacementDao()
                for (id in removed) {
                    assertThat(dao.getById(id.value)!!.isDeleted).isTrue()
                }

                assertThat(dao.getById(f.d.value)!!.isDeleted).isFalse()

                val presentation =
                    BigPicturePresentationRepository(db).snapshot()

                for (id in listOf("retired-section", "retired-nested")) {
                    assertThat(presentation.containers.single {
                        it.id == id
                    }.isDeleted).isTrue()
                }

                for (id in listOf(f.c, f.deep)) {
                    assertThat(presentation.memberships.single {
                        it.placementId == id.value
                    }.isDeleted).isTrue()
                }

                assertThat(
                    db.hierarchyPlacementLinkedAppearanceDao()
                        .getByPlacementId(f.c.value)!!.isDeleted
                ).isTrue()

                val live = graph(db)
                assertThat(live.edges.map { it.node })
                    .containsExactly(o(f.d))

                for (name in listOf("a", "b", "c", "d", "deep")) {
                    assertThat(
                        db.workspaceDao().getById(name)!!.isDeleted
                    ).isFalse()
                }
            } finally {
                db.close()
            }
        }

    @Test
    fun `typed subtree write failure rolls back structural and provenance state`(): Unit =
        runBlocking {
            val db = database()
            try {
                val f = fixture(db)
                val writer = H1StructuralMutationCoordinator(db)

                writer.createContainer(
                    "Rollback section",
                    under(o(f.a)),
                    now = 20,
                    id = "rollback-section",
                )
                writer.moveNode(
                    o(f.c),
                    under(c("rollback-section")),
                    now = 21,
                )

                HierarchyPlacementLinkedAppearanceMutationCoordinator(db)
                    .setLinkedAppearance(f.c, now = 22)

                val beforePlacements =
                    db.hierarchyPlacementDao().getAll()
                val beforePresentation =
                    BigPicturePresentationRepository(db).snapshot()
                val beforeLinked =
                    db.hierarchyPlacementLinkedAppearanceDao().getAll()
                val beforeScopes =
                    db.hierarchyPlacementGroupScopeDao().getAll()

                db.openHelper.writableDatabase.execSQL(
                    """
                    CREATE TRIGGER cc2b2_rollback
                    BEFORE UPDATE ON big_picture_containers
                    BEGIN
                        SELECT RAISE(ABORT, 'cc2b2 forced rollback');
                    END
                    """.trimIndent(),
                )

                val failure = runCatching {
                    writer.removeOccurrenceSubtree(f.a, now = 50)
                }.exceptionOrNull()

                assertThat(failure).isNotNull()

                assertThat(db.hierarchyPlacementDao().getAll())
                    .containsExactlyElementsIn(beforePlacements)
                    .inOrder()

                assertThat(BigPicturePresentationRepository(db).snapshot())
                    .isEqualTo(beforePresentation)

                assertThat(
                    db.hierarchyPlacementLinkedAppearanceDao().getAll()
                ).containsExactlyElementsIn(beforeLinked).inOrder()

                assertThat(
                    db.hierarchyPlacementGroupScopeDao().getAll()
                ).containsExactlyElementsIn(beforeScopes).inOrder()
            } finally {
                db.close()
            }
        }

}
