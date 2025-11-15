package com.romankozak.forwardappmobile.shared.features.projects.views.inbox

import kotlin.Long
import kotlin.String

public data class InboxRecords(
  public val id: String,
  public val projectId: String,
  public val text: String,
  public val createdAt: Long,
  public val itemOrder: Long,
)
