package com.romankozak.forwardappmobile.features.missions.domain.repository

import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationType
import com.romankozak.forwardappmobile.data.planning.tactical.CanonicalTacticalPlanningAdapter
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
        private val canonicalAdapter: CanonicalTacticalPlanningAdapter,
    ) {
        fun observeIterations(): Flow<List<TacticalIteration>> =
            canonicalAdapter.observeIterations()

        suspend fun getCurrentIteration(): TacticalIteration? =
            canonicalAdapter.getCurrentIteration()

        suspend fun ensureActiveIteration(currentWeekKey: String): TacticalIteration =
            canonicalAdapter.getCurrentIteration()
                ?: createTimeboxedIteration(currentWeekKey)

        suspend fun getById(iterationId: String): TacticalIteration? =
            canonicalAdapter.getById(iterationId)

        suspend fun ensureDraftTimeboxed(currentWeekKey: String): TacticalIteration =
            canonicalAdapter.getCurrentIteration()
                ?: createTimeboxedIteration(
                    weekKey = currentWeekKey,
                    status = TacticalIterationStatus.DRAFT,
                )

        suspend fun startCurrentOrCreateTimeboxed(currentWeekKey: String): TacticalIteration {
            val current = canonicalAdapter.getCurrentIteration()
            if (current?.status == TacticalIterationStatus.DRAFT) {
                val now = System.currentTimeMillis()
                val started =
                    current.copy(
                        status = TacticalIterationStatus.ACTIVE,
                        startedAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        version = current.version + 1L,
                    )
                canonicalAdapter.saveIteration(started)
                return started
            }
            return current
                ?: createTimeboxedIteration(
                    currentWeekKey,
                    status = TacticalIterationStatus.ACTIVE,
                )
        }

        suspend fun finishCurrentIteration(): TacticalIteration? {
            val current = canonicalAdapter.getCurrentIteration() ?: return null
            val now = System.currentTimeMillis()
            val closed =
                current.copy(
                    status = TacticalIterationStatus.CLOSED,
                    closedAt = now,
                    updatedAt = now,
                    syncedAt = null,
                    version = current.version + 1L,
                )
            canonicalAdapter.saveIteration(closed)
            return closed
        }

        suspend fun finishCurrentAndCreateDraft(currentWeekKey: String): TacticalIteration {
            finishCurrentIteration()
            return createTimeboxedIteration(
                weekKey = currentWeekKey,
                status = TacticalIterationStatus.DRAFT,
            )
        }

        suspend fun closeActiveAndStartOpenEnded(title: String? = null): TacticalIteration {
            closeCurrentIfPresent()
            val now = System.currentTimeMillis()
            val iteration =
                TacticalIteration(
                    id = UUID.randomUUID().toString(),
                    title = title?.trim()?.takeIf { it.isNotBlank() } ?: "Відкрита тактична ітерація",
                    startedAt = now,
                    plannedEndAt = null,
                    status = TacticalIterationStatus.DRAFT,
                    type = TacticalIterationType.OPEN_ENDED,
                    weekKey = null,
                    createdAt = now,
                    updatedAt = now,
                    version = 1L,
                )
            canonicalAdapter.saveIteration(iteration)
            return iteration
        }

        suspend fun closeActiveAndStartTimeboxed(currentWeekKey: String): TacticalIteration {
            val now = System.currentTimeMillis()
            closeCurrentIfPresent(now)
            return createTimeboxedIteration(
                weekKey = currentWeekKey,
                now = now,
                status = TacticalIterationStatus.DRAFT,
            )
        }

        private suspend fun closeCurrentIfPresent(now: Long = System.currentTimeMillis()) {
            canonicalAdapter.getCurrentIteration()?.let { current ->
                val closed =
                    current.copy(
                        status = TacticalIterationStatus.CLOSED,
                        closedAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        version = current.version + 1L,
                    )
                canonicalAdapter.saveIteration(closed)
            }
        }

        private suspend fun createTimeboxedIteration(
            weekKey: String,
            now: Long = System.currentTimeMillis(),
            status: TacticalIterationStatus = TacticalIterationStatus.ACTIVE,
        ): TacticalIteration {
            val start = weekStartMillis(weekKey) ?: now
            val iterationId =
                if (canonicalAdapter.getById(weekKey) == null) {
                    weekKey
                } else {
                    "${weekKey}-${UUID.randomUUID()}"
                }
            val iteration =
                TacticalIteration(
                    id = iterationId,
                    title = formatWeekTitle(weekKey),
                    startedAt = start,
                    plannedEndAt = start + DAYS_PER_WEEK * MILLIS_PER_DAY,
                    status = status,
                    type = TacticalIterationType.TIMEBOXED,
                    weekKey = weekKey,
                    createdAt = now,
                    updatedAt = now,
                    version = 1L,
                )
            canonicalAdapter.saveIteration(iteration)
            return iteration
        }

        private fun weekStartMillis(weekKey: String): Long? =
            runCatching {
                val match =
                    Regex("""^(\d{4})-W(\d{2})$""").matchEntire(weekKey)
                        ?: error("Invalid week key")
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
            val date =
                java.time.Instant.ofEpochMilli(start)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
            return "$weekKey · ${date.format(formatter)} - ${date.plusDays(DAYS_PER_WEEK - 1).format(formatter)}"
        }

        private companion object {
            const val DAYS_PER_WEEK = 7L
            const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
        }
    }
