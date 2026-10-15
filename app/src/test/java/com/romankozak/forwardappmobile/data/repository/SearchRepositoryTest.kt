package com.romankozak.forwardappmobile.data.repository

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalGoalSearchResult
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBacklogEntryEntity
import com.romankozak.forwardappmobile.data.workspace.WorkspaceBacklogEntryDao
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import com.romankozak.forwardappmobile.data.dao.LegacyNoteDao
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.product.CanonicalProductReadRepository
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
import com.romankozak.forwardappmobile.shared.core.domain.product.CanonicalOrientationProductProjection
import com.romankozak.forwardappmobile.shared.core.domain.product.CanonicalProductReadResult
import com.romankozak.forwardappmobile.shared.core.domain.product.CanonicalProductReadViolation
import com.romankozak.forwardappmobile.shared.core.domain.product.HierarchyAppearanceProjection
import com.romankozak.forwardappmobile.shared.core.domain.product.PlanningMembershipProjection
import com.romankozak.forwardappmobile.shared.core.domain.product.WorkspaceMembershipProjection
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagRepository
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubject
import com.romankozak.forwardappmobile.shared.core.models.orientation.Workspace
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBinding
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
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

    @Test
    fun `canonical semantic search matches Orientation title and description`() =
        runTest {
            val repository =
                repository(
                    contexts = emptyList(),
                    workspaces = emptyList(),
                    productRead =
                        CanonicalProductReadResult.Ready(
                            listOf(
                                semanticOrientation(
                                    id = "orientation-title",
                                    title = "Canonical North Star",
                                    description = "Other text",
                                ),
                                semanticOrientation(
                                    id = "orientation-description",
                                    title = "Different title",
                                    description = "Contains semantic needle",
                                ),
                            ),
                        ),
                )

            val titleResults =
                repository.searchGlobal("North Star")
                    .filterIsInstance<GlobalSearchResultItem.OrientationItem>()
            val descriptionResults =
                repository.searchGlobal("semantic needle")
                    .filterIsInstance<GlobalSearchResultItem.OrientationItem>()

            assertThat(titleResults.map { it.orientationId })
                .containsExactly("orientation-title")
            assertThat(descriptionResults.map { it.orientationId })
                .containsExactly("orientation-description")
        }

    @Test
    fun `multi-scope multi-workspace multi-hierarchy Orientation stays one semantic result`() =
        runTest {
            val repository =
                repository(
                    contexts = emptyList(),
                    workspaces = emptyList(),
                    productRead =
                        CanonicalProductReadResult.Ready(
                            listOf(
                                semanticOrientation(
                                    id = "orientation-one",
                                    title = "One semantic identity",
                                    description = null,
                                    planningMembershipCount = 3,
                                    workspaceDecorations =
                                        listOf(
                                            "workspace-a" to "Workspace A",
                                            "workspace-b" to "Workspace B",
                                        ),
                                    hierarchyAppearanceCount = 4,
                                ),
                            ),
                        ),
                )

            val results =
                repository.searchGlobal("semantic identity")
                    .filterIsInstance<GlobalSearchResultItem.OrientationItem>()

            assertThat(results).hasSize(1)
            assertThat(results.single().orientationId).isEqualTo("orientation-one")
        }

    @Test
    fun `CUT_OVER Goal duplicate is suppressed by canonical Orientation`() =
        runTest {
            val contextId = "goal-context"
            val goalId = "legacy-goal"
            val orientationId = "orientation-goal"

            val goal = mockk<Goal>(relaxed = true)
            every { goal.id } returns goalId
            every { goal.text } returns "Canonical semantic goal"

            val goalResult = mockk<GlobalGoalSearchResult>()
            every { goalResult.goal } returns goal
            every { goalResult.contextId } returns contextId

            val backlogItem = mockk<BacklogItem>(relaxed = true)
            every { backlogItem.contextId } returns contextId

            val repository =
                repository(
                    contexts = listOf(context(contextId, "Goal workspace")),
                    workspaces = listOf(workspace(contextId, "Goal workspace")),
                    productRead =
                        CanonicalProductReadResult.Ready(
                            listOf(
                                semanticOrientation(
                                    id = orientationId,
                                    title = "Canonical semantic goal",
                                    description = null,
                                ),
                            ),
                        ),
                    legacyMappings =
                        listOf(
                            cutOverMapping(
                                sourceType = LegacyOrientationSourceType.GOAL,
                                sourceId = goalId,
                                subjectId = orientationId,
                            ),
                        ),
                    goalResults = listOf(goalResult),
                    goalBacklogItems = mapOf(goalId to backlogItem),
                )

            val results = repository.searchGlobal("Canonical semantic goal")

            assertThat(
                results.filterIsInstance<GlobalSearchResultItem.OrientationItem>()
                    .map { it.orientationId },
            ).containsExactly(orientationId)

            assertThat(
                results.filterIsInstance<GlobalSearchResultItem.GoalItem>()
                    .map { it.goal.id },
            ).doesNotContain(goalId)
        }

    @Test
    fun `CUT_OVER Context duplicate is suppressed by canonical Orientation`() =
        runTest {
            val contextId = "legacy-context"
            val orientationId = "orientation-context"
            val repository =
                repository(
                    contexts = listOf(context(contextId, "Canonical semantic context")),
                    workspaces = listOf(workspace(contextId, "Canonical semantic context")),
                    productRead =
                        CanonicalProductReadResult.Ready(
                            listOf(
                                semanticOrientation(
                                    id = orientationId,
                                    title = "Canonical semantic context",
                                    description = null,
                                ),
                            ),
                        ),
                    legacyMappings =
                        listOf(
                            cutOverMapping(
                                sourceType = LegacyOrientationSourceType.CONTEXT,
                                sourceId = contextId,
                                subjectId = orientationId,
                            ),
                        ),
                )

            val results = repository.searchGlobal("Canonical semantic context")

            assertThat(
                results.filterIsInstance<GlobalSearchResultItem.OrientationItem>()
                    .map { it.orientationId },
            ).containsExactly(orientationId)

            assertThat(
                results.filterIsInstance<GlobalSearchResultItem.ContextItem>()
                    .map { it.searchResult.presentation.id },
            ).doesNotContain(contextId)

            assertThat(
                results.filterIsInstance<GlobalSearchResultItem.SubcontextItem>()
                    .map { it.searchResult.presentation.id },
            ).doesNotContain(contextId)
        }

    @Test
    fun `canonical integrity failure does not resurrect mapped Context`() =
        runTest {
            val contextId = "legacy-context"
            val repository =
                repository(
                    contexts = listOf(context(contextId, "Legacy fallback candidate")),
                    workspaces = listOf(workspace(contextId, "Legacy fallback candidate")),
                    attachments =
                        listOf(
                            AttachmentLibraryQueryResult(
                                id = "specialized-attachment",
                                entityId = "note-specialized",
                                attachmentType = "NOTE_DOCUMENT",
                                ownerContextId = null,
                                attachmentUpdatedAt = 7L,
                                noteName = "Legacy fallback candidate attachment",
                                noteContent = "specialized content",
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
                                contextName = null,
                                contextUpdatedAt = null,
                            ),
                        ),
                    productRead =
                        CanonicalProductReadResult.IntegrityFailure(
                            listOf(
                                CanonicalProductReadViolation(
                                    path = "search",
                                    code = "BROKEN_CANONICAL",
                                    message = "forced",
                                ),
                            ),
                        ),
                    legacyMappings =
                        listOf(
                            cutOverMapping(
                                sourceType = LegacyOrientationSourceType.CONTEXT,
                                sourceId = contextId,
                                subjectId = "orientation-context",
                            ),
                        ),
                )

            val results = repository.searchGlobal("Legacy fallback candidate")

            assertThat(
                results.filterIsInstance<GlobalSearchResultItem.OrientationItem>(),
            ).isEmpty()

            assertThat(
                results.filterIsInstance<GlobalSearchResultItem.ContextItem>()
                    .map { it.searchResult.presentation.id },
            ).doesNotContain(contextId)

            assertThat(
                results.filterIsInstance<GlobalSearchResultItem.AttachmentItem>()
                    .map { it.searchResult.attachmentId },
            ).containsExactly("specialized-attachment")
        }

    @Test
    fun `tombstoned canonical semantic node has no legacy resurrection`() =
        runTest {
            val contextId = "tombstoned-context"
            val repository =
                repository(
                    contexts = listOf(context(contextId, "Tombstoned semantic")),
                    workspaces = listOf(workspace(contextId, "Tombstoned semantic")),
                    // Tombstoned semantic subjects are excluded by the canonical
                    // product projector, so the live product frame is empty.
                    productRead = CanonicalProductReadResult.Ready(emptyList()),
                    legacyMappings =
                        listOf(
                            cutOverMapping(
                                sourceType = LegacyOrientationSourceType.CONTEXT,
                                sourceId = contextId,
                                subjectId = "tombstoned-orientation",
                            ),
                        ),
                )

            val results = repository.searchGlobal("Tombstoned semantic")

            assertThat(
                results.filterIsInstance<GlobalSearchResultItem.OrientationItem>(),
            ).isEmpty()

            assertThat(
                results.filterIsInstance<GlobalSearchResultItem.ContextItem>()
                    .map { it.searchResult.presentation.id },
            ).doesNotContain(contextId)
        }

    @Test
    fun `standalone Notes match title and content without Context ownership`() =
        runTest {
            val repository =
                repository(
                    contexts = emptyList(),
                    workspaces = emptyList(),
                    notes =
                        listOf(
                            LegacyNoteEntity(
                                id = "standalone-title",
                                title = "Aurora title",
                                content = "First body",
                            ),
                            LegacyNoteEntity(
                                id = "standalone-content",
                                title = "Second title",
                                content = "Aurora body",
                            ),
                        ),
                )

            val notes =
                repository.searchGlobal("%Aurora%")
                    .filterIsInstance<GlobalSearchResultItem.NoteItem>()

            assertThat(notes.map { it.uniqueId })
                .containsExactly(
                    "note_standalone-title",
                    "note_standalone-content",
                )
            assertThat(notes.map { it.note.id }.toSet()).hasSize(2)
        }

    @Test
    fun `Note search excludes tombstones but does not require historical Context`() =
        runTest {
            val repository =
                repository(
                    contexts = emptyList(),
                    workspaces = emptyList(),
                    notes =
                        listOf(
                            LegacyNoteEntity(
                                id = "orphan-note",
                                contextId = "missing-historical-context",
                                title = "Signal live",
                                content = "Body",
                            ),
                            LegacyNoteEntity(
                                id = "deleted-note",
                                title = "Signal deleted",
                                content = "Body",
                                isDeleted = true,
                            ),
                        ),
                )

            val notes =
                repository.searchGlobal("%Signal%")
                    .filterIsInstance<GlobalSearchResultItem.NoteItem>()

            assertThat(notes.map { it.note.id })
                .containsExactly("orphan-note")
            assertThat(notes.single().uniqueId).isEqualTo("note_orphan-note")
        }

    @Test
    fun `structured Note search matches content hashtags independently`() =
        runTest {
            val repository =
                repository(
                    contexts = emptyList(),
                    workspaces = emptyList(),
                    notes =
                        listOf(
                            LegacyNoteEntity(
                                id = "tagged-note",
                                title = "Daily observations",
                                content = "Aurora #field research",
                            ),
                            LegacyNoteEntity(
                                id = "wrong-tag-note",
                                title = "Aurora observations",
                                content = "#other research",
                            ),
                        ),
                )

            val notes =
                repository.searchGlobal("%Aurora #field%")
                    .filterIsInstance<GlobalSearchResultItem.NoteItem>()

            assertThat(notes.map { it.note.id })
                .containsExactly("tagged-note")
            assertThat(notes.single().matchedTags)
                .containsExactly("field")
        }

    @Test
    fun `standalone Note search has one identity across zero one and many BACKLOG placements`() =
        runTest {
            val note =
                LegacyNoteEntity(
                    id = "placement-independent-note",
                    contextId = "historical-context-only",
                    title = "Delta field log",
                    content = "Observations #field",
                    updatedAt = 120L,
                )
            val canonicalEntries =
                (1..3).map { number ->
                    WorkspaceBacklogEntryEntity(
                        id = "entry-$number",
                        workspaceId = "workspace-$number",
                        capabilityInstanceId = "backlog-instance-$number",
                        targetKind = WorkspaceBacklogTargetKind.LEGACY_NOTE.name,
                        targetId = note.id,
                        entryOrder = 0L,
                        createdAt = 100L,
                        updatedAt = 100L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    )
                }

            for (placementCount in listOf(0, 1, 3)) {
                val repository =
                    repository(
                        contexts = emptyList(),
                        workspaces = emptyList(),
                        notes = listOf(note),
                        noteBacklogEntries = canonicalEntries.take(placementCount),
                    )

                val ordinary =
                    repository.searchGlobal("%Delta%")
                        .filterIsInstance<GlobalSearchResultItem.NoteItem>()

                val structured =
                    repository.searchGlobal("%Delta #field%")
                        .filterIsInstance<GlobalSearchResultItem.NoteItem>()

                assertThat(ordinary.map { it.uniqueId })
                    .containsExactly("note_${note.id}")
                assertThat(structured.map { it.uniqueId })
                    .containsExactly("note_${note.id}")

                assertThat(ordinary.single().note.id).isEqualTo(note.id)
                assertThat(structured.single().note.id).isEqualTo(note.id)
                assertThat(structured.single().matchedTags)
                    .containsExactly("field")
            }
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
        notes: List<LegacyNoteEntity> = emptyList(),
        noteBacklogEntries: List<WorkspaceBacklogEntryEntity> = emptyList(),
        placements: List<HierarchyPlacement>? = null,
        productRead: CanonicalProductReadResult =
            CanonicalProductReadResult.Ready(emptyList()),
        legacyMappings: List<LegacySubjectMappingEntity> = emptyList(),
        goalResults: List<GlobalGoalSearchResult> = emptyList(),
        goalBacklogItems: Map<String, BacklogItem> = emptyMap(),
    ): SearchRepository {
        val contextDao = mockk<ContextDao>()
        coEvery { contextDao.getAllRaw() } returns contexts

        val workspaceDao = mockk<WorkspaceDao>()
        coEvery { workspaceDao.getAll() } returns workspaces

        val attachmentsRepository = mockk<AttachmentsRepository>()
        every { attachmentsRepository.getAttachmentLibraryItems() } returns flowOf(attachments)

        val goalDao = mockk<GoalDao>()
        coEvery { goalDao.searchGoalsGlobal(any()) } returns goalResults

        val listItemRepository = mockk<ListItemRepository>(relaxed = true)
        coEvery {
            listItemRepository.getRuntimeItemForEntityInContext(
                entityId = any(),
                itemType = any(),
                contextId = any(),
            )
        } coAnswers {
            goalBacklogItems[firstArg<String>()]
        }

        val orientationDao = mockk<OrientationDao>()
        coEvery { orientationDao.getAllLegacyMappings() } returns legacyMappings

        val database = mockk<AppDatabase>(relaxed = true)
        every { database.orientationDao() } returns orientationDao

        val legacyNoteDao = mockk<LegacyNoteDao>()
        every { database.legacyNoteDao() } returns legacyNoteDao
        coEvery { legacyNoteDao.getAllRaw() } returns notes

        val noteBacklogDao = mockk<WorkspaceBacklogEntryDao>()
        every { database.workspaceBacklogEntryDao() } returns noteBacklogDao
        coEvery {
            noteBacklogDao.getLiveByTarget(
                WorkspaceBacklogTargetKind.LEGACY_NOTE.name,
                any(),
            )
        } returns noteBacklogEntries
        coEvery { noteBacklogDao.getAll() } returns noteBacklogEntries

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
            contextDao = contextDao,
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
            database = database,
            canonicalV2ReadAssembler = CanonicalV2HierarchyReadSnapshotAssembler(),
            canonicalProductReadRepository =
                mockk<CanonicalProductReadRepository>(relaxed = true),
        ).also { repository ->
            repository.canonicalProductSearchReadProvider = { productRead }
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

    private fun semanticOrientation(
        id: String,
        title: String,
        description: String?,
        planningMembershipCount: Int = 0,
        workspaceDecorations: List<Pair<String, String>> = emptyList(),
        hierarchyAppearanceCount: Int = 0,
    ): CanonicalOrientationProductProjection {
        val subject = mockk<ManagedSubject>()
        every { subject.id } returns id
        every { subject.title } returns title
        every { subject.description } returns description
        every { subject.updatedAt } returns 100L

        val workspaceMemberships =
            workspaceDecorations.mapIndexed { index, (workspaceId, workspaceName) ->
                val workspace = mockk<Workspace>()
                every { workspace.id } returns workspaceId
                every { workspace.nameOverride } returns workspaceName

                val binding = mockk<WorkspaceBinding>()
                every { binding.bindingType } returns WorkspaceBindingType.EMBODIES
                every { binding.isPrimary } returns (index == 0)

                val membership = mockk<WorkspaceMembershipProjection>()
                every { membership.workspace } returns workspace
                every { membership.binding } returns binding
                membership
            }

        val projection = mockk<CanonicalOrientationProductProjection>()
        every { projection.subject } returns subject
        every { projection.planningMemberships } returns
            List(planningMembershipCount) {
                mockk<PlanningMembershipProjection>(relaxed = true)
            }
        every { projection.workspaceMemberships } returns workspaceMemberships
        every { projection.hierarchyAppearances } returns
            List(hierarchyAppearanceCount) {
                mockk<HierarchyAppearanceProjection>(relaxed = true)
            }
        return projection
    }

    private fun cutOverMapping(
        sourceType: LegacyOrientationSourceType,
        sourceId: String,
        subjectId: String,
    ) = LegacySubjectMappingEntity(
        id = "mapping-${sourceType.name}-$sourceId",
        sourceType = sourceType.name,
        sourceId = sourceId,
        subjectId = subjectId,
        migrationVersion = 1,
        state = LegacySubjectMappingState.CUT_OVER.name,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
    )

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
