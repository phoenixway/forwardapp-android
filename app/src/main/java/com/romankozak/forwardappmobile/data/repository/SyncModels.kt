package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.features.contexts.data.models.BacklogItem
import com.romankozak.forwardappmobile.features.contexts.data.models.Goal

enum class ChangeType { Add, Update, Delete, Move }

data class SyncChange(
    val type: ChangeType,
    val entityType: String,
    val id: String,
    val description: String,
    val longDescription: String? = null,
    val entity: Any,
)

data class SyncReport(val changes: List<SyncChange>)

internal data class LocalSyncState(
    val goals: Map<String, Goal>,
    val goalLists: Map<String, com.romankozak.forwardappmobile.features.contexts.data.models.Context>,
    val backlogItems: Map<String, BacklogItem>,
)
