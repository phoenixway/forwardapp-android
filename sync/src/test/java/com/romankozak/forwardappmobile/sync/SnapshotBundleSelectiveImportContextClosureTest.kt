package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextParentLinkSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSelectiveImportSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotBundleSelectiveImportContextClosureTest {
    private val filter = SnapshotBundleSelectiveImportFilter()

    @Test
    fun `context parent links are closed over selected contexts`() {
        val source =
            SnapshotBundle(
                version = 2,
                contexts =
                    listOf(
                        context("parent"),
                        context("child"),
                        context("unselected"),
                    ),
                contextParentLinks =
                    listOf(
                        parentLink("parent", "child"),
                        parentLink("parent", "unselected"),
                        parentLink("unselected", "child"),
                    ),
            )

        val filtered =
            filter.filter(
                source = source,
                selection =
                    WorkspaceSelectiveImportSelection(
                        selectedContextIds = setOf("parent", "child"),
                    ),
            )

        assertEquals(
            setOf("parent", "child"),
            filtered.contexts.mapTo(linkedSetOf()) { it.id },
        )
        assertEquals(
            listOf("parent" to "child"),
            filtered.contextParentLinks.map { it.parentContextId to it.childContextId },
        )
    }

    @Test
    fun `selecting only one endpoint imports no context parent link`() {
        val source =
            SnapshotBundle(
                version = 2,
                contexts =
                    listOf(
                        context("parent"),
                        context("child"),
                    ),
                contextParentLinks =
                    listOf(
                        parentLink("parent", "child"),
                    ),
            )

        val filtered =
            filter.filter(
                source = source,
                selection =
                    WorkspaceSelectiveImportSelection(
                        selectedContextIds = setOf("child"),
                    ),
            )

        assertEquals(listOf("child"), filtered.contexts.map { it.id })
        assertTrue(filtered.contextParentLinks.isEmpty())
    }

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
            tags = emptyList(),
            relatedLinks = emptyList(),
            order = 0,
            isAttachmentsExpanded = false,
            defaultViewModeName = "DIRECTION",
            isCompleted = false,
            isContextManagementEnabled = false,
            contextStatus = "NO_PLAN",
            contextStatusText = null,
            contextLogLevel = null,
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

    private fun parentLink(
        parentId: String,
        childId: String,
    ) = ContextParentLinkSnapshot(
        parentContextId = parentId,
        childContextId = childId,
        order = 0L,
        createdAt = 1L,
        updatedAt = 2L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
    )
}
