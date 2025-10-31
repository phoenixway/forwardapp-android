package com.romankozak.forwardappmobile.data.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RecentItemType {
    PROJECT,
    NOTE,
    CUSTOM_LIST,
    OBSIDIAN_LINK
}

@Entity(tableName = "recent_items")
data class RecentItem(
    @PrimaryKey
    val id: String,
    val type: RecentItemType,
    val lastAccessed: Long,
    val displayName: String,
    val target: String, // Project ID, Legacy Note ID, Note ID, or Obsidian URI
    val isPinned: Boolean = false
)
