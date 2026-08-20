package com.romankozak.forwardappmobile.data.recurrence

import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.data.dao.CanonicalFocusSplitSourceVersion
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.compareLocalDayKeys
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.localDayKeyDayOfWeek
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.localDayKeyOf
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.previousLocalDayKey
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.recurrenceOccurrenceId
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.recurrenceRuleMatchesDay
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusTemplate
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringResponsibilitySeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeriesKind
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android authoring boundary for canonical recurring focus/responsibility series.
 *
 * New recurrence-v2 authoring must not create legacy recurringKey/isEveryday
 * aliases. The canonical series is persisted first and the shared canonical
 * materializer creates the concrete DayFocusItem occurrence.
 */
@Singleton
class CanonicalFocusRecurrenceAuthoringAdapter
    @Inject
    constructor(
        private val appDatabase: AppDatabase,
        private val materializationAdapter: CanonicalRecurrenceMaterializationAdapter,
    ) {
        suspend fun createDailySeriesForPlan(
            dayPlanId: String,
            title: String,
            notes: String?,
            relatedLinks: List<RelatedLink>,
            type: DayFocusType,
            budgetPercent: Int?,
        ): DayFocusItem =
            createSeriesForPlan(
                dayPlanId = dayPlanId,
                title = title,
                notes = notes,
                relatedLinks = relatedLinks,
                type = type,
                budgetPercent = budgetPercent,
                rule =
                    RecurrenceRule(
                        frequency = RecurrenceFrequency.DAILY,
                        interval = 1,
                        daysOfWeek = null,
                    ),
            )

        suspend fun createSeriesForPlan(
            dayPlanId: String,
            title: String,
            notes: String?,
            relatedLinks: List<RelatedLink>,
            type: DayFocusType,
            budgetPercent: Int?,
            rule: RecurrenceRule,
        ): DayFocusItem {
            val dayPlan =
                checkNotNull(appDatabase.dayPlanDao().getPlanById(dayPlanId)) {
                    "Cannot create canonical recurring focus: DayPlan not found: $dayPlanId"
                }

            val now = System.currentTimeMillis()
            val seriesId = UUID.randomUUID().toString()
            val startDayKey = canonicalLocalDayKey(dayPlan.date)
            val canonicalRule = rule.normalizedForSeriesStart(startDayKey)
            val template =
                RecurringFocusTemplate(
                    title = title,
                    notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                    relatedLinks = relatedLinks.toCanonicalRelatedLinks(),
                    budgetPercent = budgetPercent,
                )

            val (kind, series) =
                buildCanonicalFocusSeries(
                    seriesId = seriesId,
                    now = now,
                    startDayKey = startDayKey,
                    type = type,
                    rule = canonicalRule,
                    template = template,
                )

            appDatabase.canonicalRecurringSeriesDao().insert(series.toAndroidEntity())

            val materialization =
                materializationAdapter.materializeForDate(
                    date = dayPlan.date,
                    now = now,
                )
            val occurrenceId =
                recurrenceOccurrenceId(
                    kind = kind,
                    seriesId = seriesId,
                    dayKey = materialization.dayKey,
                )

            return checkNotNull(
                appDatabase.dayFocusItemDao().getByIdForCanonicalRecurrenceSync(occurrenceId),
            ) {
                "Canonical recurring focus occurrence was not materialized: " +
                    "$seriesId@${materialization.dayKey}"
            }
        }

        suspend fun convertOneOffToSeries(
            item: DayFocusItem,
            title: String,
            notes: String?,
            relatedLinks: List<RelatedLink>,
            type: DayFocusType,
            budgetPercent: Int?,
            rule: RecurrenceRule,
        ): DayFocusItem {
            require(item.recurrenceSeriesId == null) {
                "Cannot convert already-recurring focus item ${item.id}"
            }
            require(!item.isDeleted) {
                "Cannot convert deleted focus item ${item.id}"
            }

            val dayPlan =
                checkNotNull(appDatabase.dayPlanDao().getPlanById(item.dayPlanId)) {
                    "Cannot convert focus to canonical recurrence: DayPlan not found: ${item.dayPlanId}"
                }

            val now = System.currentTimeMillis()
            val seriesId = UUID.randomUUID().toString()
            val startDayKey = canonicalLocalDayKey(dayPlan.date)
            val canonicalRule = rule.normalizedForSeriesStart(startDayKey)
            val template =
                RecurringFocusTemplate(
                    title = title,
                    notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                    relatedLinks = relatedLinks.toCanonicalRelatedLinks(),
                    budgetPercent = budgetPercent,
                )
            val (kind, series) =
                buildCanonicalFocusSeries(
                    seriesId = seriesId,
                    now = now,
                    startDayKey = startDayKey,
                    type = type,
                    rule = canonicalRule,
                    template = template,
                )

            val occurrence =
                item.copy(
                    id =
                        recurrenceOccurrenceId(
                            kind = kind,
                            seriesId = seriesId,
                            dayKey = startDayKey,
                        ),
                    title = template.title,
                    notes = template.notes,
                    relatedLinks = template.relatedLinks.toAndroidRelatedLinks(),
                    type = type,
                    isEveryday = false,
                    recurringKey = null,
                    recurrenceSeriesId = seriesId,
                    recurrenceOccurrenceDayKey = startDayKey,
                    recurrenceSourceSeriesVersion = series.version,
                    budgetPercent = template.budgetPercent,
                    createdAt = now,
                    updatedAt = now,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1,
                )

            appDatabase.canonicalRecurringSeriesDao().convertOneOffToCanonicalSeries(
                series = series.toAndroidEntity(),
                occurrence = occurrence,
                sourceItemId = item.id,
                sourceExpectedVersion = item.version,
                updatedAt = now,
            )

            return occurrence
        }

        suspend fun splitSeriesFromOccurrence(
            item: DayFocusItem,
            title: String,
            notes: String?,
            relatedLinks: List<RelatedLink>,
            budgetPercent: Int?,
            rule: RecurrenceRule,
        ): DayFocusItem {
            val oldSeriesId =
                requireNotNull(item.recurrenceSeriesId) {
                    "Cannot split recurrence for non-recurring focus item ${item.id}"
                }
            val splitDayKey =
                requireNotNull(item.recurrenceOccurrenceDayKey) {
                    "Cannot split canonical focus without occurrence day key: ${item.id}"
                }
            require(!item.isDeleted) {
                "Cannot split recurrence from deleted focus item ${item.id}"
            }

            val now = System.currentTimeMillis()
            val seriesDao = appDatabase.canonicalRecurringSeriesDao()
            val storedSeries =
                checkNotNull(seriesDao.getById(oldSeriesId)) {
                    "Canonical recurring focus series not found: $oldSeriesId"
                }.toCanonicalSeries()

            check(!storedSeries.isDeleted) {
                "Cannot split deleted canonical recurring focus series: $oldSeriesId"
            }
            check(compareLocalDayKeys(splitDayKey, storedSeries.startDayKey) >= 0L) {
                "Split day $splitDayKey precedes series start ${storedSeries.startDayKey}"
            }
            storedSeries.endDayKey?.let { endDayKey ->
                check(compareLocalDayKeys(splitDayKey, endDayKey) <= 0L) {
                    "Split day $splitDayKey is after series end $endDayKey"
                }
            }

            val expectedType = storedSeries.focusType()
            check(item.type == expectedType) {
                "Canonical focus type ${item.type} does not match series kind ${storedSeries.kind}"
            }

            val oldTemplate = storedSeries.focusTemplate()
            val occurrences = seriesDao.getFocusOccurrencesForSeries(oldSeriesId)
            val selectedStoredOccurrence =
                checkNotNull(
                    occurrences.firstOrNull { occurrence ->
                        occurrence.id == item.id && !occurrence.isDeleted
                    },
                ) {
                    "Selected canonical focus occurrence not found or deleted: ${item.id}"
                }
            check(selectedStoredOccurrence.recurrenceOccurrenceDayKey == splitDayKey) {
                "Selected occurrence day changed while preparing split: ${item.id}"
            }

            val newSeriesId = UUID.randomUUID().toString()
            val canonicalRule = rule.normalizedForSeriesStart(splitDayKey)
            val nextTemplate =
                RecurringFocusTemplate(
                    title = title,
                    notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                    relatedLinks = relatedLinks.toCanonicalRelatedLinks(),
                    budgetPercent = budgetPercent,
                )
            val (newKind, newSeries) =
                buildCanonicalFocusSeries(
                    seriesId = newSeriesId,
                    now = now,
                    startDayKey = splitDayKey,
                    type = expectedType,
                    rule = canonicalRule,
                    template = nextTemplate,
                )
            val nextAndroidRelatedLinks = nextTemplate.relatedLinks.toAndroidRelatedLinks()

            val liveSourceOccurrences = mutableListOf<CanonicalFocusSplitSourceVersion>()
            val replacementOccurrences = mutableListOf<DayFocusItem>()
            var selectedReplacement: DayFocusItem? = null

            occurrences.forEach { source ->
                val occurrenceDayKey =
                    requireNotNull(source.recurrenceOccurrenceDayKey) {
                        "Canonical focus occurrence has no day key: ${source.id}"
                    }
                if (compareLocalDayKeys(occurrenceDayKey, splitDayKey) < 0L) {
                    return@forEach
                }

                val matchesNewRule =
                    recurrenceRuleMatchesDay(
                        rule = canonicalRule,
                        startDayKey = splitDayKey,
                        targetDayKey = occurrenceDayKey,
                    )
                val isSelected = source.id == item.id
                val isClean =
                    source.recurrenceSourceSeriesVersion == storedSeries.version &&
                        source.matchesTemplate(
                            template = oldTemplate,
                            expectedType = expectedType,
                        )

                if (!source.isDeleted) {
                    liveSourceOccurrences +=
                        CanonicalFocusSplitSourceVersion(
                            itemId = source.id,
                            expectedVersion = source.version,
                        )
                }

                when {
                    source.isDeleted && matchesNewRule -> {
                        replacementOccurrences +=
                            source.copy(
                                id =
                                    recurrenceOccurrenceId(
                                        kind = newKind,
                                        seriesId = newSeriesId,
                                        dayKey = occurrenceDayKey,
                                    ),
                                title = nextTemplate.title,
                                notes = nextTemplate.notes,
                                relatedLinks = nextAndroidRelatedLinks,
                                type = expectedType,
                                isEveryday = false,
                                recurringKey = null,
                                recurrenceSeriesId = newSeriesId,
                                recurrenceOccurrenceDayKey = occurrenceDayKey,
                                recurrenceSourceSeriesVersion = newSeries.version,
                                budgetPercent = nextTemplate.budgetPercent,
                                createdAt = now,
                                updatedAt = now,
                                syncedAt = null,
                                isDeleted = true,
                                version = 1,
                            )
                    }

                    source.isDeleted -> Unit

                    matchesNewRule -> {
                        val useNewTemplate = isSelected || isClean
                        val replacement =
                            source.copy(
                                id =
                                    recurrenceOccurrenceId(
                                        kind = newKind,
                                        seriesId = newSeriesId,
                                        dayKey = occurrenceDayKey,
                                    ),
                                title = if (useNewTemplate) nextTemplate.title else source.title,
                                notes = if (useNewTemplate) nextTemplate.notes else source.notes,
                                relatedLinks =
                                    if (useNewTemplate) {
                                        nextAndroidRelatedLinks
                                    } else {
                                        source.relatedLinks
                                    },
                                type = expectedType,
                                isEveryday = false,
                                recurringKey = null,
                                recurrenceSeriesId = newSeriesId,
                                recurrenceOccurrenceDayKey = occurrenceDayKey,
                                recurrenceSourceSeriesVersion = newSeries.version,
                                budgetPercent =
                                    if (useNewTemplate) {
                                        nextTemplate.budgetPercent
                                    } else {
                                        source.budgetPercent
                                    },
                                createdAt = now,
                                updatedAt = now,
                                syncedAt = null,
                                isDeleted = false,
                                version = 1,
                            )
                        replacementOccurrences += replacement
                        if (isSelected) {
                            selectedReplacement = replacement
                        }
                    }

                    !isClean -> {
                        replacementOccurrences +=
                            source.copy(
                                id = UUID.randomUUID().toString(),
                                isEveryday = false,
                                recurringKey = null,
                                recurrenceSeriesId = null,
                                recurrenceOccurrenceDayKey = null,
                                recurrenceSourceSeriesVersion = null,
                                createdAt = now,
                                updatedAt = now,
                                syncedAt = null,
                                isDeleted = false,
                                version = 1,
                            )
                    }
                }
            }

            val returnedOccurrence =
                checkNotNull(selectedReplacement) {
                    "New recurrence rule did not preserve selected split occurrence: ${item.id}"
                }

            seriesDao.splitCanonicalFocusSeries(
                oldSeriesId = oldSeriesId,
                oldSeriesExpectedVersion = storedSeries.version,
                oldSeriesEndDayKey = previousLocalDayKey(splitDayKey),
                newSeries = newSeries.toAndroidEntity(),
                liveSourceOccurrences = liveSourceOccurrences,
                replacementOccurrences = replacementOccurrences,
                updatedAt = now,
            )

            return returnedOccurrence
        }

        suspend fun updateSeriesTemplate(
            item: DayFocusItem,
            title: String,
            notes: String?,
            relatedLinks: List<RelatedLink>,
            budgetPercent: Int?,
        ): DayFocusItem {
            val seriesId =
                requireNotNull(item.recurrenceSeriesId) {
                    "Cannot update canonical focus series for non-recurring item ${item.id}"
                }
            val now = System.currentTimeMillis()
            val normalizedNotes = notes?.trim()?.takeIf { it.isNotEmpty() }
            val seriesDao = appDatabase.canonicalRecurringSeriesDao()
            val storedSeries =
                checkNotNull(seriesDao.getById(seriesId)) {
                    "Canonical recurring focus series not found: $seriesId"
                }.toCanonicalSeries()

            check(!storedSeries.isDeleted) {
                "Cannot update deleted canonical recurring focus series: $seriesId"
            }

            val oldTemplate = storedSeries.focusTemplate()
            val expectedType = storedSeries.focusType()
            val nextVersion = storedSeries.version + 1
            val nextTemplate =
                RecurringFocusTemplate(
                    title = title,
                    notes = normalizedNotes,
                    relatedLinks = relatedLinks.toCanonicalRelatedLinks(),
                    budgetPercent = budgetPercent,
                )
            val nextAndroidRelatedLinks = nextTemplate.relatedLinks.toAndroidRelatedLinks()
            val updatedSeries =
                when (storedSeries) {
                    is RecurringFocusSeries ->
                        storedSeries.copy(
                            updatedAt = now,
                            syncedAt = null,
                            version = nextVersion,
                            template = nextTemplate,
                        )

                    is RecurringResponsibilitySeries ->
                        storedSeries.copy(
                            updatedAt = now,
                            syncedAt = null,
                            version = nextVersion,
                            template = nextTemplate,
                        )

                    else ->
                        error(
                            "Canonical series $seriesId is ${storedSeries.kind}, " +
                                "not FOCUS/RESPONSIBILITY",
                        )
                }

            val occurrences = seriesDao.getFocusOccurrencesForSeries(seriesId)
            check(occurrences.any { occurrence -> occurrence.id == item.id && !occurrence.isDeleted }) {
                "Selected canonical focus occurrence not found or deleted: ${item.id}"
            }

            val occurrencesToUpdate =
                occurrences.mapNotNull { occurrence ->
                    if (occurrence.isDeleted) {
                        return@mapNotNull null
                    }

                    val isSelectedOccurrence = occurrence.id == item.id
                    val isCleanOccurrence =
                        occurrence.recurrenceSourceSeriesVersion == storedSeries.version &&
                            occurrence.matchesTemplate(
                                template = oldTemplate,
                                expectedType = expectedType,
                            )

                    if (!isSelectedOccurrence && !isCleanOccurrence) {
                        return@mapNotNull null
                    }

                    occurrence.copy(
                        title = nextTemplate.title,
                        notes = nextTemplate.notes,
                        relatedLinks = nextAndroidRelatedLinks,
                        type = expectedType,
                        budgetPercent = nextTemplate.budgetPercent,
                        recurrenceSourceSeriesVersion = nextVersion,
                        updatedAt = now,
                        syncedAt = null,
                        version = occurrence.version + 1,
                    )
                }

            seriesDao.updateSeriesAndFocusOccurrences(
                series = updatedSeries.toAndroidEntity(),
                occurrences = occurrencesToUpdate,
            )

            return checkNotNull(occurrencesToUpdate.firstOrNull { occurrence -> occurrence.id == item.id }) {
                "Selected canonical focus occurrence was not updated: ${item.id}"
            }
        }

        private fun buildCanonicalFocusSeries(
            seriesId: String,
            now: Long,
            startDayKey: String,
            type: DayFocusType,
            rule: RecurrenceRule,
            template: RecurringFocusTemplate,
        ): Pair<RecurringSeriesKind, RecurringSeries> =
            when (type) {
                DayFocusType.FOCUS ->
                    RecurringSeriesKind.FOCUS to
                        RecurringFocusSeries(
                            id = seriesId,
                            createdAt = now,
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1,
                            rule = rule,
                            startDayKey = startDayKey,
                            endDayKey = null,
                            template = template,
                        )

                DayFocusType.RESPONSIBILITY ->
                    RecurringSeriesKind.RESPONSIBILITY to
                        RecurringResponsibilitySeries(
                            id = seriesId,
                            createdAt = now,
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1,
                            rule = rule,
                            startDayKey = startDayKey,
                            endDayKey = null,
                            template = template,
                        )

                else -> error("Unsupported canonical recurring focus type: $type")
            }

        private fun RecurringSeries.focusTemplate(): RecurringFocusTemplate =
            when (this) {
                is RecurringFocusSeries -> template
                is RecurringResponsibilitySeries -> template
                else -> error("Canonical series $id is $kind, not FOCUS/RESPONSIBILITY")
            }

        private fun RecurringSeries.focusType(): DayFocusType =
            when (this) {
                is RecurringFocusSeries -> DayFocusType.FOCUS
                is RecurringResponsibilitySeries -> DayFocusType.RESPONSIBILITY
                else -> error("Canonical series $id is $kind, not FOCUS/RESPONSIBILITY")
            }

        private fun DayFocusItem.matchesTemplate(
            template: RecurringFocusTemplate,
            expectedType: DayFocusType,
        ): Boolean =
            type == expectedType &&
                title == template.title &&
                notes?.trim()?.takeIf { it.isNotEmpty() } == template.notes?.trim()?.takeIf { it.isNotEmpty() } &&
                relatedLinks.orEmpty() == template.relatedLinks.toAndroidRelatedLinks() &&
                budgetPercent == template.budgetPercent

        private fun RecurrenceRule.normalizedForSeriesStart(startDayKey: String): RecurrenceRule {
            require(interval >= 1) { "Invalid recurrence interval: $interval" }

            return when (frequency) {
                RecurrenceFrequency.WEEKLY -> {
                    val selectedDays = daysOfWeek.orEmpty()
                    if (selectedDays.isEmpty()) {
                        copy(daysOfWeek = null)
                    } else {
                        val startDay = localDayKeyDayOfWeek(startDayKey)
                        copy(
                            daysOfWeek =
                                (selectedDays + startDay)
                                    .distinct()
                                    .sortedBy { it.ordinal },
                        )
                    }
                }

                else -> copy(daysOfWeek = null)
            }
        }

        private fun canonicalLocalDayKey(timestamp: Long): String {
            val calendar =
                java.util.Calendar.getInstance().apply {
                    timeInMillis = timestamp
                }
            return localDayKeyOf(
                year = calendar.get(java.util.Calendar.YEAR),
                month = calendar.get(java.util.Calendar.MONTH) + 1,
                day = calendar.get(java.util.Calendar.DAY_OF_MONTH),
            )
        }
    }
