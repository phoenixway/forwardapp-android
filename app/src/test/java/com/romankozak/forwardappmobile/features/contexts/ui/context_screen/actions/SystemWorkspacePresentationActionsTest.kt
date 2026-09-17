package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.DirectionRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagRepository
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SystemWorkspacePresentationActionsTest {
    @Test
    fun `direction chooser writes canonical System name instead of stale shell name`() =
        runTest {
            val id = SystemContexts.INBOX.raw
            val contextRepository = mockk<ContextRepository>()
            val directionRepository = mockk<DirectionRepository>(relaxed = true)
            val workspaceDao = mockk<WorkspaceDao>()
            val contextDao = mockk<ContextDao>()
            val tagAuthority = mockk<SystemWorkspaceTagAuthority>()
            coEvery { contextDao.getContextById(id) } returns context(id, "stale-inbox")
            coEvery { workspaceDao.getById(id) } returns workspace(id, "Canonical Inbox")
            coEvery { tagAuthority.resolve(id) } returns
                SystemWorkspaceTagAuthority.Resolution.Canonical(emptyList())
            val actions =
                ListChooserFlowActions(
                    contextRepository = contextRepository,
                    directionRepository = directionRepository,
                    systemWorkspacePresentationContextProjector =
                        SystemWorkspacePresentationContextProjector(
                            workspaceDao = workspaceDao,
                            systemWorkspaceTagAuthority = tagAuthority,
                            canonicalWorkspaceTagRepository =
                                mockk<CanonicalWorkspaceTagRepository>(relaxed = true),
                            contextDao = contextDao,
                        ),
                )

            actions.addDirectionLinkedToContext(
                targetContextId = id,
                currentContextId = "owner",
            )

            coVerify(exactly = 1) {
                directionRepository.addDirectionItem(
                    contextId = "owner",
                    text = "Canonical Inbox",
                    linkedContextId = id,
                )
            }
        }

    @Test
    fun `malformed System owner never writes stale shell name into direction`() =
        runTest {
            val id = SystemContexts.INBOX.raw
            val contextRepository = mockk<ContextRepository>()
            val directionRepository = mockk<DirectionRepository>(relaxed = true)
            val workspaceDao = mockk<WorkspaceDao>()
            val contextDao = mockk<ContextDao>()
            coEvery { contextDao.getContextById(id) } returns context(id, "stale-inbox")
            coEvery { workspaceDao.getById(id) } returns null
            val actions =
                ListChooserFlowActions(
                    contextRepository = contextRepository,
                    directionRepository = directionRepository,
                    systemWorkspacePresentationContextProjector =
                        SystemWorkspacePresentationContextProjector(
                            workspaceDao = workspaceDao,
                            systemWorkspaceTagAuthority = mockk(relaxed = true),
                            canonicalWorkspaceTagRepository =
                                mockk<CanonicalWorkspaceTagRepository>(relaxed = true),
                            contextDao = contextDao,
                        ),
                )

            actions.addDirectionLinkedToContext(
                targetContextId = id,
                currentContextId = "owner",
            )

            coVerify(exactly = 1) {
                directionRepository.addDirectionItem(
                    contextId = "owner",
                    text = "Context",
                    linkedContextId = id,
                )
            }
        }

    private fun context(id: String, name: String) =
        Context(
            id = id,
            name = name,
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = 2L,
        )

    private fun workspace(id: String, name: String) =
        WorkspaceEntity(
            id = id,
            nameOverride = name,
            descriptionOverride = null,
            parentWorkspaceId = null,
            roleCode = null,
            workspaceOrder = 0L,
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
            sourceContextId = null,
        )
}
