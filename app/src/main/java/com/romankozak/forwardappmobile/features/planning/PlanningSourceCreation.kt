package com.romankozak.forwardappmobile.features.planning

import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.NewTaskParameters
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.*
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.mainscreen.arc.ArcQuestRepository
import com.romankozak.forwardappmobile.features.missions.domain.repository.MissionRepository

/** Product intent adapters only: creation/transaction/lifecycle stay with each domain authority. */
suspend fun createTacticalFromEntity(
    repository: MissionRepository,
    entity: CanonicalEntitySummary,
    weekKey: String,
    iterationId: String?,
    streamId: String?,
): Long {
    com.romankozak.forwardappmobile.StartupTrace.mark("Planning.Mission.confirmed")
    val now = System.currentTimeMillis()
    return com.romankozak.forwardappmobile.StartupTrace.measure("Planning.Mission.persist") { repository.insertMissionWithAutoOrder(TacticalMission(
        title = entity.title, description = null, startTime = now, deadline = now + 7L * 24 * 60 * 60 * 1000,
        status = MissionStatus.ACTIVE, priority = MissionPriority.MEDIUM, projectId = null,
        weekKey = weekKey, iterationId = iterationId, missionStreamId = streamId, sourceType = MissionSourceType.MANUAL,
    ), entity.ref.toPlanningTarget()) }
}

suspend fun createQuestFromEntity(repository: ArcQuestRepository, entity: CanonicalEntitySummary, arcKey: String): String {
    val quest = ArcQuestEntity(arcKey = arcKey, title = entity.title)
    com.romankozak.forwardappmobile.StartupTrace.measure("Planning.Quest.persist") {
        repository.addQuest(quest, entity.ref.toPlanningTarget())
    }
    return quest.id
}

suspend fun createDayTaskFromEntity(repository: DayManagementRepository, entity: CanonicalEntitySummary, dayPlanId: String, order: Long) {
    repository.addTaskToDayPlan(NewTaskParameters(
        dayPlanId = dayPlanId, title = entity.title, order = order, planningSource = entity.ref.toPlanningTarget(),
    ))
}
