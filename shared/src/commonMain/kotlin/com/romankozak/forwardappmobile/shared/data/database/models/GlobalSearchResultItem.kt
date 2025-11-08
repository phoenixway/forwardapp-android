package com.romankozak.forwardappmobile.shared.data.database.models

sealed class GlobalSearchResultItem {
    abstract val timestamp: Long
    abstract val uniqueId: String

    data class GoalItem(
        val goal: Goal,
        val listItem: ListItem,
        val projectName: String,
        val pathSegments: List<String>
    ) : GlobalSearchResultItem() {
        override val timestamp: Long get() = goal.updatedAt ?: goal.createdAt
        override val uniqueId: String get() = "goal_${goal.id}_${listItem.projectId}"
    }

    data class LinkItem(val searchResult: GlobalLinkSearchResult) : GlobalSearchResultItem() {
        override val timestamp: Long get() = searchResult.link.createdAt
        override val uniqueId: String get() = "link_${searchResult.link.id}_${searchResult.projectId}"
    }

    data class SublistItem(val searchResult: GlobalSubprojectSearchResult) : GlobalSearchResultItem() {
        override val timestamp: Long get() = searchResult.subproject.updatedAt ?: searchResult.subproject.createdAt
        override val uniqueId: String get() = "sublist_${searchResult.subproject.id}_${searchResult.parentProjectId}"
    }

    data class ProjectItem(
        val searchResult: GlobalProjectSearchResult,
    ) : GlobalSearchResultItem() {
        override val timestamp: Long get() = searchResult.project.updatedAt ?: searchResult.project.createdAt
        override val uniqueId: String get() = "project_${searchResult.project.id}"
    }

    data class ActivityItem(val record: ActivityRecord) : GlobalSearchResultItem() {
        override val timestamp: Long get() = record.startTime ?: record.createdAt
        override val uniqueId: String get() = "activity_${record.id}"
    }

    data class InboxItem(val record: InboxRecord) : GlobalSearchResultItem() {
        override val timestamp: Long get() = record.createdAt
        override val uniqueId: String get() = "inbox_${record.id}"
    }
}
