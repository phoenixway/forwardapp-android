package com.romankozak.forwardappmobile.shared.features.attachments.linkitems

import app.cash.sqldelight.ColumnAdapter
import com.romankozak.forwardappmobile.shared.`data`.models.RelatedLink
import kotlin.Long
import kotlin.String

public data class LinkItems(
  public val id: String,
  public val linkData: RelatedLink,
  public val createdAt: Long,
) {
  public class Adapter(
    public val linkDataAdapter: ColumnAdapter<RelatedLink, String>,
    public val createdAtAdapter: ColumnAdapter<Long, Long>,
  )
}
