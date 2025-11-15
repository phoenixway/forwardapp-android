package com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments

import app.cash.sqldelight.ColumnAdapter
import kotlin.Boolean
import kotlin.Long
import kotlin.String

public data class NoteDocumentItems(
  public val id: String,
  public val listId: String,
  public val parentId: String?,
  public val content: String,
  public val isCompleted: Boolean,
  public val itemOrder: Long,
  public val createdAt: Long,
  public val updatedAt: Long,
) {
  public class Adapter(
    public val itemOrderAdapter: ColumnAdapter<Long, Long>,
    public val createdAtAdapter: ColumnAdapter<Long, Long>,
    public val updatedAtAdapter: ColumnAdapter<Long, Long>,
  )
}
