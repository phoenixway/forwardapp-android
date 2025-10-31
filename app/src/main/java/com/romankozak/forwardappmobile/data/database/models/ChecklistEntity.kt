package com.romankozak.forwardappmobile.data.database.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "checklists",
    indices = [
        Index(value = ["projectId"], name = "index_checklists_projectId"),
    ],
)
data class ChecklistEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
)

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = ChecklistEntity::class,
            parentColumns = ["id"],
            childColumns = ["checklistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["checklistId"], name = "index_checklist_items_checklistId"),
        Index(value = ["itemOrder"], name = "index_checklist_items_itemOrder"),
    ],
)
data class ChecklistItemEntity(
    @PrimaryKey val id: String,
    val checklistId: String,
    val content: String,
    val isChecked: Boolean,
    val itemOrder: Long,
)
