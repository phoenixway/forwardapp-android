package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.shared.database.ListItemQueries
import com.romankozak.forwardappmobile.core.database.models.GlobalProjectSearchResult
import com.romankozak.forwardappmobile.core.database.models.GlobalSearchResultItem
import com.romankozak.forwardappmobile.core.database.models.GlobalSubprojectSearchResult
import com.romankozak.forwardappmobile.core.database.models.ListItemTypeValues
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
    private val projectLocalDataSource: ProjectLocalDataSource,
    private val listItemQueries: ListItemQueries,
    private val activityRepository: ActivityRepository,
    private val inboxRecordDao: InboxRecordDao,
) {
    suspend fun searchGlobal(query: String): List<GlobalSearchResultItem> {
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
            listItemQueries.getAll().executeAsList()
                .asSequence()
                .filter { it.item_type == ListItemTypeValues.SUBLIST }
                .mapNotNull { listItem ->
                    val subproject = projectMap[listItem.entity_id] ?: return@mapNotNull null
                    if (!subproject.name.contains(normalizedQuery, ignoreCase = true)) return@mapNotNull null
                    val parentProject = projectMap[listItem.project_id] ?: return@mapNotNull null
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

        val combinedResults = (subprojectResults + projectResults + activityResults + inboxResults)

        return combinedResults.sortedWith(
            compareBy<GlobalSearchResultItem> { it.typeOrder }
                .thenByDescending { it.timestamp },
        )
    }
}
