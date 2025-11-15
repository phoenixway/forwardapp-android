package com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments

import app.cash.sqldelight.ColumnAdapter
import kotlin.Long
import kotlin.String

public data class NoteDocuments(
  public val id: String,
  public val projectId: String,
  public val name: String,
  public val content: String?,
  public val createdAt: Long,
  public val updatedAt: Long,
  public val lastCursorPosition: Long,
) {
  public class Adapter(
    public val createdAtAdapter: ColumnAdapter<Long, Long>,
    public val updatedAtAdapter: ColumnAdapter<Long, Long>,
    public val lastCursorPositionAdapter: ColumnAdapter<Long, Long>,
  )
}
