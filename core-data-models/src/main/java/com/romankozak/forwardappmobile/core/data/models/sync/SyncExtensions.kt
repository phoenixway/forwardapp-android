// core-data-models/.../models/SyncExtensions.kt
/*package com.romankozak.forwardappmobile.core.data.models.sync

import com.romankozak.forwardappmobile.core.data.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.NoteDocumentItemEntity

fun NoteDocumentEntity.softDelete(now: Long) = this.copy(
    isDeleted = true,
    updatedAt = now,
    version = version + 1,
    syncedAt = null
)

fun NoteDocumentItemEntity.softDelete(now: Long) = this.copy(
    isDeleted = true,
    updatedAt = now,
    version = version + 1,
    syncedAt = null
)

fun NoteDocumentEntity.bumpSync(now: Long) = this.copy(
    updatedAt = now,
    version = version + 1,
    syncedAt = null
)

fun NoteDocumentItemEntity.bumpSync(now: Long) = this.copy(
    updatedAt = now,
    version = version + 1,
    syncedAt = null
)*/