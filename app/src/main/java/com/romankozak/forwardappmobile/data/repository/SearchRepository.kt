package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.database.models.GlobalProjectSearchResult
import com.romankozak.forwardappmobile.data.database.models.GlobalSearchResultItem
import com.romankozak.forwardappmobile.data.database.models.GlobalSubprojectSearchResult
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues
import com.romankozak.forwardappmobile.features.projects.data.ProjectLocalDataSource
import com.romankozak.forwardappmobile.shared.data.database.models.Project
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
    private val projectLocalDataSource: ProjectLocalDataSource,
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
        val allProjects = projectLocalDataSource.getAll()
        val projectMap = allProjects.associateBy { it.id }
        val pathCache = mutableMapOf<String, List<String>>()
        fun resolvePath(project: Project): List<String> =
            pathCache.getOrPut(project.id) {
                val chain = mutableListOf<String>()
                var current: Project? = project
                while (current != null) {
                    chain += current.name
                    current = current.parentId?.let(projectMap::get)
                }
                chain.asReversed()
            }

        val normalizedQuery = query.trim()

        val subprojectResults =
            listItemDao.getAll()
                .asSequence()
                .filter { it.itemType == ListItemTypeValues.SUBLIST }
                .mapNotNull { listItem ->
                    val subproject = projectMap[listItem.entityId] ?: return@mapNotNull null
                    if (!subproject.name.contains(normalizedQuery, ignoreCase = true)) return@mapNotNull null
                    val parentProject = projectMap[listItem.projectId] ?: return@mapNotNull null
                    GlobalSearchResultItem.SublistItem(
                        GlobalSubprojectSearchResult(
                            subproject = subproject,
                            parentProjectId = parentProject.id,
                            parentProjectName = parentProject.name,
                            pathSegments = resolvePath(subproject),
                        ),
                    )
                }
                .toList()

        val projectResults =
            allProjects
                .asSequence()
                .filter { it.name.contains(normalizedQuery, ignoreCase = true) }
                .map {
                    GlobalSearchResultItem.ProjectItem(
                        GlobalProjectSearchResult(
                            project = it,
                            pathSegments = resolvePath(it),
                        ),
                    )
                }
                .toList()
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
