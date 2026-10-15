package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.domain.tags.buildHashTagCatalog
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagCatalogRepository
    @Inject
    constructor(
        activityRecordDao: ActivityRecordDao,
        goalRepository: GoalRepository,
        dayTaskDao: DayTaskDao,
        systemWorkspacePresentationContextProjector: SystemWorkspacePresentationContextProjector,
        systemWorkspaceTagAuthority: SystemWorkspaceTagAuthority,
    ) {
        val tags: Flow<List<String>> =
            combine(
                activityRecordDao.getAllRecordsStream(),
                goalRepository.getAllGoalsFlow(),
                dayTaskDao.getAllVisibleTasksFlow(),
                systemWorkspacePresentationContextProjector.observePresentationUniverse(),
                systemWorkspaceTagAuthority.observeEffectiveOwners(),
            ) { activityRecords, goals, dayTasks, presentations, tagOwners ->
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
                            presentations.forEach { presentation ->
                                presentation.description?.let { yield(it) }
                            }
                        },
                    explicitTags =
                        sequence {
                            goals.forEach { goal -> yieldAll(goal.tags.orEmpty()) }
                            dayTasks.forEach { task -> yieldAll(task.tags.orEmpty()) }
                            tagOwners.forEach { owner -> yieldAll(owner.tags) }
                        },
                )
            }
    }
