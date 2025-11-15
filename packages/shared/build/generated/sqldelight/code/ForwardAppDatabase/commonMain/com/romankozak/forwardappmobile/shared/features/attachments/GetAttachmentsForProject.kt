package com.romankozak.forwardappmobile.shared.features.attachments

import kotlin.Long
import kotlin.String

public data class GetAttachmentsForProject(
  public val id: String,
  public val attachmentType: String,
  public val entityId: String,
  public val ownerProjectId: String?,
  public val createdAt: Long,
  public val updatedAt: Long,
  public val projectId: String,
  public val attachmentOrder: Long,
)
