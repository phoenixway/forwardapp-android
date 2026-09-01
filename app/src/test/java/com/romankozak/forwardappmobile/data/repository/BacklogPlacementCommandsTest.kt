package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBacklogEntryEntity
import com.romankozak.forwardappmobile.data.workspace.capability.BacklogCanonicalTargetResolver
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetRef
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BacklogPlacementCommandsTest {
    @Test
    fun `direct hierarchy child is not duplicated as explicit Backlog placement`() = runTest {
        val contextDao = mockk<ContextDao>()
        val canonical = mockk<CanonicalBacklogRepository>(relaxed = true)
        val resolver = mockk<BacklogCanonicalTargetResolver>(relaxed = true)
        val child = mockk<Context>()
        every { child.parentId } returns "parent"
        coEvery { contextDao.getContextById("child") } returns child
        val commands = BacklogPlacementCommands(contextDao, canonical, resolver)

        val result = commands.addContextLinkToContextBacked("child", "parent")

        assertNull(result)
        coVerify(exactly = 0) { canonical.addEntryAtStart(any(), any(), any()) }
    }

    @Test
    fun `Context-backed add resolves identity and writes canonical placement only`() = runTest {
        val contextDao = mockk<ContextDao>(relaxed = true)
        val canonical = mockk<CanonicalBacklogRepository>()
        val resolver = mockk<BacklogCanonicalTargetResolver>()
        val target =
            WorkspaceBacklogTargetRef(
                kind = WorkspaceBacklogTargetKind.ORIENTATION,
                id = "orientation-1",
            )

        coEvery { resolver.resolveLegacy("GOAL", "goal-1") } returns target
        coEvery {
            canonical.addEntryAtStart(
                workspaceId = "context-1",
                target = target,
                now = any(),
            )
        } returns "canonical-placement"

        val commands = BacklogPlacementCommands(contextDao, canonical, resolver)

        val id =
            commands.addToContextBacked(
                contextId = "context-1",
                itemType = "GOAL",
                entityId = "goal-1",
            )

        assertEquals("canonical-placement", id)
        coVerify(exactly = 1) { resolver.resolveLegacy("GOAL", "goal-1") }
        coVerify(exactly = 1) {
            canonical.addEntryAtStart(
                workspaceId = "context-1",
                target = target,
                now = any(),
            )
        }
    }

    @Test
    fun `canonical legacy-target add resolves identity and writes canonical placement only`() = runTest {
        val contextDao = mockk<ContextDao>(relaxed = true)
        val canonical = mockk<CanonicalBacklogRepository>()
        val resolver = mockk<BacklogCanonicalTargetResolver>()
        val target =
            WorkspaceBacklogTargetRef(
                kind = WorkspaceBacklogTargetKind.ORIENTATION,
                id = "orientation-1",
            )

        coEvery { resolver.resolveLegacy("GOAL", "goal-1") } returns target
        coEvery {
            canonical.addEntry(
                workspaceId = "workspace-1",
                target = target,
                now = 123L,
            )
        } returns "canonical-placement"

        val commands = BacklogPlacementCommands(contextDao, canonical, resolver)

        val id =
            commands.addLegacyTargetToCanonicalWorkspace(
                workspaceId = "workspace-1",
                itemType = "GOAL",
                entityId = "goal-1",
                now = 123L,
            )

        assertEquals("canonical-placement", id)
        coVerify(exactly = 1) { resolver.resolveLegacy("GOAL", "goal-1") }
        coVerify(exactly = 1) {
            canonical.addEntry(
                workspaceId = "workspace-1",
                target = target,
                now = 123L,
            )
        }
    }

    @Test
    fun `Context-backed reorder excludes projections and writes canonical explicit order`() = runTest {
        val contextDao = mockk<ContextDao>(relaxed = true)
        val canonical = mockk<CanonicalBacklogRepository>(relaxed = true)
        val resolver = mockk<BacklogCanonicalTargetResolver>(relaxed = true)

        val commands = BacklogPlacementCommands(contextDao, canonical, resolver)

        commands.reorderContextBacked(
            listOf(
                item(id = "explicit-a", order = 40L),
                item(
                    id = "projection",
                    order = 50L,
                    associationOwnerContextId = "owner",
                    associationTag = "tag",
                ),
                item(id = "explicit-b", order = 60L),
            ),
        )

        coVerify(exactly = 1) {
            canonical.reorder(
                workspaceId = "context-1",
                orderedEntryIds = listOf("explicit-a", "explicit-b"),
                now = any(),
            )
        }
    }

    @Test
    fun `canonical typed add bypasses compatibility resolver and legacy storage`() = runTest {
        val contextDao = mockk<ContextDao>(relaxed = true)
        val canonical = mockk<CanonicalBacklogRepository>()
        val resolver = mockk<BacklogCanonicalTargetResolver>(relaxed = true)
        val target =
            WorkspaceBacklogTargetRef(
                kind = WorkspaceBacklogTargetKind.CHECKLIST,
                id = "checklist-1",
            )

        coEvery {
            canonical.addEntry(
                workspaceId = "workspace-1",
                target = target,
                now = 456L,
            )
        } returns "canonical-placement"

        val commands = BacklogPlacementCommands(contextDao, canonical, resolver)

        val id =
            commands.addCanonicalTarget(
                workspaceId = "workspace-1",
                target = target,
                now = 456L,
            )

        assertEquals("canonical-placement", id)
        coVerify(exactly = 1) {
            canonical.addEntry(
                workspaceId = "workspace-1",
                target = target,
                now = 456L,
            )
        }
        coVerify(exactly = 0) { resolver.resolveLegacy(any(), any()) }
    }

    @Test
    fun `Context-backed LinkItem tombstone resolves domain target and bypasses legacy storage`() = runTest {
        val contextDao = mockk<ContextDao>(relaxed = true)
        val canonical = mockk<CanonicalBacklogRepository>()
        val resolver = mockk<BacklogCanonicalTargetResolver>()
        val target =
            WorkspaceBacklogTargetRef(
                kind = WorkspaceBacklogTargetKind.LINK_ITEM,
                id = "link-1",
            )

        coEvery { resolver.resolveLegacy("LINK_ITEM", "link-1") } returns target
        coEvery {
            canonical.tombstoneEntriesTargeting(
                target = target,
                now = 789L,
            )
        } returns 2

        val commands = BacklogPlacementCommands(contextDao, canonical, resolver)

        val changed =
            commands.tombstoneContextBackedTarget(
                itemType = "LINK_ITEM",
                entityId = "link-1",
                now = 789L,
            )

        assertEquals(2, changed)
        coVerify(exactly = 1) { resolver.resolveLegacy("LINK_ITEM", "link-1") }
        coVerify(exactly = 1) {
            canonical.tombstoneEntriesTargeting(
                target = target,
                now = 789L,
            )
        }
    }

    @Test
    fun `live duplicate lookup uses canonical logical placement state`() = runTest {
        val contextDao = mockk<ContextDao>(relaxed = true)
        val canonical = mockk<CanonicalBacklogRepository>()
        val resolver = mockk<BacklogCanonicalTargetResolver>()
        val target = WorkspaceBacklogTargetRef(WorkspaceBacklogTargetKind.ORIENTATION, "orientation-1")
        coEvery { resolver.resolveLegacy("GOAL", "goal-1") } returns target
        coEvery { canonical.findPlacement("target", target) } returns entry(isDeleted = false)

        val commands = BacklogPlacementCommands(contextDao, canonical, resolver)

        assertTrue(commands.hasLiveContextBackedPlacement("target", "GOAL", "goal-1"))

        coEvery { canonical.findPlacement("target", target) } returns entry(isDeleted = true)
        assertFalse(commands.hasLiveContextBackedPlacement("target", "GOAL", "goal-1"))
    }

    private fun entry(isDeleted: Boolean) =
        WorkspaceBacklogEntryEntity(
            id = "placement",
            workspaceId = "target",
            capabilityInstanceId = "backlog-target",
            targetKind = WorkspaceBacklogTargetKind.ORIENTATION.name,
            targetId = "orientation-1",
            entryOrder = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = isDeleted,
            version = 1L,
        )

    private fun item(
        id: String,
        order: Long,
        associationOwnerContextId: String? = null,
        associationTag: String? = null,
    ) =
        BacklogItem(
            id = id,
            contextId = "context-1",
            itemType = "GOAL",
            entityId = "goal-$id",
            associationOwnerContextId = associationOwnerContextId,
            associationTag = associationTag,
            order = order,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )
}
