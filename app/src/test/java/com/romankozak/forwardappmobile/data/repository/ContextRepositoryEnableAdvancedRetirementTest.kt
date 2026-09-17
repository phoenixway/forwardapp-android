package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfile
import com.romankozak.forwardappmobile.data.logic.ContextMarkerHandler
import com.romankozak.forwardappmobile.data.logic.TagAssociationHandler
import com.romankozak.forwardappmobile.data.workspace.ContextWorkspaceWriteThrough
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import javax.inject.Provider
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ContextRepositoryEnableAdvancedRetirementTest {
    @Test
    fun `new Context configuration does not inherit preset enableAdvanced`() = runTest {
        val contextDao = mockk<ContextDao>(relaxed = true)
        val structureDao = mockk<ContextStructureDao>(relaxed = true)
        val presetDao = mockk<StructurePresetDao>()
        coEvery { presetDao.getByCode("management") } returns
            ContextRoleProfile(
                id = "preset",
                code = "management",
                label = "Management",
                description = null,
                enableInbox = true,
                enableAdvanced = true,
            )
        val writeThrough = mockk<ContextWorkspaceWriteThrough>()
        coEvery { writeThrough.mutate<Unit>(any(), any()) } coAnswers {
            secondArg<suspend () -> Unit>().invoke()
        }
        val markerProvider = mockk<Provider<ContextMarkerHandler>>()
        every { markerProvider.get() } returns mockk(relaxed = true)
        val tags = mockk<TagAssociationHandler>(relaxed = true)
        val repository =
            ContextRepository(
                contextDao = contextDao,
                contextTagRefDao = mockk(relaxed = true),
                legacyNoteRepository = mockk(relaxed = true),
                activityRepository = mockk(relaxed = true),
                recentItemsRepository = mockk(relaxed = true),
                reminderRepository = mockk(relaxed = true),
                contextLogRepository = mockk(relaxed = true),
                searchRepository = mockk(relaxed = true),
                noteDocumentRepository = mockk(relaxed = true),
                musicNoteRepository = mockk(relaxed = true),
                checklistRepository = mockk(relaxed = true),
                attachmentRepository = mockk(relaxed = true),
                goalRepository = mockk(relaxed = true),
                contextTimeTrackingRepository = mockk(relaxed = true),
                listItemRepository = mockk(relaxed = true),
                backlogPlacementCommands = mockk(relaxed = true),
                contextStructureDao = structureDao,
                structurePresetDao = presetDao,
                directionRepository = mockk(relaxed = true),
                aiEventRepository = mockk(relaxed = true),
                contextMarkerHandlerProvider = markerProvider,
                tagAssociationHandler = tags,
                systemWorkspaceTagAuthority = mockk(relaxed = true),
                workspaceWriteThrough = writeThrough,
                canonicalWorkspaceRepository = mockk(relaxed = true),
                canonicalWorkspaceTagRepository = mockk(relaxed = true),
                systemContextCanonicalInboxDirectionAccess = mockk(relaxed = true),
                canonicalKeyProblemsRepository = mockk(relaxed = true),
                canonicalInboxRepository = mockk(relaxed = true),
                canonicalConnectionsRepository = mockk(relaxed = true),
                canonicalBacklogRepository = mockk(relaxed = true),
                backlogPresentationLifecycle = mockk(relaxed = true),
            )

        repository.createContextWithId(
            id = "context",
            name = "Context",
            parentId = null,
            roleCode = "management",
        )

        coVerify(exactly = 1) {
            structureDao.insertStructure(
                match {
                    it.contextId == "context" &&
                        it.basePresetCode == "management" &&
                        it.enableInbox == true &&
                        it.enableAdvanced == null
                },
            )
        }
    }
}
