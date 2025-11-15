package com.romankozak.forwardappmobile.shared.features.attachments.linkitems

import com.romankozak.forwardappmobile.shared.`data`.models.RelatedLink
import kotlin.Long
import kotlin.String

public data class SearchLinkItems(
  public val id: String,
  public val linkData: RelatedLink,
  public val createdAt: Long,
  public val projectId: String,
  public val projectName: String,
  public val listItemId: String,
  public val projectPath: String?,
)
