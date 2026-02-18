package com.romankozak.forwardappmobile.core.data.models.sync.mappers

import com.romankozak.forwardappmobile.core.data.models.entities.MusicNoteEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.MusicNoteSnapshot

fun MusicNoteEntity.toSnapshot(): MusicNoteSnapshot =
    MusicNoteSnapshot(
        id = id,
        name = name,
        contextId = contextId,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
        isDeleted = isDeleted,
    )

fun MusicNoteSnapshot.toEntity(): MusicNoteEntity =
    MusicNoteEntity(
        id = id,
        contextId = contextId ?: "",
        name = name,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
        isDeleted = isDeleted,
    )
