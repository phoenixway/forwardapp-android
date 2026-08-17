package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.data.dao.DayFocusItemDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DayFocusesRepository
    @Inject
    constructor(
        private val dayFocusItemDao: DayFocusItemDao,
    ) {
        fun getItemsForDayPlan(dayPlanId: String): Flow<List<DayFocusItem>> = dayFocusItemDao.getItemsForDayPlan(dayPlanId)

        suspend fun nextOrderForDayPlan(dayPlanId: String): Long =
            dayFocusItemDao.getItemsForDayPlanSync(dayPlanId)
                .filterNot { it.isDeleted }
                .size
                .toLong()

        suspend fun addItem(
            dayPlanId: String,
            title: String,
            notes: String?,
            relatedLinks: List<RelatedLink>,
            type: DayFocusType,
            order: Long,
            isEveryday: Boolean,
            budgetPercent: Int?,
        ): DayFocusItem {
            val now = System.currentTimeMillis()
            val item =
                DayFocusItem(
                    dayPlanId = dayPlanId,
                    title = title,
                    notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                    relatedLinks = relatedLinks,
                    type = type,
                    isEveryday = isEveryday,
                    budgetPercent = budgetPercent,
                    recurringKey = if (isEveryday) java.util.UUID.randomUUID().toString() else null,
                    order = order,
                    createdAt = now,
                    updatedAt = now,
                    syncedAt = null,
                    version = 1,
                )
            dayFocusItemDao.insert(item)
            return item
        }

        suspend fun updateItem(
            item: DayFocusItem,
            title: String,
            notes: String?,
            relatedLinks: List<RelatedLink>,
            type: DayFocusType,
            isEveryday: Boolean,
            budgetPercent: Int?,
        ): DayFocusItem {
            val now = System.currentTimeMillis()
            val updatedItem =
                item.copy(
                    title = title,
                    notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                    relatedLinks = relatedLinks,
                    type = type,
                    isEveryday = isEveryday,
                    budgetPercent = budgetPercent,
                    recurringKey =
                        when {
                            isEveryday -> item.recurringKey ?: item.id
                            else -> item.recurringKey
                        },
                    updatedAt = now,
                    syncedAt = null,
                    version = item.version + 1,
                )
            dayFocusItemDao.update(updatedItem)
            return updatedItem
        }

        suspend fun upsertEverydayItemForDayPlan(
            source: DayFocusItem,
            targetDayPlanId: String,
        ): DayFocusItem {
            if (source.dayPlanId == targetDayPlanId) {
                return source
            }
            val recurringKey = source.recurringKey ?: source.id
            val now = System.currentTimeMillis()
            val matchingItems =
                dayFocusItemDao
                    .getItemsForDayPlanSync(targetDayPlanId)
                    .filter { item -> item.recurringKey == recurringKey }
            val existing = matchingItems.firstOrNull { item -> !item.isDeleted }
            if (existing == null) {
                matchingItems.firstOrNull { item -> item.isDeleted }?.let { tombstone ->
                    return tombstone
                }
            }
            val targetItem =
                existing?.copy(
                    title = source.title,
                    notes = source.notes,
                    relatedLinks = source.relatedLinks,
                    type = source.type,
                    isEveryday = true,
                    recurringKey = recurringKey,
                    budgetPercent = source.budgetPercent,
                    updatedAt = now,
                    syncedAt = null,
                    version = existing.version + 1,
                )
                    ?: DayFocusItem(
                        dayPlanId = targetDayPlanId,
                        title = source.title,
                        notes = source.notes,
                        relatedLinks = source.relatedLinks,
                        type = source.type,
                        isEveryday = true,
                        budgetPercent = source.budgetPercent,
                        recurringKey = recurringKey,
                        order = nextOrderForDayPlan(targetDayPlanId),
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        version = 1,
                    )
            if (existing == null) {
                dayFocusItemDao.insert(targetItem)
            } else {
                dayFocusItemDao.update(targetItem)
            }
            return targetItem
        }

        suspend fun deleteItem(itemId: String) {
            dayFocusItemDao.softDelete(itemId = itemId, updatedAt = System.currentTimeMillis())
        }

        suspend fun deleteItemEverywhere(recurringKey: String) {
            dayFocusItemDao.softDeleteByRecurringKey(recurringKey, System.currentTimeMillis())
        }

        suspend fun reorderItems(items: List<DayFocusItem>) {
            val now = System.currentTimeMillis()
            items.forEachIndexed { index, item ->
                dayFocusItemDao.updateOrder(
                    itemId = item.id,
                    order = index.toLong(),
                    updatedAt = now,
                )
            }
        }
    }
