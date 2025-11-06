package com.romankozak.forwardappmobile.shared.features.checklists.data.model

import com.benasher44.uuid.uuid4

/** Basic checklist entity persisted via SQLDelight. */
data class Checklist(
    val id: String = uuid4().toString(),
    val projectId: String,
    val name: String,
)

/** Individual checklist item belonging to a checklist. */
data class ChecklistItem(
    val id: String = uuid4().toString(),
    val checklistId: String,
    val content: String,
    val isChecked: Boolean = false,
    val itemOrder: Long = 0,
)
