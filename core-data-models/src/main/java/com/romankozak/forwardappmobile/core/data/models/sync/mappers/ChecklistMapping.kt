// File: ChecklistMapping.kt

package com.romankozak.forwardappmobile.core.data.models.sync.mappers

import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistItemEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ChecklistSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ChecklistItemSnapshot

// --- Checklist Mapping ---
fun ChecklistEntity.toSnapshot(): ChecklistSnapshot = ChecklistSnapshot(
    id = this.id,
    name = this.name,
    contextId = this.contextId,
    createdAt = this.createdAt,
    // Warning прибрано: якщо updatedAt вже Long (non-nullable), Elvis оператор не потрібен
    updatedAt = this.updatedAt,
    version = this.version,
    isDeleted = this.isDeleted,
)

fun ChecklistSnapshot.toEntity(): ChecklistEntity = ChecklistEntity(
    id = this.id,
    name = this.name,
    // Виправляємо Error: Entity очікує String, а Snapshot дає String?
    // Якщо contextId обов'язковий в Entity, ставимо пустий рядок як fallback
    contextId = this.contextId ?: "",
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    version = this.version,
    isDeleted = this.isDeleted,
)

// --- Checklist Item Mapping ---
fun ChecklistItemEntity.toSnapshot(): ChecklistItemSnapshot = ChecklistItemSnapshot(
    id = this.id,
    checklistId = this.checklistId,
    text = this.content,
    isChecked = this.isChecked,
    // Виправляємо Error: Long -> Int
    order = this.itemOrder.toInt(),
    // Якщо тут updatedAt теж може бути nullable в Entity, залишаємо ?:
    updatedAt = this.updatedAt ?: this.version,
    version = this.version,
    isDeleted = this.isDeleted,
)

fun ChecklistItemSnapshot.toEntity(): ChecklistItemEntity = ChecklistItemEntity(
    id = this.id,
    checklistId = this.checklistId,
    content = this.text,
    isChecked = this.isChecked,
    // Виправляємо Error: Int -> Long
    itemOrder = this.order.toLong(),
    updatedAt = this.updatedAt,
    version = this.version,
    isDeleted = this.isDeleted,
)