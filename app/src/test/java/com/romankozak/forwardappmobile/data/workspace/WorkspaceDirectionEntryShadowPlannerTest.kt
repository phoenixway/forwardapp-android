package com.romankozak.forwardappmobile.data.workspace

import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceDirectionEntryEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceDirectionEntryProvenance
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceDirectionEntryShadowPlannerTest {
    @Test
    fun `unlinked legacy Direction becomes semantic ordered entry`() {
        val row = direction(
            id = "direction-1",
            contextId = "owner",
            text = "Semantic",
            linkedContextId = null,
            order = 7,
        )

        val plan =
            planWorkspaceDirectionEntryShadow(
                rows = listOf(row),
                workspaces = listOf(workspace("owner")),
                capabilities = listOf(directionCapability("owner", "capability-owner")),
                mappings = listOf(directionMapping(row.id, "orientation-1")),
                subjects = listOf(directionSubject("orientation-1")),
                orientations = listOf(directionOrientation("orientation-1")),
                existingEntries = emptyList(),
                now = 100L,
            )

        assertTrue(plan.issues.isEmpty())

        val entry = plan.changes.single()
        assertEquals(row.id, entry.id)
        assertEquals("owner", entry.workspaceId)
        assertEquals("capability-owner", entry.capabilityInstanceId)
        assertEquals("orientation-1", entry.orientationId)
        assertNull(entry.targetWorkspaceId)
        assertNull(entry.labelOverride)
        assertEquals(7L, entry.entryOrder)
        assertEquals(
            WorkspaceDirectionEntryProvenance.LEGACY_DIRECTION_ITEM.name,
            entry.provenance,
        )
    }

    @Test
    fun `linked legacy row becomes Workspace navigation entry without semantic guess`() {
        val row = direction(
            id = "direction-link",
            contextId = "owner",
            text = "Child label",
            linkedContextId = "child",
            order = 2,
        )

        val plan =
            planWorkspaceDirectionEntryShadow(
                rows = listOf(row),
                workspaces = listOf(workspace("owner"), workspace("child")),
                capabilities = listOf(directionCapability("owner", "capability-owner")),
                mappings = emptyList(),
                subjects = emptyList(),
                orientations = emptyList(),
                existingEntries = emptyList(),
                now = 100L,
            )

        assertTrue(plan.issues.isEmpty())

        val entry = plan.changes.single()
        assertNull(entry.orientationId)
        assertEquals("child", entry.targetWorkspaceId)
        assertEquals("Child label", entry.labelOverride)
        assertEquals(2L, entry.entryOrder)
    }

    @Test
    fun `Workspace endpoint requires proven Context-backed identity`() {
        val row = direction(
            id = "direction-link",
            contextId = "owner",
            text = "Child",
            linkedContextId = "child",
            order = 1,
        )

        val invalidTarget =
            workspace(
                id = "different-id",
                sourceContextId = "child",
            )

        val plan =
            planWorkspaceDirectionEntryShadow(
                rows = listOf(row),
                workspaces = listOf(workspace("owner"), invalidTarget),
                capabilities = listOf(directionCapability("owner", "capability-owner")),
                mappings = emptyList(),
                subjects = emptyList(),
                orientations = emptyList(),
                existingEntries = emptyList(),
                now = 100L,
            )

        assertTrue(plan.changes.isEmpty())
        assertEquals(
            "DIRECTION_ENTRY_TARGET_WORKSPACE_UNRESOLVED",
            plan.issues.single().code,
        )
    }

    @Test
    fun `unresolved provenance tombstones existing legacy shadow and resolution resurrects same id`() {
        val row = direction(
            id = "direction-1",
            contextId = "owner",
            text = "Semantic",
            linkedContextId = null,
            order = 3,
        )
        val existing =
            entry(
                id = row.id,
                orientationId = "orientation-1",
                entryOrder = 3L,
                version = 4L,
            )

        val unresolved =
            planWorkspaceDirectionEntryShadow(
                rows = listOf(row),
                workspaces = listOf(workspace("owner")),
                capabilities = listOf(directionCapability("owner", "capability-owner")),
                mappings = emptyList(),
                subjects = emptyList(),
                orientations = emptyList(),
                existingEntries = listOf(existing),
                now = 100L,
            )

        assertEquals(
            "DIRECTION_ENTRY_ORIENTATION_UNRESOLVED",
            unresolved.issues.single().code,
        )
        val tombstone = unresolved.changes.single()
        assertEquals(row.id, tombstone.id)
        assertTrue(tombstone.isDeleted)
        assertEquals(5L, tombstone.version)

        val restored =
            planWorkspaceDirectionEntryShadow(
                rows = listOf(row),
                workspaces = listOf(workspace("owner")),
                capabilities = listOf(directionCapability("owner", "capability-owner")),
                mappings = listOf(directionMapping(row.id, "orientation-1")),
                subjects = listOf(directionSubject("orientation-1")),
                orientations = listOf(directionOrientation("orientation-1")),
                existingEntries = listOf(tombstone),
                now = 101L,
            )

        assertTrue(restored.issues.isEmpty())
        val resurrected = restored.changes.single()
        assertEquals(row.id, resurrected.id)
        assertFalse(resurrected.isDeleted)
        assertEquals(6L, resurrected.version)
    }

    @Test
    fun `canonical-only id collision fails closed`() {
        val row = direction(
            id = "collision",
            contextId = "owner",
            text = "Semantic",
            linkedContextId = null,
            order = 1,
        )

        val canonicalOnly =
            entry(
                id = row.id,
                provenance = WorkspaceDirectionEntryProvenance.CANONICAL_ONLY.name,
            )

        val plan =
            planWorkspaceDirectionEntryShadow(
                rows = listOf(row),
                workspaces = listOf(workspace("owner")),
                capabilities = listOf(directionCapability("owner", "capability-owner")),
                mappings = listOf(directionMapping(row.id, "orientation-1")),
                subjects = listOf(directionSubject("orientation-1")),
                orientations = listOf(directionOrientation("orientation-1")),
                existingEntries = listOf(canonicalOnly),
                now = 100L,
            )

        assertTrue(plan.changes.isEmpty())
        assertEquals(
            "DIRECTION_ENTRY_ID_COLLISION",
            plan.issues.single().code,
        )
    }

    @Test
    fun `semantic title edit does not version placement but reorder does`() {
        val row =
            direction(
                id = "direction-1",
                contextId = "owner",
                text = "Renamed content",
                linkedContextId = null,
                order = 3,
                version = 9L,
            )

        val existing =
            entry(
                id = row.id,
                orientationId = "orientation-1",
                entryOrder = 3L,
                version = 4L,
            )

        val unchangedPlacement =
            planWorkspaceDirectionEntryShadow(
                rows = listOf(row),
                workspaces = listOf(workspace("owner")),
                capabilities = listOf(directionCapability("owner", "capability-owner")),
                mappings = listOf(directionMapping(row.id, "orientation-1")),
                subjects = listOf(directionSubject("orientation-1", title = "Renamed content")),
                orientations = listOf(directionOrientation("orientation-1")),
                existingEntries = listOf(existing),
                now = 100L,
            )

        assertTrue(unchangedPlacement.changes.isEmpty())

        val reordered =
            planWorkspaceDirectionEntryShadow(
                rows = listOf(row.copy(itemOrder = 8)),
                workspaces = listOf(workspace("owner")),
                capabilities = listOf(directionCapability("owner", "capability-owner")),
                mappings = listOf(directionMapping(row.id, "orientation-1")),
                subjects = listOf(directionSubject("orientation-1", title = "Renamed content")),
                orientations = listOf(directionOrientation("orientation-1")),
                existingEntries = listOf(existing),
                now = 101L,
            )

        val changed = reordered.changes.single()
        assertEquals(8L, changed.entryOrder)
        assertEquals(5L, changed.version)
        assertNull(changed.syncedAt)
    }

    @Test
    fun `disabled archived or tombstoned capability preserves stable placement identity`() {
        val row = direction(
            id = "direction-1",
            contextId = "owner",
            text = "Semantic",
            linkedContextId = null,
            order = 4,
        )

        listOf(
            directionCapability(
                workspaceId = "owner",
                id = "capability-owner",
                state = "DISABLED",
            ),
            directionCapability(
                workspaceId = "owner",
                id = "capability-owner",
                state = "ARCHIVED",
            ),
            directionCapability(
                workspaceId = "owner",
                id = "capability-owner",
                state = "ACTIVE",
                isDeleted = true,
            ),
        ).forEach { capability ->
            val plan =
                planWorkspaceDirectionEntryShadow(
                    rows = listOf(row),
                    workspaces = listOf(workspace("owner")),
                    capabilities = listOf(capability),
                    mappings = listOf(directionMapping(row.id, "orientation-1")),
                    subjects = listOf(directionSubject("orientation-1")),
                    orientations = listOf(directionOrientation("orientation-1")),
                    existingEntries = emptyList(),
                    now = 100L,
                )

            assertTrue(
                "Capability lifecycle metadata must preserve entry identity: $capability",
                plan.issues.isEmpty(),
            )
            assertEquals(
                "capability-owner",
                plan.changes.single().capabilityInstanceId,
            )
        }
    }

    @Test
    fun `deleted or absent legacy row tombstones only legacy-owned entry`() {
        val deletedRow =
            direction(
                id = "deleted",
                contextId = "owner",
                text = "Deleted",
                linkedContextId = null,
                order = 1,
            ).copy(isDeleted = true)

        val deletedShadow =
            entry(
                id = "deleted",
                version = 2L,
            )

        val canonicalOnly =
            entry(
                id = "canonical",
                provenance = WorkspaceDirectionEntryProvenance.CANONICAL_ONLY.name,
                version = 7L,
            )

        val deletedPlan =
            planWorkspaceDirectionEntryShadow(
                rows = listOf(deletedRow),
                workspaces = emptyList(),
                capabilities = emptyList(),
                mappings = emptyList(),
                subjects = emptyList(),
                orientations = emptyList(),
                existingEntries = listOf(deletedShadow, canonicalOnly),
                now = 500L,
            )

        val deletedChange = deletedPlan.changes.single()
        assertEquals("deleted", deletedChange.id)
        assertTrue(deletedChange.isDeleted)
        assertEquals(3L, deletedChange.version)
        assertFalse(deletedPlan.changes.any { it.id == "canonical" })

        val absentPlan =
            planWorkspaceDirectionEntryShadow(
                rows = emptyList(),
                workspaces = emptyList(),
                capabilities = emptyList(),
                mappings = emptyList(),
                subjects = emptyList(),
                orientations = emptyList(),
                existingEntries = listOf(deletedShadow, canonicalOnly),
                now = 600L,
            )

        val absentChange = absentPlan.changes.single()
        assertEquals("deleted", absentChange.id)
        assertTrue(absentChange.isDeleted)
        assertFalse(absentPlan.changes.any { it.id == "canonical" })
    }

    private fun direction(
        id: String,
        contextId: String,
        text: String,
        linkedContextId: String?,
        order: Int,
        version: Long = 1L,
    ) = DirectionItemEntity(
        id = id,
        contextId = contextId,
        text = text,
        linkedContextId = linkedContextId,
        itemOrder = order,
        updatedAt = 10L,
        version = version,
    )

    private fun workspace(
        id: String,
        sourceContextId: String = id,
    ) = WorkspaceEntity(
        id = id,
        nameOverride = id,
        descriptionOverride = null,
        parentWorkspaceId = null,
        roleCode = "default",
        workspaceOrder = 0L,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
        provenance = "CONTEXT_BACKED",
        sourceContextId = sourceContextId,
    )

    private fun directionCapability(
        workspaceId: String,
        id: String,
        state: String = "ACTIVE",
        isDeleted: Boolean = false,
    ) = WorkspaceCapabilityInstanceEntity(
        id = id,
        workspaceId = workspaceId,
        capabilityType = "DIRECTION",
        instanceKey = "default",
        capabilityOrder = 0L,
        state = state,
        configurationVersion = 1,
        configuration = """{"autoLinkChildWorkspaces":true}""",
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = isDeleted,
        version = 1L,
    )

    private fun directionMapping(
        sourceId: String,
        subjectId: String,
    ) = LegacySubjectMappingEntity(
        id = "mapping-$sourceId",
        sourceType = "DIRECTION",
        sourceId = sourceId,
        subjectId = subjectId,
        migrationVersion = 3,
        state = "MATERIALIZED",
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
    )

    private fun directionSubject(
        id: String,
        title: String = "Direction",
    ) = ManagedSubjectEntity(
        id = id,
        subjectType = "ORIENTATION",
        title = title,
        description = null,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
    )

    private fun directionOrientation(id: String) =
        OrientationEntity(
            subjectId = id,
            kind = "DIRECTION",
            lifecycle = null,
            lifecycleOrigin = "UNSET",
        )

    private fun entry(
        id: String,
        orientationId: String? = "orientation-1",
        entryOrder: Long = 1L,
        provenance: String = WorkspaceDirectionEntryProvenance.LEGACY_DIRECTION_ITEM.name,
        version: Long = 1L,
    ) = WorkspaceDirectionEntryEntity(
        id = id,
        workspaceId = "owner",
        capabilityInstanceId = "capability-owner",
        orientationId = orientationId,
        targetWorkspaceId = null,
        labelOverride = null,
        entryOrder = entryOrder,
        provenance = provenance,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = 1L,
        isDeleted = false,
        version = version,
    )
}
