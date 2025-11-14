package com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.data.mappers

import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.ChecklistItems
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.Checklists
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.domain.model.Checklist
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.domain.model.ChecklistItem

fun Checklists.toDomain(): Checklist = Checklist(
    id = id,
    projectId = projectId,
    name = name,
)

fun ChecklistItems.toDomain(): ChecklistItem = ChecklistItem(
    id = id,
    checklistId = checklistId,
    content = content,
    isChecked = isChecked,
    order = itemOrder,
)
