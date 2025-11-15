package com.romankozak.forwardappmobile.shared.features.daymanagement.dailymetrics

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.`data`.database.models.StringDoubleMap
import kotlin.Any
import kotlin.Double
import kotlin.Long
import kotlin.String

public class DailyMetricsQueries(
  driver: SqlDriver,
  private val DailyMetricsAdapter: DailyMetrics.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getDailyMetrics(mapper: (
    id: String,
    dayPlanId: String,
    date: Long,
    tasksPlanned: Long,
    tasksCompleted: Long,
    completionRate: Double,
    totalPlannedTime: Long,
    totalActiveTime: Long,
    completedPoints: Long,
    totalBreakTime: Long,
    morningEnergyLevel: Long?,
    eveningEnergyLevel: Long?,
    overallMood: String?,
    stressLevel: Long?,
    customMetrics: StringDoubleMap?,
    createdAt: Long,
    updatedAt: Long?,
  ) -> T): Query<T> = Query(9_134_529, arrayOf("DailyMetrics"), driver, "DailyMetrics.sq",
      "getDailyMetrics",
      "SELECT DailyMetrics.id, DailyMetrics.dayPlanId, DailyMetrics.date, DailyMetrics.tasksPlanned, DailyMetrics.tasksCompleted, DailyMetrics.completionRate, DailyMetrics.totalPlannedTime, DailyMetrics.totalActiveTime, DailyMetrics.completedPoints, DailyMetrics.totalBreakTime, DailyMetrics.morningEnergyLevel, DailyMetrics.eveningEnergyLevel, DailyMetrics.overallMood, DailyMetrics.stressLevel, DailyMetrics.customMetrics, DailyMetrics.createdAt, DailyMetrics.updatedAt FROM DailyMetrics ORDER BY date DESC") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      DailyMetricsAdapter.dateAdapter.decode(cursor.getLong(2)!!),
      DailyMetricsAdapter.tasksPlannedAdapter.decode(cursor.getLong(3)!!),
      DailyMetricsAdapter.tasksCompletedAdapter.decode(cursor.getLong(4)!!),
      DailyMetricsAdapter.completionRateAdapter.decode(cursor.getDouble(5)!!),
      DailyMetricsAdapter.totalPlannedTimeAdapter.decode(cursor.getLong(6)!!),
      DailyMetricsAdapter.totalActiveTimeAdapter.decode(cursor.getLong(7)!!),
      DailyMetricsAdapter.completedPointsAdapter.decode(cursor.getLong(8)!!),
      DailyMetricsAdapter.totalBreakTimeAdapter.decode(cursor.getLong(9)!!),
      cursor.getLong(10),
      cursor.getLong(11),
      cursor.getString(12),
      cursor.getLong(13),
      cursor.getString(14)?.let { DailyMetricsAdapter.customMetricsAdapter.decode(it) },
      DailyMetricsAdapter.createdAtAdapter.decode(cursor.getLong(15)!!),
      cursor.getLong(16)
    )
  }

  public fun getDailyMetrics(): Query<DailyMetrics> = getDailyMetrics { id, dayPlanId, date,
      tasksPlanned, tasksCompleted, completionRate, totalPlannedTime, totalActiveTime,
      completedPoints, totalBreakTime, morningEnergyLevel, eveningEnergyLevel, overallMood,
      stressLevel, customMetrics, createdAt, updatedAt ->
    DailyMetrics(
      id,
      dayPlanId,
      date,
      tasksPlanned,
      tasksCompleted,
      completionRate,
      totalPlannedTime,
      totalActiveTime,
      completedPoints,
      totalBreakTime,
      morningEnergyLevel,
      eveningEnergyLevel,
      overallMood,
      stressLevel,
      customMetrics,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> getMetricsForDayPlan(dayPlanId: String, mapper: (
    id: String,
    dayPlanId: String,
    date: Long,
    tasksPlanned: Long,
    tasksCompleted: Long,
    completionRate: Double,
    totalPlannedTime: Long,
    totalActiveTime: Long,
    completedPoints: Long,
    totalBreakTime: Long,
    morningEnergyLevel: Long?,
    eveningEnergyLevel: Long?,
    overallMood: String?,
    stressLevel: Long?,
    customMetrics: StringDoubleMap?,
    createdAt: Long,
    updatedAt: Long?,
  ) -> T): Query<T> = GetMetricsForDayPlanQuery(dayPlanId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      DailyMetricsAdapter.dateAdapter.decode(cursor.getLong(2)!!),
      DailyMetricsAdapter.tasksPlannedAdapter.decode(cursor.getLong(3)!!),
      DailyMetricsAdapter.tasksCompletedAdapter.decode(cursor.getLong(4)!!),
      DailyMetricsAdapter.completionRateAdapter.decode(cursor.getDouble(5)!!),
      DailyMetricsAdapter.totalPlannedTimeAdapter.decode(cursor.getLong(6)!!),
      DailyMetricsAdapter.totalActiveTimeAdapter.decode(cursor.getLong(7)!!),
      DailyMetricsAdapter.completedPointsAdapter.decode(cursor.getLong(8)!!),
      DailyMetricsAdapter.totalBreakTimeAdapter.decode(cursor.getLong(9)!!),
      cursor.getLong(10),
      cursor.getLong(11),
      cursor.getString(12),
      cursor.getLong(13),
      cursor.getString(14)?.let { DailyMetricsAdapter.customMetricsAdapter.decode(it) },
      DailyMetricsAdapter.createdAtAdapter.decode(cursor.getLong(15)!!),
      cursor.getLong(16)
    )
  }

  public fun getMetricsForDayPlan(dayPlanId: String): Query<DailyMetrics> =
      getMetricsForDayPlan(dayPlanId) { id, dayPlanId_, date, tasksPlanned, tasksCompleted,
      completionRate, totalPlannedTime, totalActiveTime, completedPoints, totalBreakTime,
      morningEnergyLevel, eveningEnergyLevel, overallMood, stressLevel, customMetrics, createdAt,
      updatedAt ->
    DailyMetrics(
      id,
      dayPlanId_,
      date,
      tasksPlanned,
      tasksCompleted,
      completionRate,
      totalPlannedTime,
      totalActiveTime,
      completedPoints,
      totalBreakTime,
      morningEnergyLevel,
      eveningEnergyLevel,
      overallMood,
      stressLevel,
      customMetrics,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> getMetricById(metricId: String, mapper: (
    id: String,
    dayPlanId: String,
    date: Long,
    tasksPlanned: Long,
    tasksCompleted: Long,
    completionRate: Double,
    totalPlannedTime: Long,
    totalActiveTime: Long,
    completedPoints: Long,
    totalBreakTime: Long,
    morningEnergyLevel: Long?,
    eveningEnergyLevel: Long?,
    overallMood: String?,
    stressLevel: Long?,
    customMetrics: StringDoubleMap?,
    createdAt: Long,
    updatedAt: Long?,
  ) -> T): Query<T> = GetMetricByIdQuery(metricId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      DailyMetricsAdapter.dateAdapter.decode(cursor.getLong(2)!!),
      DailyMetricsAdapter.tasksPlannedAdapter.decode(cursor.getLong(3)!!),
      DailyMetricsAdapter.tasksCompletedAdapter.decode(cursor.getLong(4)!!),
      DailyMetricsAdapter.completionRateAdapter.decode(cursor.getDouble(5)!!),
      DailyMetricsAdapter.totalPlannedTimeAdapter.decode(cursor.getLong(6)!!),
      DailyMetricsAdapter.totalActiveTimeAdapter.decode(cursor.getLong(7)!!),
      DailyMetricsAdapter.completedPointsAdapter.decode(cursor.getLong(8)!!),
      DailyMetricsAdapter.totalBreakTimeAdapter.decode(cursor.getLong(9)!!),
      cursor.getLong(10),
      cursor.getLong(11),
      cursor.getString(12),
      cursor.getLong(13),
      cursor.getString(14)?.let { DailyMetricsAdapter.customMetricsAdapter.decode(it) },
      DailyMetricsAdapter.createdAtAdapter.decode(cursor.getLong(15)!!),
      cursor.getLong(16)
    )
  }

  public fun getMetricById(metricId: String): Query<DailyMetrics> = getMetricById(metricId) { id,
      dayPlanId, date, tasksPlanned, tasksCompleted, completionRate, totalPlannedTime,
      totalActiveTime, completedPoints, totalBreakTime, morningEnergyLevel, eveningEnergyLevel,
      overallMood, stressLevel, customMetrics, createdAt, updatedAt ->
    DailyMetrics(
      id,
      dayPlanId,
      date,
      tasksPlanned,
      tasksCompleted,
      completionRate,
      totalPlannedTime,
      totalActiveTime,
      completedPoints,
      totalBreakTime,
      morningEnergyLevel,
      eveningEnergyLevel,
      overallMood,
      stressLevel,
      customMetrics,
      createdAt,
      updatedAt
    )
  }

  public fun insertDailyMetric(
    id: String,
    dayPlanId: String,
    date: Long,
    tasksPlanned: Long,
    tasksCompleted: Long,
    completionRate: Double,
    totalPlannedTime: Long,
    totalActiveTime: Long,
    completedPoints: Long,
    totalBreakTime: Long,
    morningEnergyLevel: Long?,
    eveningEnergyLevel: Long?,
    overallMood: String?,
    stressLevel: Long?,
    customMetrics: StringDoubleMap?,
    createdAt: Long,
    updatedAt: Long?,
  ) {
    driver.execute(921_743_185, """
        |INSERT OR REPLACE INTO DailyMetrics(
        |    id,
        |    dayPlanId,
        |    date,
        |    tasksPlanned,
        |    tasksCompleted,
        |    completionRate,
        |    totalPlannedTime,
        |    totalActiveTime,
        |    completedPoints,
        |    totalBreakTime,
        |    morningEnergyLevel,
        |    eveningEnergyLevel,
        |    overallMood,
        |    stressLevel,
        |    customMetrics,
        |    createdAt,
        |    updatedAt
        |) VALUES (
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?
        |)
        """.trimMargin(), 17) {
          bindString(0, id)
          bindString(1, dayPlanId)
          bindLong(2, DailyMetricsAdapter.dateAdapter.encode(date))
          bindLong(3, DailyMetricsAdapter.tasksPlannedAdapter.encode(tasksPlanned))
          bindLong(4, DailyMetricsAdapter.tasksCompletedAdapter.encode(tasksCompleted))
          bindDouble(5, DailyMetricsAdapter.completionRateAdapter.encode(completionRate))
          bindLong(6, DailyMetricsAdapter.totalPlannedTimeAdapter.encode(totalPlannedTime))
          bindLong(7, DailyMetricsAdapter.totalActiveTimeAdapter.encode(totalActiveTime))
          bindLong(8, DailyMetricsAdapter.completedPointsAdapter.encode(completedPoints))
          bindLong(9, DailyMetricsAdapter.totalBreakTimeAdapter.encode(totalBreakTime))
          bindLong(10, morningEnergyLevel)
          bindLong(11, eveningEnergyLevel)
          bindString(12, overallMood)
          bindLong(13, stressLevel)
          bindString(14, customMetrics?.let { DailyMetricsAdapter.customMetricsAdapter.encode(it) })
          bindLong(15, DailyMetricsAdapter.createdAtAdapter.encode(createdAt))
          bindLong(16, updatedAt)
        }
    notifyQueries(921_743_185) { emit ->
      emit("DailyMetrics")
    }
  }

  public fun deleteDailyMetric(metricId: String) {
    driver.execute(-1_441_005_281, """DELETE FROM DailyMetrics WHERE id = ?""", 1) {
          bindString(0, metricId)
        }
    notifyQueries(-1_441_005_281) { emit ->
      emit("DailyMetrics")
    }
  }

  private inner class GetMetricsForDayPlanQuery<out T : Any>(
    public val dayPlanId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("DailyMetrics", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("DailyMetrics", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_549_294_088,
        """SELECT DailyMetrics.id, DailyMetrics.dayPlanId, DailyMetrics.date, DailyMetrics.tasksPlanned, DailyMetrics.tasksCompleted, DailyMetrics.completionRate, DailyMetrics.totalPlannedTime, DailyMetrics.totalActiveTime, DailyMetrics.completedPoints, DailyMetrics.totalBreakTime, DailyMetrics.morningEnergyLevel, DailyMetrics.eveningEnergyLevel, DailyMetrics.overallMood, DailyMetrics.stressLevel, DailyMetrics.customMetrics, DailyMetrics.createdAt, DailyMetrics.updatedAt FROM DailyMetrics WHERE dayPlanId = ? ORDER BY date DESC""",
        mapper, 1) {
      bindString(0, dayPlanId)
    }

    override fun toString(): String = "DailyMetrics.sq:getMetricsForDayPlan"
  }

  private inner class GetMetricByIdQuery<out T : Any>(
    public val metricId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("DailyMetrics", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("DailyMetrics", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(216_276_153,
        """SELECT DailyMetrics.id, DailyMetrics.dayPlanId, DailyMetrics.date, DailyMetrics.tasksPlanned, DailyMetrics.tasksCompleted, DailyMetrics.completionRate, DailyMetrics.totalPlannedTime, DailyMetrics.totalActiveTime, DailyMetrics.completedPoints, DailyMetrics.totalBreakTime, DailyMetrics.morningEnergyLevel, DailyMetrics.eveningEnergyLevel, DailyMetrics.overallMood, DailyMetrics.stressLevel, DailyMetrics.customMetrics, DailyMetrics.createdAt, DailyMetrics.updatedAt FROM DailyMetrics WHERE id = ?""",
        mapper, 1) {
      bindString(0, metricId)
    }

    override fun toString(): String = "DailyMetrics.sq:getMetricById"
  }
}
