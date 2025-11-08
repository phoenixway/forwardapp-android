package com.romankozak.forwardappmobile.data.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_items")
data class RecentItemRoomEntity(
    @PrimaryKey val id: String,
    val type: RecentItemType,
    val lastAccessed: Long,
    val displayName: String,
    val target: String,
    val isPinned: Boolean = false,
)
