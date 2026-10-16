package com.romankozak.forwardappmobile.features.entitylibrary

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.features.entitybrowser.*
import org.junit.Test

class EntityLibrarySortTest {
    private val items = listOf(
        CanonicalEntitySummary(CanonicalEntityRef.Orientation("o"), "Zulu", createdAt = 10, updatedAt = 30),
        CanonicalEntitySummary(CanonicalEntityRef.Aspect("a"), "Alpha", createdAt = 30, updatedAt = 10),
        CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Middle", createdAt = 20, updatedAt = 20),
    )

    @Test fun `all six sorts order mixed canonical classes`() {
        val expected = mapOf(
            EntityLibrarySort.NAME_ASC to listOf("a", "w", "o"),
            EntityLibrarySort.NAME_DESC to listOf("o", "w", "a"),
            EntityLibrarySort.CREATED_ASC to listOf("o", "w", "a"),
            EntityLibrarySort.CREATED_DESC to listOf("a", "w", "o"),
            EntityLibrarySort.UPDATED_ASC to listOf("a", "w", "o"),
            EntityLibrarySort.UPDATED_DESC to listOf("o", "w", "a"),
        )
        expected.forEach { (sort, ids) ->
            assertThat(sortLibraryEntities(items, sort).map { it.ref.id }).containsExactlyElementsIn(ids).inOrder()
        }
    }

    @Test fun `ties use title class and id and missing dates are always last`() {
        val tied = listOf(
            CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Same", createdAt = 1, updatedAt = 1),
            CanonicalEntitySummary(CanonicalEntityRef.Orientation("o2"), "Same", createdAt = 1, updatedAt = 1),
            CanonicalEntitySummary(CanonicalEntityRef.Orientation("o1"), "Same", createdAt = 1, updatedAt = 1),
            CanonicalEntitySummary(CanonicalEntityRef.Aspect("a"), "Same", createdAt = 1, updatedAt = 1),
            CanonicalEntitySummary(CanonicalEntityRef.Aspect("missing"), "A missing date"),
        )
        EntityLibrarySort.entries.filterNot { it.name.startsWith("NAME") }.forEach { sort ->
            assertThat(sortLibraryEntities(tied, sort).map { it.ref.id })
                .containsExactly("a", "o1", "o2", "w", "missing").inOrder()
        }
        EntityLibrarySort.entries.filter { it.name.startsWith("NAME") }.forEach { sort ->
            assertThat(sortLibraryEntities(tied.take(4), sort).map { it.ref.id })
                .containsExactly("a", "o1", "o2", "w").inOrder()
        }
    }

    @Test fun `fuzzy search tolerates typo and token order while short queries stay precise`() {
        val catalog = listOf(CanonicalEntitySummary(CanonicalEntityRef.Aspect("a"), "Personal Health"))
        listOf("helth", "health persnal", "PERSONAL").forEach { text ->
            assertThat(queryCanonicalEntities(catalog, CanonicalEntityBrowserQuery(text = text))).hasSize(1)
        }
        assertThat(queryCanonicalEntities(catalog, CanonicalEntityBrowserQuery(text = "zz"))).isEmpty()
    }

    @Test fun `sort composes with fuzzy search and class filtering without changing membership`() {
        val result = queryCanonicalEntities(items, CanonicalEntityBrowserQuery(text = "Alpa", allowedClasses = setOf(CanonicalEntityClass.ASPECT)))
        EntityLibrarySort.entries.forEach { sort ->
            assertThat(sortLibraryEntities(result, sort).map { it.ref }).containsExactly(CanonicalEntityRef.Aspect("a"))
        }
    }
}
