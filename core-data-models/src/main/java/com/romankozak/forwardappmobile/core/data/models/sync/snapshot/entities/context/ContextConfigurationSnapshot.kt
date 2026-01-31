package com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context

/**
 * Снапшот конфігурації функцій конкретного контексту.
 */
data class ContextConfigurationSnapshot(
    val id: String,
    val contextId: String,
    val basePresetCode: String?,
    val applyMode: String, // "ADDITIVE" тощо
    // Прапорці увімкнення модулів
    val enableInbox: Boolean?,
    val enableLog: Boolean?,
    val enableArtifact: Boolean?,
    val enableAdvanced: Boolean?,
    val enableDashboard: Boolean?,
    val enableBacklog: Boolean?,
    val enableAttachments: Boolean?,
    val enableAutoLinkSubprojects: Boolean?,
    val version: Long,
    val updatedAt: Long,
    val isDeleted: Boolean
)