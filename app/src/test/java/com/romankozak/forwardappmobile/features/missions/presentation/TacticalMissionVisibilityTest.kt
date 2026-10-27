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

    @Test
    fun `carried mission with historical backlog provenance is visible but original backlog row stays separate`() {
        val cycle = draftIteration()
        val carried = mission(id = 2, iterationId = cycle.id, streamId = "general").copy(
            sourceType = com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionSourceType.PREVIOUS_WEEK,
            sourceBacklogItemId = "old-entry", carriedFromMissionId = 1,
        )
        val original = carried.copy(id = 3,
            sourceType = com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionSourceType.CONTEXT_BACKLOG_ITEM,
            carriedFromMissionId = null)
        val visible = buildVisibleTacticalMissions(listOf(original, carried), TacticsWorkspaceMode.PLAN,
            "general", listOf(stream("general")), cycle, CURRENT_WEEK)
        assertThat(visible).containsExactly(carried)
        assertThat(carried.sourceBacklogItemId).isEqualTo("old-entry")
    }

    @Test
    fun `carried backlog-origin mission is visible in its selected execution stream`() {
        val cycle = draftIteration().copy(status = TacticalIterationStatus.ACTIVE)
        val carried = mission(id = 2, iterationId = cycle.id, streamId = "work").copy(
            sourceType = com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionSourceType.PREVIOUS_WEEK,
            sourceBacklogItemId = "old-entry", carriedFromMissionId = 1,
        )
        assertThat(buildVisibleTacticalMissions(listOf(carried), TacticsWorkspaceMode.STREAMS,
            "work", listOf(stream("general"), stream("work")), cycle, CURRENT_WEEK)).containsExactly(carried)
        assertThat(buildVisibleTacticalMissions(listOf(carried), TacticsWorkspaceMode.STREAMS,
            "general", listOf(stream("general"), stream("work")), cycle, CURRENT_WEEK)).isEmpty()
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
