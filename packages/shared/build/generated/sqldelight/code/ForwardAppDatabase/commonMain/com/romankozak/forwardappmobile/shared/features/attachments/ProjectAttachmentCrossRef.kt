package com.romankozak.forwardappmobile.shared.features.attachments

import kotlin.Long
import kotlin.String

public data class ProjectAttachmentCrossRef(
  public val projectId: String,
  public val attachmentId: String,
  public val attachmentOrder: Long,
)
