@file:Suppress("PackageNaming")

package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TagManagerTest {
    @Test
    fun `loadTags uses canonical presentation names and tags`() =
        runTest {
            val contextRepository = mockk<ContextRepository>()
            val projector =
                mockk<SystemWorkspacePresentationContextProjector>()

            val staleRaw =
                Context(
                    id = "retired",
                    name = "Stale raw name",
                    description = null,
                    parentId = null,
                    createdAt = 0L,
                    updatedAt = 1L,
                    tags = listOf("stale-tag"),
                    relatedLinks = emptyList(),
                    isDeleted = true,
                )

            every { contextRepository.getAllContextsFlow() } returns
                flowOf(listOf(staleRaw))

            every {
                projector.observePresentationUniverse(any())
            } returns
                flowOf(
                    listOf(
                        ContextPresentation(
                            id = "retired",
                            name = "Canonical name",
                            description = null,
                            parentId = null,
                            roleCode = null,
                            order = 0L,
                            tags = listOf("canonical-tag"),
                        ),
                        ContextPresentation(
                            id = "second",
                            name = "Another project",
                            description = null,
                            parentId = null,
                            roleCode = null,
                            order = 1L,
                            tags = listOf("second-tag"),
                        ),
                    ),
                )

            val manager =
                TagManager(
                    contextRepository = contextRepository,
                    systemWorkspacePresentationContextProjector = projector,
                    scope = this,
                )

            manager.loadTags()
            advanceUntilIdle()

            assertThat(manager.allTags.value)
                .containsExactly("canonical-tag", "second-tag")
                .inOrder()

            assertThat(manager.allContexts.value)
                .containsExactly("Another project", "Canonical name")
                .inOrder()
        }
}
