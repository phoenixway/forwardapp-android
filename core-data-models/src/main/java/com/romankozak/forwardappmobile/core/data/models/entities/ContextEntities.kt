package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "backlog_orders",
    indices = [
        Index("list_id"),
        Index(value = ["list_id", "item_id"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Context::class,
            parentColumns = ["id"],
            childColumns = ["list_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class BacklogOrder(
    @PrimaryKey @SerializedName("id") val id: String,
    @SerializedName(value = "listId", alternate = ["contextId", "projectId"])
    @ColumnInfo(name = "list_id")
    val listId: String = "",
    @ColumnInfo(name = "item_id") @SerializedName("itemId")
    val itemId: String,
    @ColumnInfo(name = "item_order") @SerializedName("order") val order: Long,
    @ColumnInfo(name = "order_version", defaultValue = "0") @SerializedName("orderVersion") val orderVersion: Long = 0,
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @ColumnInfo(name = "synced_at") @SerializedName("syncedAt") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") @SerializedName("isDeleted") val isDeleted: Boolean = false,
)

@Entity(tableName = "link_items")
data class LinkItemEntity(
    @PrimaryKey @SerializedName("id") val id: String,
    @ColumnInfo(name = "link_data") @SerializedName("linkData")
    val linkData: RelatedLink,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @ColumnInfo(name = "synced_at") @SerializedName("syncedAt") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "version", defaultValue = "0") @SerializedName("version") val version: Long = 0,
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey @SerializedName("id") val id: String,
    @SerializedName("text") val text: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("completed") val completed: Boolean,
    @ColumnInfo(name = "goal_status", defaultValue = "'ACTIVE'") @SerializedName("goalStatus") val goalStatus: String = GoalStatusValues.ACTIVE,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long?,
    @ColumnInfo(name = "synced_at") @SerializedName("syncedAt") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "version", defaultValue = "0") @SerializedName("version") val version: Long = 0,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("relatedLinks") val relatedLinks: List<RelatedLink>? = null,
    @ColumnInfo(defaultValue = "0.0") @SerializedName("valueImportance") val valueImportance: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") @SerializedName("valueImpact") val valueImpact: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") @SerializedName("effort") val effort: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") @SerializedName("cost") val cost: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") @SerializedName("risk") val risk: Float = 0f,
    @ColumnInfo(defaultValue = "1.0") @SerializedName("weightEffort") val weightEffort: Float = 1f,
    @ColumnInfo(defaultValue = "1.0") @SerializedName("weightCost") val weightCost: Float = 1f,
    @ColumnInfo(defaultValue = "1.0") @SerializedName("weightRisk") val weightRisk: Float = 1f,
    @ColumnInfo(defaultValue = "0.0") @SerializedName("rawScore") val rawScore: Float = 0f,
    @ColumnInfo(defaultValue = "0") @SerializedName("displayScore") val displayScore: Int = 0,
    @ColumnInfo(name = "scoring_status") @SerializedName("scoringStatus") val scoringStatus: String = ScoringStatusValues.NOT_ASSESSED,
    @ColumnInfo(defaultValue = "0") @SerializedName("relativeSize") val relativeSize: Int = 0,
    @ColumnInfo(defaultValue = "0.0") @SerializedName("parentValueImportance") val parentValueImportance: Float? = null,
    @ColumnInfo(defaultValue = "0.0") @SerializedName("impactOnParentGoal") val impactOnParentGoal: Float? = null,
    @ColumnInfo(defaultValue = "0.0") @SerializedName("timeCost") val timeCost: Float? = null,
    @ColumnInfo(defaultValue = "0.0") @SerializedName("financialCost") val financialCost: Float? = null,
)

@Entity(
    tableName = "context_execution_logs",
    foreignKeys = [
        ForeignKey(
            entity = Context::class,
            parentColumns = ["id"],
            childColumns = ["contextId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ContextLog(
    @PrimaryKey @SerializedName("id") val id: String,
    @SerializedName(value = "contextId", alternate = ["projectId"])
    @ColumnInfo(index = true) val contextId: String = "",
    @SerializedName("timestamp") val timestamp: Long,
    @ColumnInfo(name = "type") @SerializedName("type") val type: String,
    @SerializedName("description") val description: String,
    @SerializedName("details") val details: String? = null,
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @ColumnInfo(name = "synced_at") @SerializedName("syncedAt") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "version", defaultValue = "0") @SerializedName("version") val version: Long = 0,
)

@Entity(
    tableName = "inbox_records",
    foreignKeys = [
        ForeignKey(
            entity = Context::class,
            parentColumns = ["id"],
            childColumns = ["contextId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class InboxRecord(
    @PrimaryKey @SerializedName("id") val id: String,
    @SerializedName(value = "contextId", alternate = ["projectId"])
    @ColumnInfo(index = true) val contextId: String,
    @SerializedName("text") val text: String,
    @SerializedName("createdAt") val createdAt: Long,
    @ColumnInfo(name = "item_order") @SerializedName("order") val order: Long,
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @ColumnInfo(name = "synced_at") @SerializedName("syncedAt") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "hide_in_owner_inbox", defaultValue = "0")
    @SerializedName("hideInOwnerInbox")
    val hideInOwnerInbox: Boolean = false,
    @ColumnInfo(name = "version", defaultValue = "0") val version: Long = 0,
)

@Entity(
    tableName = "list_items",
    indices = [
        Index(value = ["context_id", "itemType", "entityId", "is_deleted"], unique = true),
        Index(value = ["entityId", "itemType", "association_owner_context_id"]),
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
data class BacklogItem(
    @PrimaryKey @SerializedName("id") val id: String,
    @SerializedName(value = "contextId", alternate = ["listId", "projectId"])
    @ColumnInfo(name = "context_id", index = true)
    val contextId: String = "",
    @SerializedName("itemType") val itemType: String,
    @SerializedName("entityId") val entityId: String,
    @ColumnInfo(name = "association_owner_context_id") @SerializedName("associationOwnerContextId") val associationOwnerContextId: String? = null,
    @ColumnInfo(name = "association_tag") @SerializedName("associationTag") val associationTag: String? = null,
    @ColumnInfo(name = "item_order") @SerializedName("order") val order: Long,
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @ColumnInfo(name = "synced_at") @SerializedName("syncedAt") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "version", defaultValue = "0") @SerializedName("version") val version: Long = 0,
)

@Fts4(contentEntity = Goal::class)
@Entity(tableName = "goals_fts")
data class GoalFts(
    @SerializedName("text") val text: String,
    @SerializedName("description") val description: String?,
)

@Fts4(contentEntity = Context::class)
@Entity(tableName = "contexts_fts")
data class ContextsFts(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
)

data class GlobalGoalSearchResult(
    @Embedded @SerializedName("goal")
    val goal: Goal,
    @SerializedName("contextId") val contextId: String,
    @SerializedName("contextName") val contextName: String,
    @TypeConverters(PathSegmentsConverter::class) @SerializedName("pathSegments")
    val pathSegments: List<String>,
)

data class GlobalLinkSearchResult(
    @Embedded @SerializedName("link")
    val link: LinkItemEntity,
    @SerializedName("contextId") val contextId: String,
    @SerializedName("contextName") val contextName: String,
    @SerializedName("listItemId") val listItemId: String,
    @TypeConverters(PathSegmentsConverter::class) @SerializedName("pathSegments")
    val pathSegments: List<String>,
)

data class GlobalSubcontextSearchResult(
    @Embedded @SerializedName("subcontext")
    val subcontext: Context,
    @SerializedName("parentContextId") val parentContextId: String,
    @SerializedName("parentContextName") val parentContextName: String,
    @TypeConverters(PathSegmentsConverter::class) @SerializedName("pathSegments")
    val pathSegments: List<String>,
)

data class GlobalContextSearchResult(
    @Embedded @SerializedName("context")
    val context: Context,
    @TypeConverters(PathSegmentsConverter::class) @SerializedName("pathSegments")
    val pathSegments: List<String>,
    @SerializedName("matchedTags")
    val matchedTags: List<String> = emptyList(),
)

data class GlobalContextSearchRow(
    @Embedded @SerializedName("context")
    val context: Context,
    @TypeConverters(PathSegmentsConverter::class) @SerializedName("pathSegments")
    val pathSegments: List<String>,
)

data class GlobalAttachmentSearchResult(
    @SerializedName("attachmentId") val attachmentId: String,
    @SerializedName("entityId") val entityId: String,
    @SerializedName("attachmentType") val attachmentType: String,
    @SerializedName("ownerContextId") val ownerContextId: String?,
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String?,
    @SerializedName("contextName") val contextName: String?,
    @SerializedName("updatedAt") val updatedAt: Long,
)

sealed class GlobalSearchResultItem {
    abstract val timestamp: Long
    abstract val uniqueId: String

    data class GoalItem(
        @SerializedName("goal") val goal: Goal,
        @SerializedName("backlogItem") val backlogItem: BacklogItem,
        @SerializedName("projectName") val projectName: String,
        @SerializedName("pathSegments") val pathSegments: List<String>,
    ) : GlobalSearchResultItem() {
        override val timestamp: Long get() = goal.updatedAt ?: goal.createdAt
        override val uniqueId: String get() = "goal_${goal.id}_${backlogItem.contextId}"
    }

    data class LinkItem(@SerializedName("searchResult") val searchResult: GlobalLinkSearchResult) : GlobalSearchResultItem() {
        override val timestamp: Long get() = searchResult.link.createdAt
        override val uniqueId: String get() = "link_${searchResult.link.id}_${searchResult.contextId}"
    }

    data class SubcontextItem(@SerializedName("searchResult") val searchResult: GlobalSubcontextSearchResult) : GlobalSearchResultItem() {
        override val timestamp: Long get() = searchResult.subcontext.updatedAt ?: searchResult.subcontext.createdAt
        override val uniqueId: String get() = "sublist_${searchResult.subcontext.id}_${searchResult.parentContextId}"
    }

    data class ContextItem(
        @SerializedName("searchResult") val searchResult: GlobalContextSearchResult,
    ) : GlobalSearchResultItem() {
        override val timestamp: Long get() = searchResult.context.updatedAt ?: searchResult.context.createdAt
        override val uniqueId: String get() = "context_${searchResult.context.id}"
    }

    data class ActivityItem(@SerializedName("record") val record: ActivityRecord) : GlobalSearchResultItem() {
        override val timestamp: Long get() = record.startTime ?: record.createdAt
        override val uniqueId: String get() = "activity_${record.id}"
    }

    data class InboxItem(@SerializedName("record") val record: InboxRecord) : GlobalSearchResultItem() {
        override val timestamp: Long get() = record.createdAt
        override val uniqueId: String get() = "inbox_${record.id}"
    }

    data class AttachmentItem(@SerializedName("searchResult") val searchResult: GlobalAttachmentSearchResult) : GlobalSearchResultItem() {
        override val timestamp: Long get() = searchResult.updatedAt
        override val uniqueId: String get() = "attachment_${searchResult.attachmentId}"
    }
}
