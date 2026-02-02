// File: NoteDocumentMapping.kt

package com.romankozak.forwardappmobile.core.data.models.sync.mappers

import com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.NoteDocumentSnapshot

fun NoteDocumentEntity.toSnapshot(): NoteDocumentSnapshot = NoteDocumentSnapshot(
    id = this.id,
    name = this.name,
    contextId = this.contextId,
    // ВИПРАВЛЕНО: Додаємо fallback, якщо в Entity content == null
    content = this.content ?: "",
    createdAt = this.createdAt,
    // Якщо в Entity поле updatedAt дійсно не nullable, залишаємо як є
    updatedAt = this.updatedAt,
    version = this.version,
    isDeleted = this.isDeleted,
)

fun NoteDocumentSnapshot.toEntity(): NoteDocumentEntity = NoteDocumentEntity(
    id = this.id,
    name = this.name,
    // String? -> String (Entity очікує String, тому fallback обов'язковий)
    contextId = this.contextId ?: "",
    content = this.content, // Тут помилки не буде, бо Snapshot гарантує String
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    version = this.version,
    isDeleted = this.isDeleted,
)