package com.romankozak.forwardappmobile.data.workspace.capability

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogGoalAssociationLink
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.LinkItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBacklogEntryEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalBacklogCompatibilityReaderRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `canonical explicit entries project to stable legacy DTO identities`() = runBlocking {
        val database = database()
        try {
            seedContextBackedWorkspace(database, "owner")
            seedContextBackedWorkspace(database, "child")
            seedGoalOrientationMapping(database, goalId = "goal", orientationId = "orientation")

            database.workspaceBacklogEntryDao().upsert(
                listOf(
                    entry(
                        id = "placement-goal",
                        owner = "owner",
                        kind = "ORIENTATION",
                        targetId = "orientation",
                        order = 0L,
                    ),
                    entry(
                        id = "placement-child",
                        owner = "owner",
                        kind = "WORKSPACE",
                        targetId = "child",
                        order = 1L,
                    ),
                    entry(
                        id = "placement-note",
                        owner = "owner",
                        kind = "LEGACY_NOTE",
                        targetId = "note",
                        order = 2L,
                    ),
                ),
            )

            val reader = CanonicalBacklogCompatibilityReader(database)
            val items = reader.getDirectItemsForContext("owner")

            assertEquals(
                listOf("placement-goal", "placement-child", "placement-note"),
                items.map { it.id },
            )
            assertEquals(
                listOf(
                    BacklogItemTypeValues.GOAL,
                    BacklogItemTypeValues.SUBLIST,
                    BacklogItemTypeValues.NOTE,
                ),
                items.map { it.itemType },
            )
            assertEquals(listOf("goal", "child", "note"), items.map { it.entityId })
            assertEquals(listOf(0L, 1L, 2L), items.map { it.order })
        } finally {
            database.close()
        }
    }

    @Test
    fun `Context stream composes canonical explicit rows with hashtag projection cache`() = runBlocking {
        val database = database()
        try {
            seedContext(database, "owner")
            seedContext(database, "associated")
            seedContextBackedWorkspace(database, "owner")
            seedGoal(database, "goal")

            database.workspaceBacklogEntryDao().upsert(
                listOf(
                    entry(
                        id = "explicit",
                        owner = "owner",
                        kind = "CHECKLIST",
                        targetId = "checklist",
                        order = 5L,
                    ),
                ),
            )
            database.backlogGoalAssociationLinkDao().insertAll(
                listOf(
                    BacklogGoalAssociationLink(
                        projectionId = "projection",
                        goalId = "goal",
                        contextId = "owner",
                        ownerContextId = "associated",
                        associationTag = "home",
                        order = 1L,
                        linkedAt = 50L,
                    ),
                ),
            )

            val reader = CanonicalBacklogCompatibilityReader(database)
            val items = reader.observeItemsForContext("owner").first()

            assertEquals(listOf("projection", "explicit"), items.map { it.id })
            val projection = items.first()
            assertEquals(BacklogItemTypeValues.GOAL, projection.itemType)
            assertEquals("goal", projection.entityId)
            assertEquals("associated", projection.associationOwnerContextId)
            assertEquals("home", projection.associationTag)
        } finally {
            database.close()
        }
    }

    @Test
    fun `lookup by placement ids preserves requested canonical and projection ids`() = runBlocking {
        val database = database()
        try {
            seedContext(database, "owner")
            seedContext(database, "associated")
            seedContextBackedWorkspace(database, "owner")
            seedGoal(database, "goal")

            database.workspaceBacklogEntryDao().upsert(
                listOf(
                    entry(
                        id = "explicit",
                        owner = "owner",
                        kind = "MUSIC_NOTE",
                        targetId = "music",
                        order = 0L,
                    ),
                ),
            )
            database.backlogGoalAssociationLinkDao().insertAll(
                listOf(
                    BacklogGoalAssociationLink(
                        projectionId = "projection",
                        goalId = "goal",
                        contextId = "owner",
                        ownerContextId = "associated",
                        associationTag = "tag",
                        order = 1L,
                        linkedAt = 20L,
                    ),
                ),
            )

            val reader = CanonicalBacklogCompatibilityReader(database)
            val items =
                reader.getItemsByIds(
                    listOf("projection", "missing", "explicit"),
                )

            assertEquals(listOf("projection", "explicit"), items.map { it.id })
            assertEquals(
                listOf(BacklogItemTypeValues.GOAL, BacklogItemTypeValues.MUSIC_NOTE),
                items.map { it.itemType },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `Orientation without live CUT_OVER GOAL mapping fails closed`() = runBlocking {
        val database = database()
        try {
            seedContextBackedWorkspace(database, "owner")
            database.workspaceBacklogEntryDao().upsert(
                listOf(
                    entry(
                        id = "bad",
                        owner = "owner",
                        kind = "ORIENTATION",
                        targetId = "orientation",
                        order = 0L,
                    ),
                ),
            )

            val reader = CanonicalBacklogCompatibilityReader(database)

            assertTrue(
                runCatching {
                    reader.getDirectItemsForContext("owner")
                }.isFailure,
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `Goal runtime queries use canonical placement and ignore retained legacy authority`() = runBlocking {
        val database = database()
        try {
            seedContext(database, "owner")
            seedContextBackedWorkspace(database, "owner")
            seedGoal(database, "canonical-goal")
            seedGoal(database, "legacy-only-goal")
            seedGoalOrientationMapping(
                database,
                goalId = "canonical-goal",
                orientationId = "canonical-orientation",
            )
            database.workspaceBacklogEntryDao().upsert(
                listOf(
                    entry(
                        id = "canonical-placement",
                        owner = "owner",
                        kind = "ORIENTATION",
                        targetId = "canonical-orientation",
                        order = 0L,
                    ),
                    entry(
                        id = "canonical-link-placement",
                        owner = "owner",
                        kind = "LINK_ITEM",
                        targetId = "canonical-link",
                        order = 1L,
                    ),
                ),
            )
            database.linkItemDao().insert(linkItem("canonical-link"))
            database.linkItemDao().insert(linkItem("legacy-only-link"))
            database.listItemDao().insertItem(
                BacklogItem(
                    id = "retained-legacy-placement",
                    contextId = "owner",
                    itemType = BacklogItemTypeValues.GOAL,
                    entityId = "legacy-only-goal",
                    order = 0L,
                ),
            )
            database.listItemDao().insertItem(
                BacklogItem(
                    id = "retained-legacy-link-placement",
                    contextId = "owner",
                    itemType = BacklogItemTypeValues.LINK_ITEM,
                    entityId = "legacy-only-link",
                    order = 1L,
                ),
            )

            assertEquals(
                listOf("canonical-goal"),
                database.goalDao().getGoalsByContextIdFlow("owner").first().map { it.id },
            )
            assertEquals(
                listOf("canonical-goal"),
                database.goalDao().searchGoalsGlobal("%goal%").map { it.goal.id },
            )
            assertEquals(
                listOf("canonical-link"),
                database.linkItemDao().searchLinksGlobal("%link%").map { it.link.id },
            )
            assertEquals(
                listOf("canonical-link-placement"),
                database.linkItemDao().searchLinksGlobal("%link%").map { it.listItemId },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `structural child query follows Context hierarchy without legacy Backlog row`() = runBlocking {
        val database = database()
        try {
            seedContext(database, "owner")
            database.contextDao().insert(
                com.romankozak.forwardappmobile.core.data.models.entities.Context(
                    id = "child",
                    name = "child",
                    description = null,
                    parentId = "owner",
                    createdAt = 1L,
                    updatedAt = 1L,
                ),
            )

            assertEquals(
                listOf("child"),
                database.contextDao().getSubprojectsByParentIdFlow("owner").first().map { it.id },
            )
        } finally {
            database.close()
        }
    }

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private suspend fun seedContext(
        database: AppDatabase,
        id: String,
    ) {
        database.contextDao().insert(
            com.romankozak.forwardappmobile.core.data.models.entities.Context(
                id = id,
                name = id,
                description = null,
                parentId = null,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )
    }

    private suspend fun seedContextBackedWorkspace(
        database: AppDatabase,
        id: String,
    ) {
        database.workspaceDao().upsert(
            listOf(
                WorkspaceEntity(
                    id = id,
                    nameOverride = id,
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
                    sourceContextId = id,
                ),
            ),
        )
        database.orientationDao().upsertWorkspaceCapabilities(
            listOf(
                WorkspaceCapabilityInstanceEntity(
                    id = "cap-$id",
                    workspaceId = id,
                    capabilityType = "BACKLOG",
                    instanceKey = "default",
                    capabilityOrder = 0L,
                    state = "ACTIVE",
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

    private suspend fun seedGoal(
        database: AppDatabase,
        id: String,
    ) {
        database.goalDao().insertGoal(
            Goal(
                id = id,
                text = id,
                description = null,
                completed = false,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )
    }

    private suspend fun seedGoalOrientationMapping(
        database: AppDatabase,
        goalId: String,
        orientationId: String,
    ) {
        database.orientationDao().upsertManagedSubjects(
            listOf(
                ManagedSubjectEntity(
                    id = orientationId,
                    subjectType = ManagedSubjectType.ORIENTATION.name,
                    title = goalId,
                    description = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
        database.orientationDao().upsertOrientations(
            listOf(
                OrientationEntity(
                    subjectId = orientationId,
                    kind = "GOAL",
                    lifecycle = null,
                    lifecycleOrigin = "UNSET",
                ),
            ),
        )
        database.orientationDao().upsertLegacyMappings(
            listOf(
                LegacySubjectMappingEntity(
                    id = "mapping-$goalId",
                    sourceType = "GOAL",
                    sourceId = goalId,
                    subjectId = orientationId,
                    migrationVersion = 1,
                    state = "CUT_OVER",
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
    }

    private fun entry(
        id: String,
        owner: String,
        kind: String,
        targetId: String,
        order: Long,
    ) = WorkspaceBacklogEntryEntity(
        id = id,
        workspaceId = owner,
        capabilityInstanceId = "cap-$owner",
        targetKind = kind,
        targetId = targetId,
        entryOrder = order,
        createdAt = 0L,
        updatedAt = 10L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
    )

    private fun linkItem(id: String): LinkItemEntity =
        LinkItemEntity(
            id = id,
            linkData =
                RelatedLink(
                    type = LinkType.URL,
                    target = "https://$id.example",
                    displayName = id,
                ),
            createdAt = 1L,
        )
}
