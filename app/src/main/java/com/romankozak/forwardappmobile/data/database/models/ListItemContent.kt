package com.romankozak.forwardappmobile.data.database.models

sealed class ListItemContent {
    
    
    abstract val listItem: ListItem

    data class GoalItem(val goal: Goal, override val listItem: ListItem) : ListItemContent()

    data class SublistItem(val project: Project, override val listItem: ListItem) : ListItemContent()

    data class LinkItem(val link: LinkItemEntity, override val listItem: ListItem) : ListItemContent()

    data class NoteItem(val note: NoteEntity, override val listItem: ListItem) : ListItemContent()

    data class CustomListItem(val customList: CustomListEntity, override val listItem: ListItem) : ListItemContent()
}
