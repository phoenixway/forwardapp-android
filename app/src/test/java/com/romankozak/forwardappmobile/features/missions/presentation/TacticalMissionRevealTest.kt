package com.romankozak.forwardappmobile.features.missions.presentation

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.*
import org.junit.Test

class TacticalMissionRevealTest {
    private fun row(id: Long, status: MissionStatus) = TacticalMission(id = id, title = "Mission", description = null,
        projectId = null, deadline = 100, status = status)

    @Test fun `draft reveal uses unfinished rendered rows rather than unfiltered input index`() {
        val rows = listOf(row(1, MissionStatus.COMPLETED), row(2, MissionStatus.ACTIVE), row(3, MissionStatus.INACTIVE))
        assertThat(tacticalRenderedMissionIds(rows, TacticalIterationStatus.DRAFT)).containsExactly(2L, 3L).inOrder()
    }

    @Test fun `execution reveal retains exact compact list order`() {
        val rows = listOf(row(1, MissionStatus.COMPLETED), row(2, MissionStatus.ACTIVE))
        assertThat(tacticalRenderedMissionIds(rows, TacticalIterationStatus.ACTIVE)).containsExactly(1L, 2L).inOrder()
    }

    @Test fun `sectioned reveal uses actual status section order`() {
        val rows = listOf(row(1, MissionStatus.COMPLETED), row(2, MissionStatus.INACTIVE), row(3, MissionStatus.ACTIVE))
        assertThat(tacticalRenderedMissionIds(rows, null)).containsExactly(3L, 2L, 1L).inOrder()
    }
}
