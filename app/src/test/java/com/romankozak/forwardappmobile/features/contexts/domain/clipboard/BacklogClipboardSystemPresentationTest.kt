package com.romankozak.forwardappmobile.features.contexts.domain.clipboard

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagRepository
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BacklogClipboardSystemPresentationTest {
    @Test
    fun `live Context name uses canonical System presentation and fails closed without owner`() =
        runTest {
            val id = SystemContexts.INBOX.raw
            val contextRepository = mockk<ContextRepository>()
            val workspaceDao = mockk<WorkspaceDao>()
            val contextDao = mockk<ContextDao>()
            val tagAuthority = mockk<SystemWorkspaceTagAuthority>()
            val useCase =
                BacklogClipboardUseCase(
                    clipboardService = mockk(relaxed = true),
                    goalRepository = mockk(relaxed = true),
                    listItemRepository = mockk(relaxed = true),
                    backlogPlacementCommands = mockk(relaxed = true),
                    directionRepository = mockk(relaxed = true),
                    contextRepository = contextRepository,
                    checklistRepository = mockk(relaxed = true),
                    dayManagementRepository = mockk(relaxed = true),
                    missionRepository = mockk(relaxed = true),
                    inboxRepository = mockk(relaxed = true),
                    systemWorkspacePresentationContextProjector =
                        SystemWorkspacePresentationContextProjector(
                            workspaceDao = workspaceDao,
                            systemWorkspaceTagAuthority = tagAuthority,
                            canonicalWorkspaceTagRepository =
                                mockk<CanonicalWorkspaceTagRepository>(relaxed = true),
                            contextDao = contextDao,
                        ),
                )

            coEvery { contextDao.getContextById(id) } returns
                Context(
                    id = id,
                    name = "Stale Inbox",
                    description = null,
                    parentId = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                )
            coEvery { workspaceDao.getById(id) } returns
                WorkspaceEntity(
                    id = id,
                    nameOverride = "Canonical Inbox",
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

            coEvery { tagAuthority.resolve(id) } returns
                SystemWorkspaceTagAuthority.Resolution.Canonical(emptyList())

            assertEquals("Canonical Inbox", useCase.currentContextName(id))

            coEvery { workspaceDao.getById(id) } returns null

            assertEquals("", useCase.currentContextName(id))
        }
}
