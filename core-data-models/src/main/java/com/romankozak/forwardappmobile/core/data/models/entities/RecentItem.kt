package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

enum class RecentItemType {
    PROJECT,
    NOTE,
    NOTE_DOCUMENT,
    OBSIDIAN_LINK,
    CHECKLIST,
}

@Entity(tableName = "recent_items")
data class RecentItem(
    @PrimaryKey @SerializedName("id")
    val id: String,
    @SerializedName("type")
    val type: RecentItemType,
    @SerializedName("lastAccessed")
    val lastAccessed: Long,
    @SerializedName("displayName")
    val displayName: String,
    @SerializedName("target")
    val target: String, // Project ID, Legacy Note ID, Note ID, or Obsidian URI
    @SerializedName("isPinned")
    val isPinned: Boolean = false,
)
