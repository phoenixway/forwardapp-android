package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "structure_presets",
    indices = [Index(value = ["code"], unique = true)],
)
data class ContextRoleProfile(
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
    // Додаємо відсутні поля:
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 0,
    val isDeleted: Boolean = false
)