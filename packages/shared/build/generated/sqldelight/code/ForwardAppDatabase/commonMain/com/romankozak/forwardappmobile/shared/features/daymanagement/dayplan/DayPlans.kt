package com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan

import app.cash.sqldelight.ColumnAdapter
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.DayStatus
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String

public data class DayPlans(
  public val id: String,
  public val date: Long,
  public val name: String?,
  public val status: DayStatus,
  public val reflection: String?,
  public val energyLevel: Int?,
  public val mood: String?,
  public val weatherConditions: String?,
  public val totalPlannedMinutes: Long,
  public val totalCompletedMinutes: Long,
  public val completionPercentage: Double,
  public val createdAt: Long,
  public val updatedAt: Long?,
) {
  public class Adapter(
    public val dateAdapter: ColumnAdapter<Long, Long>,
    public val statusAdapter: ColumnAdapter<DayStatus, String>,
    public val energyLevelAdapter: ColumnAdapter<Int, Long>,
    public val totalPlannedMinutesAdapter: ColumnAdapter<Long, Long>,
    public val totalCompletedMinutesAdapter: ColumnAdapter<Long, Long>,
    public val completionPercentageAdapter: ColumnAdapter<Double, Double>,
    public val createdAtAdapter: ColumnAdapter<Long, Long>,
  )
}
