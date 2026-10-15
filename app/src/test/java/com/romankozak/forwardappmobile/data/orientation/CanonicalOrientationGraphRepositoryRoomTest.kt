package com.romankozak.forwardappmobile.data.orientation

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalOrientationGraphRepositoryRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `add preserves direction distinct types and duplicate idempotence`() = runBlocking {
        withDatabase { db, graph ->
            seed(db)

            val first = graph.addRelation("a", "b", OrientationRelationType.SUPPORTS, 20L)
            val duplicate = graph.addRelation("a", "b", OrientationRelationType.SUPPORTS, 21L)
            val reverse = graph.addRelation("b", "a", OrientationRelationType.SUPPORTS, 22L)
            val differentType = graph.addRelation("a", "b", OrientationRelationType.REALIZES, 23L)

            assertEquals(first, duplicate)
            assertNotEquals(first, reverse)
            assertNotEquals(first, differentType)

            val live = db.orientationDao().getAllOrientationRelations().filterNot { it.isDeleted }
            assertEquals(3, live.size)
            val original = live.single { it.id == first }
            assertEquals("a", original.fromOrientationId)
            assertEquals("b", original.toOrientationId)
            assertEquals(OrientationRelationType.SUPPORTS.name, original.relationType)
            assertEquals(1L, original.version)
            assertEquals(20L, original.updatedAt)
            assertEquals(null, original.syncedAt)
        }
    }

    @Test
    fun `conflict relations canonicalize unordered endpoints`() = runBlocking {
        withDatabase { db, graph ->
            seed(db)

            val first = graph.addRelation("b", "a", OrientationRelationType.CONFLICTS_WITH, 20L)
            val duplicate = graph.addRelation("a", "b", OrientationRelationType.CONFLICTS_WITH, 21L)

            assertEquals(first, duplicate)
            val relation = db.orientationDao().getAllOrientationRelations().single()
            assertEquals("a", relation.fromOrientationId)
            assertEquals("b", relation.toOrientationId)
            assertEquals(1L, relation.version)
        }
    }

    @Test
    fun `self link and product PART_OF mutation are rejected without writes`() = runBlocking {
        withDatabase { db, graph ->
            seed(db)
            val before = db.orientationDao().getAllOrientationRelations()

            assertRejected {
                graph.addRelation("a", "a", OrientationRelationType.SUPPORTS)
            }
            assertRejected {
                graph.addRelation("a", "b", OrientationRelationType.PART_OF)
            }

            assertEquals(before, db.orientationDao().getAllOrientationRelations())
        }
    }

    @Test
    fun `acyclic relation types reject cycles atomically`() = runBlocking {
        val acyclic = listOf(
            OrientationRelationType.DEPENDS_ON,
            OrientationRelationType.PRECEDES,
            OrientationRelationType.REFINES,
            OrientationRelationType.DERIVED_FROM,
        )

        withDatabase { db, graph ->
            seed(db)

            for (type in acyclic) {
                graph.addRelation("a", "b", type, 20L)
                graph.addRelation("b", "c", type, 21L)
                val before = db.orientationDao().getAllOrientationRelations()

                assertRejected {
                    graph.addRelation("c", "a", type, 22L)
                }

                assertEquals(before, db.orientationDao().getAllOrientationRelations())
            }
        }
    }

    @Test
    fun `cycles remain permitted for SUPPORTS and REALIZES`() = runBlocking {
        withDatabase { db, graph ->
            seed(db)

            for (type in listOf(
                OrientationRelationType.SUPPORTS,
                OrientationRelationType.REALIZES,
            )) {
                graph.addRelation("a", "b", type, 20L)
                graph.addRelation("b", "a", type, 21L)
            }

            assertEquals(4, db.orientationDao().getAllOrientationRelations().count { !it.isDeleted })
        }
    }

    @Test
    fun `remove tombstones edge and resurrection retains identity and increments version`() = runBlocking {
        withDatabase { db, graph ->
            seed(db)
            val dao = db.orientationDao()

            val id = graph.addRelation("a", "b", OrientationRelationType.SUPPORTS, 20L)
            assertEquals(1, graph.observeRelations("a").first().size)

            graph.removeRelation(id, expectedVersion = 1L, now = 30L)
            val deleted = dao.getAllOrientationRelations().single()

            assertTrue(deleted.isDeleted)
            assertEquals(id, deleted.id)
            assertEquals(2L, deleted.version)
            assertEquals(20L, deleted.createdAt)
            assertEquals(30L, deleted.updatedAt)
            assertEquals(null, deleted.syncedAt)
            assertTrue(graph.observeRelations("a").first().isEmpty())

            val resurrectedId =
                graph.addRelation("a", "b", OrientationRelationType.SUPPORTS, 40L)
            val resurrected = dao.getAllOrientationRelations().single()

            assertEquals(id, resurrectedId)
            assertFalse(resurrected.isDeleted)
            assertEquals(3L, resurrected.version)
            assertEquals(20L, resurrected.createdAt)
            assertEquals(40L, resurrected.updatedAt)
            assertEquals(null, resurrected.syncedAt)
        }
    }

    @Test
    fun `stale version and repeated removal fail without changing persisted state`() = runBlocking {
        withDatabase { db, graph ->
            seed(db)
            val dao = db.orientationDao()
            val id = graph.addRelation("a", "b", OrientationRelationType.SUPPORTS, 20L)
            val before = dao.getAllOrientationRelations()

            assertRejected {
                graph.removeRelation(id, expectedVersion = 0L, now = 30L)
            }
            assertEquals(before, dao.getAllOrientationRelations())

            graph.removeRelation(id, expectedVersion = 1L, now = 31L)
            val tombstone = dao.getAllOrientationRelations()
            assertRejected {
                graph.removeRelation(id, expectedVersion = 1L, now = 32L)
            }
            assertEquals(tombstone, dao.getAllOrientationRelations())
        }
    }

    @Test
    fun `missing deleted wrong-type and node-less endpoints fail closed`() = runBlocking {
        withDatabase { db, graph ->
            seed(db)
            val dao = db.orientationDao()
            dao.upsertManagedSubjects(
                listOf(
                    subject("deleted", deleted = true),
                    subject("wrong-type", type = ManagedSubjectType.ASPECT),
                    subject("no-node"),
                ),
            )
            dao.upsertOrientations(listOf(orientation("deleted")))
            val before = dao.getAllOrientationRelations()

            for (invalidId in listOf("missing", "deleted", "wrong-type", "no-node")) {
                assertRejected {
                    graph.addRelation("a", invalidId, OrientationRelationType.SUPPORTS)
                }
                assertRejected {
                    graph.addRelation(invalidId, "a", OrientationRelationType.SUPPORTS)
                }
            }

            assertEquals(before, dao.getAllOrientationRelations())
        }
    }

    @Test
    fun `removal refuses deleted endpoints without modifying tombstone or identity`() = runBlocking {
        withDatabase { db, graph ->
            seed(db)
            val dao = db.orientationDao()
            val id = graph.addRelation("a", "b", OrientationRelationType.SUPPORTS, 20L)
            val subject = requireNotNull(dao.getManagedSubject("b"))
            dao.upsertManagedSubjects(listOf(subject.copy(isDeleted = true, version = 2L)))
            val before = dao.getAllOrientationRelations()

            assertRejected {
                graph.removeRelation(id, expectedVersion = 1L, now = 30L)
            }

            assertEquals(before, dao.getAllOrientationRelations())
            assertTrue(dao.getManagedSubject("b")?.isDeleted == true)
        }
    }

    @Test
    fun `narrow read contains incoming outgoing and read-only PART_OF`() = runBlocking {
        withDatabase { db, graph ->
            seed(db)
            val dao = db.orientationDao()

            val incoming = graph.addRelation("b", "a", OrientationRelationType.SUPPORTS, 20L)
            val outgoing = graph.addRelation("a", "c", OrientationRelationType.REALIZES, 21L)
            val partOf = relation("part-of-1", "a", "b", OrientationRelationType.PART_OF, 0L)
            dao.upsertOrientationRelations(listOf(partOf))

            val visible = graph.observeRelations("a").first()
            assertEquals(setOf(incoming, outgoing, partOf.id), visible.map { it.id }.toSet())
            assertTrue(graph.observeRelations("c").first().any { it.id == outgoing })

            val before = dao.getAllOrientationRelations()
            assertRejected {
                graph.removeRelation(partOf.id, expectedVersion = 1L, now = 30L)
            }
            assertEquals(before, dao.getAllOrientationRelations())
        }
    }

    @Test
    fun `cycle rejection during tombstone resurrection preserves tombstone`() = runBlocking {
        withDatabase { db, graph ->
            seed(db)
            val dao = db.orientationDao()
            graph.addRelation("a", "b", OrientationRelationType.DEPENDS_ON, 20L)
            dao.upsertOrientationRelations(
                listOf(
                    relation(
                        "historical-edge", "b", "a",
                        OrientationRelationType.DEPENDS_ON,
                        deleted = true,
                        version = 2L,
                    ),
                ),
            )
            val before = dao.getAllOrientationRelations()

            assertRejected {
                graph.addRelation("b", "a", OrientationRelationType.DEPENDS_ON, 30L)
            }

            assertEquals(before, dao.getAllOrientationRelations())
        }
    }

    @Test
    fun `edge removal preserves subjects nodes and unrelated relation families`() = runBlocking {
        withDatabase { db, graph ->
            seed(db)
            val dao = db.orientationDao()
            val subjects = dao.getAllManagedSubjects()
            val orientations = dao.getAllOrientations()
            val aspects = dao.getAllAspectOrientationRefs()
            val bindings = dao.getAllWorkspaceBindings()

            val id = graph.addRelation("a", "b", OrientationRelationType.SUPPORTS, 20L)
            graph.removeRelation(id, expectedVersion = 1L, now = 30L)

            assertEquals(subjects, dao.getAllManagedSubjects())
            assertEquals(orientations, dao.getAllOrientations())
            assertEquals(aspects, dao.getAllAspectOrientationRefs())
            assertEquals(bindings, dao.getAllWorkspaceBindings())
            assertTrue(dao.getAllOrientationRelations().single().isDeleted)
        }
    }

    private suspend fun seed(db: AppDatabase) {
        db.orientationDao().upsertManagedSubjects(
            listOf(subject("a"), subject("b"), subject("c")),
        )
        db.orientationDao().upsertOrientations(
            listOf(orientation("a"), orientation("b"), orientation("c")),
        )
    }

    private fun subject(
        id: String,
        type: ManagedSubjectType = ManagedSubjectType.ORIENTATION,
        deleted: Boolean = false,
    ) = ManagedSubjectEntity(
        id, type.name, id, null, 1L, 1L, null, deleted, 1L,
    )

    private fun orientation(id: String) =
        OrientationEntity(id, "INTENT", null, "EXPLICIT")

    private fun relation(
        id: String,
        from: String,
        to: String,
        type: OrientationRelationType,
        order: Long? = null,
        deleted: Boolean = false,
        version: Long = 1L,
    ) = OrientationRelationEntity(
        id, from, to, type.name, order,
        10L, 10L, null, deleted, version,
    )

    private suspend fun assertRejected(block: suspend () -> Unit) {
        try {
            block()
        } catch (_: IllegalArgumentException) {
            return
        }
        fail("Expected canonical graph invariant rejection")
    }

    private suspend fun withDatabase(
        block: suspend (AppDatabase, CanonicalOrientationGraphRepository) -> Unit,
    ) {
        val database =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        try {
            block(
                database,
                CanonicalOrientationGraphRepository(
                    database,
                    database.orientationDao(),
                    database.workspaceDao(),
                ),
            )
        } finally {
            database.close()
        }
    }
}
