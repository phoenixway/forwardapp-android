package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceInboxRecordEntity

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
            entity = WorkspaceInboxRecordEntity::class,
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

/**
 * Rebuildable local projection for hashtag-routed Goal appearances in Backlog.
 *
 * Authority remains Goal + Context tags + the explicit owner placement.
 * These rows are never sync, backup, or canonical placement authority.
 */
@Entity(
    tableName = "backlog_goal_association_links",
    primaryKeys = ["goal_id", "context_id"],
    indices = [
        Index(value = ["context_id"]),
        Index(value = ["goal_id"]),
        Index(value = ["owner_context_id", "goal_id"]),
        Index(value = ["projection_id"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Goal::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
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
data class BacklogGoalAssociationLink(
    @ColumnInfo(name = "projection_id") val projectionId: String,
    @ColumnInfo(name = "goal_id") val goalId: String,
    @ColumnInfo(name = "context_id") val contextId: String,
    @ColumnInfo(name = "owner_context_id") val ownerContextId: String,
    @ColumnInfo(name = "association_tag") val associationTag: String? = null,
    @ColumnInfo(name = "item_order") val order: Long,
    @ColumnInfo(name = "linked_at") val linkedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "context_parent_links",
    primaryKeys = ["parent_context_id", "child_context_id"],
    indices = [
        Index(value = ["parent_context_id", "link_order"]),
        Index(value = ["child_context_id"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Context::class,
            parentColumns = ["id"],
            childColumns = ["parent_context_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Context::class,
            parentColumns = ["id"],
            childColumns = ["child_context_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ContextParentLink(
    @ColumnInfo(name = "parent_context_id") val parentContextId: String,
    @ColumnInfo(name = "child_context_id") val childContextId: String,
    @ColumnInfo(name = "link_order", defaultValue = "0") val order: Long = 0L,
    @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updatedAt") val updatedAt: Long? = null,
    @ColumnInfo(name = "synced_at") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "version", defaultValue = "0") val version: Long = 0L,
)

data class ContextTagLookup(
    @ColumnInfo(name = "context_id") val contextId: String,
    @ColumnInfo(name = "normalized_tag") val normalizedTag: String,
)
