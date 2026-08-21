package com.romankozak.forwardappmobile.features.missions.presentation

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStream
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.NO_DEADLINE
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationType
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import org.junit.Test

class TacticalMissionVisibilityTest {
    @Test
    fun `draft iteration shows new mission even when restored workspace mode filters another stream`() {
        val iteration = draftIteration()
        val newlyAddedMission = mission(id = 1, iterationId = iteration.id, streamId = "work")

        val visible =
            buildVisibleTacticalMissions(
                allMissions = listOf(newlyAddedMission),
                requestedMode = TacticsWorkspaceMode.STREAMS,
                selectedStreamId = "general",
                streams = listOf(stream("general"), stream("work")),
                activeIteration = iteration,
                currentWeekKey = CURRENT_WEEK,
            )

        assertThat(visible).containsExactly(newlyAddedMission)
    }

    @Test
    fun `active iteration still applies selected stream filter`() {
        val iteration = draftIteration().copy(status = TacticalIterationStatus.ACTIVE)
        val generalMission = mission(id = 1, iterationId = iteration.id, streamId = "general")
        val workMission = mission(id = 2, iterationId = iteration.id, streamId = "work")

        val visible =
            buildVisibleTacticalMissions(
                allMissions = listOf(generalMission, workMission),
                requestedMode = TacticsWorkspaceMode.STREAMS,
                selectedStreamId = "general",
                streams = listOf(stream("general"), stream("work")),
                activeIteration = iteration,
                currentWeekKey = CURRENT_WEEK,
            )

        assertThat(visible).containsExactly(generalMission)
    }

    @Test
    fun `implicit current week includes new and previously hidden missions without active iteration`() {
        val newMission = mission(id = 1, iterationId = null, streamId = "general")
        val previouslyHiddenMission = mission(id = 2, iterationId = CURRENT_WEEK, streamId = "general")

        val visible =
            buildVisibleTacticalMissions(
                allMissions = listOf(newMission, previouslyHiddenMission),
                requestedMode = TacticsWorkspaceMode.ALL,
                selectedStreamId = "general",
                streams = listOf(stream("general")),
                activeIteration = null,
                currentWeekKey = CURRENT_WEEK,
            )

        assertThat(visible).containsExactly(newMission, previouslyHiddenMission)
    }

    private fun draftIteration(): TacticalIteration =
        TacticalIteration(
            id = "draft-cycle",
            title = "Draft",
            startedAt = 1L,
            status = TacticalIterationStatus.DRAFT,
            type = TacticalIterationType.TIMEBOXED,
            weekKey = CURRENT_WEEK,
        )

    private fun mission(
        id: Long,
        iterationId: String?,
        streamId: String,
    ): TacticalMission =
        TacticalMission(
            id = id,
            title = "Mission $id",
            description = null,
            deadline = NO_DEADLINE,
            projectId = null,
            weekKey = CURRENT_WEEK,
            iterationId = iterationId,
            missionStreamId = streamId,
        )

    private fun stream(id: String): MissionStream = MissionStream(id = id, title = id)

    private companion object {
        const val CURRENT_WEEK = "2026-W34"
    }
}
