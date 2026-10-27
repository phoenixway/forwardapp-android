package com.romankozak.forwardappmobile.features.workspace

import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.GENERAL_MISSION_STREAM_ID
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.mainscreen.arc.ArcQuestRepository
import com.romankozak.forwardappmobile.features.missions.domain.repository.*
import com.romankozak.forwardappmobile.features.planning.*
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.first

enum class WorkspaceSendTarget(val label: String, val subtitle: String) {
    DAY_TASK("Завдання дня", "Сьогодні"),
    TACTICAL_MISSION("Тактична місія", "Поточний цикл · General"),
    ARC_QUEST("Квест арки", "Поточна арка"),
}

/** Intent adapter only: the established Planning writers own creation and SOURCE lifecycle. */
class WorkspaceSendToActions @Inject constructor(
    private val catalog: CanonicalEntityCatalogRepository,
    private val days: DayManagementRepository,
    private val dayTasks: DayTaskDao,
    private val missions: MissionRepository,
    private val iterations: TacticalIterationRepository,
    private val streams: MissionStreamRepository,
    private val quests: ArcQuestRepository,
) {
    suspend fun source(workspaceId: String): CanonicalEntitySummary =
        requireNotNull(catalog.observeCatalog().first().singleOrNull { it.ref == CanonicalEntityRef.Workspace(workspaceId) }) {
            "Workspace недоступний"
        }

    suspend fun send(workspaceId: String, target: WorkspaceSendTarget, now: Long = System.currentTimeMillis()) {
        val entity = source(workspaceId)
        val date = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
        when (target) {
            WorkspaceSendTarget.DAY_TASK -> {
                val plan = days.createOrUpdateDayPlan(now)
                val order = (dayTasks.getMinOrderForDayPlan(plan.id) ?: 0L) - 1L
                createDayTaskFromEntity(days, entity, plan.id, order)
            }
            WorkspaceSendTarget.TACTICAL_MISSION -> {
                val cycle = iterations.getCurrentIteration()
                val fields = WeekFields.ISO
                val week = String.format(Locale.ROOT, "%04d-W%02d", date.get(fields.weekBasedYear()), date.get(fields.weekOfWeekBasedYear()))
                streams.ensureDefaultStream()
                createTacticalFromEntity(missions, entity, week, cycle?.id, GENERAL_MISSION_STREAM_ID)
            }
            WorkspaceSendTarget.ARC_QUEST -> createQuestFromEntity(quests, entity, YearMonth.from(date).toString())
        }
    }
}
