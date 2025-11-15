package com.romankozak.forwardappmobile.shared.features.aichat

import app.cash.sqldelight.ColumnAdapter
import kotlin.Long
import kotlin.String

public data class Conversations(
  public val id: Long,
  public val title: String,
  public val creationTimestamp: Long,
  public val folderId: Long?,
) {
  public class Adapter(
    public val idAdapter: ColumnAdapter<Long, Long>,
    public val creationTimestampAdapter: ColumnAdapter<Long, Long>,
  )
}
