package com.romankozak.forwardappmobile.sync

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.Converters
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.tactical.tactical.TacticalMissionSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toSnapshot
import org.junit.Test

class MissionStatusCompatibilityTest {
    private val converters = Converters()

    @Test
    fun `room converter supports new mission statuses`() {
        assertThat(converters.toMissionStatus("ACTIVE")).isEqualTo(MissionStatus.ACTIVE)
        assertThat(converters.toMissionStatus("INACTIVE")).isEqualTo(MissionStatus.INACTIVE)
        assertThat(converters.toMissionStatus("PAUSED")).isEqualTo(MissionStatus.PAUSED)
    }

    @Test
    fun `room converter keeps compatibility with legacy statuses`() {
        assertThat(converters.toMissionStatus("IN_PROGRESS")).isEqualTo(MissionStatus.ACTIVE)
        assertThat(converters.toMissionStatus("PENDING")).isEqualTo(MissionStatus.INACTIVE)
        assertThat(converters.toMissionStatus("OVERDUE")).isEqualTo(MissionStatus.PAUSED)
        assertThat(converters.toMissionStatus("COMPLETED")).isEqualTo(MissionStatus.COMPLETED)
    }

    @Test
    fun `snapshot mapper supports new and legacy mission statuses`() {
        val active =
            TacticalMissionSnapshot(
                id = 1L,
                title = "Active",
                description = null,
                startTime = null,
                deadline = 100L,
                status = "ACTIVE",
                priority = "MEDIUM",
                projectId = null,
                linkedProjectIds = emptyList(),
                linkedAttachmentIds = emptyList(),
                order = 0L,
            ).toEntity()

        val pendingLegacy =
            TacticalMissionSnapshot(
                id = 2L,
                title = "Legacy pending",
                description = null,
                startTime = null,
                deadline = 100L,
                status = "PENDING",
                priority = "MEDIUM",
                projectId = null,
                linkedProjectIds = emptyList(),
                linkedAttachmentIds = emptyList(),
                order = 0L,
            ).toEntity()

        assertThat(active.status).isEqualTo(MissionStatus.ACTIVE)
        assertThat(pendingLegacy.status).isEqualTo(MissionStatus.INACTIVE)
    }

    @Test
    fun `snapshot mapper preserves mission stream id`() {
        val mission =
            TacticalMission(
                id = 10L,
                title = "Manual work",
                description = "Weekly manual stream item",
                deadline = 1_000L,
                projectId = null,
                linkedProjectIds = emptyList(),
                linkedAttachmentIds = emptyList(),
                order = 3L,
                missionStreamId = "manual-work",
            )

        val snapshot = mission.toSnapshot()
        val restoredMission = snapshot.toEntity()

        assertThat(snapshot.missionStreamId).isEqualTo("manual-work")
        assertThat(restoredMission.missionStreamId).isEqualTo("manual-work")
    }
}
