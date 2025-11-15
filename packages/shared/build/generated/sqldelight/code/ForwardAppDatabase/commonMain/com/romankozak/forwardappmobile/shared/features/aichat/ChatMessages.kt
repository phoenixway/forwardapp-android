package com.romankozak.forwardappmobile.shared.features.aichat

import app.cash.sqldelight.ColumnAdapter
import kotlin.Boolean
import kotlin.Long
import kotlin.String

public data class ChatMessages(
  public val id: Long,
  public val conversationId: Long,
  public val text: String,
  public val isFromUser: Boolean,
  public val isError: Boolean,
  public val timestamp: Long,
  public val isStreaming: Boolean,
) {
  public class Adapter(
    public val idAdapter: ColumnAdapter<Long, Long>,
    public val conversationIdAdapter: ColumnAdapter<Long, Long>,
    public val timestampAdapter: ColumnAdapter<Long, Long>,
  )
}
