package com.romankozak.forwardappmobile.shared.features.activitytracker

import app.cash.sqldelight.ColumnAdapter
import com.romankozak.forwardappmobile.shared.`data`.database.models.RelatedLinkList
import com.romankozak.forwardappmobile.shared.`data`.database.models.StringList
import kotlin.Boolean
import kotlin.Long
import kotlin.String

public data class ActivityRecords(
  public val id: String,
  public val name: String,
  public val description: String?,
  public val createdAt: Long,
  public val startTime: Long?,
  public val endTime: Long?,
  public val totalTimeSpentMinutes: Long?,
  public val tags: StringList?,
  public val relatedLinks: RelatedLinkList?,
  public val isCompleted: Boolean,
  public val activityType: String,
  public val parentProjectId: String?,
) {
  public class Adapter(
    public val createdAtAdapter: ColumnAdapter<Long, Long>,
    public val tagsAdapter: ColumnAdapter<StringList, String>,
    public val relatedLinksAdapter: ColumnAdapter<RelatedLinkList, String>,
  )
}
