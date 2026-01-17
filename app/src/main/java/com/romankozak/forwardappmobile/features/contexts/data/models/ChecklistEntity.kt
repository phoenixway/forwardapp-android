package com.romankozak.forwardappmobile.features.contexts.data.models

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
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["projectId"], name = "index_checklists_projectId")],
)
data class ChecklistEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    var name: String,
    val updatedAt: Long? = null,
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false,
    val version: Long = 0,
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
    indices = [Index(value = ["checklistId"], name = "index_checklist_items_checklistId")],
)
data class ChecklistItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val checklistId: String,
    var content: String,
    @ColumnInfo(defaultValue = "0") var isChecked: Boolean = false,
    var itemOrder: Long = 0,
    val updatedAt: Long? = null,
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false,
    val version: Long = 0,
)
