package com.romankozak.forwardappmobile.features.globalsearch

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalContextSearchResult
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSubcontextSearchResult
import org.junit.Test

class GlobalSearchResultOrderingTest {
    @Test
    fun `contexts and subcontexts precede other results while preserving relative order`() {
        val firstActivity = GlobalSearchResultItem.ActivityItem(ActivityRecord(id = "activity-1", text = "First"))
        val context =
            GlobalSearchResultItem.ContextItem(
                GlobalContextSearchResult(context = context("context"), pathSegments = emptyList()),
            )
        val secondActivity = GlobalSearchResultItem.ActivityItem(ActivityRecord(id = "activity-2", text = "Second"))
        val subcontext =
            GlobalSearchResultItem.SubcontextItem(
                GlobalSubcontextSearchResult(
                    subcontext = context("subcontext"),
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

    private fun context(id: String) =
        Context(
            id = id,
            name = id,
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = null,
        )
}
