package com.romankozak.forwardappmobile.shared.features.attachments.types.checklists

import app.cash.sqldelight.ColumnAdapter
import kotlin.Boolean
import kotlin.Long
import kotlin.String

public data class ChecklistItems(
  public val id: String,
  public val checklistId: String,
  public val content: String,
  public val isChecked: Boolean,
  public val itemOrder: Long,
) {
  public class Adapter(
    public val itemOrderAdapter: ColumnAdapter<Long, Long>,
  )
}
