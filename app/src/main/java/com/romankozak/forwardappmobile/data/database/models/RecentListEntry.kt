package com.romankozak.forwardappmobile.data.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "recent_list_entries",
    foreignKeys = [
        ForeignKey(
            entity = GoalList::class,
            parentColumns = ["id"],
            childColumns = ["list_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RecentListEntry(
    @PrimaryKey
    @ColumnInfo(name = "list_id")
    val listId: String,
    @ColumnInfo(name = "last_accessed")
    val lastAccessed: Long,
)
