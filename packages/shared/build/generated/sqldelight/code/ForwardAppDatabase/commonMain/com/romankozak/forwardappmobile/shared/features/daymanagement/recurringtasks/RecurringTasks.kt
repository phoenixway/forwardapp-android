package com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks

import app.cash.sqldelight.ColumnAdapter
import com.romankozak.forwardappmobile.shared.`data`.database.models.StringList
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.TaskPriority
import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.domain.model.RecurrenceFrequency
import kotlin.Int
import kotlin.Long
import kotlin.String

public data class RecurringTasks(
  public val id: String,
  public val title: String,
  public val description: String?,
  public val goalId: String?,
  public val duration: Int?,
  public val priority: TaskPriority,
  public val points: Int,
  public val frequency: RecurrenceFrequency,
  public val interval: Int,
  public val daysOfWeek: StringList?,
  public val startDate: Long,
  public val endDate: Long?,
) {
  public class Adapter(
    public val durationAdapter: ColumnAdapter<Int, Long>,
    public val priorityAdapter: ColumnAdapter<TaskPriority, String>,
    public val pointsAdapter: ColumnAdapter<Int, Long>,
    public val frequencyAdapter: ColumnAdapter<RecurrenceFrequency, String>,
    public val intervalAdapter: ColumnAdapter<Int, Long>,
    public val daysOfWeekAdapter: ColumnAdapter<StringList, String>,
    public val startDateAdapter: ColumnAdapter<Long, Long>,
  )
}
