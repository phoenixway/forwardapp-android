package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextParentLinkDao
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconRepository
import com.romankozak.forwardappmobile.sync.SyncRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ContextActionsUseCaseTest {
    private val contextRepository = mockk<ContextRepository>(relaxed = true)
    private val contextParentLinkDao = mockk<ContextParentLinkDao>(relaxed = true)
    private val syncRepository = mockk<SyncRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val mainBeaconRepository = mockk<MainBeaconRepository>(relaxed = true)

    private val useCase =
        ContextActionsUseCase(
            contextRepository = contextRepository,
            contextParentLinkDao = contextParentLinkDao,
            syncRepository = syncRepository,
            settingsRepository = settingsRepository,
            mainBeaconRepository = mainBeaconRepository,
            ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
        )

    @Test
    fun getMoveProjectRouteTreatsOrphanedContextAsRoot() = runTest {
        val orphanParent = "missing-parent"
        val project =
            context(
                id = "orphan",
                parentId = orphanParent,
            )

        val route = useCase.getMoveProjectRoute(project, allProjects = listOf(project))

        assertEquals("root", route.currentParentId)
    }

    @Test
    fun personalManagementCanBeMovedUnderAnotherContext() = runTest {
        val targetParent = context(id = "target-parent")
        val personalManagement = context(id = SystemContexts.PERSONAL_MANAGEMENT.raw)

        useCase.onListChooserResult(
            newParentId = targetParent.id,
            projectBeingMovedId = personalManagement.id,
            allProjects = listOf(personalManagement, targetParent),
        )

        coVerify(exactly = 1) {
            contextRepository.moveContext(
                contextToMove = personalManagement,
                newParentId = targetParent.id,
                allowSystemMoves = true,
            )
        }
    }

    private fun context(
        id: String,
        parentId: String? = null,
        name: String = id,
        order: Long = 0,
    ): Context =
        Context(
            id = id,
            name = name,
            description = null,
            parentId = parentId,
            createdAt = 0L,
            updatedAt = 0L,
            order = order,
        )
}
