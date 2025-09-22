package com.romankozak.forwardappmobile.data.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "recent_project_entries",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RecentProjectEntry(
    @PrimaryKey
    @SerializedName(value = "projectId", alternate = ["listId"])
    @ColumnInfo(name = "project_id")
    val projectId: String,
    @ColumnInfo(name = "last_accessed")
    val lastAccessed: Long,
)
