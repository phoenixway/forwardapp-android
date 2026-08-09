package com.romankozak.forwardappmobile.features.missions.domain.repository

import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationType
import com.romankozak.forwardappmobile.features.missions.data.TacticalIterationDao
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TacticalIterationRepository
    @Inject
    constructor(
        private val tacticalIterationDao: TacticalIterationDao,
    ) {
        fun observeIterations(): Flow<List<TacticalIteration>> = tacticalIterationDao.observeIterations()

        suspend fun ensureActiveIteration(currentWeekKey: String): TacticalIteration {
            return tacticalIterationDao.getActiveIteration() ?: createTimeboxedIteration(currentWeekKey)
        }

        suspend fun getById(iterationId: String): TacticalIteration? = tacticalIterationDao.getById(iterationId)

        suspend fun closeActiveAndStartOpenEnded(title: String? = null): TacticalIteration {
            val now = System.currentTimeMillis()
            tacticalIterationDao.getActiveIteration()?.let { active ->
                tacticalIterationDao.closeIteration(active.id, now)
            }
            val iteration =
                TacticalIteration(
                    id = UUID.randomUUID().toString(),
                    title = title?.trim()?.takeIf { it.isNotBlank() } ?: "Відкрита тактична ітерація",
                    startedAt = now,
                    plannedEndAt = null,
                    status = TacticalIterationStatus.ACTIVE,
                    type = TacticalIterationType.OPEN_ENDED,
                    weekKey = null,
                    createdAt = now,
                    updatedAt = now,
                    version = 1L,
                )
            tacticalIterationDao.insert(iteration)
            return iteration
        }

        suspend fun closeActiveAndStartTimeboxed(currentWeekKey: String): TacticalIteration {
            val now = System.currentTimeMillis()
            tacticalIterationDao.getActiveIteration()?.let { active ->
                tacticalIterationDao.closeIteration(active.id, now)
            }
            return createTimeboxedIteration(currentWeekKey, now)
        }

        private suspend fun createTimeboxedIteration(
            weekKey: String,
            now: Long = System.currentTimeMillis(),
        ): TacticalIteration {
            val start = weekStartMillis(weekKey) ?: now
            val iteration =
                TacticalIteration(
                    id = weekKey,
                    title = formatWeekTitle(weekKey),
                    startedAt = start,
                    plannedEndAt = start + DAYS_PER_WEEK * MILLIS_PER_DAY,
                    status = TacticalIterationStatus.ACTIVE,
                    type = TacticalIterationType.TIMEBOXED,
                    weekKey = weekKey,
                    createdAt = now,
                    updatedAt = now,
                    version = 1L,
                )
            tacticalIterationDao.insert(iteration)
            return iteration
        }

        private fun weekStartMillis(weekKey: String): Long? =
            runCatching {
                val match = Regex("""^(\d{4})-W(\d{2})$""").matchEntire(weekKey) ?: error("Invalid week key")
                val year = match.groupValues[1].toInt()
                val week = match.groupValues[2].toInt()
                LocalDate.now()
                    .with(WeekFields.ISO.weekBasedYear(), year.toLong())
                    .with(WeekFields.ISO.weekOfWeekBasedYear(), week.toLong())
                    .with(DayOfWeek.MONDAY)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()

        private fun formatWeekTitle(weekKey: String): String {
            val start = weekStartMillis(weekKey) ?: return weekKey
            val date = java.time.Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDate()
            val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
            return "$weekKey · ${date.format(formatter)} - ${date.plusDays(DAYS_PER_WEEK - 1).format(formatter)}"
        }

        private companion object {
            const val DAYS_PER_WEEK = 7L
            const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
        }
    }
