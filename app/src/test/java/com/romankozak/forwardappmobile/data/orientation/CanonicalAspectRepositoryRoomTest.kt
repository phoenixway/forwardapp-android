package com.romankozak.forwardappmobile.data.orientation

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectOrientationRefEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectOrientationRelationType
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalAspectRepositoryRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `older live membership cannot resurrect a newer tombstone`() {
        val live = membership(version = 1L, deleted = false)
        val tombstone = membership(version = 2L, deleted = true)

        val accepted =
            mergeByFreshness(
                local = listOf(tombstone),
                incoming = listOf(live),
                id = { it.id },
                version = { it.version },
                updatedAt = { it.updatedAt },
            )

        assertTrue(accepted.isEmpty())
    }

    @Test
    fun `hierarchy rejects cycles and parent tombstone promotes children without deleting them`() = runBlocking {
        val database = database()
        try {
            val repository = CanonicalAspectRepository(database, database.orientationDao())
            val parentId = repository.create("Engineering", now = 10L)
            val siblingId = repository.create("Home", now = 11L)
            val childId = repository.create("Software", parentAspectId = parentId, now = 20L)

            repository.reorderSiblings(null, listOf(siblingId, parentId), now = 21L)
            repository.updateDetails(childId, "Software engineering", "Systems", now = 22L)
            repository.setArchived(childId, archived = true, now = 23L)
            assertEquals(0L, database.orientationDao().getAspect(siblingId)?.aspectOrder)
            assertEquals("Software engineering", database.orientationDao().getManagedSubject(childId)?.title)
            assertTrue(database.orientationDao().getAspect(childId)?.archived == true)

            val failure = runCatching { repository.move(parentId, childId, now = 30L) }.exceptionOrNull()
            assertTrue(failure is IllegalArgumentException)
            assertNull(database.orientationDao().getAspect(parentId)?.parentAspectId)

            repository.tombstone(parentId, now = 40L)

            assertTrue(database.orientationDao().getManagedSubject(parentId)?.isDeleted == true)
            assertFalse(database.orientationDao().getManagedSubject(childId)?.isDeleted == true)
            assertNull(database.orientationDao().getAspect(childId)?.parentAspectId)
            assertEquals(4L, database.orientationDao().getManagedSubject(childId)?.version)
        } finally {
            database.close()
        }
    }

    @Test
    fun `memberships preserve secondary refs and switch one primary aspect`() = runBlocking {
        val database = database()
        try {
            val aspects = CanonicalAspectRepository(database, database.orientationDao())
            val links = CanonicalAspectLinksRepository(database, database.orientationDao(), database.contextDao())
            val engineeringId = aspects.create("Engineering", now = 10L)
            val homeId = aspects.create("Home", now = 11L)
            insertOrientation(database, ORIENTATION_ID)

            val relevantId =
                links.linkOrientation(
                    engineeringId,
                    ORIENTATION_ID,
                    AspectOrientationRelationType.RELEVANT_TO,
                    now = 20L,
                )
            val firstPrimaryId =
                links.linkOrientation(
                    engineeringId,
                    ORIENTATION_ID,
                    AspectOrientationRelationType.BELONGS_TO,
                    makePrimary = true,
                    now = 21L,
                )
            val secondPrimaryId =
                links.linkOrientation(
                    homeId,
                    ORIENTATION_ID,
                    AspectOrientationRelationType.BELONGS_TO,
                    makePrimary = true,
                    now = 22L,
                )

            val refs = database.orientationDao().getAllAspectOrientationRefs().associateBy { it.id }
            assertFalse(refs.getValue(relevantId).isDeleted)
            assertFalse(refs.getValue(firstPrimaryId).isPrimary)
            assertTrue(refs.getValue(secondPrimaryId).isPrimary)
            assertEquals(1, refs.values.count { !it.isDeleted && it.isPrimary })
            assertEquals(
                secondPrimaryId,
                links.linkOrientation(
                    homeId,
                    ORIENTATION_ID,
                    AspectOrientationRelationType.BELONGS_TO,
                    now = 22L,
                ),
            )
            assertTrue(
                database.orientationDao().getAllAspectOrientationRefs().first { it.id == secondPrimaryId }.isPrimary,
            )

            links.reorderMemberships(engineeringId, listOf(firstPrimaryId, relevantId), now = 23L)
            assertEquals(
                0L,
                database.orientationDao().getAllAspectOrientationRefs().first { it.id == firstPrimaryId }.refOrder,
            )

            aspects.tombstone(engineeringId, now = 24L)
            val afterDelete = database.orientationDao().getAllAspectOrientationRefs().associateBy { it.id }
            assertTrue(afterDelete.getValue(relevantId).isDeleted)
            assertTrue(afterDelete.getValue(firstPrimaryId).isDeleted)
            assertFalse(afterDelete.getValue(secondPrimaryId).isDeleted)
        } finally {
            database.close()
        }
    }

    @Test
    fun `compatibility workspace embodiment is replaced transactionally and tombstoned with aspect`() = runBlocking {
        val database = database()
        try {
            val aspects = CanonicalAspectRepository(database, database.orientationDao())
            val links = CanonicalAspectLinksRepository(database, database.orientationDao(), database.contextDao())
            val firstAspectId = aspects.create("Engineering", now = 10L)
            val secondAspectId = aspects.create("Operations", now = 11L)
            database.contextDao().insert(contextEntity(WORKSPACE_ID))

            val firstBindingId = links.bindCompatibilityWorkspace(firstAspectId, WORKSPACE_ID, now = 20L)
            val secondBindingId = links.bindCompatibilityWorkspace(secondAspectId, WORKSPACE_ID, now = 30L)

            assertNotEquals(firstBindingId, secondBindingId)
            var bindings = database.orientationDao().getAllWorkspaceBindings().associateBy { it.id }
            assertTrue(bindings.getValue(firstBindingId).isDeleted)
            assertFalse(bindings.getValue(secondBindingId).isDeleted)

            aspects.tombstone(secondAspectId, now = 40L)
            bindings = database.orientationDao().getAllWorkspaceBindings().associateBy { it.id }
            assertTrue(bindings.getValue(secondBindingId).isDeleted)
            assertFalse(database.contextDao().getContextById(WORKSPACE_ID)?.isDeleted == true)
        } finally {
            database.close()
        }
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private suspend fun insertOrientation(database: AppDatabase, id: String) {
        database.orientationDao().upsertManagedSubjects(
            listOf(
                ManagedSubjectEntity(
                    id = id,
                    subjectType = ManagedSubjectType.ORIENTATION.name,
                    title = "Autonomous home",
                    description = null,
                    createdAt = 10L,
                    updatedAt = 10L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
        database.orientationDao().upsertOrientations(
            listOf(OrientationEntity(id, "GOAL", null, "UNSET")),
        )
    }

    private fun contextEntity(id: String) =
        ContextEntity(
            id = id,
            name = "Engineering workspace",
            description = null,
            parentId = null,
            createdAt = 10L,
            updatedAt = 10L,
        )

    private fun membership(version: Long, deleted: Boolean) =
        AspectOrientationRefEntity(
            id = "membership-1",
            aspectId = "aspect-1",
            orientationId = ORIENTATION_ID,
            relationType = AspectOrientationRelationType.BELONGS_TO.name,
            isPrimary = true,
            refOrder = 0L,
            createdAt = 10L,
            updatedAt = version * 10L,
            syncedAt = null,
            isDeleted = deleted,
            version = version,
        )

    private companion object {
        const val ORIENTATION_ID = "orientation-1"
        const val WORKSPACE_ID = "context-workspace-1"
    }
}
