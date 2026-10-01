package com.romankozak.forwardappmobile.data.repository

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2HierarchyReadSnapshotAssembler
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ProductionHierarchyReadAdapter
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.toHierarchyPresentationNode
import com.romankozak.forwardappmobile.data.hierarchy.toCanonicalV2WorkspacePresentation
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.database.AppDatabase
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
                    placements =
                        listOf(
                            v2Placement("today-placement", todayId),
                            v2Placement(
                                "inbox-placement",
                                inboxId,
                                parentId = "today-placement",
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
                    contexts =
                        listOf(
                            context(canonicalRootId, "Canonical root"),
                            context(canonicalParentId, "Canonical parent"),
                        ),
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
                    placements =
                        listOf(
                            v2Placement("root-placement", canonicalRootId),
                            v2Placement(
                                "parent-placement",
                                canonicalParentId,
                                parentId = "root-placement",
                            ),
                            v2Placement(
                                "inbox-placement",
                                inboxId,
                                parentId = "parent-placement",
                            ),
                            v2Placement("today-placement", todayId, order = 1L),
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
                    placements =
                        listOf(
                            v2Placement("today-placement", todayId),
                            v2Placement(
                                "inbox-placement",
                                inboxId,
                                parentId = "today-placement",
                            ),
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
    fun `shell-free System search uses canonical H1 ordinary parent`() =
        runTest {
            val inboxId = SystemContexts.INBOX.raw
            val ordinaryParent = context(id = "ordinary-parent", name = "Ordinary parent")
            val repository =
                repository(
                    contexts = listOf(ordinaryParent),
                    workspaces =
                        listOf(
                            workspace(id = inboxId, name = "Canonical Inbox", parentId = ordinaryParent.id),
                        ),
                    placements =
                        listOf(
                            v2Placement("ordinary-parent-placement", ordinaryParent.id),
                            v2Placement(
                                "inbox-placement",
                                inboxId,
                                parentId = "ordinary-parent-placement",
                            ),
                        ),
                )

            assertThat(repository.contextResult("%Canonical Inbox%", inboxId).searchResult.pathSegments)
                .containsExactly("Ordinary parent", "Canonical Inbox")
                .inOrder()
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

    @Test
    fun `V2 global search resolves duplicate appearances through FIRST_VISIBLE not legacy parent`() =
        runTest {
            val rootA = context("v2-root-a", "Canonical A")
            val rootB = context("v2-root-b", "Canonical B")
            val target = context(
                id = "v2-shared",
                name = "Shared Workspace",
                parentId = rootB.id,
            )
            val repository = repository(
                contexts = listOf(rootA, rootB, target),
                workspaces = emptyList(),
            )
            repository.canonicalV2SearchReadProvider = { presentations ->
                CanonicalV2ProductionHierarchyReadAdapter().read(
                    placements = listOf(
                        v2Placement("v2-root-a-placement", rootA.id, order = 0L),
                        v2Placement(
                            "v2-shared-first-visible",
                            target.id,
                            parentId = "v2-root-a-placement",
                            kind = PlacementKind.LINK,
                            order = 0L,
                        ),
                        v2Placement("v2-root-b-placement", rootB.id, order = 1L),
                        v2Placement(
                            "v2-shared-primary",
                            target.id,
                            parentId = "v2-root-b-placement",
                            kind = PlacementKind.PRIMARY,
                            order = 0L,
                        ),
                    ),
                    admittedWorkspacePresentations =
                        presentations.map { it.toCanonicalV2WorkspacePresentation() },
                    managedSubjects = emptyList(),
                )
            }

            val result = repository.contextResult("%Shared Workspace%", target.id)
            assertThat(result.searchResult.pathSegments.takeLast(2))
                .containsExactly("Canonical A", "Shared Workspace").inOrder()
            assertThat(result.searchResult.presentation.parentId).isEqualTo(rootA.id)
            val subcontext = repository.searchGlobal("%Shared Workspace%")
                .filterIsInstance<GlobalSearchResultItem.SubcontextItem>()
                .single { it.searchResult.presentation.id == target.id }
            assertThat(subcontext.searchResult.parentContextId).isEqualTo(rootA.id)
            assertThat(subcontext.searchResult.parentContextName).isEqualTo("Canonical A")
            assertThat(subcontext.searchResult.pathSegments.takeLast(2))
                .containsExactly("Canonical A", "Shared Workspace").inOrder()
        }

    @Test
    fun `V2 global search never falls back to legacy ancestry when occurrence disappears`() =
        runTest {
            val parent = context("legacy-parent", "Legacy Parent")
            val orphan = context(
                id = "missing-v2-appearance",
                name = "Missing Canonical Appearance",
                parentId = parent.id,
            )
            val repository = repository(
                contexts = listOf(parent, orphan),
                workspaces = emptyList(),
            )
            repository.canonicalV2SearchReadProvider = { presentations ->
                CanonicalV2ProductionHierarchyReadAdapter().read(
                    placements = listOf(v2Placement("only-parent", parent.id)),
                    admittedWorkspacePresentations =
                        presentations.map { it.toCanonicalV2WorkspacePresentation() },
                    managedSubjects = emptyList(),
                )
            }

            assertThat(repository.contextResultIds("%Missing Canonical Appearance%"))
                .doesNotContain(orphan.id)
            assertThat(
                repository.searchGlobal("%Missing Canonical Appearance%")
                    .filterIsInstance<GlobalSearchResultItem.SubcontextItem>()
                    .map { it.searchResult.presentation.id },
            ).doesNotContain(orphan.id)
            assertThat(repository.contextResultIds("%Legacy Parent%")).contains(parent.id)
        }

    private fun v2Placement(
        placementId: String,
        workspaceId: String,
        parentId: String? = null,
        kind: PlacementKind = PlacementKind.PRIMARY,
        order: Long = 0L,
    ) = HierarchyPlacement(
        id = PlacementId(placementId),
        hierarchyId = HierarchyId.GENERAL,
        target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, workspaceId),
        parentPlacementId = parentId?.let(::PlacementId),
        placementKind = kind,
        siblingOrder = order,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
    )

    private fun repository(
        contexts: List<Context>,
        workspaces: List<WorkspaceEntity>,
        canonicalSystemTags: Map<String, List<String>> = emptyMap(),
        unavailableSystemTagIds: Set<String> = emptySet(),
        attachments: List<AttachmentLibraryQueryResult> = emptyList(),
        placements: List<HierarchyPlacement>? = null,
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
            database = mockk<AppDatabase>(relaxed = true),
            canonicalV2ReadAssembler = CanonicalV2HierarchyReadSnapshotAssembler(),
        ).also { repository ->
            repository.canonicalV2SearchReadProvider = { presentations ->
                CanonicalV2ProductionHierarchyReadAdapter().read(
                    placements =
                        placements ?: presentations.mapIndexed { index, presentation ->
                            v2Placement(
                                placementId = "test-root-${presentation.id}",
                                workspaceId = presentation.id,
                                order = index.toLong(),
                            )
                        },
                    admittedWorkspacePresentations =
                        presentations.map { it.toCanonicalV2WorkspacePresentation() },
                    managedSubjects = emptyList(),
                )
            }
        }
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
        roleCode = null,
        createdAt = 1L,
        updatedAt = updatedAt,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
        provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
        sourceContextId = null,
    )
}
