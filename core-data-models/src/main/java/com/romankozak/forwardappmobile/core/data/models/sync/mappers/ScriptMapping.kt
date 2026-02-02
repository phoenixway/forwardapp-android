package com.romankozak.forwardappmobile.core.data.models.sync.mappers

import com.romankozak.forwardappmobile.core.data.models.entities.ScriptEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ScriptSnapshot

fun ScriptEntity.toSnapshot(): ScriptSnapshot = ScriptSnapshot(
    id = this.id,
    name = this.name,
    content = this.content,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    version = this.version,
    isDeleted = this.isDeleted,
)

fun ScriptSnapshot.toEntity(): ScriptEntity = ScriptEntity(
    id = this.id,
    name = this.name,
    content = this.content,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    version = this.version,
    isDeleted = this.isDeleted,
    // Якщо Entity вимагає contextId, а в Snapshot його немає - передаємо null
    contextId = null,
)