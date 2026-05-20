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

        suspend fun addItem(
            dayPlanId: String,
            title: String,
            notes: String?,
            relatedLinks: List<RelatedLink>,
            type: DayFocusType,
            order: Long,
            isEveryday: Boolean,
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
        ) {
            val now = System.currentTimeMillis()
            dayFocusItemDao.update(
                item.copy(
                    title = title,
                    notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                    relatedLinks = relatedLinks,
                    type = type,
                    isEveryday = isEveryday,
                    recurringKey =
                        when {
                            isEveryday -> item.recurringKey ?: item.id
                            else -> item.recurringKey
                        },
                    updatedAt = now,
                    syncedAt = null,
                    version = item.version + 1,
                ),
            )
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
