package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSelectiveImportSelection
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SnapshotBundleSelectiveImportRetirementClosureTest {
    private val filter = SnapshotBundleSelectiveImportFilter()

    @Test
    fun `selected Context keeps same-id canonical Workspace retirement evidence`() {
        val source =
            SnapshotBundle(
                version = 2,
                contexts = listOf(context("retired-context")),
                workspaces =
                    listOf(
                        canonicalWorkspace("retired-context"),
                        canonicalWorkspace("other-workspace"),
                    ),
            )

        val filtered =
            filter.filter(
                source = source,
                selection =
                    WorkspaceSelectiveImportSelection(
                        selectedContextIds = setOf("retired-context"),
                    ),
            )

        assertEquals(listOf("retired-context"), filtered.contexts.map { it.id })

        val retirementWorkspace =
            filtered.workspaces.orEmpty().singleOrNull {
                it.id == "retired-context"
            }

        assertNotNull(retirementWorkspace)
        assertEquals(
            WorkspaceProvenance.CANONICAL_ONLY.name,
            retirementWorkspace!!.provenance,
        )
        assertEquals(null, retirementWorkspace.sourceContextId)
    }

    private fun canonicalWorkspace(id: String) =
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
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
            sourceContextId = null,
        )

    private fun context(id: String) =
        ContextSnapshot(
            id = id,
            name = id,
            parentId = null,
            description = null,
            createdAt = 1L,
            updatedAt = 2L,
            isExpanded = false,
            isDeleted = false,
            version = 1L,
            contextStatus = "NO_PLAN",
            contextLogLevel = null,
            isContextManagementEnabled = false,
            contextStatusText = null,
            tags = emptyList(),
            relatedLinks = emptyList(),
            order = 0,
            isAttachmentsExpanded = false,
            defaultViewModeName = "DIRECTION",
            isCompleted = false,
            totalTimeSpentMinutes = 0L,
            valueImportance = 0,
            valueImpact = 0,
            effort = 0,
            cost = 0,
            risk = 0,
            weightEffort = 1f,
            weightCost = 1f,
            weightRisk = 1f,
            rawScore = 0.0,
            displayScore = 0.0,
            scoringStatus = "NOT_ASSESSED",
            showCheckboxes = false,
            roleCode = null,
        )
}
