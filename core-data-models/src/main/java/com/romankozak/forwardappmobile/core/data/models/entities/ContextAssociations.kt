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
 * Authority remains Goal + effective owner tags + the explicit owner placement.
 * Reserved System tag authority may come from canonical Workspace tags.
 * context_id is therefore a stable logical target id, not Context ownership:
 * it may identify either an ordinary Context or an exact canonical System
 * Workspace. These rows are never sync, backup, or canonical placement
 * authority.
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

data class ContextTagLookup(
    @ColumnInfo(name = "context_id") val contextId: String,
    @ColumnInfo(name = "normalized_tag") val normalizedTag: String,
)
