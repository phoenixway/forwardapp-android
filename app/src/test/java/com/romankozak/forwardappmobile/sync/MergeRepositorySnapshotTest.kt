package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.Context
import com.romankozak.forwardappmobile.core.data.models.sync.ChangeType
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.toSnapshot
import com.romankozak.forwardappmobile.sync.datasource.MergeLocalDataSource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context.ContextSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context.GoalSnapshot
import com.romankozak.forwardappmobile.core.data.models.Goal


class MergeRepositorySnapshotTest {

    private lateinit var mergeRepository: MergeRepository
    private val mockLocalDataSource: MergeLocalDataSource = mockk()
    private val syncLogicHelper: SyncLogicHelper = SyncLogicHelper()

    @Before
    fun setup() {
        mergeRepository = MergeRepository(mockLocalDataSource, syncLogicHelper)
    }

    @Test
    fun `createBackupDiff with new entity in incoming`() = runBlocking {
        // Given
        val localContext = Context(
            id = "c1",
            name = "Local Context",
            parentId = null,
            description = null,
            createdAt = 100L,
            updatedAt = 100L,
            isExpanded = true,
            isDeleted = false,
            version = 1,
            tags = emptyList(),
            relatedLinks = emptyList(),
            order = 0L,
            isAttachmentsExpanded = false,
            defaultViewModeName = null,
            isCompleted = false,
            isContextManagementEnabled = false,
            contextStatus = "NO_PLAN",
            contextStatusText = null,
            contextLogLevel = null,
            totalTimeSpentMinutes = null,
            valueImportance = 0f,
            valueImpact = 0f,
            effort = 0f,
            cost = 0f,
            risk = 0f,
            weightEffort = 1f,
            weightCost = 1f,
            weightRisk = 1f,
            rawScore = 0f,
            displayScore = 0,
            scoringStatus = "NOT_ASSESSED",
            showCheckboxes = false,
            roleCode = null
        )
        val incomingContext = ContextSnapshot(
            id = "c2",
            name = "New Context",
            parentId = null,
            description = null,
            createdAt = 200L,
            updatedAt = 200L,
            isExpanded = true,
            isDeleted = false,
            version = 1,
            tags = emptyList(),
            relatedLinks = emptyList(),
            order = 0L,
            isAttachmentsExpanded = false,
            defaultViewModeName = null,
            isCompleted = false,
            isContextManagementEnabled = false,
            contextStatus = "NO_PLAN",
            contextStatusText = null,
            contextLogLevel = null,
            totalTimeSpentMinutes = null,
            valueImportance = 0f,
            valueImpact = 0f,
            effort = 0f,
            cost = 0f,
            risk = 0f,
            weightEffort = 1f,
            weightCost = 1f,
            weightRisk = 1f,
            rawScore = 0f,
            displayScore = 0,
            scoringStatus = "NOT_ASSESSED",
            showCheckboxes = false,
            roleCode = null
        )

        coEvery { mockLocalDataSource.loadFullSnapshotBundle() } returns SnapshotBundle(
            contexts = listOf(localContext.toSnapshot())
        )

        val incomingBundle = SnapshotBundle(contexts = listOf(localContext.toSnapshot(), incomingContext))

        // When
        val diff = mergeRepository.createBackupDiff(incomingBundle)

        // Then
        assertThat(diff.projects.added).hasSize(1)
        assertThat(diff.projects.added.first().id).isEqualTo(incomingContext.id)
    }

    @Test
    fun `createBackupDiff with updated entity in incoming`() = runBlocking {
        // Given
        val localContext = Context(
            id = "c1",
            name = "Local Context",
            parentId = null,
            description = null,
            createdAt = 100L,
            updatedAt = 100L,
            isExpanded = true,
            isDeleted = false,
            version = 1,
            tags = emptyList(),
            relatedLinks = emptyList(),
            order = 0L,
            isAttachmentsExpanded = false,
            defaultViewModeName = null,
            isCompleted = false,
            isContextManagementEnabled = false,
            contextStatus = "NO_PLAN",
            contextStatusText = null,
            contextLogLevel = null,
            totalTimeSpentMinutes = null,
            valueImportance = 0f,
            valueImpact = 0f,
            effort = 0f,
            cost = 0f,
            risk = 0f,
            weightEffort = 1f,
            weightCost = 1f,
            weightRisk = 1f,
            rawScore = 0f,
            displayScore = 0,
            scoringStatus = "NOT_ASSESSED",
            showCheckboxes = false,
            roleCode = null
        )
        val incomingContext = ContextSnapshot(
            id = "c1",
            name = "Updated Context",
            parentId = null,
            description = null,
            createdAt = 100L,
            updatedAt = 200L,
            isExpanded = true,
            isDeleted = false,
            version = 2,
            tags = emptyList(),
            relatedLinks = emptyList(),
            order = 0L,
            isAttachmentsExpanded = false,
            defaultViewModeName = null,
            isCompleted = false,
            isContextManagementEnabled = false,
            contextStatus = "NO_PLAN",
            contextStatusText = null,
            contextLogLevel = null,
            totalTimeSpentMinutes = null,
            valueImportance = 0f,
            valueImpact = 0f,
            effort = 0f,
            cost = 0f,
            risk = 0f,
            weightEffort = 1f,
            weightCost = 1f,
            weightRisk = 1f,
            rawScore = 0f,
            displayScore = 0,
            scoringStatus = "NOT_ASSESSED",
            showCheckboxes = false,
            roleCode = null
        )

        coEvery { mockLocalDataSource.loadFullSnapshotBundle() } returns SnapshotBundle(
            contexts = listOf(localContext.toSnapshot())
        )

        val incomingBundle = SnapshotBundle(contexts = listOf(incomingContext))

        // When
        val diff = mergeRepository.createBackupDiff(incomingBundle)

        // Then
        assertThat(diff.projects.updated).hasSize(1)
        assertThat(diff.projects.updated.first().incoming.id).isEqualTo(incomingContext.id)
        assertThat(diff.projects.updated.first().incoming.name).isEqualTo(incomingContext.name)
        assertThat(diff.projects.updated.first().local.name).isEqualTo(localContext.name)
    }

    @Test
    fun `createSyncReport with new entity in incoming`() = runBlocking {
        // Given
        val localContext = Context(
            id = "c1",
            name = "Local Context",
            parentId = null,
            description = null,
            createdAt = 100L,
            updatedAt = 100L,
            isExpanded = true,
            isDeleted = false,
            version = 1,
            tags = emptyList(),
            relatedLinks = emptyList(),
            order = 0L,
            isAttachmentsExpanded = false,
            defaultViewModeName = null,
            isCompleted = false,
            isContextManagementEnabled = false,
            contextStatus = "NO_PLAN",
            contextStatusText = null,
            contextLogLevel = null,
            totalTimeSpentMinutes = null,
            valueImportance = 0f,
            valueImpact = 0f,
            effort = 0f,
            cost = 0f,
            risk = 0f,
            weightEffort = 1f,
            weightCost = 1f,
            weightRisk = 1f,
            rawScore = 0f,
            displayScore = 0,
            scoringStatus = "NOT_ASSESSED",
            showCheckboxes = false,
            roleCode = null
        )
        val incomingContext = ContextSnapshot(
            id = "c2",
            name = "New Context",
            parentId = null,
            description = null,
            createdAt = 200L,
            updatedAt = 200L,
            isExpanded = true,
            isDeleted = false,
            version = 1,
            tags = emptyList(),
            relatedLinks = emptyList(),
            order = 0L,
            isAttachmentsExpanded = false,
            defaultViewModeName = null,
            isCompleted = false,
            isContextManagementEnabled = false,
            contextStatus = "NO_PLAN",
            contextStatusText = null,
            contextLogLevel = null,
            totalTimeSpentMinutes = null,
            valueImportance = 0f,
            valueImpact = 0f,
            effort = 0f,
            cost = 0f,
            risk = 0f,
            weightEffort = 1f,
            weightCost = 1f,
            weightRisk = 1f,
            rawScore = 0f,
            displayScore = 0,
            scoringStatus = "NOT_ASSESSED",
            showCheckboxes = false,
            roleCode = null
        )

        coEvery { mockLocalDataSource.loadFullSnapshotBundle() } returns SnapshotBundle(
            contexts = listOf(localContext.toSnapshot())
        )

        val incomingBundle = SnapshotBundle(contexts = listOf(localContext.toSnapshot(), incomingContext))

        // When
        val report = mergeRepository.createSyncReport(incomingBundle)

        // Then
        assertThat(report.changes).hasSize(1)
        assertThat(report.changes.first().type).isEqualTo(ChangeType.Add)
        assertThat(report.changes.first().id).isEqualTo(incomingContext.id)
    }

    @Test
    fun `createSyncReport with updated entity in incoming`() = runBlocking {
        // Given
        val localContext = Context(
            id = "c1",
            name = "Local Context",
            parentId = null,
            description = null,
            createdAt = 100L,
            updatedAt = 100L,
            isExpanded = true,
            isDeleted = false,
            version = 1,
            tags = emptyList(),
            relatedLinks = emptyList(),
            order = 0L,
            isAttachmentsExpanded = false,
            defaultViewModeName = null,
            isCompleted = false,
            isContextManagementEnabled = false,
            contextStatus = "NO_PLAN",
            contextStatusText = null,
            contextLogLevel = null,
            totalTimeSpentMinutes = null,
            valueImportance = 0f,
            valueImpact = 0f,
            effort = 0f,
            cost = 0f,
            risk = 0f,
            weightEffort = 1f,
            weightCost = 1f,
            weightRisk = 1f,
            rawScore = 0f,
            displayScore = 0,
            scoringStatus = "NOT_ASSESSED",
            showCheckboxes = false,
            roleCode = null
        )
        val incomingContext = ContextSnapshot(
            id = "c1",
            name = "Updated Context",
            parentId = null,
            description = null,
            createdAt = 100L,
            updatedAt = 200L,
            isExpanded = true,
            isDeleted = false,
            version = 2,
            tags = emptyList(),
            relatedLinks = emptyList(),
            order = 0L,
            isAttachmentsExpanded = false,
            defaultViewModeName = null,
            isCompleted = false,
            isContextManagementEnabled = false,
            contextStatus = "NO_PLAN",
            contextStatusText = null,
            contextLogLevel = null,
            totalTimeSpentMinutes = null,
            valueImportance = 0f,
            valueImpact = 0f,
            effort = 0f,
            cost = 0f,
            risk = 0f,
            weightEffort = 1f,
            weightCost = 1f,
            weightRisk = 1f,
            rawScore = 0f,
            displayScore = 0,
            scoringStatus = "NOT_ASSESSED",
            showCheckboxes = false,
            roleCode = null
        )

        coEvery { mockLocalDataSource.loadFullSnapshotBundle() } returns SnapshotBundle(
            contexts = listOf(localContext.toSnapshot())
        )

        val incomingBundle = SnapshotBundle(contexts = listOf(incomingContext))

        // When
        val report = mergeRepository.createSyncReport(incomingBundle)

        // Then
        assertThat(report.changes).hasSize(1)
        assertThat(report.changes.first().type).isEqualTo(ChangeType.Update)
        assertThat(report.changes.first().id).isEqualTo(incomingContext.id)
    }

    // Add more tests for other entities and scenarios (e.g., deleted, no changes)
}
