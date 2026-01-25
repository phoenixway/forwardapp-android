package com.romankozak.forwardappmobile.features.contexts.data.models

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "context_structures",
    indices = [Index(value = ["contextId"], unique = true)]
)
data class ContextConfiguration(
    @PrimaryKey val id: String,
    val contextId: String,
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
    tableName = "context_structure_items",
    foreignKeys = [
        ForeignKey(
            entity = ContextConfiguration::class,
            parentColumns = ["id"],
            childColumns = ["contextStructureId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["contextStructureId"]),
        Index(value = ["contextStructureId", "roleCode"], unique = true),
    ]
)
data class ContextStructureItem(
    @PrimaryKey val id: String,
    val contextStructureId: String,
    val entityType: String,
    val roleCode: String,
    val containerType: String?,
    val title: String,
    @ColumnInfo(defaultValue = "0") val mandatory: Boolean = false,
    @ColumnInfo(name = "is_enabled", defaultValue = "1") val isEnabled: Boolean = true,
)
