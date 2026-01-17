package com.romankozak.forwardappmobile.features.contexts.data.models

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import com.romankozak.forwardappmobile.features.contexts.data.models.Project
import com.romankozak.forwardappmobile.features.contexts.data.models.RelatedLink
import com.romankozak.forwardappmobile.features.contexts.data.models.ScoringStatusValues
import com.romankozak.forwardappmobile.features.contexts.data.models.PathSegmentsConverter


@Entity(
    tableName = "backlog_orders",
    indices = [
        Index("list_id"),
        Index(value = ["list_id", "item_id"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["list_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class BacklogOrder(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "list_id")
    val listId: String,
    @ColumnInfo(name = "item_id")
    val itemId: String,
    @ColumnInfo(name = "item_order") val order: Long,
    @ColumnInfo(name = "order_version", defaultValue = "0") val orderVersion: Long = 0,
    val updatedAt: Long? = null,
    @ColumnInfo(name = "synced_at") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
)

@Entity(tableName = "link_items")
data class LinkItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "link_data")
    val linkData: RelatedLink,
    val createdAt: Long,
    val updatedAt: Long? = null,
    @ColumnInfo(name = "synced_at") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "version", defaultValue = "0") val version: Long = 0,
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: String,
    val text: String,
    val description: String? = null,
    val completed: Boolean,
    val createdAt: Long,
    val updatedAt: Long?,
    @ColumnInfo(name = "synced_at") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "version", defaultValue = "0") val version: Long = 0,
    val tags: List<String>? = null,
    val relatedLinks: List<RelatedLink>? = null,
    @ColumnInfo(defaultValue = "0.0") val valueImportance: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") val valueImpact: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") val effort: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") val cost: Float = 0f,
    @ColumnInfo(defaultValue = "0.0") val risk: Float = 0f,
    @ColumnInfo(defaultValue = "1.0") val weightEffort: Float = 1f,
    @ColumnInfo(defaultValue = "1.0") val weightCost: Float = 1f,
    @ColumnInfo(defaultValue = "1.0") val weightRisk: Float = 1f,
    @ColumnInfo(defaultValue = "0.0") val rawScore: Float = 0f,
    @ColumnInfo(defaultValue = "0") val displayScore: Int = 0,
    @ColumnInfo(name = "scoring_status") val scoringStatus: String = ScoringStatusValues.NOT_ASSESSED,
    @ColumnInfo(defaultValue = "0.0") val parentValueImportance: Float? = null,
    @ColumnInfo(defaultValue = "0.0") val impactOnParentGoal: Float? = null,
    @ColumnInfo(defaultValue = "0.0") val timeCost: Float? = null,
    @ColumnInfo(defaultValue = "0.0") val financialCost: Float? = null,
)

@Entity(
    tableName = "project_execution_logs",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ProjectExecutionLog(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true) val projectId: String,
    val timestamp: Long,
    @ColumnInfo(name = "type") val type: String,
    val description: String,
    val details: String? = null,
    val updatedAt: Long? = null,
    @ColumnInfo(name = "synced_at") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "version", defaultValue = "0") val version: Long = 0,
)

@Entity(
    tableName = "inbox_records",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class InboxRecord(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true) val projectId: String,
    val text: String,
    val createdAt: Long,
    @ColumnInfo(name = "item_order") val order: Long,
    val updatedAt: Long? = null,
    @ColumnInfo(name = "synced_at") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "version", defaultValue = "0") val version: Long = 0,
)

@Entity(
    tableName = "list_items",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ListItem(
    @PrimaryKey val id: String,
    @SerializedName(value = "projectId", alternate = ["listId"])
    @ColumnInfo(name = "project_id", index = true)
    val projectId: String,
    val itemType: String,
    val entityId: String,
    @ColumnInfo(name = "item_order") val order: Long,
    val updatedAt: Long? = null,
    @ColumnInfo(name = "synced_at") val syncedAt: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "version", defaultValue = "0") val version: Long = 0,
)

@Fts4(contentEntity = Goal::class)
@Entity(tableName = "goals_fts")
data class GoalFts(
    val text: String,
    val description: String?,
)

@Fts4(contentEntity = Project::class)
@Entity(tableName = "projects_fts")
data class ProjectFts(
    val name: String,
    val description: String?,
)

data class GlobalGoalSearchResult(
    @Embedded
    val goal: Goal,
    val projectId: String,
    val projectName: String,
    @TypeConverters(PathSegmentsConverter::class)
    val pathSegments: List<String>,
)

data class GlobalLinkSearchResult(
    @Embedded
    val link: LinkItemEntity,
    val projectId: String,
    val projectName: String,
    val listItemId: String,
    @TypeConverters(PathSegmentsConverter::class)
    val pathSegments: List<String>,
)

data class GlobalSubprojectSearchResult(
    @Embedded
    val subproject: Project,
    val parentProjectId: String,
    val parentProjectName: String,
    @TypeConverters(PathSegmentsConverter::class)
    val pathSegments: List<String>,
)

data class GlobalProjectSearchResult(
    @Embedded
    val project: Project,
    @TypeConverters(PathSegmentsConverter::class)
    val pathSegments: List<String>,
)

sealed class GlobalSearchResultItem {
    abstract val timestamp: Long
    abstract val uniqueId: String

    data class GoalItem(
        val goal: Goal,
        val listItem: ListItem,
        val projectName: String,
        val pathSegments: List<String>
    ) : GlobalSearchResultItem() {
        override val timestamp: Long get() = goal.updatedAt ?: goal.createdAt
        override val uniqueId: String get() = "goal_${goal.id}_${listItem.projectId}"
    }

    data class LinkItem(val searchResult: GlobalLinkSearchResult) : GlobalSearchResultItem() {
        override val timestamp: Long get() = searchResult.link.createdAt
        override val uniqueId: String get() = "link_${searchResult.link.id}_${searchResult.projectId}"
    }

    data class SublistItem(val searchResult: GlobalSubprojectSearchResult) : GlobalSearchResultItem() {
        override val timestamp: Long get() = searchResult.subproject.updatedAt ?: searchResult.subproject.createdAt
        override val uniqueId: String get() = "sublist_${searchResult.subproject.id}_${searchResult.parentProjectId}"
    }

    data class ProjectItem(
        val searchResult: GlobalProjectSearchResult,
    ) : GlobalSearchResultItem() {
        override val timestamp: Long get() = searchResult.project.updatedAt ?: searchResult.project.createdAt
        override val uniqueId: String get() = "project_${searchResult.project.id}"
    }

    data class ActivityItem(val record: ActivityRecord) : GlobalSearchResultItem() {
        override val timestamp: Long get() = record.startTime ?: record.createdAt
        override val uniqueId: String get() = "activity_${record.id}"
    }

    data class InboxItem(val record: InboxRecord) : GlobalSearchResultItem() {
        override val timestamp: Long get() = record.createdAt
        override val uniqueId: String get() = "inbox_${record.id}"
    }
}
