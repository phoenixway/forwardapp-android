package com.romankozak.forwardappmobile.data.repository

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TagCatalogSystemTagAuthorityTest {
    @Test
    fun `catalog uses canonical owner tags and projected descriptions without stale System shell tags`() =
        runTest {
            val activityDao = mockk<ActivityRecordDao>()
            val goalRepository = mockk<GoalRepository>()
            val dayTaskDao = mockk<DayTaskDao>()
            val projector = mockk<SystemWorkspacePresentationContextProjector>()
            val authority = mockk<SystemWorkspaceTagAuthority>()

            val systemId = SystemContexts.INBOX.raw

            every { activityDao.getAllRecordsStream() } returns flowOf(emptyList())
            every { goalRepository.getAllGoalsFlow() } returns flowOf(emptyList())
            every { dayTaskDao.getAllVisibleTasksFlow() } returns flowOf(emptyList())
            every { projector.observePresentationUniverse() } returns
                flowOf(
                    listOf(
                        mockk<ContextPresentation>().also { presentation ->
                            every { presentation.description } returns "description-tag #from-description"
                        },
                    ),
                )
            every {
                authority.observeEffectiveOwners()
            } returns
                flowOf(
                    listOf(
                        SystemWorkspaceTagAuthority.TagOwner(
                            id = systemId,
                            tags = listOf("canonical-system"),
                        ),
                        SystemWorkspaceTagAuthority.TagOwner(
                            id = "ordinary-context",
                            tags = listOf("ordinary"),
                        ),
                    ),
                )

            val tags =
                TagCatalogRepository(
                    activityRecordDao = activityDao,
                    goalRepository = goalRepository,
                    dayTaskDao = dayTaskDao,
                    systemWorkspacePresentationContextProjector = projector,
                    systemWorkspaceTagAuthority = authority,
                ).tags.first()

            val normalized =
                tags.map { it.removePrefix("#").lowercase() }

            assertThat(normalized).contains("canonical-system")
            assertThat(normalized).contains("ordinary")
            assertThat(normalized).contains("from-description")
            assertThat(normalized).doesNotContain("stale-system-shell")
        }
}
