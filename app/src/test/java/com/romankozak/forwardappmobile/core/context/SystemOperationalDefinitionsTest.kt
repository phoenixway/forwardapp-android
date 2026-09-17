package com.romankozak.forwardappmobile.core.context

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemOperationalDefinitionsTest {
    @Test
    fun definitionsAreCompleteUniqueReservedAndParentBeforeChild() {
        val definitions = SystemOperationalDefinitions.all
        val definitionIds = definitions.map(SystemOperationalDefinition::id)
        val definitionIndexById = definitionIds.withIndex().associate { (index, id) -> id to index }

        assertEquals(20, definitions.size)
        assertEquals(definitions.size, definitionIds.distinct().size)
        assertTrue(definitions.all { SystemContexts.isSystem(ContextId(it.id)) })
        assertTrue(
            definitions
                .filter { it.defaultParentId != null }
                .all { definition ->
                    val parentIndex = definitionIndexById[definition.defaultParentId]
                    parentIndex != null && parentIndex < definitionIndexById.getValue(definition.id)
                },
        )
    }

    @Test
    fun representativeDefaultsRemainStable() {
        assertDefinition(
            id = SystemContexts.PERSONAL_MANAGEMENT.raw,
            name = "personal-management",
            parentId = null,
        )
        assertDefinition(
            id = SystemContexts.SESSION_IMPROVE.raw,
            name = "mode-improve",
            parentId = SystemContexts.PERSONAL_MANAGEMENT.raw,
        )
        assertDefinition(
            id = SystemContexts.TODAY.raw,
            name = "day-management",
            parentId = SystemContexts.LEVELS.raw,
        )
        assertDefinition(
            id = SystemContexts.INBOX.raw,
            name = "inbox",
            parentId = SystemContexts.TODAY.raw,
        )
        assertDefinition(
            id = SystemContexts.STRATEGIC_REVIEW.raw,
            name = "strategic-review",
            parentId = SystemContexts.STRATEGIC.raw,
        )
    }

    private fun assertDefinition(
        id: String,
        name: String,
        parentId: String?,
    ) {
        assertEquals(
            SystemOperationalDefinition(
                id = id,
                defaultName = name,
                defaultParentId = parentId,
            ),
            SystemOperationalDefinitions.all.single { it.id == id },
        )
    }
}
