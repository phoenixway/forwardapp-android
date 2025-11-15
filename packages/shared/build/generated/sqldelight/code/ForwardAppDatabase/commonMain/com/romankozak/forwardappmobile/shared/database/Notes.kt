package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.ColumnAdapter
import kotlin.Long
import kotlin.String

public data class Notes(
  public val id: String,
  public val projectId: String,
  public val title: String,
  public val content: String?,
  public val createdAt: Long,
  public val updatedAt: Long?,
) {
  public class Adapter(
    public val idAdapter: ColumnAdapter<String, String>,
    public val projectIdAdapter: ColumnAdapter<String, String>,
    public val titleAdapter: ColumnAdapter<String, String>,
    public val createdAtAdapter: ColumnAdapter<Long, Long>,
  )
}
