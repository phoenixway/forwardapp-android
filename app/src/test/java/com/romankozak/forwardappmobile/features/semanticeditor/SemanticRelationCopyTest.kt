package com.romankozak.forwardappmobile.features.semanticeditor

import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import org.junit.Assert.*
import org.junit.Test

class SemanticRelationCopyTest {
    @Test fun `outgoing sentence reads from current to selected`() {
        assertEquals("«Лікування» підтримує «Ліки»", orientationRelationSentence(OrientationRelationType.SUPPORTS, "Лікування", "Ліки"))
    }
    @Test fun `incoming sentence reverses endpoints not relation meaning`() {
        assertEquals("«Ліки» підтримує «Лікування»", orientationRelationSentence(OrientationRelationType.SUPPORTS, "Лікування", "Ліки", OrientationRelationEditDirection.INCOMING))
    }
    @Test fun `dependency direction is unambiguous`() {
        assertEquals("«Лікування» залежить від «Ліки»", orientationRelationSentence(OrientationRelationType.DEPENDS_ON, "Лікування", "Ліки"))
    }
    @Test fun `equal titles retain human endpoint distinction`() {
        val outgoing = orientationRelationSentence(OrientationRelationType.SUPPORTS, "Огляд", "Огляд")
        val incoming = orientationRelationSentence(OrientationRelationType.SUPPORTS, "Огляд", "Огляд", OrientationRelationEditDirection.INCOMING)
        assertNotEquals(outgoing, incoming)
        assertTrue(outgoing.contains("відкритий орієнтир")); assertTrue(outgoing.contains("вибраний орієнтир"))
    }
    @Test fun `every supported relation has a human explanation`() {
        OrientationRelationType.entries.forEach { assertTrue(it.explanation().isNotBlank()) }
    }
    @Test fun `all directed relations retain endpoint direction`() {
        OrientationRelationType.entries.filterNot { it == OrientationRelationType.CONFLICTS_WITH }.forEach {
            assertTrue(orientationRelationSentence(it, "Current", "Selected").startsWith("«Current»"))
            assertTrue(orientationRelationSentence(it, "Current", "Selected", OrientationRelationEditDirection.INCOMING).startsWith("«Selected»"))
        }
    }
    @Test fun `Workspace binding always reads from Workspace to subject`() {
        WorkspaceBindingType.entries.forEach {
            assertTrue(workspaceBindingSentence(it, "Кабінет", "Оснастити").startsWith("Робочий простір «Кабінет»"))
            assertTrue(workspaceBindingSentence(it, "Кабінет", "Оснастити").endsWith("«Оснастити»"))
            assertTrue(it.explanation().isNotBlank())
        }
    }
    @Test fun `Aspect membership and relevance are different statements`() {
        assertEquals("«Лікування» належить до аспекту «Здоров’я»", aspectAssociationSentence(AspectOrientationRelationType.BELONGS_TO, "Лікування", "Здоров’я"))
        assertEquals("«Лікування» стосується аспекту «Здоров’я»", aspectAssociationSentence(AspectOrientationRelationType.RELEVANT_TO, "Лікування", "Здоров’я"))
    }
}
