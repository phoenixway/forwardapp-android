package com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.domain.model

data class ChecklistItem(
    val id: String,
    val checklistId: String,
    val content: String,
    val isChecked: Boolean,
    val order: Long,
)
