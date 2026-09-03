package com.romankozak.forwardappmobile.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.logic.ContextMarkerHandler
import com.romankozak.forwardappmobile.data.logic.TagAssociationHandler
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.workspace.capability.BacklogCanonicalTargetResolver
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogCompatibilityReader
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogTargetValidator
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalCapabilityInstanceStore
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
class GoalCanonicalSubjectCreationRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `new Goal composes canonical subject mapping placement and remains readable`() = runBlocking {
        val database = database()
        try {
            seedOwner(database, active = true)
            val repository = goalRepository(database)

            val placementId = repository.addGoalToContext("Room canonical goal", OWNER_ID)
            val goal = database.goalDao().getAll().single()
            val subject = database.orientationDao().getAllManagedSubjects().single()
            val orientation = database.orientationDao().getAllOrientations().single()
            val mapping = requireNotNull(
                database.orientationDao().getLegacyMapping(
                    LegacyOrientationSourceType.GOAL.name,
                    goal.id,
                ),
            )
            val placement = requireNotNull(database.workspaceBacklogEntryDao().getById(placementId))

            assertEquals("Room canonical goal", goal.text)
            assertFalse(goal.isDeleted)
            assertEquals(ManagedSubjectType.ORIENTATION.name, subject.subjectType)
            assertEquals(subject.id, orientation.subjectId)
            assertEquals(1, database.orientationDao().getAllAssessments().count { it.orientationId == subject.id })
            assertEquals(1, database.orientationDao().getAllAssessmentRevisions().count { it.orientationId == subject.id })
            assertEquals(LegacyOrientationSourceType.GOAL.name, mapping.sourceType)
            assertEquals(goal.id, mapping.sourceId)
            assertEquals(subject.id, mapping.subjectId)
            assertEquals(LegacySubjectMappingState.CUT_OVER.name, mapping.state)
            assertFalse(mapping.isDeleted)
            assertEquals(WorkspaceBacklogTargetKind.ORIENTATION.name, placement.targetKind)
            assertEquals(subject.id, placement.targetId)
            assertEquals(OWNER_ID, placement.workspaceId)
            assertEquals("backlog-$OWNER_ID", placement.capabilityInstanceId)
            assertEquals(1L, placement.version)
            assertFalse(placement.isDeleted)

            val resolved = BacklogCanonicalTargetResolver(database.orientationDao(), database.workspaceDao())
                .resolveLegacy("GOAL", goal.id)
            assertEquals(WorkspaceBacklogTargetKind.ORIENTATION, resolved.kind)
            assertEquals(subject.id, resolved.id)

            val presented = CanonicalBacklogCompatibilityReader(database)
                .getDirectItemsForContext(OWNER_ID)
            assertEquals(1, presented.size)
            assertEquals(goal.id, presented.single().entityId)
            assertEquals("GOAL", presented.single().itemType)
            assertEquals(goal.text, database.goalDao().getGoalById(presented.single().entityId)?.text)

            assertTrue(database.orientationDao().getAllManagedSubjects().all { it.syncedAt == null })
            assertTrue(database.orientationDao().getAllAssessments().all { it.syncedAt == null })
            assertTrue(database.orientationDao().getAllAssessmentRevisions().all { it.syncedAt == null })
            assertTrue(database.orientationDao().getAllLegacyMappings().all { it.syncedAt == null })
            assertTrue(database.workspaceBacklogEntryDao().getUnsynced().any { it.id == placementId })
            assertTrue(database.backlogOrderDao().getAllRaw().isEmpty())
            assertTrue(database.listItemDao().getAllRaw().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `placement validation failure rolls back Goal subject orientation mapping and placement`() = runBlocking {
        val database = database()
        try {
            seedOwner(database, active = false)
            val repository = goalRepository(database)

            val failure = runCatching { repository.addGoalToContext("should roll back", OWNER_ID) }
            assertTrue(failure.isFailure)
            assertTrue(database.goalDao().getAll().none { it.text == "should roll back" })
            assertTrue(database.orientationDao().getAllManagedSubjects().isEmpty())
            assertTrue(database.orientationDao().getAllOrientations().isEmpty())
            assertTrue(database.orientationDao().getAllAssessments().isEmpty())
            assertTrue(database.orientationDao().getAllAssessmentRevisions().isEmpty())
            assertTrue(database.orientationDao().getAllLegacyMappings().isEmpty())
            assertTrue(database.workspaceBacklogEntryDao().getAll().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `removing placement does not delete composed target domain state`() = runBlocking {
        val database = database()
        try {
            seedOwner(database, active = true)
            val repository = goalRepository(database)
            val placementId = repository.addGoalToContext("survives remove", OWNER_ID)
            val goal = database.goalDao().getAll().single()
            val subject = database.orientationDao().getAllManagedSubjects().single()
            val target = BacklogCanonicalTargetResolver(database.orientationDao(), database.workspaceDao())
                .resolveLegacy("GOAL", goal.id)

            canonicalBacklog(database).setPlacementVisible(OWNER_ID, target, visible = false, now = 20L)

            assertTrue(requireNotNull(database.workspaceBacklogEntryDao().getById(placementId)).isDeleted)
            assertFalse(requireNotNull(database.goalDao().getGoalById(goal.id)).isDeleted)
            assertFalse(requireNotNull(database.orientationDao().getManagedSubject(subject.id)).isDeleted)
            assertTrue(database.orientationDao().getAllOrientations().any { it.subjectId == subject.id })
            assertFalse(requireNotNull(database.orientationDao().getLegacyMapping("GOAL", goal.id)).isDeleted)
        } finally {
            database.close()
        }
    }

    private fun goalRepository(database: AppDatabase): GoalRepository {
        val placements = BacklogPlacementCommands(
            contextDao = mockk(relaxed = true),
            canonicalRepository = canonicalBacklog(database),
            canonicalTargetResolver = BacklogCanonicalTargetResolver(database.orientationDao(), database.workspaceDao()),
        )
        val associations = mockk<TagAssociationHandler>()
        coEvery { associations.syncGoalAssociations(any(), any()) } returns emptyMap()
        coEvery { associations.findGoalAssociationOwnerContextId(any()) } returns null
        val markerHandler = mockk<ContextMarkerHandler>(relaxed = true)
        val markerProvider = mockk<Provider<ContextMarkerHandler>>()
        every { markerProvider.get() } returns markerHandler
        return GoalRepository(
            goalDao = database.goalDao(),
            reminderRepository = mockk(relaxed = true),
            contextMarkerHandlerProvider = markerProvider,
            contextDao = mockk(relaxed = true),
            tagAssociationHandler = associations,
            contextStructureRepository = mockk(relaxed = true),
            backlogPlacementCommands = placements,
            database = database,
            orientationDao = database.orientationDao(),
            canonicalOrientationRepository = CanonicalOrientationRepository(database, database.orientationDao()),
        )
    }

    private fun canonicalBacklog(database: AppDatabase): CanonicalBacklogRepository =
        CanonicalBacklogRepository(
            database = database,
            instanceStore = CanonicalCapabilityInstanceStore(database, database.workspaceDao(), database.orientationDao()),
            entryDao = database.workspaceBacklogEntryDao(),
            targetValidator = CanonicalBacklogTargetValidator(database),
        )

    private suspend fun seedOwner(database: AppDatabase, active: Boolean) {
        database.contextDao().insert(
            ContextEntity(
                id = OWNER_ID,
                name = OWNER_ID,
                description = null,
                parentId = null,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )
        database.workspaceDao().upsert(
            listOf(
                WorkspaceEntity(
                    id = OWNER_ID,
                    nameOverride = OWNER_ID,
                    descriptionOverride = null,
                    parentWorkspaceId = null,
                    roleCode = null,
                    workspaceOrder = 0L,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                    provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
                    sourceContextId = OWNER_ID,
                ),
            ),
        )
        database.orientationDao().upsertWorkspaceCapabilities(
            listOf(
                WorkspaceCapabilityInstanceEntity(
                    id = "backlog-$OWNER_ID",
                    workspaceId = OWNER_ID,
                    capabilityType = "BACKLOG",
                    instanceKey = "default",
                    capabilityOrder = 0L,
                    state = if (active) "ACTIVE" else "DISABLED",
                    configurationVersion = 1,
                    configuration = "{}",
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
    }

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private companion object {
        const val OWNER_ID = "goal-room-owner"
    }
}
