package com.romankozak.forwardappmobile.features.contexts.domain.clipboard

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BacklogClipboardPlacementPolicyTest {
    @Test
    fun `move excludes hashtag projections and canonical target duplicates`() = runTest {
        val movable = item(id = "move", entityId = "goal-new")
        val duplicate = item(id = "duplicate", entityId = "goal-existing")
        val projection =
            item(
                id = "projection",
                entityId = "goal-projected",
                associationOwnerContextId = "owner",
                associationTag = "tag",
            )

        val result =
            partitionCanonicalBacklogMove(
                items = listOf(movable, duplicate, projection),
                targetContextId = "target",
                hasLivePlacement = { contextId, itemType, entityId ->
                    contextId == "target" && itemType == "GOAL" && entityId == "goal-existing"
                },
            )

        assertEquals(listOf("move"), result.movable.map { it.id })
        assertEquals(1, result.duplicateCount)
        assertEquals(1, result.invalidCount)
    }

    private fun item(
        id: String,
        entityId: String,
        associationOwnerContextId: String? = null,
        associationTag: String? = null,
    ) =
        BacklogItem(
            id = id,
            contextId = "source",
            itemType = "GOAL",
            entityId = entityId,
            associationOwnerContextId = associationOwnerContextId,
            associationTag = associationTag,
            order = 0L,
        )
}
