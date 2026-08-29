package com.romankozak.forwardappmobile.data.orientation

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectionOrientationShadowRepairTest {
    @Test
    fun `new linked row is review-required and is not materialized as Direction`() {
        val plan =
            planDirectionOrientationShadowRepair(
                rows = listOf(linkedRow()),
                mappings = emptyList(),
                subjects = emptyList(),
                orientations = emptyList(),
                now = 100L,
                migrationVersion = DIRECTION_SHADOW_REPAIR_VERSION,
            )

        assertTrue(plan.projectableRows.isEmpty())
        assertTrue(plan.subjectChanges.isEmpty())
        assertTrue(plan.mappingChanges.isEmpty())
        assertEquals(listOf(LINKED_REVIEW_ISSUE), plan.issues.map { it.code })
    }

    @Test
    fun `existing linked shadow is tombstoned and mapping is quarantined`() {
        val linked = linkedRow()
        val canonical = unlinkedRow().canonicalRows()

        val plan =
            planDirectionOrientationShadowRepair(
                rows = listOf(linked),
                mappings = listOf(canonical.mapping),
                subjects = listOf(canonical.subject),
                orientations = listOf(canonical.orientation),
                now = 100L,
                migrationVersion = DIRECTION_SHADOW_REPAIR_VERSION,
            )

        assertTrue(plan.projectableRows.isEmpty())
        assertTrue(plan.subjectChanges.single().isDeleted)
        assertEquals(canonical.subject.version + 1L, plan.subjectChanges.single().version)
        assertEquals(
            LegacySubjectMappingState.QUARANTINED.name,
            plan.mappingChanges.single().state,
        )
        assertFalse(plan.mappingChanges.single().isDeleted)
    }

    @Test
    fun `quarantine is idempotent and unlinked row restores the same identity`() {
        val canonical = unlinkedRow().canonicalRows()
        val quarantine =
            planDirectionOrientationShadowRepair(
                rows = listOf(linkedRow()),
                mappings = listOf(canonical.mapping),
                subjects = listOf(canonical.subject.copy(description = "Canonical detail")),
                orientations = listOf(canonical.orientation),
                now = 100L,
                migrationVersion = DIRECTION_SHADOW_REPAIR_VERSION,
            )
        val quarantinedSubject = quarantine.subjectChanges.single()
        val quarantinedMapping = quarantine.mappingChanges.single()

        val repeated =
            planDirectionOrientationShadowRepair(
                rows = listOf(linkedRow()),
                mappings = listOf(quarantinedMapping),
                subjects = listOf(quarantinedSubject),
                orientations = listOf(canonical.orientation),
                now = 110L,
                migrationVersion = DIRECTION_SHADOW_REPAIR_VERSION,
            )
        assertTrue(repeated.subjectChanges.isEmpty())
        assertTrue(repeated.mappingChanges.isEmpty())

        val restored =
            planDirectionOrientationShadowRepair(
                rows = listOf(unlinkedRow().copy(text = "Restored direction")),
                mappings = listOf(quarantinedMapping),
                subjects = listOf(quarantinedSubject),
                orientations = listOf(canonical.orientation),
                now = 120L,
                migrationVersion = DIRECTION_SHADOW_REPAIR_VERSION,
            )

        assertEquals(listOf("direction-row"), restored.projectableRows.map { it.id })
        assertFalse(restored.subjectChanges.single().isDeleted)
        assertEquals("Restored direction", restored.subjectChanges.single().title)
        assertEquals("Canonical detail", restored.subjectChanges.single().description)
        assertEquals(canonical.subject.id, restored.subjectChanges.single().id)
        assertEquals(
            LegacySubjectMappingState.MATERIALIZED.name,
            restored.mappingChanges.single().state,
        )
    }

    @Test
    fun `quarantine owned by another migration is not restored automatically`() {
        val canonical = unlinkedRow().canonicalRows()
        val foreignMapping =
            canonical.mapping.copy(
                state = LegacySubjectMappingState.QUARANTINED.name,
                migrationVersion = DIRECTION_SHADOW_REPAIR_VERSION - 1,
            )

        val plan =
            planDirectionOrientationShadowRepair(
                rows = listOf(unlinkedRow()),
                mappings = listOf(foreignMapping),
                subjects = listOf(canonical.subject.copy(isDeleted = true)),
                orientations = listOf(canonical.orientation),
                now = 100L,
                migrationVersion = DIRECTION_SHADOW_REPAIR_VERSION,
            )

        assertTrue(plan.subjectChanges.isEmpty())
        assertTrue(plan.mappingChanges.isEmpty())
        assertTrue(plan.issues.any { it.code == "DIRECTION_QUARANTINE_OWNER_MISMATCH" })
    }

    @Test
    fun `deleted source tombstones shadow and mapping without creating a new projection`() {
        val canonical = unlinkedRow().canonicalRows()
        val plan =
            planDirectionOrientationShadowRepair(
                rows = listOf(unlinkedRow().copy(isDeleted = true)),
                mappings = listOf(canonical.mapping),
                subjects = listOf(canonical.subject),
                orientations = listOf(canonical.orientation),
                now = 100L,
                migrationVersion = DIRECTION_SHADOW_REPAIR_VERSION,
            )

        assertTrue(plan.projectableRows.isEmpty())
        assertTrue(plan.subjectChanges.single().isDeleted)
        assertTrue(plan.mappingChanges.single().isDeleted)
        assertTrue(plan.issues.isEmpty())
    }

    private fun unlinkedRow() =
        DirectionItemEntity(
            id = "direction-row",
            contextId = "owner",
            text = "Direction",
            linkedContextId = null,
            itemOrder = 1,
            updatedAt = 10L,
            version = 1L,
        )

    private fun linkedRow() = unlinkedRow().copy(linkedContextId = "target")

    private fun DirectionItemEntity.canonicalRows() =
        toEffectiveOrientation(LegacySubjectUuid).toCanonicalRows(
            Gson(),
            migrationVersion = DIRECTION_SHADOW_REPAIR_VERSION - 1,
        )
}
