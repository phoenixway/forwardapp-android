package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ProjectDao
import com.romankozak.forwardappmobile.features.contexts.data.models.GlobalSearchResultItem
import javax.inject.Inject
import javax.inject.Singleton

private val GlobalSearchResultItem.typeOrder: Int
    get() =
        when (this) {
            is GlobalSearchResultItem.ProjectItem,
            is GlobalSearchResultItem.SublistItem,
            -> 0
            is GlobalSearchResultItem.GoalItem -> 1
            else -> 2
        }

@Singleton
class SearchRepository
@Inject
constructor(
    private val goalDao: GoalDao,
    private val projectDao: ProjectDao,
    private val listItemDao: ListItemDao,
    private val linkItemDao: LinkItemDao,
    private val activityRepository: ActivityRepository,
    private val inboxRecordDao: InboxRecordDao,
) {
    suspend fun searchGlobal(query: String): List<GlobalSearchResultItem> {
        val goalResults =
            goalDao.searchGoalsGlobal(query).mapNotNull { searchResult ->
                val listItem = listItemDao.getListItemByEntityId(searchResult.goal.id)
                listItem?.let {
                    GlobalSearchResultItem.GoalItem(
                        goal = searchResult.goal,
                        listItem = it,
                        projectName = searchResult.projectName,
                        pathSegments = searchResult.pathSegments,
                    )
                }
            }
        val linkResults =
            linkItemDao.searchLinksGlobal(query).map {
                GlobalSearchResultItem.LinkItem(it)
            }
        val subprojectResults =
            projectDao.searchSubprojectsGlobal(query).map {
                GlobalSearchResultItem.SublistItem(it)
            }
        val projectResults =
            projectDao.searchProjectsGlobal(query).map {
                GlobalSearchResultItem.ProjectItem(it)
            }
        val activityResults =
            activityRepository.searchActivities(query).map {
                GlobalSearchResultItem.ActivityItem(it)
            }
        val inboxResults =
            inboxRecordDao.searchInboxRecordsGlobal(query).map {
                GlobalSearchResultItem.InboxItem(it)
            }

        val combinedResults = (goalResults + linkResults + subprojectResults + projectResults + activityResults + inboxResults)

        return combinedResults.sortedWith(
            compareBy<GlobalSearchResultItem> { it.typeOrder }
                .thenByDescending { it.timestamp },
        )
    }
}
