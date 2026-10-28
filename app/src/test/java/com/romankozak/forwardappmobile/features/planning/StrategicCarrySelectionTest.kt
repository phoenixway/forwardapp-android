package com.romankozak.forwardappmobile.features.planning

import com.romankozak.forwardappmobile.data.planning.strategic.*
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentId
import org.junit.Assert.*
import org.junit.Test

class StrategicCarrySelectionTest {
    @Test fun `completed components start unselected without being forbidden`() {
        val active = StrategicCarryComponent(PlanningCommitmentId("active"), "Active", false)
        val completed = StrategicCarryComponent(PlanningCommitmentId("completed"), "Completed", true)
        val options = listOf(active, completed)
        assertEquals(setOf(active.id), defaultStrategicCarrySelection(options))
        assertEquals(listOf(active, completed), options)
        assertEquals(emptySet<PlanningCommitmentId>(), defaultStrategicCarrySelection(listOf(completed)))
    }
}
