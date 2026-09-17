package com.romankozak.forwardappmobile.features.globalsearch

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalContextSearchResult
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchContextPresentation
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSubcontextSearchResult
import org.junit.Test

class GlobalSearchResultOrderingTest {
    @Test
    fun `contexts and subcontexts precede other results while preserving relative order`() {
        val firstActivity = GlobalSearchResultItem.ActivityItem(ActivityRecord(id = "activity-1", text = "First"))
        val context =
            GlobalSearchResultItem.ContextItem(
                GlobalContextSearchResult(presentation = presentation("context"), pathSegments = emptyList()),
            )
        val secondActivity = GlobalSearchResultItem.ActivityItem(ActivityRecord(id = "activity-2", text = "Second"))
        val subcontext =
            GlobalSearchResultItem.SubcontextItem(
                GlobalSubcontextSearchResult(
                    presentation = presentation("subcontext"),
                    parentContextId = "parent",
                    parentContextName = "Parent",
                    pathSegments = emptyList(),
                ),
            )

        val ordered = listOf(firstActivity, context, secondActivity, subcontext).withContextsFirst()

        assertThat(ordered.map { it.uniqueId })
            .containsExactly(context.uniqueId, subcontext.uniqueId, firstActivity.uniqueId, secondActivity.uniqueId)
            .inOrder()
    }

    @Test
    fun `Context result items rank from read-only presentation timestamp`() {
        val context =
            GlobalSearchResultItem.ContextItem(
                GlobalContextSearchResult(
                    presentation = presentation("context", rankingTimestamp = 20L),
                    pathSegments = emptyList(),
                ),
            )
        val subcontext =
            GlobalSearchResultItem.SubcontextItem(
                GlobalSubcontextSearchResult(
                    presentation = presentation("subcontext", rankingTimestamp = 30L),
                    parentContextId = "parent",
                    parentContextName = "Parent",
                    pathSegments = emptyList(),
                ),
            )

        assertThat(context.timestamp).isEqualTo(20L)
        assertThat(subcontext.timestamp).isEqualTo(30L)
        assertThat(context.uniqueId).isEqualTo("context_context")
        assertThat(subcontext.uniqueId).isEqualTo("sublist_subcontext_parent")
    }

    private fun presentation(
        id: String,
        rankingTimestamp: Long = 1L,
    ) =
        GlobalSearchContextPresentation(
            id = id,
            name = id,
            description = null,
            parentId = null,
            tags = emptyList(),
            rankingTimestamp = rankingTimestamp,
        )
}
