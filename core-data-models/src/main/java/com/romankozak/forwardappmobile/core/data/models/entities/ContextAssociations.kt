package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "context_tag_refs",
    primaryKeys = ["context_id", "normalized_tag"],
    indices = [
        Index(value = ["normalized_tag"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Context::class,
            parentColumns = ["id"],
            childColumns = ["context_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ContextTagRef(
    @ColumnInfo(name = "context_id") val contextId: String,
    @ColumnInfo(name = "normalized_tag") val normalizedTag: String,
)

@Entity(
    tableName = "inbox_record_links",
    primaryKeys = ["record_id", "context_id"],
    indices = [
        Index(value = ["context_id"]),
        Index(value = ["record_id"]),
        Index(value = ["owner_context_id", "record_id"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = InboxRecord::class,
            parentColumns = ["id"],
            childColumns = ["record_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Context::class,
            parentColumns = ["id"],
            childColumns = ["context_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class InboxRecordLink(
    @ColumnInfo(name = "record_id") val recordId: String,
    @ColumnInfo(name = "context_id") val contextId: String,
    @ColumnInfo(name = "owner_context_id") val ownerContextId: String,
    @ColumnInfo(name = "association_tag") val associationTag: String? = null,
    @ColumnInfo(name = "linked_at") val linkedAt: Long = System.currentTimeMillis(),
)

data class ContextTagLookup(
    @ColumnInfo(name = "context_id") val contextId: String,
    @ColumnInfo(name = "normalized_tag") val normalizedTag: String,
)
