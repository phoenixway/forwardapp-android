package com.romankozak.forwardappmobile.shared.features.daymanagement.dailymetrics

import app.cash.sqldelight.ColumnAdapter
import com.romankozak.forwardappmobile.shared.`data`.database.models.StringDoubleMap
import kotlin.Double
import kotlin.Long
import kotlin.String

public data class DailyMetrics(
  public val id: String,
  public val dayPlanId: String,
  public val date: Long,
  public val tasksPlanned: Long,
  public val tasksCompleted: Long,
  public val completionRate: Double,
  public val totalPlannedTime: Long,
  public val totalActiveTime: Long,
  public val completedPoints: Long,
  public val totalBreakTime: Long,
  public val morningEnergyLevel: Long?,
  public val eveningEnergyLevel: Long?,
  public val overallMood: String?,
  public val stressLevel: Long?,
  public val customMetrics: StringDoubleMap?,
  public val createdAt: Long,
  public val updatedAt: Long?,
) {
  public class Adapter(
    public val dateAdapter: ColumnAdapter<Long, Long>,
    public val tasksPlannedAdapter: ColumnAdapter<Long, Long>,
    public val tasksCompletedAdapter: ColumnAdapter<Long, Long>,
    public val completionRateAdapter: ColumnAdapter<Double, Double>,
    public val totalPlannedTimeAdapter: ColumnAdapter<Long, Long>,
    public val totalActiveTimeAdapter: ColumnAdapter<Long, Long>,
    public val completedPointsAdapter: ColumnAdapter<Long, Long>,
    public val totalBreakTimeAdapter: ColumnAdapter<Long, Long>,
    public val customMetricsAdapter: ColumnAdapter<StringDoubleMap, String>,
    public val createdAtAdapter: ColumnAdapter<Long, Long>,
  )
}
