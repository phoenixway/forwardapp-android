package com.romankozak.forwardappmobile.features.contexts.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ColumnInfo

@Entity(
    tableName = "structure_presets",
    indices = [Index(value = ["code"], unique = true)]
)
data class StructurePreset(
    @PrimaryKey val id: String,
    val code: String,
    val label: String,
    val description: String?,
    @ColumnInfo(name = "enable_inbox") val enableInbox: Boolean? = null,
    @ColumnInfo(name = "enable_log") val enableLog: Boolean? = null,
    @ColumnInfo(name = "enable_artifact") val enableArtifact: Boolean? = null,
    @ColumnInfo(name = "enable_advanced") val enableAdvanced: Boolean? = null,
    @ColumnInfo(name = "enable_dashboard") val enableDashboard: Boolean? = null,
    @ColumnInfo(name = "enable_backlog") val enableBacklog: Boolean? = null,
    @ColumnInfo(name = "enable_attachments") val enableAttachments: Boolean? = null,
    @ColumnInfo(name = "enable_auto_link_subprojects") val enableAutoLinkSubprojects: Boolean? = null,
)
