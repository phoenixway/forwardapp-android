package com.romankozak.forwardappmobile.data.workspace

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.coVerify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemWorkspacePresentationContextProjectorTest {
    @Test
    fun `owner labels preserve active authority and resolve retired ordinary canonical Workspace`() =
        runTest {
            val systemId = SystemContexts.INBOX.raw
            val ordinary = context("ordinary", "ordinary")
            val retiredId = "retired-ordinary-owner"
            val pureWorkspaceId = "pure-shell-free-workspace"
            val retiredTombstone =
                context(retiredId, "stale retired").copy(isDeleted = true)

            val dao = mockk<WorkspaceDao>()
            val contextDao = mockk<ContextDao>()
            val liveContexts = MutableStateFlow(listOf(ordinary))

            every { dao.observeAll() } returns
                MutableStateFlow(
                    listOf(
                        workspace(systemId, "canonical inbox"),
                        workspace(ordinary.id, "must-not-hijack-active"),
                        workspace(retiredId, "canonical retired"),
                        workspace(pureWorkspaceId, "must-not-become-project"),
                    ),
                )
            every { contextDao.getAllContextsFlow() } returns
                MutableStateFlow(listOf(ordinary, retiredTombstone))

            val labels =
                projector(
                    dao = dao,
                    contextDao = contextDao,
                ).observeOwnerLabels(liveContexts)
                    .first()

            assertEquals("canonical inbox", labels[systemId])
            assertEquals("ordinary", labels[ordinary.id])
            assertEquals("canonical retired", labels[retiredId])
            assertNull(labels[pureWorkspaceId])
        }

    @Test
    fun `presentation universe synthesizes exact canonical System Workspace without Context shell`() =
        runTest {
            val systemId = SystemContexts.INBOX.raw
            val ordinary = context("ordinary", "ordinary", parentId = "legacy-parent", order = 7L)
            val workspace =
                workspace(
                    id = systemId,
                    name = "Canonical Inbox",
                    parentId = SystemContexts.TODAY.raw,
                    order = 3L,
                )
            val dao = mockk<WorkspaceDao>()
            val tagAuthority = mockk<SystemWorkspaceTagAuthority>()
            coEvery { dao.getAll() } returns listOf(workspace)
            coEvery { dao.getById(systemId) } returns workspace
            coEvery { tagAuthority.resolve(systemId) } returns
                SystemWorkspaceTagAuthority.Resolution.Canonical(listOf("focus", "inbox"))
            val projector = projector(dao, tagAuthority)

            val universe = projector.projectPresentationUniverse(listOf(ordinary))
            val synthesized = requireNotNull(universe.singleOrNull { it.id == systemId })
            val resolved = requireNotNull(projector.resolvePresentation(systemId, context = null))

            assertEquals("Canonical Inbox", synthesized.name)
            assertEquals("canonical-description", synthesized.description)
            assertEquals(SystemContexts.TODAY.raw, synthesized.parentId)
            assertEquals("canonical-role", synthesized.roleCode)
            assertEquals(3L, synthesized.order)
            assertEquals(listOf("focus", "inbox"), synthesized.tags)
            assertEquals(synthesized, resolved)
            assertEquals(
                listOf(ordinary.id, systemId),
                universe.map { it.id },
            )
            coVerify(exactly = 0) { dao.upsert(any()) }
        }

    @Test
    fun `presentation universe keeps canonical empty tags and ordinary sys prefix Context unchanged`() =
        runTest {
            val systemId = SystemContexts.INBOX.raw
            val prefixOnly = context("sys_not_reserved", "ordinary sys")
            val workspace = workspace(systemId, "Canonical Inbox")
            val dao = mockk<WorkspaceDao>()
            val tagAuthority = mockk<SystemWorkspaceTagAuthority>()
            coEvery { dao.getAll() } returns listOf(workspace)
            coEvery { dao.getById("sys_not_reserved_missing") } returns null
            coEvery { tagAuthority.resolve(systemId) } returns
                SystemWorkspaceTagAuthority.Resolution.Canonical(emptyList())
            val projector = projector(dao, tagAuthority)

            val universe = projector.projectPresentationUniverse(listOf(prefixOnly))

            assertEquals(prefixOnly.name, requireNotNull(universe.singleOrNull { it.id == prefixOnly.id }).name)
            assertEquals(prefixOnly.tags, requireNotNull(universe.singleOrNull { it.id == prefixOnly.id }).tags)
            assertEquals(emptyList<String>(), requireNotNull(universe.singleOrNull { it.id == systemId }).tags)
            assertNull(projector.resolvePresentation("sys_not_reserved_missing", context = null))
        }

    @Test
    fun `presentation universe excludes arbitrary canonical nonSystem Workspace`() =
        runTest {
            val systemId = SystemContexts.INBOX.raw
            val nonSystemWorkspaceId = "workspace-only"
            val systemWorkspace = workspace(systemId, "Canonical Inbox")
            val dao = mockk<WorkspaceDao>()
            val tagAuthority = mockk<SystemWorkspaceTagAuthority>()
            coEvery { dao.getAll() } returns
                listOf(
                    systemWorkspace,
                    workspace(nonSystemWorkspaceId, "Not a Context hierarchy node"),
                )
            coEvery { tagAuthority.resolve(systemId) } returns
                SystemWorkspaceTagAuthority.Resolution.Canonical(emptyList())

            val universe = projector(dao, tagAuthority).projectPresentationUniverse(emptyList())

            assertEquals(listOf(systemId), universe.map { it.id })
        }

    @Test
    fun `presentation universe admits standalone operational Workspace without Context shell`() =
        runTest {
            val standaloneId = "workspace-created-by-user"
            val standalone =
                workspace(
                    id = standaloneId,
                    name = "Operations",
                    parentId = "parent-workspace",
                    order = 7L,
                    provenance = WorkspaceProvenance.STANDALONE,
                )
            val dao = mockk<WorkspaceDao>()
            val canonicalTags = mockk<CanonicalWorkspaceTagRepository>()
            coEvery { dao.getAll() } returns listOf(standalone)
            coEvery { dao.getById(standaloneId) } returns standalone
            coEvery { canonicalTags.getLiveTagsByWorkspace() } returns
                mapOf(standaloneId to listOf("operations"))
            coEvery { canonicalTags.getTags(standaloneId) } returns listOf("operations")

            val projector =
                projector(
                    dao = dao,
                    canonicalTagRepository = canonicalTags,
                )

            val presentation = requireNotNull(projector.projectPresentationUniverse(emptyList()).singleOrNull())

            assertEquals(standaloneId, presentation.id)
            assertEquals("Operations", presentation.name)
            assertEquals("parent-workspace", presentation.parentId)
            assertEquals(7L, presentation.order)
            assertEquals(listOf("operations"), presentation.tags)
            assertEquals(presentation, projector.resolvePresentation(standaloneId, context = null))
        }

    @Test
    fun `retired ordinary canonical Workspace is presentation owner but pure Workspace is excluded`() =
        runTest {
            val retiredId = "retired-project"
            val pureWorkspaceId = "workspace-only"
            val retiredTombstone =
                context(
                    id = retiredId,
                    name = "stale legacy title",
                    parentId = "stale-parent",
                    order = 99L,
                ).copy(
                    isDeleted = true,
                    description = "stale legacy description",
                    roleCode = "stale-role",
                    tags = listOf("stale-tag"),
                )

            val retiredWorkspace =
                workspace(
                    id = retiredId,
                    name = "Canonical project",
                    parentId = "canonical-parent",
                    order = 4L,
                )
            val pureWorkspace =
                workspace(
                    id = pureWorkspaceId,
                    name = "Pure workspace",
                )

            val dao = mockk<WorkspaceDao>()
            coEvery { dao.getAll() } returns listOf(retiredWorkspace, pureWorkspace)
            coEvery { dao.getById(retiredId) } returns retiredWorkspace
            coEvery { dao.getById(pureWorkspaceId) } returns pureWorkspace

            val contextDao = mockk<ContextDao>()
            coEvery { contextDao.getAllRaw() } returns listOf(retiredTombstone)
            coEvery { contextDao.getContextById(retiredId) } returns retiredTombstone
            coEvery { contextDao.getContextById(pureWorkspaceId) } returns null

            val canonicalTags = mockk<CanonicalWorkspaceTagRepository>()
            coEvery { canonicalTags.getLiveTagsByWorkspace() } returns
                mapOf(
                    retiredId to listOf("canonical-tag"),
                    pureWorkspaceId to listOf("workspace-tag"),
                )
            coEvery { canonicalTags.getTags(retiredId) } returns listOf("canonical-tag")
            coEvery { canonicalTags.getTags(pureWorkspaceId) } returns listOf("workspace-tag")

            val projector =
                projector(
                    dao = dao,
                    contextDao = contextDao,
                    canonicalTagRepository = canonicalTags,
                )

            val universe = projector.projectPresentationUniverse(emptyList())
            val retired = requireNotNull(universe.singleOrNull { it.id == retiredId })

            assertEquals(listOf(retiredId), universe.map { it.id })
            assertEquals("Canonical project", retired.name)
            assertEquals("canonical-description", retired.description)
            assertEquals("canonical-parent", retired.parentId)
            assertEquals("canonical-role", retired.roleCode)
            assertEquals(4L, retired.order)
            assertEquals(listOf("canonical-tag"), retired.tags)

            assertEquals(
                retired,
                projector.resolvePresentation(retiredId, context = null),
            )
            assertNull(
                projector.resolvePresentation(pureWorkspaceId, context = null),
            )
        }

    @Test
    fun `reactive presentation universe uses ordinary retirement evidence without reading tombstone fields`() =
        runTest {
            val retiredId = "retired-reactive"
            val pureWorkspaceId = "pure-reactive"
            val activeContexts = MutableStateFlow(emptyList<Context>())
            val allContexts =
                MutableStateFlow(
                    listOf(
                        context(retiredId, "stale tombstone").copy(
                            isDeleted = true,
                            tags = listOf("stale-tag"),
                        ),
                    ),
                )
            val workspaces =
                MutableStateFlow(
                    listOf(
                        workspace(retiredId, "Canonical reactive", order = 8L),
                        workspace(pureWorkspaceId, "Pure reactive"),
                    ),
                )

            val dao = mockk<WorkspaceDao>()
            every { dao.observeAll() } returns workspaces

            val contextDao = mockk<ContextDao>()
            every { contextDao.getAllContextsFlow() } returns allContexts

            val canonicalTags = mockk<CanonicalWorkspaceTagRepository>()
            every { canonicalTags.observeLiveTagsByWorkspace() } returns
                MutableStateFlow(
                    mapOf(
                        retiredId to listOf("canonical-reactive"),
                        pureWorkspaceId to listOf("pure-tag"),
                    ),
                )

            val tagAuthority = mockk<SystemWorkspaceTagAuthority>()
            every { tagAuthority.observeEffectiveOwners(activeContexts) } returns
                MutableStateFlow(emptyList())

            val universe =
                projector(
                    dao = dao,
                    tagAuthority = tagAuthority,
                    contextDao = contextDao,
                    canonicalTagRepository = canonicalTags,
                ).observePresentationUniverse(activeContexts)
                    .first()

            assertEquals(listOf(retiredId), universe.map { it.id })
            val retired = universe.single()
            assertEquals("Canonical reactive", retired.name)
            assertEquals(8L, retired.order)
            assertEquals(listOf("canonical-reactive"), retired.tags)
        }

    @Test
    fun `presentation universe fails closed for unavailable System ownership and scoped APIs stay shell-only`() =
        runTest {
            val canonicalId = SystemContexts.INBOX.raw
            val contextBackedId = SystemContexts.TODAY.raw
            val deletedId = SystemContexts.WEEK.raw
            val malformedId = SystemContexts.STRATEGIC_REVIEW.raw
            val missingId = SystemContexts.STRATEGIC_INBOX.raw
            val legacyBacked = context(contextBackedId, "legacy backed")
            val dao = mockk<WorkspaceDao>()
            val tagAuthority = mockk<SystemWorkspaceTagAuthority>()
            val canonical = workspace(canonicalId, "canonical")
            coEvery { dao.getAll() } returns
                listOf(
                    canonical,
                    workspace(
                        contextBackedId,
                        "ignored",
                        provenance = WorkspaceProvenance.CONTEXT_BACKED,
                        sourceContextId = contextBackedId,
                    ),
                    workspace(deletedId, "deleted", isDeleted = true),
                    workspace(malformedId, "malformed", sourceContextId = malformedId),
                )
            coEvery { dao.getById(canonicalId) } returns canonical
            coEvery { dao.getById(deletedId) } returns workspace(deletedId, "deleted", isDeleted = true)
            coEvery { dao.getById(malformedId) } returns
                workspace(malformedId, "malformed", sourceContextId = malformedId)
            coEvery { dao.getById(missingId) } returns null
            coEvery { tagAuthority.resolve(canonicalId) } returns SystemWorkspaceTagAuthority.Resolution.Unavailable
            every { dao.observeAll() } returns MutableStateFlow(listOf(canonical))
            val projector = projector(dao, tagAuthority)

            val universe = projector.projectPresentationUniverse(listOf(legacyBacked))

            assertEquals(listOf(contextBackedId), universe.map { it.id })
            assertNull(projector.resolvePresentation(canonicalId, context = null))
            assertNull(projector.resolvePresentation(missingId, context = null))
            assertNull(projector.resolvePresentation(deletedId, context = null))
            assertNull(projector.resolvePresentation(malformedId, context = null))
        }

    @Test
    fun `id-only resolver preserves active ordinary Context compatibility owner`() =
        runTest {
            val id = "active-ordinary"
            val active = context(id, "Active ordinary")
            val dao = mockk<WorkspaceDao>()
            val contextDao = mockk<ContextDao>()

            coEvery { contextDao.getContextById(id) } returns active
            coEvery { dao.getById(id) } returns null

            val presentation =
                projector(
                    dao = dao,
                    contextDao = contextDao,
                ).resolvePresentation(id)

            assertEquals(id, presentation?.id)
            assertEquals("Active ordinary", presentation?.name)
        }

    @Test
    fun `id-only resolver uses canonical Workspace fields for retired ordinary Context`() =
        runTest {
            val id = "retired-ordinary"
            val tombstone =
                context(id, "Stale tombstone").copy(
                    isDeleted = true,
                    tags = listOf("stale-tag"),
                )
            val dao = mockk<WorkspaceDao>()
            val contextDao = mockk<ContextDao>()
            val canonicalTags = mockk<CanonicalWorkspaceTagRepository>()

            coEvery { contextDao.getContextById(id) } returns tombstone
            coEvery { dao.getById(id) } returns workspace(id, "Canonical retired")
            coEvery { canonicalTags.getTags(id) } returns listOf("canonical-tag")

            val presentation =
                projector(
                    dao = dao,
                    contextDao = contextDao,
                    canonicalTagRepository = canonicalTags,
                ).resolvePresentation(id)

            assertEquals(id, presentation?.id)
            assertEquals("Canonical retired", presentation?.name)
            assertEquals(listOf("canonical-tag"), presentation?.tags)
        }

    @Test
    fun `id-only resolver ignores stale System shell fields behind canonical owner`() =
        runTest {
            val id = SystemContexts.INBOX.raw
            val stale = context(id, "stale-inbox")
            val dao = mockk<WorkspaceDao>()
            val contextDao = mockk<ContextDao>()
            val tagAuthority = mockk<SystemWorkspaceTagAuthority>()

            coEvery { contextDao.getContextById(id) } returns stale
            coEvery { dao.getById(id) } returns workspace(id, "Canonical Inbox")
            coEvery { tagAuthority.resolve(id) } returns
                SystemWorkspaceTagAuthority.Resolution.Canonical(listOf("canonical-tag"))

            val presentation =
                projector(
                    dao = dao,
                    tagAuthority = tagAuthority,
                    contextDao = contextDao,
                ).resolvePresentation(id)

            assertEquals(id, presentation?.id)
            assertEquals("Canonical Inbox", presentation?.name)
            assertEquals(listOf("canonical-tag"), presentation?.tags)
        }

    private fun projector(
        dao: WorkspaceDao,
        tagAuthority: SystemWorkspaceTagAuthority = mockk(relaxed = true),
        contextDao: ContextDao? = null,
        canonicalTagRepository: CanonicalWorkspaceTagRepository? = null,
    ): SystemWorkspacePresentationContextProjector {
        val resolvedContextDao =
            contextDao
                ?: mockk<ContextDao>().also { mock ->
                    coEvery { mock.getAllRaw() } returns emptyList()
                    coEvery { mock.getContextById(any()) } returns null
                    every { mock.getAllContextsFlow() } returns
                        MutableStateFlow(emptyList())
                }

        val resolvedCanonicalTagRepository =
            canonicalTagRepository
                ?: mockk<CanonicalWorkspaceTagRepository>().also { mock ->
                    coEvery { mock.getLiveTagsByWorkspace() } returns emptyMap()
                    coEvery { mock.getTags(any()) } returns emptyList()
                    every { mock.observeLiveTagsByWorkspace() } returns
                        MutableStateFlow(emptyMap())
                }

        return SystemWorkspacePresentationContextProjector(
            workspaceDao = dao,
            systemWorkspaceTagAuthority = tagAuthority,
            canonicalWorkspaceTagRepository = resolvedCanonicalTagRepository,
            contextDao = resolvedContextDao,
        )
    }

    @Test
    fun `reactive presentation universe includes shell-free canonical System and preserves ordinary sys prefix`() =
        runTest {
            val systemId = SystemContexts.INBOX.raw
            val prefixOnly = context("sys_not_reserved", "ordinary sys")
            val contexts = MutableStateFlow(listOf(prefixOnly))
            val workspaces =
                MutableStateFlow(
                    listOf(
                        workspace(
                            id = systemId,
                            name = "Canonical Inbox",
                            parentId = SystemContexts.TODAY.raw,
                            order = 4L,
                        ),
                    ),
                )
            val dao = mockk<WorkspaceDao>()
            every { dao.observeAll() } returns workspaces
            val tagAuthority = mockk<SystemWorkspaceTagAuthority>()
            every { tagAuthority.observeEffectiveOwners(contexts) } returns
                MutableStateFlow(
                    listOf(
                        SystemWorkspaceTagAuthority.TagOwner(
                            id = prefixOnly.id,
                            tags = prefixOnly.tags.orEmpty(),
                        ),
                        SystemWorkspaceTagAuthority.TagOwner(
                            id = systemId,
                            tags = listOf("focus", "inbox"),
                        ),
                    ),
                )

            val universe =
                projector(dao, tagAuthority)
                    .observePresentationUniverse(contexts)
                    .first()

            assertEquals(
                listOf(prefixOnly.id, systemId),
                universe.map { it.id },
            )
            assertEquals(
                "ordinary sys",
                requireNotNull(universe.singleOrNull { it.id == prefixOnly.id }).name,
            )
            val system = requireNotNull(universe.singleOrNull { it.id == systemId })
            assertEquals("Canonical Inbox", system.name)
            assertEquals(SystemContexts.TODAY.raw, system.parentId)
            assertEquals(4L, system.order)
            assertEquals(listOf("focus", "inbox"), system.tags)
        }

    @Test
    fun `reactive presentation universe fails closed when canonical System tags are unavailable`() =
        runTest {
            val systemId = SystemContexts.INBOX.raw
            val contexts = MutableStateFlow(emptyList<Context>())
            val workspace = workspace(systemId, "Canonical Inbox")
            val dao = mockk<WorkspaceDao>()
            every { dao.observeAll() } returns MutableStateFlow(listOf(workspace))
            val tagAuthority = mockk<SystemWorkspaceTagAuthority>()
            every { tagAuthority.observeEffectiveOwners(contexts) } returns
                MutableStateFlow(emptyList())

            val universe =
                projector(dao, tagAuthority)
                    .observePresentationUniverse(contexts)
                    .first()

            assertNull(universe.singleOrNull { it.id == systemId })
        }

    private fun context(
        id: String,
        name: String,
        parentId: String? = null,
        order: Long = 0L,
    ) = Context(
        id = id,
        name = name,
        description = "legacy-description",
        parentId = parentId,
        createdAt = 1L,
        updatedAt = 2L,
        order = order,
        roleCode = "legacy-role",
    )

    private fun workspace(
        id: String,
        name: String?,
        parentId: String? = null,
        order: Long = 0L,
        provenance: WorkspaceProvenance = WorkspaceProvenance.CANONICAL_ONLY,
        sourceContextId: String? = null,
        isDeleted: Boolean = false,
    ) = WorkspaceEntity(
        id = id,
        nameOverride = name,
        descriptionOverride = "canonical-description",
        parentWorkspaceId = parentId,
        roleCode = "canonical-role",
        workspaceOrder = order,
        createdAt = 1L,
        updatedAt = 2L,
        syncedAt = null,
        isDeleted = isDeleted,
        version = 1L,
        provenance = provenance.name,
        sourceContextId = sourceContextId,
    )
}
