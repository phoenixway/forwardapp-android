package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "context_inbox_sorting",
    indices = [Index(value = ["updated_at"])],
)
data class ContextInboxSortingEntity(
    @PrimaryKey
    @ColumnInfo(name = "context_id")
    val contextId: String,
    @ColumnInfo(name = "rules_text")
    val rulesText: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

