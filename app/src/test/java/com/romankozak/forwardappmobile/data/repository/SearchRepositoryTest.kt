package com.romankozak.forwardappmobile.data.repository

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceAncestryPresentation
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagRepository
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.sync.AttachmentLibraryQueryResult
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SearchRepositoryTest {
    @Test
    fun `buildSafeActivityFtsQuery strips leading minus and keeps searchable token`() {
        val result = buildSafeActivityFtsQuery("%-model%")

        assertThat(result).isEqualTo("\"model\"")
    }

    @Test
    fun `buildSafeActivityFtsQuery returns null for punctuation only query`() {
        val result = buildSafeActivityFtsQuery("%---%")

        assertThat(result).isNull()
    }

    @Test
    fun `buildSafeActivityFtsQuery keeps multiple tokens as quoted terms`() {
        val result = buildSafeActivityFtsQuery("%foo-bar baz%")

        assertThat(result).isEqualTo("\"foo\" \"bar\" \"baz\"")
    }

    @Test
    fun `global Context search matches canonical System name and canonical parent path`() =
        runTest {
            val inboxId = SystemContexts.INBOX.raw
            val todayId = SystemContexts.TODAY.raw
            val repository =
                repository(
                    contexts =
                        listOf(
                            context(
                                id = inboxId,
                                name = "stale-inbox-name",
                                parentId = "stale-parent",
                            ),
                            context(
                                id = todayId,
                                name = "stale-today-name",
                            ),
                        ),
                    workspaces =
                        listOf(
                            workspace(
                                id = todayId,
                                name = "Canonical Today",
                            ),
                            workspace(
                                id = inboxId,
                                name = "Canonical Inbox",
                                parentId = todayId,
                            ),
                        ),
                )

            val canonicalResult =
                repository
                    .searchGlobal("%Canonical Inbox%")
                    .filterIsInstance<GlobalSearchResultItem.ContextItem>()
                    .single { it.searchResult.presentation.id == inboxId }

            assertThat(canonicalResult.searchResult.presentation.name).isEqualTo("Canonical Inbox")
            assertThat(canonicalResult.searchResult.pathSegments)
                .containsExactly("Canonical Today", "Canonical Inbox")
                .inOrder()

            val staleResultIds =
                repository
                    .searchGlobal("%stale-inbox-name%")
                    .filterIsInstance<GlobalSearchResultItem.ContextItem>()
                    .map { it.searchResult.presentation.id }

            assertThat(staleResultIds).doesNotContain(inboxId)
        }

    @Test
    fun `global Context search drops System shell when canonical owner is missing`() =
        runTest {
            val inboxId = SystemContexts.INBOX.raw
            val todayId = SystemContexts.TODAY.raw
            val repository =
                repository(
                    contexts =
                        listOf(
                            context(
                                id = inboxId,
                                name = "stale-inbox-name",
                                parentId = todayId,
                            ),
                            context(
                                id = todayId,
                                name = "stale-today-name",
                            ),
                        ),
                    workspaces =
                        listOf(
                            workspace(
                                id = todayId,
                                name = "Canonical Today",
                            ),
                        ),
                )

            val resultIds =
                repository
                    .searchGlobal("%stale-inbox-name%")
                    .filterIsInstance<GlobalSearchResultItem.ContextItem>()
                    .map { it.searchResult.presentation.id }

            assertThat(resultIds).doesNotContain(inboxId)
        }


    @Test
    fun `global Context search uses canonical System tags and ignores stale shell tags`() =
        runTest {
            val inboxId = SystemContexts.INBOX.raw
            val repository =
                repository(
                    contexts =
                        listOf(
                            context(
                                id = inboxId,
                                name = "stale-inbox-name",
                                tags = listOf("stale-shell-tag"),
                            ),
                        ),
                    workspaces =
                        listOf(
                            workspace(
                                id = inboxId,
                                name = "Canonical Inbox",
                            ),
                        ),
                    canonicalSystemTags =
                        mapOf(
                            inboxId to listOf("canonical-system-tag"),
                        ),
                )

            val canonicalIds =
                repository
                    .searchGlobal("%canonical-system-tag%")
                    .filterIsInstance<GlobalSearchResultItem.ContextItem>()
                    .map { it.searchResult.presentation.id }

            val staleIds =
                repository
                    .searchGlobal("%stale-shell-tag%")
                    .filterIsInstance<GlobalSearchResultItem.ContextItem>()
                    .map { it.searchResult.presentation.id }

            assertThat(canonicalIds).contains(inboxId)
            assertThat(staleIds).doesNotContain(inboxId)
        }

    @Test
    fun `global Context search maps ordinary Context to read-only presentation with legacy ranking timestamp`() =
        runTest {
            val ordinary =
                context(
                    id = "ordinary",
                    name = "Ordinary Context",
                    tags = listOf("tag-a"),
                    createdAt = 10L,
                    updatedAt = 20L,
                )
            val repository = repository(contexts = listOf(ordinary), workspaces = emptyList())

            val result =
                repository
                    .searchGlobal("%Ordinary Context%")
                    .filterIsInstance<GlobalSearchResultItem.ContextItem>()
                    .single()
                    .searchResult

            assertThat(result.presentation.id).isEqualTo(ordinary.id)
            assertThat(result.presentation.name).isEqualTo(ordinary.name)
            assertThat(result.presentation.description).isEqualTo(ordinary.description)
            assertThat(result.presentation.parentId).isEqualTo(ordinary.parentId)
            assertThat(result.presentation.tags).containsExactly("tag-a")
            assertThat(result.presentation.rankingTimestamp).isEqualTo(20L)
            assertThat(result.pathSegments).containsExactly("Ordinary Context")

            val createdOnly =
                context(
                    id = "created-only",
                    name = "Created Only",
                    createdAt = 30L,
                    updatedAt = null,
                )
            val createdOnlyResult =
                repository(contexts = listOf(createdOnly), workspaces = emptyList())
                    .searchGlobal("%Created Only%")
                    .filterIsInstance<GlobalSearchResultItem.ContextItem>()
                    .single()

            assertThat(createdOnlyResult.searchResult.presentation.rankingTimestamp).isEqualTo(30L)
        }

    @Test
    fun `shell-free System Workspace is searchable from canonical presentation and tags`() =
        runTest {
            val inboxId = SystemContexts.INBOX.raw
            val repository =
                repository(
                    contexts = emptyList(),
                    workspaces =
                        listOf(
                            workspace(
                                id = inboxId,
                                name = "Canonical Inbox",
                                description = "Canonical inbox description",
                                updatedAt = 42L,
                            ),
                        ),
                    canonicalSystemTags = mapOf(inboxId to listOf("canonical-tag")),
                )

            val byName = repository.contextResult("%Canonical Inbox%", inboxId)
            val byTag = repository.contextResult("%canonical-tag%", inboxId)

            assertThat(byName.searchResult.presentation.name).isEqualTo("Canonical Inbox")
            assertThat(byName.searchResult.presentation.description).isEqualTo("Canonical inbox description")
            assertThat(byName.searchResult.presentation.tags).containsExactly("canonical-tag")
            assertThat(byName.searchResult.presentation.rankingTimestamp).isEqualTo(42L)
            assertThat(byName.searchResult.pathSegments).containsExactly("Canonical Inbox")
            assertThat(byTag.searchResult.matchedTags).containsExactly("canonical-tag")
        }

    @Test
    fun `shell-free System paths traverse System and canonical Workspace ancestors`() =
        runTest {
            val inboxId = SystemContexts.INBOX.raw
            val todayId = SystemContexts.TODAY.raw
            val canonicalRootId = "canonical-root"
            val canonicalParentId = "canonical-parent"
            val repository =
                repository(
                    contexts = emptyList(),
                    workspaces =
                        listOf(
                            workspace(
                                id = inboxId,
                                name = "Canonical Inbox",
                                parentId = canonicalParentId,
                            ),
                            workspace(id = todayId, name = "Canonical Today"),
                            workspace(
                                id = canonicalParentId,
                                name = "Canonical parent",
                                parentId = canonicalRootId,
                            ),
                            workspace(id = canonicalRootId, name = "Canonical root"),
                        ),
                )

            val canonicalParentPath = repository.contextResult("%Canonical Inbox%", inboxId)
            assertThat(canonicalParentPath.searchResult.pathSegments)
                .containsExactly("Canonical root", "Canonical parent", "Canonical Inbox")
                .inOrder()
            val subcontextResult =
                repository
                    .searchGlobal("%Canonical Inbox%")
                    .filterIsInstance<GlobalSearchResultItem.SubcontextItem>()
                    .single { result -> result.searchResult.presentation.id == inboxId }
            assertThat(subcontextResult.searchResult.parentContextName).isEqualTo("Canonical parent")
            assertThat(subcontextResult.searchResult.pathSegments)
                .containsExactly("Canonical root", "Canonical parent", "Canonical Inbox")
                .inOrder()

            val systemChildRepository =
                repository(
                    contexts = emptyList(),
                    workspaces =
                        listOf(
                            workspace(id = inboxId, name = "Canonical Inbox", parentId = todayId),
                            workspace(id = todayId, name = "Canonical Today"),
                        ),
                )
            assertThat(systemChildRepository.contextResult("%Canonical Inbox%", inboxId).searchResult.pathSegments)
                .containsExactly("Canonical Today", "Canonical Inbox")
                .inOrder()
        }

    @Test
    fun `shell-free System attachment uses canonical owner label without Context history`() =
        runTest {
            val systemId = SystemContexts.INBOX.raw
            val repository =
                repository(
                    contexts = emptyList(),
                    workspaces = listOf(workspace(id = systemId, name = "Canonical System")),
                    attachments =
                        listOf(
                            AttachmentLibraryQueryResult(
                                id = "attachment-1",
                                entityId = "note-1",
                                attachmentType = "NOTE_DOCUMENT",
                                ownerContextId = systemId,
                                attachmentUpdatedAt = 7L,
                                noteName = "Document",
                                noteContent = "body",
                                musicNoteName = null,
                                musicNoteContent = null,
                                noteUpdatedAt = null,
                                checklistName = null,
                                checklistContent = null,
                                linkDisplayName = null,
                                linkTarget = null,
                                linkCreatedAt = null,
                                scriptName = null,
                                scriptDescription = null,
                                scriptContent = null,
                                contextName = "stale shell name",
                                contextUpdatedAt = 99L,
                            ),
                        ),
                )

            val result =
                repository
                    .searchGlobal("%Canonical System%")
                    .filterIsInstance<GlobalSearchResultItem.AttachmentItem>()
                    .single()
                    .searchResult

            assertThat(result.ownerContextId).isEqualTo(systemId)
            assertThat(result.contextName).isEqualTo("Canonical System")
            assertThat(result.updatedAt).isEqualTo(7L)
            assertThat(
                repository
                    .searchGlobal("%stale shell name%")
                    .filterIsInstance<GlobalSearchResultItem.AttachmentItem>(),
            ).isEmpty()
        }

    @Test
    fun `shell-free System search supports ordinary parent and fails closed on invalid ancestry`() =
        runTest {
            val inboxId = SystemContexts.INBOX.raw
            val ordinaryParent = context(id = "ordinary-parent", name = "Ordinary parent")
            val mixedRepository =
                repository(
                    contexts = listOf(ordinaryParent),
                    workspaces =
                        listOf(
                            workspace(id = inboxId, name = "Canonical Inbox", parentId = ordinaryParent.id),
                        ),
                )
            assertThat(mixedRepository.contextResult("%Canonical Inbox%", inboxId).searchResult.pathSegments)
                .containsExactly("Ordinary parent", "Canonical Inbox")
                .inOrder()

            val missingAncestor =
                repository(
                    contexts = emptyList(),
                    workspaces = listOf(workspace(id = inboxId, name = "Canonical Inbox", parentId = "missing")),
                )
            assertThat(missingAncestor.contextResultIds("%Canonical Inbox%")).doesNotContain(inboxId)

            val deletedAncestor =
                repository(
                    contexts = emptyList(),
                    workspaces =
                        listOf(
                            workspace(id = inboxId, name = "Canonical Inbox", parentId = "deleted"),
                            workspace(id = "deleted", name = "Deleted").copy(isDeleted = true),
                        ),
                )
            assertThat(deletedAncestor.contextResultIds("%Canonical Inbox%")).doesNotContain(inboxId)

            val malformedAncestor =
                repository(
                    contexts = emptyList(),
                    workspaces =
                        listOf(
                            workspace(id = inboxId, name = "Canonical Inbox", parentId = "malformed"),
                            workspace(id = "malformed", name = "Malformed").copy(sourceContextId = "legacy"),
                        ),
                )
            assertThat(malformedAncestor.contextResultIds("%Canonical Inbox%")).doesNotContain(inboxId)

            val cycle =
                repository(
                    contexts = emptyList(),
                    workspaces =
                        listOf(
                            workspace(id = inboxId, name = "Canonical Inbox", parentId = "canonical-parent"),
                            workspace(
                                id = "canonical-parent",
                                name = "Canonical parent",
                                parentId = inboxId,
                            ),
                        ),
                )
            assertThat(cycle.contextResultIds("%Canonical Inbox%")).doesNotContain(inboxId)
        }

    @Test
    fun `shell-free System canonical empty tags do not fall back to stale Context tags`() =
        runTest {
            val inboxId = SystemContexts.INBOX.raw
            val repository =
                repository(
                    contexts = listOf(context(id = inboxId, name = "Stale Inbox", tags = listOf("stale-shell-tag"))),
                    workspaces = listOf(workspace(id = inboxId, name = "Canonical Inbox")),
                    canonicalSystemTags = mapOf(inboxId to emptyList()),
                )

            val result = repository.contextResult("%Canonical Inbox%", inboxId)

            assertThat(result.searchResult.presentation.tags).isEmpty()
            assertThat(repository.contextResultIds("%stale-shell-tag%")).doesNotContain(inboxId)
        }

    @Test
    fun `invalid System canonical ownership and unavailable tags fail closed without stale shell fallback`() =
        runTest {
            val inboxId = SystemContexts.INBOX.raw
            val staleShell = context(id = inboxId, name = "Stale Inbox")

            val deletedOwner =
                repository(
                    contexts = listOf(staleShell),
                    workspaces = listOf(workspace(id = inboxId, name = "Canonical Inbox").copy(isDeleted = true)),
                )
            val malformedOwner =
                repository(
                    contexts = listOf(staleShell),
                    workspaces =
                        listOf(
                            workspace(id = inboxId, name = "Canonical Inbox").copy(sourceContextId = "legacy"),
                        ),
                )
            val unavailableTags =
                repository(
                    contexts = listOf(staleShell),
                    workspaces = listOf(workspace(id = inboxId, name = "Canonical Inbox")),
                    unavailableSystemTagIds = setOf(inboxId),
                )

            assertThat(deletedOwner.contextResultIds("%Stale Inbox%")).doesNotContain(inboxId)
            assertThat(malformedOwner.contextResultIds("%Stale Inbox%")).doesNotContain(inboxId)
            assertThat(unavailableTags.contextResultIds("%Stale Inbox%")).doesNotContain(inboxId)
        }

    @Test
    fun `non-reserved sys shaped Context remains ordinary search data`() =
        runTest {
            val ordinary = context(id = "sys_custom", name = "Ordinary sys Context")
            val repository = repository(contexts = listOf(ordinary), workspaces = emptyList())

            val result = repository.contextResult("%Ordinary sys Context%", ordinary.id)

            assertThat(result.searchResult.presentation.id).isEqualTo(ordinary.id)
            assertThat(result.searchResult.presentation.name).isEqualTo(ordinary.name)
        }

    private fun repository(
        contexts: List<Context>,
        workspaces: List<WorkspaceEntity>,
        canonicalSystemTags: Map<String, List<String>> = emptyMap(),
        unavailableSystemTagIds: Set<String> = emptySet(),
        attachments: List<AttachmentLibraryQueryResult> = emptyList(),
    ): SearchRepository {
        val contextDao = mockk<ContextDao>()
        coEvery { contextDao.getAllRaw() } returns contexts

        val workspaceDao = mockk<WorkspaceDao>()
        coEvery { workspaceDao.getAll() } returns workspaces

        val attachmentsRepository = mockk<AttachmentsRepository>()
        every { attachmentsRepository.getAttachmentLibraryItems() } returns flowOf(attachments)

        val systemWorkspaceTagAuthority = mockk<SystemWorkspaceTagAuthority>()
        coEvery { systemWorkspaceTagAuthority.resolve(any()) } coAnswers {
            val contextId = firstArg<String>()
            if (contextId in unavailableSystemTagIds) {
                SystemWorkspaceTagAuthority.Resolution.Unavailable
            } else {
                SystemWorkspaceTagAuthority.Resolution.Canonical(
                    canonicalSystemTags[contextId] ?: emptyList(),
                )
            }
        }
        val canonicalWorkspaceRepository = mockk<CanonicalWorkspaceRepository>()
        coEvery { canonicalWorkspaceRepository.getLiveCanonicalAncestryPresentation(any()) } coAnswers {
            workspaces
                .firstOrNull { workspace -> workspace.id == firstArg<String>() }
                ?.takeIf { workspace ->
                    !workspace.isDeleted &&
                        workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                        workspace.sourceContextId == null &&
                        !workspace.nameOverride.isNullOrBlank()
                }?.let { workspace ->
                    CanonicalWorkspaceAncestryPresentation(
                        id = workspace.id,
                        name = requireNotNull(workspace.nameOverride),
                        parentWorkspaceId = workspace.parentWorkspaceId,
                    )
                }
        }

        return SearchRepository(
            goalDao = mockk(relaxed = true),
            contextDao = contextDao,
            listItemRepository = mockk(relaxed = true),
            linkItemDao = mockk(relaxed = true),
            activityRepository = mockk(relaxed = true),
            inboxRecordDao = mockk(relaxed = true),
            attachmentsRepository = attachmentsRepository,
            systemWorkspacePresentationContextProjector =
                SystemWorkspacePresentationContextProjector(
                    workspaceDao = workspaceDao,
                    systemWorkspaceTagAuthority = systemWorkspaceTagAuthority,
                    canonicalWorkspaceTagRepository =
                        mockk<CanonicalWorkspaceTagRepository>(relaxed = true).also { repository ->
                            coEvery { repository.getLiveTagsByWorkspace() } returns emptyMap()
                            coEvery { repository.getTags(any()) } returns emptyList()
                        },
                    contextDao = contextDao,
                ),
            workspaceDao = workspaceDao,
            canonicalWorkspaceRepository = canonicalWorkspaceRepository,
        )
    }

    private suspend fun SearchRepository.contextResult(
        query: String,
        contextId: String,
    ): GlobalSearchResultItem.ContextItem =
        searchGlobal(query)
            .filterIsInstance<GlobalSearchResultItem.ContextItem>()
            .single { result -> result.searchResult.presentation.id == contextId }

    private suspend fun SearchRepository.contextResultIds(query: String): List<String> =
        searchGlobal(query)
            .filterIsInstance<GlobalSearchResultItem.ContextItem>()
            .map { result -> result.searchResult.presentation.id }

    private fun context(
        id: String,
        name: String,
        parentId: String? = null,
        tags: List<String>? = null,
        createdAt: Long = 1L,
        updatedAt: Long? = 1L,
    ) = Context(
        id = id,
        name = name,
        description = null,
        parentId = parentId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        tags = tags,
    )

    private fun workspace(
        id: String,
        name: String,
        description: String? = null,
        parentId: String? = null,
        updatedAt: Long = 1L,
    ) = WorkspaceEntity(
        id = id,
        nameOverride = name,
        descriptionOverride = description,
        parentWorkspaceId = parentId,
        roleCode = null,
        workspaceOrder = 0L,
        createdAt = 1L,
        updatedAt = updatedAt,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
        provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
        sourceContextId = null,
    )
}
