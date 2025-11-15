package com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan

import app.cash.sqldelight.ColumnAdapter
import com.romankozak.forwardappmobile.shared.`data`.database.models.StringList
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.TaskPriority
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.TaskStatus
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String

public data class DayTasks(
  public val id: String,
  public val dayPlanId: String,
  public val title: String,
  public val description: String?,
  public val goalId: String?,
  public val projectId: String?,
  public val activityRecordId: String?,
  public val recurringTaskId: String?,
  public val taskType: String?,
  public val entityId: String?,
  public val order: Long,
  public val priority: TaskPriority,
  public val status: TaskStatus,
  public val completed: Boolean,
  public val scheduledTime: Long?,
  public val estimatedDurationMinutes: Long?,
  public val actualDurationMinutes: Long?,
  public val dueTime: Long?,
  public val valueImportance: Double,
  public val valueImpact: Double,
  public val effort: Double,
  public val cost: Double,
  public val risk: Double,
  public val location: String?,
  public val tags: StringList?,
  public val notes: String?,
  public val createdAt: Long,
  public val updatedAt: Long?,
  public val completedAt: Long?,
  public val nextOccurrenceTime: Long?,
  public val points: Int,
) {
  public class Adapter(
    public val orderAdapter: ColumnAdapter<Long, Long>,
    public val priorityAdapter: ColumnAdapter<TaskPriority, String>,
    public val statusAdapter: ColumnAdapter<TaskStatus, String>,
    public val valueImportanceAdapter: ColumnAdapter<Double, Double>,
    public val valueImpactAdapter: ColumnAdapter<Double, Double>,
    public val effortAdapter: ColumnAdapter<Double, Double>,
    public val costAdapter: ColumnAdapter<Double, Double>,
    public val riskAdapter: ColumnAdapter<Double, Double>,
    public val tagsAdapter: ColumnAdapter<StringList, String>,
    public val createdAtAdapter: ColumnAdapter<Long, Long>,
    public val pointsAdapter: ColumnAdapter<Int, Long>,
  )
}
