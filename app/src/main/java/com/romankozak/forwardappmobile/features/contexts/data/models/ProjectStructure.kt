package com.romankozak.forwardappmobile.features.contexts.data.models

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "project_structures",
    indices = [Index(value = ["projectId"], unique = true)]
)
data class ProjectStructure(
    @PrimaryKey val id: String,
    val projectId: String,
    @ColumnInfo(name = "base_preset_code") val basePresetCode: String? = null,
    @ColumnInfo(name = "apply_mode") val applyMode: String = "ADDITIVE",
    @ColumnInfo(name = "enable_inbox") val enableInbox: Boolean? = null,
    @ColumnInfo(name = "enable_log") val enableLog: Boolean? = null,
    @ColumnInfo(name = "enable_artifact") val enableArtifact: Boolean? = null,
    @ColumnInfo(name = "enable_advanced") val enableAdvanced: Boolean? = null,
    @ColumnInfo(name = "enable_dashboard") val enableDashboard: Boolean? = null,
    @ColumnInfo(name = "enable_backlog") val enableBacklog: Boolean? = null,
    @ColumnInfo(name = "enable_attachments") val enableAttachments: Boolean? = null,
    @ColumnInfo(name = "enable_auto_link_subprojects") val enableAutoLinkSubprojects: Boolean? = null,
)

@Entity(
    tableName = "project_structure_items",
    foreignKeys = [
        ForeignKey(
            entity = ProjectStructure::class,
            parentColumns = ["id"],
            childColumns = ["projectStructureId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["projectStructureId"]),
        Index(value = ["projectStructureId", "roleCode"], unique = true),
    ]
)
data class ProjectStructureItem(
    @PrimaryKey val id: String,
    val projectStructureId: String,
    val entityType: String,
    val roleCode: String,
    val containerType: String?,
    val title: String,
    @ColumnInfo(defaultValue = "0") val mandatory: Boolean = false,
    @ColumnInfo(name = "is_enabled", defaultValue = "1") val isEnabled: Boolean = true,
)
