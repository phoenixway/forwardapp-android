package com.romankozak.forwardappmobile.shared.features.aichat

import app.cash.sqldelight.ColumnAdapter
import kotlin.Long
import kotlin.String

public data class ConversationFolders(
  public val id: Long,
  public val name: String,
) {
  public class Adapter(
    public val idAdapter: ColumnAdapter<Long, Long>,
  )
}
