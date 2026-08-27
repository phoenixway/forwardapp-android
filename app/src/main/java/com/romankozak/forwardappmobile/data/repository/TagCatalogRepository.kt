package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.domain.tags.buildHashTagCatalog
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagCatalogRepository
    @Inject
    constructor(
        activityRecordDao: ActivityRecordDao,
        goalDao: GoalDao,
        dayTaskDao: DayTaskDao,
        contextDao: ContextDao,
    ) {
        val tags: Flow<List<String>> =
            combine(
                activityRecordDao.getAllRecordsStream(),
                goalDao.getAllVisibleGoalsFlow(),
                dayTaskDao.getAllVisibleTasksFlow(),
                contextDao.getAllContexts(),
            ) { activityRecords, goals, dayTasks, contexts ->
                buildHashTagCatalog(
                    texts =
                        sequence {
                            activityRecords.forEach { record -> yield(record.text) }
                            goals.forEach { goal ->
                                yield(goal.text)
                                goal.description?.let { yield(it) }
                            }
                            dayTasks.forEach { task ->
                                yield(task.title)
                                task.description?.let { yield(it) }
                                task.notes?.let { yield(it) }
                            }
                            contexts.forEach { context -> context.description?.let { yield(it) } }
                        },
                    explicitTags =
                        sequence {
                            goals.forEach { goal -> yieldAll(goal.tags.orEmpty()) }
                            dayTasks.forEach { task -> yieldAll(task.tags.orEmpty()) }
                            contexts.forEach { context -> yieldAll(context.tags.orEmpty()) }
                        },
                )
            }
    }
