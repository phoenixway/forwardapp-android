package com.romankozak.forwardappmobile.data.workspace.capability

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogGoalAssociationLink
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
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
    fun `initial reactive snapshot is one joined statement rather than queued multi-table transaction`() = runBlocking {
        val queries = java.util.Collections.synchronizedList(mutableListOf<String>())
        val database = database(queries)
        try {
            seedContextBackedWorkspace(database, "owner")
            queries.clear()
            assertTrue(CanonicalBacklogCompatibilityReader(database).observeItemsForContext("owner").first().isEmpty())
            val reads = queries.filter { it.trimStart().startsWith("SELECT", ignoreCase = true) && it.contains("FROM workspace_backlog_entries e") }
            assertEquals("Exactly one coherent joined statement: $reads", 1, reads.size)
            assertTrue(reads.single().contains("UNION ALL"))
            assertTrue(reads.single().contains("legacy_subject_mappings"))
            assertTrue(queries.none { it.trim().equals("SELECT * FROM legacy_subject_mappings", true) })
        } finally { database.close() }
    }

    @Test
    fun `one committed add and removal reactively deliver coherent exact placements`() = runBlocking {
        val database = database()
        val updates = Channel<List<com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem>>(Channel.UNLIMITED)
        var job: kotlinx.coroutines.Job? = null
        try {
            seedContextBackedWorkspace(database, "owner")
            job = launch(Dispatchers.IO) {
                CanonicalBacklogCompatibilityReader(database).observeItemsForContext("owner").collect { updates.send(it) }
            }
            assertTrue(withTimeout(5_000) { updates.receive() }.isEmpty())
            val placement = entry("new", "owner", "ORIENTATION", "semantic", 0)
            database.withTransaction {
                seedGoalOrientationMapping(database, "goal", "semantic")
                database.workspaceBacklogEntryDao().upsert(listOf(placement))
            }
            val added = withTimeout(5_000) { updates.receive() }
            assertEquals(listOf("new"), added.map { it.id })
            assertEquals(listOf("goal"), added.map { it.entityId })
            database.workspaceBacklogEntryDao().upsert(listOf(placement.copy(isDeleted = true, version = 2)))
            assertTrue(withTimeout(5_000) { updates.receive() }.isEmpty())
        } finally { job?.cancel(); job?.join(); database.close() }
    }

    @Test
    fun `Workspace Goal query uses indexed identity lookup rather than scanning every Goal`() = runBlocking {
        val queries = java.util.Collections.synchronizedList(mutableListOf<String>())
        val database = database(queries)
        try {
            seedContextBackedWorkspace(database, "owner")
            seedGoal(database, "goal")
            seedGoal(database, "unrelated")
            seedGoalOrientationMapping(database, "goal", "semantic")
            database.workspaceBacklogEntryDao().upsert(listOf(entry("placement", "owner", "ORIENTATION", "semantic", 0)))
            queries.clear()
            assertEquals(listOf("goal"), database.goalDao().getGoalsByContextIdFlow("owner").first().map { it.id })
            val sql = queries.single { it.contains("FROM goals g") }
            val plan = database.openHelper.readableDatabase.query(
                androidx.sqlite.db.SimpleSQLiteQuery("EXPLAIN QUERY PLAN $sql", arrayOf("owner", "owner")),
            ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(3)) } }
            assertTrue("Expected indexed Goal identity lookup: $plan", plan.any {
                it.startsWith("SEARCH") && (it.contains("goals AS g") || it.startsWith("SEARCH g ")) && it.contains("(id=?)")
            })
            assertTrue("Must not scan every Goal: $plan", plan.none {
                it.startsWith("SCAN") && (it.contains("goals AS g") || Regex("SCAN g(?: |$)").containsMatchIn(it))
            })
        } finally { database.close() }
    }

    @Test
    fun `joined reader preserves typed targets with same raw Orientation and Workspace id`() = runBlocking {
        val database = database()
        try {
            seedContextBackedWorkspace(database, "owner")
            seedContextBackedWorkspace(database, "shared")
            seedGoalOrientationMapping(database, "goal", "shared")
            database.workspaceBacklogEntryDao().upsert(listOf(
                entry("semantic", "owner", "ORIENTATION", "shared", 0),
                entry("operational", "owner", "WORKSPACE", "shared", 1),
            ))
            val rows = CanonicalBacklogCompatibilityReader(database).getDirectItemsForContext("owner")
            assertEquals(listOf(BacklogItemTypeValues.GOAL, BacklogItemTypeValues.SUBLIST), rows.map { it.itemType })
            assertEquals(listOf("goal", "shared"), rows.map { it.entityId })
        } finally { database.close() }
    }

    @Test
    fun `joined reader follows authoritative mapping replacement without retaining old Goal id`() = runBlocking {
        val database = database()
        try {
            seedContextBackedWorkspace(database, "owner")
            seedGoalOrientationMapping(database, "goal-one", "semantic")
            val mapping = database.orientationDao().getAllLegacyMappings().single()
            database.orientationDao().upsertLegacyMappings(listOf(mapping.copy(sourceId = "goal-two", version = 2)))
            database.workspaceBacklogEntryDao().upsert(listOf(entry("placement", "owner", "ORIENTATION", "semantic", 0)))
            assertEquals(1, database.orientationDao().getAllLegacyMappings().count { it.subjectId == "semantic" })
            assertEquals(listOf("goal-two"), CanonicalBacklogCompatibilityReader(database).getItemsForContext("owner").map { it.entityId })
        } finally { database.close() }
    }

    @Test
    fun `joined reader rejects missing Workspace target instead of fabricating a live DTO`() = runBlocking {
        val database = database()
        try {
            seedContextBackedWorkspace(database, "owner")
            database.workspaceBacklogEntryDao().upsert(listOf(entry("placement", "owner", "WORKSPACE", "missing", 0)))
            assertTrue(runCatching { CanonicalBacklogCompatibilityReader(database).getItemsForContext("owner") }.isFailure)
        } finally { database.close() }
    }

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
    fun `Goal runtime queries use canonical placement after legacy local retirement`() = runBlocking {
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
    fun `global goal search admits canonical shell-free Workspace placement`() = runBlocking {
        val database = database()
        try {
            seedContextBackedWorkspace(database, "shell-free-owner")
            seedGoal(database, "shell-free-goal")
            seedGoalOrientationMapping(
                database,
                goalId = "shell-free-goal",
                orientationId = "shell-free-orientation",
            )
            database.workspaceBacklogEntryDao().upsert(
                listOf(
                    entry(
                        id = "shell-free-placement",
                        owner = "shell-free-owner",
                        kind = "ORIENTATION",
                        targetId = "shell-free-orientation",
                        order = 0L,
                    ),
                ),
            )

            assertEquals(
                emptyList<String>(),
                database.contextDao().getAllRaw().map { it.id },
            )

            val results = database.goalDao().searchGoalsGlobal("%shell-free%")

            assertEquals(listOf("shell-free-goal"), results.map { it.goal.id })
            assertEquals(listOf("shell-free-owner"), results.map { it.contextId })
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
                database.contextDao().getActiveContextsByParentId("owner").map { it.id },
            )
        } finally {
            database.close()
        }
    }

    private fun database(queries: MutableList<String>? = null): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryCallback({ sql, _ -> queries?.add(sql) }, java.util.concurrent.Executor { it.run() })
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
                    roleCode = null,
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
