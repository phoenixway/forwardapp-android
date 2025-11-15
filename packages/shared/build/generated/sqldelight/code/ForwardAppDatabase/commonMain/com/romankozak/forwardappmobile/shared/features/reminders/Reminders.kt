package com.romankozak.forwardappmobile.shared.features.reminders

import app.cash.sqldelight.ColumnAdapter
import kotlin.Long
import kotlin.String

public data class Reminders(
  public val id: String,
  public val entityId: String,
  public val entityType: String,
  public val reminderTime: Long,
  public val status: String,
  public val creationTime: Long,
  public val snoozeUntil: Long?,
) {
  public class Adapter(
    public val reminderTimeAdapter: ColumnAdapter<Long, Long>,
    public val creationTimeAdapter: ColumnAdapter<Long, Long>,
  )
}
