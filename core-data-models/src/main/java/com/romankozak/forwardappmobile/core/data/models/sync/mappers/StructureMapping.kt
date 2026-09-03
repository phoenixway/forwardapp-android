package com.romankozak.forwardappmobile.core.data.models.sync.mappers

import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfile
import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfileItem
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStructureItem
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.*

// --- ContextRoleProfile Mappings ---
fun ContextRoleProfile.toSnapshot(): ContextRoleProfileSnapshot = ContextRoleProfileSnapshot(
    id = id,
    code = code,
    label = label,
    description = description,
    enableInbox = enableInbox ?: false, // Boolean? -> Boolean
    enableLog = enableLog ?: false,
    enableAdvanced = enableAdvanced ?: false,
    enableDashboard = enableDashboard ?: false,
    enableBacklog = enableBacklog ?: false,
    enableAttachments = enableAttachments ?: false,
    enableAutoLinkSubprojects = enableAutoLinkSubprojects ?: false,
    version = version,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

fun ContextRoleProfileSnapshot.toEntity(): ContextRoleProfile = ContextRoleProfile(
    id = id,
    code = code,
    label = label,
    description = description,
    enableInbox = enableInbox,
    enableLog = enableLog,
    enableAdvanced = enableAdvanced,
    enableDashboard = enableDashboard,
    enableBacklog = enableBacklog,
    enableAttachments = enableAttachments,
    enableAutoLinkSubprojects = enableAutoLinkSubprojects,
    version = version,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    createdAt = System.currentTimeMillis(),
)

// --- ContextRoleProfileItem Mappings ---
fun ContextRoleProfileItem.toSnapshot(): ContextRoleProfileItemSnapshot = ContextRoleProfileItemSnapshot(
    id = id,
    presetId = presetId,
    entityType = entityType,
    roleCode = roleCode,
    containerType = containerType ?: "", // String? -> String
    title = title ?: "Untitled", // String? -> String
    mandatory = mandatory,
    version = version,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

// File: StructureMapping.kt

fun ContextRoleProfileItemSnapshot.toEntity(): ContextRoleProfileItem = ContextRoleProfileItem(
    id = id,
    presetId = presetId,
    entityType = entityType,
    // ВИПРАВЛЕНО: Додаємо fallback, бо Snapshot дає String?, а Entity хоче String
    roleCode = roleCode ?: "",

    // Тут помилки не буде: Snapshot дає String, що легко "вкладається" в String? сутності
    containerType = containerType,

    title = title,
    mandatory = mandatory,
    itemOrder = 0, // Fallback order, як ви і вказали
    version = version,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

// --- ContextConfiguration Mappings ---
// File: StructureMapping.kt

fun ContextConfiguration.toSnapshot(): ContextConfigurationSnapshot = ContextConfigurationSnapshot(
    id = this.id,
    contextId = this.contextId,
    // ВИПРАВЛЕНО: Entity має String?, а Snapshot очікує String.
    // Додаємо значення за замовчуванням (fallback).
    basePresetCode = this.basePresetCode ?: "DEFAULT",
    experimentalCapabilityIds = this.experimentalCapabilityIds,

    // ВИПРАВЛЕНО: IDE каже, що applyMode в Entity вже є non-nullable String.
    // Тому оператор ?: "DEFAULT" тут зайвий (Redundant).
    applyMode = this.applyMode,

    // Якщо ці поля в Entity є nullable (Boolean?), залишаємо ?: false.
    // Якщо вони non-nullable, можна прибрати ?: false, як у випадку з applyMode.
    enableInbox = this.enableInbox ?: false,
    enableLog = this.enableLog ?: false,
    enableAdvanced = this.enableAdvanced ?: false,
    enableDashboard = this.enableDashboard ?: false,
    enableBacklog = this.enableBacklog ?: false,
    enableAttachments = this.enableAttachments ?: false,
    enableAutoLinkSubprojects = this.enableAutoLinkSubprojects ?: false,
    removeInboxEntryAfterTagAutocopy = this.removeInboxEntryAfterTagAutocopy ?: false,
    removeBacklogEntryAfterTagAutocopy = this.removeBacklogEntryAfterTagAutocopy ?: false,

    version = this.version,
    updatedAt = this.updatedAt,
    isDeleted = this.isDeleted
)

fun ContextConfigurationSnapshot.toEntity(): ContextConfiguration = ContextConfiguration(
    id = id,
    contextId = contextId,
    basePresetCode = basePresetCode,
    experimentalCapabilityIds =
        experimentalCapabilityIds
            .orEmpty()
            .mapNotNull { capability ->
                runCatching { capability.raw.trim() }
                    .getOrNull()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { com.romankozak.forwardappmobile.core.capability.CapabilityId(it) }
            },
    applyMode = applyMode,
    enableInbox = enableInbox,
    enableLog = enableLog,
    enableAdvanced = enableAdvanced,
    enableDashboard = enableDashboard,
    enableBacklog = enableBacklog,
    enableAttachments = enableAttachments,
    enableAutoLinkSubprojects = enableAutoLinkSubprojects,
    removeInboxEntryAfterTagAutocopy = removeInboxEntryAfterTagAutocopy ?: false,
    removeBacklogEntryAfterTagAutocopy = removeBacklogEntryAfterTagAutocopy ?: false,
    version = version,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

// --- ContextStructureItem Mappings ---
fun ContextStructureItem.toSnapshot(): ContextStructureItemSnapshot = ContextStructureItemSnapshot(
    id = id,
    contextStructureId = contextStructureId,
    entityType = entityType,
    roleCode = roleCode,
    containerType = containerType ?: "",
    title = title ?: "",
    mandatory = mandatory,
    isEnabled = isEnabled,
    version = version,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

fun ContextStructureItemSnapshot.toEntity(): ContextStructureItem = ContextStructureItem(
    id = id,
    contextStructureId = contextStructureId,
    entityType = entityType,
    roleCode = roleCode,
    containerType = containerType,
    title = title,
    mandatory = mandatory,
    isEnabled = isEnabled,
    itemOrder = 0,
    updatedAt = updatedAt,
    version = version,
    isDeleted = isDeleted,
)
