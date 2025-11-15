package com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes

import app.cash.sqldelight.ColumnAdapter
import kotlin.Long
import kotlin.String

public data class LegacyNotes(
  public val id: String,
  public val projectId: String,
  public val title: String,
  public val content: String?,
  public val createdAt: Long,
  public val updatedAt: Long?,
) {
  public class Adapter(
    public val createdAtAdapter: ColumnAdapter<Long, Long>,
  )
}
