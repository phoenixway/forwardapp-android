package com.romankozak.forwardappmobile.shared.features.aichat

import kotlin.Boolean
import kotlin.Long
import kotlin.String

public data class GetConversationsWithLastMessageWithoutFolder(
  public val id: Long,
  public val title: String,
  public val creationTimestamp: Long,
  public val folderId: Long?,
  public val lastMessageId: Long?,
  public val lastMessageText: String?,
  public val lastMessageIsFromUser: Boolean?,
  public val lastMessageIsError: Boolean?,
  public val lastMessageTimestamp: Long?,
  public val lastMessageIsStreaming: Boolean?,
)
