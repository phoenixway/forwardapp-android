package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.romankozak.forwardappmobile.core.capability.CapabilityId

@Entity(
    tableName = "context_structures",
    indices = [Index(value = ["contextId"], unique = true)],
)
data class ContextConfiguration(
    @PrimaryKey val id: String,
    
    @SerializedName(value = "contextId", alternate = ["projectId"])
    val contextId: String = "",
    
    @ColumnInfo(name = "base_preset_code") 
    val basePresetCode: String? = null,
    
    // Нове поле для зберігання списку експериментальних можливостей (Крок 1.2)
    @ColumnInfo(name = "experimental_capability_ids") 
    val experimentalCapabilityIds: List<CapabilityId> = emptyList(),

    @ColumnInfo(name = "apply_mode") 
    val applyMode: String = "ADDITIVE",
    
    @ColumnInfo(name = "enable_inbox") val enableInbox: Boolean? = null,
    @ColumnInfo(name = "enable_log") val enableLog: Boolean? = null,
    @ColumnInfo(name = "enable_advanced") val enableAdvanced: Boolean? = null,
    @ColumnInfo(name = "enable_dashboard") val enableDashboard: Boolean? = null,
    @ColumnInfo(name = "enable_backlog") val enableBacklog: Boolean? = null,
    @ColumnInfo(name = "enable_attachments") val enableAttachments: Boolean? = null,
    @ColumnInfo(name = "enable_auto_link_subprojects") val enableAutoLinkSubprojects: Boolean? = null,
    @ColumnInfo(name = "remove_inbox_entry_after_tag_autocopy") val removeInboxEntryAfterTagAutocopy: Boolean? = null,
    @ColumnInfo(name = "remove_backlog_entry_after_tag_autocopy") val removeBacklogEntryAfterTagAutocopy: Boolean? = null,
    
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 0,
    val isDeleted: Boolean = false
){
    companion object {
        /**
         * Створює дефолтну конфігурацію, якщо вона відсутня в БД.
         * Як ID використовуємо префікс + contextId, щоб забезпечити унікальність.
         */
        fun default(contextId: String): ContextConfiguration {
            return ContextConfiguration(
                id = "default_config_$contextId",
                contextId = contextId,
                basePresetCode = "default",
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}

@Entity(
    tableName = "context_structure_items",
    foreignKeys = [
        ForeignKey(
            entity = ContextConfiguration::class,
            parentColumns = ["id"],
            childColumns = ["contextStructureId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["contextStructureId"]),
        Index(value = ["contextStructureId", "roleCode"], unique = true),
    ],
)
data class ContextStructureItem(
    @PrimaryKey val id: String,
    
    @SerializedName(value = "contextStructureId", alternate = ["projectId", "contextId"])
    val contextStructureId: String = "",
    
    val entityType: String,
    val roleCode: String,
    val containerType: String?,
    val title: String,
    
    @ColumnInfo(defaultValue = "0") 
    val mandatory: Boolean = false,
    
    @ColumnInfo(name = "is_enabled", defaultValue = "1") 
    val isEnabled: Boolean = true,
    
    val itemOrder: Long = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 0,
    val isDeleted: Boolean = false
)
