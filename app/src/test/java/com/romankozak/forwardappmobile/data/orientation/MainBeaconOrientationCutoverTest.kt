package com.romankozak.forwardappmobile.data.orientation

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroupMember
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainBeaconOrientationCutoverTest {
    private val gson = Gson()
    private val beacon = MainBeacon(id = "beacon-1", title = "North star", createdAt = 10L, updatedAt = 20L)
    private val group = MainBeaconGroup(id = "group-1", title = "Core", createdAt = 11L, updatedAt = 21L)

    @Test
    fun `cutover promotes mappings and creates ordered PART_OF relation`() {
        val fixture = fixture()
        val plan =
            planMainBeaconCutover(
                projections = fixture.projections,
                mappings = fixture.rows.map { it.mapping },
                subjects = fixture.rows.map { it.subject },
                orientations = fixture.rows.map { it.orientation },
                legacyMembers = listOf(MainBeaconGroupMember(group.id, beacon.id, order = 7L)),
                existingRelations = emptyList(),
                now = 100L,
                migrationVersion = 2,
            )

        assertTrue(plan.issues.isEmpty())
        assertEquals(2, plan.mappings.size)
        assertTrue(plan.mappings.all { it.state == LegacySubjectMappingState.CUT_OVER.name })
        with(plan.relationChanges.single()) {
            assertEquals(fixture.rows[0].subject.id, fromOrientationId)
            assertEquals(fixture.rows[1].subject.id, toOrientationId)
            assertEquals("PART_OF", relationType)
            assertEquals(7L, relationOrder)
            assertFalse(isDeleted)
        }
    }

    @Test
    fun `cutover is idempotent once mapping and relation are canonical`() {
        val fixture = fixture()
        val first = cutOver(fixture)
        val cutOverMappings =
            fixture.rows.map { row -> first.mappings.first { it.id == row.mapping.id } }
        val second =
            planMainBeaconCutover(
                projections = fixture.projections,
                mappings = cutOverMappings,
                subjects = fixture.rows.map { it.subject },
                orientations = fixture.rows.map { it.orientation },
                legacyMembers = listOf(MainBeaconGroupMember(group.id, beacon.id, order = 3L)),
                existingRelations = first.relationChanges,
                now = 200L,
                migrationVersion = 2,
            )

        assertTrue(second.issues.isEmpty())
        assertTrue(second.mappings.isEmpty())
        assertTrue(second.relationChanges.isEmpty())
    }

    @Test
    fun `pre-cutover common-field divergence blocks ownership transition`() {
        val fixture = fixture()
        val divergentSubjects = fixture.rows.mapIndexed { index, row ->
            if (index == 0) row.subject.copy(title = "Different") else row.subject
        }
        val plan =
            planMainBeaconCutover(
                projections = fixture.projections,
                mappings = fixture.rows.map { it.mapping },
                subjects = divergentSubjects,
                orientations = fixture.rows.map { it.orientation },
                legacyMembers = emptyList(),
                existingRelations = emptyList(),
                now = 100L,
                migrationVersion = 2,
            )

        assertTrue(plan.mappings.isEmpty())
        assertTrue(plan.relationChanges.isEmpty())
        assertEquals("CUTOVER_SHADOW_DIVERGENCE", plan.issues.single().code)
    }

    @Test
    fun `removing and restoring membership tombstones then resurrects same relation`() {
        val fixture = fixture()
        val first = cutOver(fixture)
        val cutOverMappings = first.mappings
        val live = first.relationChanges.single()
        val removed =
            planCanonicalMainBeaconMembershipChanges(
                legacyMembers = emptyList(),
                mappings = cutOverMappings,
                existingRelations = listOf(live),
                now = 200L,
            ).single()
        val restored =
            planCanonicalMainBeaconMembershipChanges(
                legacyMembers = listOf(MainBeaconGroupMember(group.id, beacon.id, order = 9L)),
                mappings = cutOverMappings,
                existingRelations = listOf(removed),
                now = 300L,
            ).single()

        assertTrue(removed.isDeleted)
        assertEquals(live.id, removed.id)
        assertFalse(restored.isDeleted)
        assertEquals(live.id, restored.id)
        assertEquals(9L, restored.relationOrder)
        assertEquals(3L, restored.version)
    }

    private fun cutOver(fixture: Fixture) =
        planMainBeaconCutover(
            projections = fixture.projections,
            mappings = fixture.rows.map { it.mapping },
            subjects = fixture.rows.map { it.subject },
            orientations = fixture.rows.map { it.orientation },
            legacyMembers = listOf(MainBeaconGroupMember(group.id, beacon.id, order = 3L)),
            existingRelations = emptyList(),
            now = 100L,
            migrationVersion = 2,
        )

    private fun fixture(): Fixture {
        val projections =
            listOf(
                beacon.toEffectiveOrientation(LegacySubjectUuid),
                group.toEffectiveOrientation(LegacySubjectUuid),
            )
        return Fixture(
            projections = projections,
            rows = projections.map { it.toCanonicalRows(gson, migrationVersion = 1) },
        )
    }

    private data class Fixture(
        val projections: List<com.romankozak.forwardappmobile.shared.core.models.orientation.EffectiveOrientation>,
        val rows: List<CanonicalOrientationRows>,
    )
}
