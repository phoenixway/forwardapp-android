package com.romankozak.forwardappmobile.shared.core.models.sync

import kotlin.js.JsExport

/**
 * Canonical metadata shared by persisted entities participating in replication.
 *
 * These fields are intentionally non-optional so conflict resolution never has
 * to infer whether version or deletion metadata exists.
 */
@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
interface SyncEntityMeta {
    val id: String
    val createdAt: Long
    val updatedAt: Long
    val syncedAt: Long?
    val isDeleted: Boolean
    val version: Long
}
