package com.romankozak.forwardappmobile.core.data.models.sync

import com.romankozak.forwardappmobile.core.data.models.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.Context
import com.romankozak.forwardappmobile.core.data.models.Goal

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
    val goalLists: Map<String, Context>,
    val backlogItems: Map<String, BacklogItem>,
)
