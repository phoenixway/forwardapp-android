package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "structure_presets",
    indices = [Index(value = ["code"], unique = true)],
)
data class ContextRoleProfile(
    @PrimaryKey @SerializedName("id") val id: String,
    @SerializedName("code") val code: String,
    @SerializedName("label") val label: String,
    @SerializedName("description") val description: String?,
    @ColumnInfo(name = "enable_inbox") @SerializedName("enableInbox") val enableInbox: Boolean? = null,
    @ColumnInfo(name = "enable_log") @SerializedName("enableLog") val enableLog: Boolean? = null,
    @ColumnInfo(name = "enable_advanced") @SerializedName("enableAdvanced") val enableAdvanced: Boolean? = null,
    @ColumnInfo(name = "enable_dashboard") @SerializedName("enableDashboard") val enableDashboard: Boolean? = null,
    @ColumnInfo(name = "enable_backlog") @SerializedName("enableBacklog") val enableBacklog: Boolean? = null,
    @ColumnInfo(name = "enable_attachments") @SerializedName("enableAttachments") val enableAttachments: Boolean? = null,
    @ColumnInfo(name = "enable_auto_link_subprojects") @SerializedName("enableAutoLinkSubprojects") val enableAutoLinkSubprojects: Boolean? = null,
    // Додаємо відсутні поля:
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @SerializedName("version") val version: Long = 0,
    @SerializedName("isDeleted") val isDeleted: Boolean = false
)