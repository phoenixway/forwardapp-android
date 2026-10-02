package com.romankozak.forwardappmobile.shared.core.domain.planning

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlanningContractTest {
    @Test
    fun `scope vocabulary is the accepted common vocabulary`() {
        assertEquals(
            setOf(
                PlanningScopeKind.DAY,
                PlanningScopeKind.TACTICAL_CYCLE,
                PlanningScopeKind.STRATEGIC_ARC,
            ),
            PlanningScopeKind.entries.toSet(),
        )
        assertEquals(
            setOf(
                PlanningScopeLifecycle.DRAFT,
                PlanningScopeLifecycle.ACTIVE,
                PlanningScopeLifecycle.CLOSED,
                PlanningScopeLifecycle.ARCHIVED,
            ),
            PlanningScopeLifecycle.entries.toSet(),
        )
    }

    @Test
    fun `commitment vocabulary is the accepted common vocabulary`() {
        assertEquals(
            setOf(
                PlanningCommitmentStatus.PLANNED,
                PlanningCommitmentStatus.ACTIVE,
                PlanningCommitmentStatus.PAUSED,
                PlanningCommitmentStatus.COMPLETED,
                PlanningCommitmentStatus.DROPPED,
            ),
            PlanningCommitmentStatus.entries.toSet(),
        )
        assertEquals(
            setOf(
                PlanningPriorityLevel.LOW,
                PlanningPriorityLevel.MEDIUM,
                PlanningPriorityLevel.HIGH,
                PlanningPriorityLevel.CRITICAL,
            ),
            PlanningPriorityLevel.entries.toSet(),
        )
    }

    @Test
    fun `blank canonical planning identities fail closed`() {
        assertFailsWith<IllegalArgumentException> { PlanningScopeId(" ") }
        assertFailsWith<IllegalArgumentException> { PlanningCommitmentId("") }
    }

    @Test
    fun `common and initial domain roles have stable codes and value equality`() {
        assertEquals(PlanningRoleCode.MISSION, PlanningRoleCode.fromStableCode("MISSION"))
        assertEquals(PlanningRoleCode.PRIORITY, PlanningRoleCode.fromStableCode("PRIORITY"))
        assertEquals(PlanningRoleCode.FOCUS, PlanningRoleCode.fromStableCode("FOCUS"))
        assertEquals(PlanningRoleCode.DAY_THEME, PlanningRoleCode.fromStableCode("DAY.THEME"))
        assertEquals(
            PlanningRoleCode.DAY_THEME.hashCode(),
            PlanningRoleCode.fromStableCode("DAY.THEME").hashCode(),
        )
        assertEquals("DAY.THEME", PlanningRoleCode.DAY_THEME.toString())
        assertEquals("DAY.RESPONSIBILITY", PlanningRoleCode.DAY_RESPONSIBILITY.value)
        assertEquals("DAY.TASK", PlanningRoleCode.DAY_TASK.value)
    }

    @Test
    fun `well formed namespaced roles can be registered without changing the common enum`() {
        val reviewer = PlanningRoleCode.namespaced("TACTICAL.REVIEWER")
        val registry =
            PlanningRoleRegistry(
                PlanningRoleCode.initialDomain + reviewer,
            )

        assertTrue(registry.contains(reviewer))
        assertEquals(reviewer, PlanningRoleCode.fromStableCode(reviewer.value))
    }

    @Test
    fun `malformed or unknown unnamespaced role codes are rejected`() {
        listOf("", " ", "day.theme", "DAY", "DAY.", ".THEME", "DAY..THEME", "DAY-THEME")
            .forEach { code ->
                assertFailsWith<IllegalArgumentException>(code) {
                    PlanningRoleCode.fromStableCode(code)
                }
            }
    }

    @Test
    fun `well formed but unregistered domain role is rejected by contract validation`() {
        val commitment = commitment(role = PlanningRoleCode.namespaced("TACTICAL.REVIEWER"))

        assertViolation(validatePlanningCommitment(commitment), "UNREGISTERED_ROLE")
    }

    @Test
    fun `priority role remains independent of optional priority level`() {
        val commitment =
            commitment(
                role = PlanningRoleCode.PRIORITY,
                priorityLevel = null,
            )

        assertEquals(PlanningRoleCode.PRIORITY, commitment.role)
        assertNull(commitment.priorityLevel)
        assertTrue(validatePlanningCommitment(commitment).isEmpty())

        val highMission =
            commitment(
                id = "mission",
                role = PlanningRoleCode.MISSION,
                priorityLevel = PlanningPriorityLevel.HIGH,
            )
        assertEquals(PlanningPriorityLevel.HIGH, highMission.priorityLevel)
        assertTrue(validatePlanningCommitment(highMission).isEmpty())
    }

    @Test
    fun `scope horizon is optional and only rejects an end before its start`() {
        val openEnded = scope(startsAt = null, endsAt = null)
        val startOnly = scope(id = "start-only", startsAt = 20L, endsAt = null)
        val bounded = scope(id = "bounded", startsAt = 20L, endsAt = 20L)

        assertTrue(validatePlanningScope(openEnded).isEmpty())
        assertTrue(validatePlanningScope(startOnly).isEmpty())
        assertTrue(validatePlanningScope(bounded).isEmpty())
        assertViolation(
            validatePlanningScope(scope(id = "invalid", startsAt = 21L, endsAt = 20L)),
            "INVALID_HORIZON",
        )
    }

    @Test
    fun `negative versions and blank optional metadata fail closed`() {
        assertViolation(validatePlanningScope(scope(version = -1L)), "NEGATIVE_VERSION")
        assertViolation(validatePlanningScope(scope(id = "blank-title", title = " ")), "BLANK_TITLE")
        assertViolation(
            validatePlanningCommitment(commitment(version = -1L)),
            "NEGATIVE_VERSION",
        )
        assertViolation(
            validatePlanningCommitment(
                commitment(
                    provenance =
                        PlanningProvenance(
                            kind = PlanningProvenanceKind.MIGRATED,
                            sourceType = " ",
                            sourceId = "legacy-1",
                        ),
                ),
            ),
            "BLANK_SOURCE_TYPE",
        )
    }

    @Test
    fun `same Orientation may have separate live commitments for different roles`() {
        val scopes = listOf(scope())
        val commitments =
            listOf(
                commitment(id = "mission", role = PlanningRoleCode.MISSION),
                commitment(id = "focus", role = PlanningRoleCode.FOCUS),
            )

        assertValid(scopes, commitments)
    }

    @Test
    fun `same Orientation and role may participate in different scopes`() {
        val scopes = listOf(scope(id = "scope-a"), scope(id = "scope-b"))
        val commitments =
            listOf(
                commitment(id = "a", scopeId = "scope-a"),
                commitment(id = "b", scopeId = "scope-b"),
            )

        assertValid(scopes, commitments)
    }

    @Test
    fun `duplicate live scope Orientation and role participation is rejected`() {
        val commitments =
            listOf(
                commitment(id = "a"),
                commitment(id = "b"),
            )

        assertViolation(
            validate(scopes = listOf(scope()), commitments = commitments),
            "DUPLICATE_LIVE_PARTICIPATION",
        )
    }

    @Test
    fun `tombstoned participation does not conflict with a live replacement`() {
        val commitments =
            listOf(
                commitment(id = "historical", isDeleted = true),
                commitment(id = "current"),
            )

        assertValid(listOf(scope()), commitments)
    }

    @Test
    fun `commitments order deterministically by order then identity`() {
        val commitments =
            listOf(
                commitment(id = "b", order = 10L),
                commitment(id = "c", order = -1L),
                commitment(id = "a", order = 10L),
            )

        val ids = commitments.sortedWith(planningCommitmentOrderComparator).map { it.id.value }

        assertEquals(listOf("c", "a", "b"), ids)
    }

    @Test
    fun `live commitments require one live scope and Orientation`() {
        val missingScope = validate(emptyList(), listOf(commitment()))
        assertViolation(missingScope, "MISSING_SCOPE")

        val tombstonedScope = validate(listOf(scope(isDeleted = true)), listOf(commitment()))
        assertViolation(tombstonedScope, "TOMBSTONED_SCOPE")

        val missingOrientation =
            validateProspectivePlanningState(
                scopes = listOf(scope()),
                commitments = listOf(commitment()),
                isOrientationLive = { false },
            )
        assertViolation(missingOrientation, "ORIENTATION_NOT_LIVE")
    }

    @Test
    fun `tombstoned commitment may retain historical missing references`() {
        val violations =
            validateProspectivePlanningState(
                scopes = emptyList(),
                commitments = listOf(commitment(isDeleted = true)),
                isOrientationLive = { false },
            )

        assertTrue(violations.isEmpty(), violations.toString())
    }

    @Test
    fun `record identities remain globally unique including tombstones`() {
        val duplicateScopes = listOf(scope(), scope(isDeleted = true))
        assertViolation(validate(duplicateScopes, emptyList()), "DUPLICATE_SCOPE_ID")

        val duplicateCommitments =
            listOf(
                commitment(isDeleted = true),
                commitment(isDeleted = true),
            )
        assertViolation(
            validate(listOf(scope()), duplicateCommitments),
            "DUPLICATE_COMMITMENT_ID",
        )
    }

    private fun assertValid(
        scopes: List<PlanningScope>,
        commitments: List<PlanningCommitment>,
    ) {
        val violations = validate(scopes, commitments)
        assertTrue(violations.isEmpty(), violations.toString())
    }

    private fun validate(
        scopes: List<PlanningScope>,
        commitments: List<PlanningCommitment>,
    ): List<PlanningContractViolation> =
        validateProspectivePlanningState(
            scopes = scopes,
            commitments = commitments,
            isOrientationLive = { it == ORIENTATION_ID },
        )

    private fun assertViolation(
        violations: List<PlanningContractViolation>,
        code: String,
    ) {
        assertTrue(violations.any { it.code == code }, violations.toString())
    }

    private fun scope(
        id: String = SCOPE_ID,
        title: String? = null,
        startsAt: Long? = null,
        endsAt: Long? = null,
        version: Long = 0L,
        isDeleted: Boolean = false,
    ) = PlanningScope(
        id = PlanningScopeId(id),
        kind = PlanningScopeKind.TACTICAL_CYCLE,
        lifecycle = PlanningScopeLifecycle.ACTIVE,
        title = title,
        startsAt = startsAt,
        endsAt = endsAt,
        createdAt = 1L,
        updatedAt = null,
        syncedAt = null,
        version = version,
        isDeleted = isDeleted,
    )

    private fun commitment(
        id: String = COMMITMENT_ID,
        scopeId: String = SCOPE_ID,
        orientationId: String = ORIENTATION_ID,
        role: PlanningRoleCode = PlanningRoleCode.MISSION,
        order: Long = 0L,
        priorityLevel: PlanningPriorityLevel? = null,
        provenance: PlanningProvenance = PlanningProvenance(PlanningProvenanceKind.MANUAL),
        version: Long = 0L,
        isDeleted: Boolean = false,
    ) = PlanningCommitment(
        id = PlanningCommitmentId(id),
        scopeId = PlanningScopeId(scopeId),
        orientationId = orientationId,
        role = role,
        order = order,
        priorityLevel = priorityLevel,
        status = PlanningCommitmentStatus.PLANNED,
        provenance = provenance,
        createdAt = 1L,
        updatedAt = null,
        syncedAt = null,
        version = version,
        isDeleted = isDeleted,
    )

    private companion object {
        const val SCOPE_ID = "scope"
        const val COMMITMENT_ID = "commitment"
        const val ORIENTATION_ID = "orientation"
    }
}
