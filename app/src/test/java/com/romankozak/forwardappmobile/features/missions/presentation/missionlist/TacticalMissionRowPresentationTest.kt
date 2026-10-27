package com.romankozak.forwardappmobile.features.missions.presentation.missionlist

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionPriority
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.NO_DEADLINE
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.features.missions.presentation.buildCompactMissionMeta
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedItemState
import org.junit.Test

class TacticalMissionRowPresentationTest {
    private fun mission(id: Long, status: MissionStatus = MissionStatus.ACTIVE) =
        TacticalMission(
            id = id,
            title = "Mission $id",
            description = null,
            deadline = NO_DEADLINE,
            status = status,
            projectId = null,
        )

    @Test fun `no deadline does not create a metadata chip`() {
        assertThat(missionDeadlineLabel(mission(1))).isNull()
    }

    @Test fun `actual deadline remains visible`() {
        val mission = mission(1).copy(deadline = 1_800_000_000_000L)
        assertThat(missionDeadlineLabel(mission)).isEqualTo(formatMissionDate(mission.deadline))
        assertThat(missionDeadlineLabel(mission)).isNotEqualTo("Без дедлайну")
    }

    @Test fun `color-only priorities retain distinct accessible labels`() {
        val labels = MissionPriority.entries.map { it.missionPriorityDescription() }
        assertThat(labels).hasSize(4)
        assertThat(labels.distinct()).hasSize(4)
        assertThat(labels.all { it.isNotBlank() }).isTrue()
    }

    @Test fun `flat list preserves exact supplied manual order and mission objects`() {
        val missions = listOf(mission(3, MissionStatus.COMPLETED), mission(1), mission(2, MissionStatus.PAUSED))
        assertThat(orderedMissions(missions, showStatusSections = false)).isSameInstanceAs(missions)
    }

    @Test fun `status sections retain relative order inside each section`() {
        val missions = listOf(mission(5, MissionStatus.COMPLETED), mission(2, MissionStatus.PAUSED),
            mission(3), mission(4, MissionStatus.INACTIVE), mission(1))
        assertThat(orderedMissions(missions, showStatusSections = true).map { it.id })
            .containsExactly(3L, 1L, 2L, 4L, 5L).inOrder()
    }

    @Test fun `completed and overdue visual states retain existing semantics`() {
        assertThat(missionItemState(MissionStatus.COMPLETED, overdue = true)).isEqualTo(UnifiedItemState.COMPLETED)
        assertThat(missionItemState(MissionStatus.ACTIVE, overdue = true)).isEqualTo(UnifiedItemState.OVERDUE)
        assertThat(missionItemState(MissionStatus.PAUSED, overdue = false)).isEqualTo(UnifiedItemState.DEFAULT)
    }

    @Test fun `linked metadata keeps unique nonblank exact target identities`() {
        assertThat(listOf(" workspace ", "", "workspace", "attachment").normalizedLinkedIds())
            .containsExactly("workspace", "attachment").inOrder()
    }

    @Test fun `execution rows without stream or status do not reserve metadata space`() {
        assertThat(buildCompactMissionMeta(mission(1), null, showStream = true, isPaused = false)).isEmpty()
        assertThat(buildCompactMissionMeta(mission(1), "Work", showStream = false, isPaused = false)).isEmpty()
    }

    @Test fun `execution rows retain stream and paused inactive metadata`() {
        assertThat(buildCompactMissionMeta(mission(1, MissionStatus.PAUSED), "Work", showStream = true, isPaused = true))
            .isEqualTo("Work · Paused")
        assertThat(buildCompactMissionMeta(mission(1, MissionStatus.INACTIVE), null, showStream = true, isPaused = false))
            .isEqualTo("Inactive")
    }
}
