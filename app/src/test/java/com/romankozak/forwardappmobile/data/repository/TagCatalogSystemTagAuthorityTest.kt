package com.romankozak.forwardappmobile.data.repository

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TagCatalogSystemTagAuthorityTest {
    @Test
    fun `catalog uses effective canonical System tags plus ordinary Context tags and ignores stale System shell tags`() =
        runTest {
            val activityDao = mockk<ActivityRecordDao>()
            val goalDao = mockk<GoalDao>()
            val dayTaskDao = mockk<DayTaskDao>()
            val contextDao = mockk<ContextDao>()
            val authority = mockk<SystemWorkspaceTagAuthority>()

            val systemId = SystemContexts.INBOX.raw
            val contextFlow =
                flowOf(
                    listOf(
                        context(
                            id = systemId,
                            tags = listOf("stale-system-shell"),
                        ),
                        context(
                            id = "ordinary-context",
                            tags = listOf("ordinary"),
                        ),
                    ),
                )

            every { activityDao.getAllRecordsStream() } returns flowOf(emptyList())
            every { goalDao.getAllVisibleGoalsFlow() } returns flowOf(emptyList())
            every { dayTaskDao.getAllVisibleTasksFlow() } returns flowOf(emptyList())
            every { contextDao.getAllContexts() } returns contextFlow
            every {
                authority.observeEffectiveOwners(any())
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
                    goalDao = goalDao,
                    dayTaskDao = dayTaskDao,
                    contextDao = contextDao,
                    systemWorkspaceTagAuthority = authority,
                ).tags.first()

            val normalized =
                tags.map { it.removePrefix("#").lowercase() }

            assertThat(normalized).contains("canonical-system")
            assertThat(normalized).contains("ordinary")
            assertThat(normalized).doesNotContain("stale-system-shell")
        }

    private fun context(
        id: String,
        tags: List<String>?,
    ): Context =
        Context(
            id = id,
            name = id,
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = 1L,
            tags = tags,
        )
}
