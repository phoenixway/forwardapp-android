package com.romankozak.forwardappmobile.data.database.models

sealed class ListItemContent {
    // Усі елементи списку мають спільну властивість - ListItem,
    // яка містить інформацію про порядок, ID проєкту тощо.
    abstract val listItem: ListItem

    data class GoalItem(val goal: Goal, override val listItem: ListItem) : ListItemContent()
    data class SublistItem(val project: Project, override val listItem: ListItem) : ListItemContent()
    data class LinkItem(val link: LinkItemEntity, override val listItem: ListItem) : ListItemContent()
}