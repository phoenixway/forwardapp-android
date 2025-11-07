package com.romankozak.forwardappmobile.core.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "checklists",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["projectId"], name = "index_checklists_projectId")],
)
data class ChecklistRoomEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val name: String,
)

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = ChecklistRoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["checklistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["checklistId"], name = "index_checklist_items_checklistId")],
)
data class ChecklistItemRoomEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val checklistId: String,
    val content: String,
    @ColumnInfo(defaultValue = "0") val isChecked: Boolean = false,
    val itemOrder: Long = 0,
)
