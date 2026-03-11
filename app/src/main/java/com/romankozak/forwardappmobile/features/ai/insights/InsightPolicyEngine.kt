package com.romankozak.forwardappmobile.features.ai.insights

import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.ai.AiInsightEntity
import com.romankozak.forwardappmobile.domain.userawareness.UserAwarenessStateType
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.max

data class PlanBaseline(
    val hadNoDayPlan: Boolean,
    val focusCountBefore: Int,
)

data class InsightEvaluationResult(
    val insights: List<AiInsightEntity>,
    val staleInsightIds: Set<String>,
)

class InsightPolicyEngine
    @Inject
    constructor() {
        fun evaluate(
            records: List<ActivityRecord>,
            activeStateType: UserAwarenessStateType,
            hasFocusedContexts: Boolean,
            planBaseline: PlanBaseline,
            now: Long = System.currentTimeMillis(),
        ): InsightEvaluationResult {
            val todayStart = startOfDay(now)
            val yesterdayStart = todayStart - 24 * 60 * 60 * 1000
            val fiveHoursAgo = now - 5 * 60 * 60 * 1000
            val sevenDaysAgo = now - 7L * 24L * 60L * 60L * 1000L

            val todayRecords = records.filter { it.createdAt in todayStart until (todayStart + 24 * 60 * 60 * 1000) }
            val yesterdayRecords = records.filter { it.createdAt in yesterdayStart until todayStart }
            val lastFiveHours = records.filter { it.createdAt >= fiveHoursAgo }
            val lastDay = records.filter { it.createdAt >= yesterdayStart }
            val lastSevenDays = records.filter { it.createdAt >= sevenDaysAgo }

            val messages = mutableListOf<AiInsightEntity>()
            val staleIds = mutableSetOf<String>()

            if (todayRecords.isEmpty()) {
                messages.add(
                    AiInsightEntity(
                        id = TODAY_NO_ACTIVITY_INSIGHT_ID,
                        text = "Сьогодні ще не було активностей. Заплануй або відслідкуй невелику дію, щоб розігрітися.",
                        type = MessageType.WARNING.name,
                        timestamp = now,
                        isRead = false,
                    ),
                )
            }
            if (planBaseline.hadNoDayPlan) {
                messages.add(
                    AiInsightEntity(
                        id = DAY_PLAN_MISSING_INSIGHT_ID,
                        text = "На сьогодні не було плану дня. Створи/уточни план, щоб зафіксувати курс доби.",
                        type = MessageType.WARNING.name,
                        timestamp = now,
                        isRead = false,
                    ),
                )
            }
            if (planBaseline.focusCountBefore < TARGET_DAY_FOCUS_COUNT) {
                messages.add(
                    AiInsightEntity(
                        id = "day_focus_missing_${planBaseline.focusCountBefore}",
                        text = "У плані дня лише ${planBaseline.focusCountBefore}/$TARGET_DAY_FOCUS_COUNT фокуси #day_focus. Додай/уточни пріоритети на добу.",
                        type = MessageType.WARNING.name,
                        timestamp = now,
                        isRead = false,
                    ),
                )
            }
            if (activeStateType == UserAwarenessStateType.CRISIS && !hasFocusedContexts) {
                messages.add(
                    AiInsightEntity(
                        id = CRISIS_NO_FOCUS_CONTEXT_INSIGHT_ID,
                        text = "Кризовий режим активний, але фокус-контекстів немає. Створи окремий контекст для цієї кризи та познач його як фокус.",
                        type = MessageType.WARNING.name,
                        timestamp = now,
                        isRead = false,
                    ),
                )
            } else {
                staleIds.add(CRISIS_NO_FOCUS_CONTEXT_INSIGHT_ID)
            }
            if (lastSevenDays.size < TRACKER_USAGE_MIN_RECORDS_PER_WEEK) {
                messages.add(
                    AiInsightEntity(
                        id = TRACKER_UNDERUSED_INSIGHT_ID,
                        text = "Трекер активностей майже не використовується. Спробуй логувати більше дій протягом дня, щоб AI Insights давали точніші підказки.",
                        type = MessageType.INFO.name,
                        timestamp = now,
                        isRead = false,
                    ),
                )
            } else {
                staleIds.add(TRACKER_UNDERUSED_INSIGHT_ID)
            }

            buildFocusMessages(messages, "focus_5h", "останні 5 год", lastFiveHours, minMinutes = 20, now = now)
            buildFocusMessages(messages, "focus_24h", "добу", lastDay, minMinutes = 60, now = now)

            val yesterdayXp = yesterdayRecords.sumOf { it.xpGained ?: 0 }
            val yesterdayAnti = yesterdayRecords.sumOf { it.antyXp ?: 0 }
            if (yesterdayRecords.isNotEmpty() && (yesterdayXp + yesterdayAnti) <= 3) {
                messages.add(
                    AiInsightEntity(
                        id = YESTERDAY_LOW_ACTIVITY_INSIGHT_ID,
                        text = "Вчора було мало руху (+$yesterdayXp / -$yesterdayAnti). Спробуй запланувати один сфокусований блок сьогодні.",
                        type = MessageType.INFO.name,
                        timestamp = now,
                        isRead = false,
                    ),
                )
            }

            if (messages.isEmpty()) {
                messages.add(
                    AiInsightEntity(
                        id = KEEP_IT_UP_INSIGHT_ID,
                        text = "Продовжуй у тому ж дусі! Якщо хочеш, додай маленьку дію для підтримки ритму.",
                        type = MessageType.MOTIVATION.name,
                        timestamp = now,
                        isRead = false,
                    ),
                )
            }

            return InsightEvaluationResult(insights = messages, staleInsightIds = staleIds)
        }

        fun mergeWithPersistedFlags(
            generated: AiInsightEntity,
            persisted: AiInsightEntity,
            now: Long,
        ): AiInsightEntity {
            val todayStart = startOfDay(now)
            val persistedDayStart = startOfDay(persisted.timestamp)
            val shouldReopenUnreadToday = isDailyScaleInsightId(generated.id) && persistedDayStart < todayStart
            return generated.copy(
                isRead = if (shouldReopenUnreadToday) false else persisted.isRead,
                isFavorite = persisted.isFavorite,
                version = persisted.version,
                isDeleted = persisted.isDeleted,
                timestamp = if (shouldReopenUnreadToday) now else generated.timestamp,
            )
        }

        private fun buildFocusMessages(
            sink: MutableList<AiInsightEntity>,
            windowId: String,
            windowLabel: String,
            records: List<ActivityRecord>,
            minMinutes: Long,
            now: Long,
        ) {
            val grouped =
                records
                    .filter { it.contextId != null }
                    .groupBy { it.contextId!! }
                    .mapValues { entry -> entry.value.sumOf { durationMinutes(it) } }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(3)
                    .filter { it.second >= minMinutes }

            grouped.forEachIndexed { index, (contextId, minutes) ->
                sink.add(
                    AiInsightEntity(
                        id = "${windowId}_${contextId}_$index",
                        text = "Фокус за $windowLabel: проєкт $contextId ~ $minutes хв.",
                        type = MessageType.INFO.name,
                        timestamp = now,
                        isRead = false,
                    ),
                )
            }
        }

        private fun isDailyScaleInsightId(id: String): Boolean {
            if (id.startsWith("day_focus_missing_")) return true
            return id in DAILY_SCALE_INSIGHT_IDS
        }

        private fun durationMinutes(record: ActivityRecord): Long = record.durationInMillis?.let { max(1L, it / 60_000) } ?: 1L

        private fun startOfDay(timestamp: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = timestamp
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        companion object {
            private const val TARGET_DAY_FOCUS_COUNT = 3
            private const val TRACKER_USAGE_MIN_RECORDS_PER_WEEK = 2

            private const val TODAY_NO_ACTIVITY_INSIGHT_ID = "today_no_activity"
            private const val DAY_PLAN_MISSING_INSIGHT_ID = "day_plan_missing"
            private const val YESTERDAY_LOW_ACTIVITY_INSIGHT_ID = "yesterday_low_activity"
            private const val KEEP_IT_UP_INSIGHT_ID = "keep_it_up"
            const val CRISIS_NO_FOCUS_CONTEXT_INSIGHT_ID = "crisis_no_focus_context"
            const val TRACKER_UNDERUSED_INSIGHT_ID = "tracker_underused"

            private val DAILY_SCALE_INSIGHT_IDS =
                setOf(
                    TODAY_NO_ACTIVITY_INSIGHT_ID,
                    DAY_PLAN_MISSING_INSIGHT_ID,
                    YESTERDAY_LOW_ACTIVITY_INSIGHT_ID,
                    TRACKER_UNDERUSED_INSIGHT_ID,
                )
        }
    }
