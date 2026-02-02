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
    enableArtifact = enableArtifact ?: false,
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
    enableArtifact = enableArtifact,
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

fun ContextRoleProfileItemSnapshot.toEntity(): ContextRoleProfileItem = ContextRoleProfileItem(
    id = id,
    presetId = presetId,
    entityType = entityType,
    roleCode = roleCode,
    containerType = containerType,
    title = title,
    mandatory = mandatory,
    itemOrder = 0, // Fallback order
    version = version,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

// --- ContextConfiguration Mappings ---
fun ContextConfiguration.toSnapshot(): ContextConfigurationSnapshot = ContextConfigurationSnapshot(
    id = id,
    contextId = contextId,
    basePresetCode = basePresetCode,
    applyMode = applyMode ?: "DEFAULT", // String? -> String
    enableInbox = enableInbox ?: false,
    enableLog = enableLog ?: false,
    enableArtifact = enableArtifact ?: false,
    enableAdvanced = enableAdvanced ?: false,
    enableDashboard = enableDashboard ?: false,
    enableBacklog = enableBacklog ?: false,
    enableAttachments = enableAttachments ?: false,
    enableAutoLinkSubprojects = enableAutoLinkSubprojects ?: false,
    version = version,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

fun ContextConfigurationSnapshot.toEntity(): ContextConfiguration = ContextConfiguration(
    id = id,
    contextId = contextId,
    basePresetCode = basePresetCode,
    applyMode = applyMode,
    enableInbox = enableInbox,
    enableLog = enableLog,
    enableArtifact = enableArtifact,
    enableAdvanced = enableAdvanced,
    enableDashboard = enableDashboard,
    enableBacklog = enableBacklog,
    enableAttachments = enableAttachments,
    enableAutoLinkSubprojects = enableAutoLinkSubprojects,
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