package com.romankozak.forwardappmobile.sync

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceSnapshot
import com.romankozak.forwardappmobile.sync.datasource.FullBackupLocalDataSource
import com.romankozak.forwardappmobile.sync.datasource.MergeLocalDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class MergeRepositorySnapshotTest {
    private lateinit var mergeRepository: MergeRepository
    private lateinit var mockMergeLocalDataSource: MergeLocalDataSource
    private val mockLocalDataSource: FullBackupLocalDataSource = mockk()
    private val syncLogicHelper: SyncLogicHelper = SyncLogicHelper()

    @Before
    fun setup() {
        mockMergeLocalDataSource = mockk()
        mergeRepository = MergeRepository(mockMergeLocalDataSource, mockLocalDataSource, syncLogicHelper)
    }

    @Test
    fun `selective snapshot import uses selective merge boundary only`() = runBlocking {
        val bundle = SnapshotBundle(version = 2)
        coEvery {
            mockMergeLocalDataSource.applySelectiveSnapshotBundle(bundle)
        } returns Unit

        val result = mergeRepository.importSelectedSnapshotBundle(bundle)

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) {
            mockMergeLocalDataSource.applySelectiveSnapshotBundle(bundle)
        }
        coVerify(exactly = 0) {
            mockMergeLocalDataSource.applySnapshotBundle(any())
        }
    }

    @Test
    fun `createBackupDiff reports new canonical Workspace`() =
        runBlocking {
            val localWorkspace =
                WorkspaceSnapshot(
                    id = "w1",
                    nameOverride = "Local Workspace",
                    descriptionOverride = null,
                    parentWorkspaceId = null,
                    roleCode = null,
                    workspaceOrder = 0L,
                    createdAt = 100L,
                    updatedAt = 100L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                    provenance = "CANONICAL_ONLY",
                    sourceContextId = null,
                )
            val incomingWorkspace =
                WorkspaceSnapshot(
                    id = "w2",
                    nameOverride = "New Workspace",
                    descriptionOverride = null,
                    parentWorkspaceId = null,
                    roleCode = null,
                    workspaceOrder = 0L,
                    createdAt = 200L,
                    updatedAt = 200L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                    provenance = "CANONICAL_ONLY",
                    sourceContextId = null,
                )

            coEvery { mockLocalDataSource.loadFullSnapshotBundle() } returns
                SnapshotBundle(workspaces = listOf(localWorkspace))

            val incomingBundle =
                SnapshotBundle(workspaces = listOf(localWorkspace, incomingWorkspace))

            val diff = mergeRepository.createBackupDiff(incomingBundle)

            assertThat(diff.projects.added).hasSize(1)
            assertThat(diff.projects.added.first().id).isEqualTo(incomingWorkspace.id)
            assertThat(diff.projects.added.first().nameOverride)
                .isEqualTo(incomingWorkspace.nameOverride)
        }

    @Test
    fun `createBackupDiff reports updated canonical Workspace`() =
        runBlocking {
            val localWorkspace =
                WorkspaceSnapshot(
                    id = "w1",
                    nameOverride = "Local Workspace",
                    descriptionOverride = null,
                    parentWorkspaceId = null,
                    roleCode = null,
                    workspaceOrder = 0L,
                    createdAt = 100L,
                    updatedAt = 100L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                    provenance = "CANONICAL_ONLY",
                    sourceContextId = null,
                )
            val incomingWorkspace =
                localWorkspace.copy(
                    nameOverride = "Updated Workspace",
                    updatedAt = 200L,
                    version = 2L,
                )

            coEvery { mockLocalDataSource.loadFullSnapshotBundle() } returns
                SnapshotBundle(workspaces = listOf(localWorkspace))

            val incomingBundle =
                SnapshotBundle(workspaces = listOf(incomingWorkspace))

            val diff = mergeRepository.createBackupDiff(incomingBundle)

            assertThat(diff.projects.updated).hasSize(1)
            assertThat(diff.projects.updated.first().incoming.id)
                .isEqualTo(incomingWorkspace.id)
            assertThat(diff.projects.updated.first().incoming.nameOverride)
                .isEqualTo(incomingWorkspace.nameOverride)
            assertThat(diff.projects.updated.first().local.nameOverride)
                .isEqualTo(localWorkspace.nameOverride)
        }

    @Test
    fun `createSyncReport excludes new Context from legacy approval changes`() =
        runBlocking {
            // Given
            val localContext =
                Context(
                    id = "c1",
                    name = "Local Context",
                    parentId = null,
                    description = null,
                    createdAt = 100L,
                    updatedAt = 100L,
                    isExpanded = true,
                    isDeleted = false,
                    version = 1L,
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
                    totalTimeSpentMinutes = 0L,
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
                    roleCode = null,
                )
            val incomingContext =
                ContextSnapshot(
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
                    order = 0,
                    isAttachmentsExpanded = false,
                    defaultViewModeName = null,
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

            coEvery { mockLocalDataSource.loadFullSnapshotBundle() } returns
                SnapshotBundle(
                    contexts = listOf(localContext.toSnapshot()),
                )

            val incomingBundle = SnapshotBundle(contexts = listOf(localContext.toSnapshot(), incomingContext))

            // When
            val report = mergeRepository.createSyncReport(incomingBundle)

            // Then
            assertThat(report.changes).isEmpty()
        }

    @Test
    fun `createSyncReport excludes updated Context from legacy approval changes`() =
        runBlocking {
            // Given
            val localContext =
                Context(
                    id = "c1",
                    name = "Local Context",
                    parentId = null,
                    description = null,
                    createdAt = 100L,
                    updatedAt = 100L,
                    isExpanded = true,
                    isDeleted = false,
                    version = 1L,
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
                    totalTimeSpentMinutes = 0L,
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
                    roleCode = null,
                )
            val incomingContext =
                ContextSnapshot(
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
                    order = 0,
                    isAttachmentsExpanded = false,
                    defaultViewModeName = null,
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

            coEvery { mockLocalDataSource.loadFullSnapshotBundle() } returns
                SnapshotBundle(
                    contexts = listOf(localContext.toSnapshot()),
                )

            val incomingBundle = SnapshotBundle(contexts = listOf(incomingContext))

            // When
            val report = mergeRepository.createSyncReport(incomingBundle)

            // Then
            assertThat(report.changes).isEmpty()
        }

    // Add more tests for other entities and scenarios (e.g., deleted, no changes)
}
