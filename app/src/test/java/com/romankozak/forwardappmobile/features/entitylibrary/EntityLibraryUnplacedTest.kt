package com.romankozak.forwardappmobile.features.entitylibrary

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import org.junit.Test

class EntityLibraryUnplacedTest {
    @Test
    fun `typed identity prevents Workspace and ManagedSubject raw ID collision`() {
        val catalog =
            listOf(
                CanonicalEntitySummary(CanonicalEntityRef.Orientation("shared"), "Orientation"),
                CanonicalEntitySummary(CanonicalEntityRef.Aspect("aspect"), "Aspect"),
                CanonicalEntitySummary(CanonicalEntityRef.Workspace("shared"), "Workspace"),
                CanonicalEntitySummary(CanonicalEntityRef.Workspace("orphan"), "Orphan"),
            )
        val placed =
            setOf(
                HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, "shared"),
                HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "orphan"),
            )

        assertThat(deriveUnplacedCanonicalEntities(catalog, placed).map { it.ref })
            .containsExactly(
                CanonicalEntityRef.Aspect("aspect"),
                CanonicalEntityRef.Workspace("shared"),
            )
            .inOrder()
    }

    @Test
    fun `Orientation Aspect and Workspace each count their matching hierarchy target`() {
        val catalog =
            listOf(
                CanonicalEntitySummary(CanonicalEntityRef.Orientation("o"), "Orientation"),
                CanonicalEntitySummary(CanonicalEntityRef.Aspect("a"), "Aspect"),
                CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Workspace"),
            )
        val placed =
            setOf(
                HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, "o"),
                HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, "a"),
                HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "w"),
            )

        assertThat(deriveUnplacedCanonicalEntities(catalog, emptySet()).map { it.ref })
            .containsExactlyElementsIn(catalog.map { it.ref })
            .inOrder()
        assertThat(deriveUnplacedCanonicalEntities(catalog, placed)).isEmpty()
        assertThat(deriveUnplacedCanonicalEntities(emptyList(), placed)).isEmpty()
    }
}
