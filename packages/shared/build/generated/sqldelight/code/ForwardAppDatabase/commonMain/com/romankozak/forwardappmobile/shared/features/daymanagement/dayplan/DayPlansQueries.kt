package com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.DayStatus
import kotlin.Any
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String

public class DayPlansQueries(
  driver: SqlDriver,
  private val DayPlansAdapter: DayPlans.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getAllDayPlans(mapper: (
    id: String,
    date: Long,
    name: String?,
    status: DayStatus,
    reflection: String?,
    energyLevel: Int?,
    mood: String?,
    weatherConditions: String?,
    totalPlannedMinutes: Long,
    totalCompletedMinutes: Long,
    completionPercentage: Double,
    createdAt: Long,
    updatedAt: Long?,
  ) -> T): Query<T> = Query(-1_136_458_541, arrayOf("DayPlans"), driver, "DayPlans.sq",
      "getAllDayPlans",
      "SELECT DayPlans.id, DayPlans.date, DayPlans.name, DayPlans.status, DayPlans.reflection, DayPlans.energyLevel, DayPlans.mood, DayPlans.weatherConditions, DayPlans.totalPlannedMinutes, DayPlans.totalCompletedMinutes, DayPlans.completionPercentage, DayPlans.createdAt, DayPlans.updatedAt FROM DayPlans ORDER BY date DESC") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      DayPlansAdapter.dateAdapter.decode(cursor.getLong(1)!!),
      cursor.getString(2),
      DayPlansAdapter.statusAdapter.decode(cursor.getString(3)!!),
      cursor.getString(4),
      cursor.getLong(5)?.let { DayPlansAdapter.energyLevelAdapter.decode(it) },
      cursor.getString(6),
      cursor.getString(7),
      DayPlansAdapter.totalPlannedMinutesAdapter.decode(cursor.getLong(8)!!),
      DayPlansAdapter.totalCompletedMinutesAdapter.decode(cursor.getLong(9)!!),
      DayPlansAdapter.completionPercentageAdapter.decode(cursor.getDouble(10)!!),
      DayPlansAdapter.createdAtAdapter.decode(cursor.getLong(11)!!),
      cursor.getLong(12)
    )
  }

  public fun getAllDayPlans(): Query<DayPlans> = getAllDayPlans { id, date, name, status,
      reflection, energyLevel, mood, weatherConditions, totalPlannedMinutes, totalCompletedMinutes,
      completionPercentage, createdAt, updatedAt ->
    DayPlans(
      id,
      date,
      name,
      status,
      reflection,
      energyLevel,
      mood,
      weatherConditions,
      totalPlannedMinutes,
      totalCompletedMinutes,
      completionPercentage,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> getDayPlanById(planId: String, mapper: (
    id: String,
    date: Long,
    name: String?,
    status: DayStatus,
    reflection: String?,
    energyLevel: Int?,
    mood: String?,
    weatherConditions: String?,
    totalPlannedMinutes: Long,
    totalCompletedMinutes: Long,
    completionPercentage: Double,
    createdAt: Long,
    updatedAt: Long?,
  ) -> T): Query<T> = GetDayPlanByIdQuery(planId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      DayPlansAdapter.dateAdapter.decode(cursor.getLong(1)!!),
      cursor.getString(2),
      DayPlansAdapter.statusAdapter.decode(cursor.getString(3)!!),
      cursor.getString(4),
      cursor.getLong(5)?.let { DayPlansAdapter.energyLevelAdapter.decode(it) },
      cursor.getString(6),
      cursor.getString(7),
      DayPlansAdapter.totalPlannedMinutesAdapter.decode(cursor.getLong(8)!!),
      DayPlansAdapter.totalCompletedMinutesAdapter.decode(cursor.getLong(9)!!),
      DayPlansAdapter.completionPercentageAdapter.decode(cursor.getDouble(10)!!),
      DayPlansAdapter.createdAtAdapter.decode(cursor.getLong(11)!!),
      cursor.getLong(12)
    )
  }

  public fun getDayPlanById(planId: String): Query<DayPlans> = getDayPlanById(planId) { id, date,
      name, status, reflection, energyLevel, mood, weatherConditions, totalPlannedMinutes,
      totalCompletedMinutes, completionPercentage, createdAt, updatedAt ->
    DayPlans(
      id,
      date,
      name,
      status,
      reflection,
      energyLevel,
      mood,
      weatherConditions,
      totalPlannedMinutes,
      totalCompletedMinutes,
      completionPercentage,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> getDayPlanByDate(planDate: Long, mapper: (
    id: String,
    date: Long,
    name: String?,
    status: DayStatus,
    reflection: String?,
    energyLevel: Int?,
    mood: String?,
    weatherConditions: String?,
    totalPlannedMinutes: Long,
    totalCompletedMinutes: Long,
    completionPercentage: Double,
    createdAt: Long,
    updatedAt: Long?,
  ) -> T): Query<T> = GetDayPlanByDateQuery(planDate) { cursor ->
    mapper(
      cursor.getString(0)!!,
      DayPlansAdapter.dateAdapter.decode(cursor.getLong(1)!!),
      cursor.getString(2),
      DayPlansAdapter.statusAdapter.decode(cursor.getString(3)!!),
      cursor.getString(4),
      cursor.getLong(5)?.let { DayPlansAdapter.energyLevelAdapter.decode(it) },
      cursor.getString(6),
      cursor.getString(7),
      DayPlansAdapter.totalPlannedMinutesAdapter.decode(cursor.getLong(8)!!),
      DayPlansAdapter.totalCompletedMinutesAdapter.decode(cursor.getLong(9)!!),
      DayPlansAdapter.completionPercentageAdapter.decode(cursor.getDouble(10)!!),
      DayPlansAdapter.createdAtAdapter.decode(cursor.getLong(11)!!),
      cursor.getLong(12)
    )
  }

  public fun getDayPlanByDate(planDate: Long): Query<DayPlans> = getDayPlanByDate(planDate) { id,
      date, name, status, reflection, energyLevel, mood, weatherConditions, totalPlannedMinutes,
      totalCompletedMinutes, completionPercentage, createdAt, updatedAt ->
    DayPlans(
      id,
      date,
      name,
      status,
      reflection,
      energyLevel,
      mood,
      weatherConditions,
      totalPlannedMinutes,
      totalCompletedMinutes,
      completionPercentage,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> getDayPlansInRange(
    startDate: Long,
    endDate: Long,
    mapper: (
      id: String,
      date: Long,
      name: String?,
      status: DayStatus,
      reflection: String?,
      energyLevel: Int?,
      mood: String?,
      weatherConditions: String?,
      totalPlannedMinutes: Long,
      totalCompletedMinutes: Long,
      completionPercentage: Double,
      createdAt: Long,
      updatedAt: Long?,
    ) -> T,
  ): Query<T> = GetDayPlansInRangeQuery(startDate, endDate) { cursor ->
    mapper(
      cursor.getString(0)!!,
      DayPlansAdapter.dateAdapter.decode(cursor.getLong(1)!!),
      cursor.getString(2),
      DayPlansAdapter.statusAdapter.decode(cursor.getString(3)!!),
      cursor.getString(4),
      cursor.getLong(5)?.let { DayPlansAdapter.energyLevelAdapter.decode(it) },
      cursor.getString(6),
      cursor.getString(7),
      DayPlansAdapter.totalPlannedMinutesAdapter.decode(cursor.getLong(8)!!),
      DayPlansAdapter.totalCompletedMinutesAdapter.decode(cursor.getLong(9)!!),
      DayPlansAdapter.completionPercentageAdapter.decode(cursor.getDouble(10)!!),
      DayPlansAdapter.createdAtAdapter.decode(cursor.getLong(11)!!),
      cursor.getLong(12)
    )
  }

  public fun getDayPlansInRange(startDate: Long, endDate: Long): Query<DayPlans> =
      getDayPlansInRange(startDate, endDate) { id, date, name, status, reflection, energyLevel,
      mood, weatherConditions, totalPlannedMinutes, totalCompletedMinutes, completionPercentage,
      createdAt, updatedAt ->
    DayPlans(
      id,
      date,
      name,
      status,
      reflection,
      energyLevel,
      mood,
      weatherConditions,
      totalPlannedMinutes,
      totalCompletedMinutes,
      completionPercentage,
      createdAt,
      updatedAt
    )
  }

  public fun insertDayPlan(
    id: String,
    date: Long,
    name: String?,
    status: DayStatus,
    reflection: String?,
    energyLevel: Int?,
    mood: String?,
    weatherConditions: String?,
    totalPlannedMinutes: Long,
    totalCompletedMinutes: Long,
    completionPercentage: Double,
    createdAt: Long,
    updatedAt: Long?,
  ) {
    driver.execute(-948_763_758, """
        |INSERT OR REPLACE INTO DayPlans(
        |    id,
        |    date,
        |    name,
        |    status,
        |    reflection,
        |    energyLevel,
        |    mood,
        |    weatherConditions,
        |    totalPlannedMinutes,
        |    totalCompletedMinutes,
        |    completionPercentage,
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
        |    ?
        |)
        """.trimMargin(), 13) {
          bindString(0, id)
          bindLong(1, DayPlansAdapter.dateAdapter.encode(date))
          bindString(2, name)
          bindString(3, DayPlansAdapter.statusAdapter.encode(status))
          bindString(4, reflection)
          bindLong(5, energyLevel?.let { DayPlansAdapter.energyLevelAdapter.encode(it) })
          bindString(6, mood)
          bindString(7, weatherConditions)
          bindLong(8, DayPlansAdapter.totalPlannedMinutesAdapter.encode(totalPlannedMinutes))
          bindLong(9, DayPlansAdapter.totalCompletedMinutesAdapter.encode(totalCompletedMinutes))
          bindDouble(10, DayPlansAdapter.completionPercentageAdapter.encode(completionPercentage))
          bindLong(11, DayPlansAdapter.createdAtAdapter.encode(createdAt))
          bindLong(12, updatedAt)
        }
    notifyQueries(-948_763_758) { emit ->
      emit("DayPlans")
    }
  }

  public fun deleteDayPlan(planId: String) {
    driver.execute(-642_028_448, """DELETE FROM DayPlans WHERE id = ?""", 1) {
          bindString(0, planId)
        }
    notifyQueries(-642_028_448) { emit ->
      emit("DayPlans")
      emit("DayTasks")
    }
  }

  public fun deleteAllDayPlans() {
    driver.execute(-1_216_620_822, """DELETE FROM DayPlans""", 0)
    notifyQueries(-1_216_620_822) { emit ->
      emit("DayPlans")
      emit("DayTasks")
    }
  }

  private inner class GetDayPlanByIdQuery<out T : Any>(
    public val planId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("DayPlans", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("DayPlans", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-2_141_898_181,
        """SELECT DayPlans.id, DayPlans.date, DayPlans.name, DayPlans.status, DayPlans.reflection, DayPlans.energyLevel, DayPlans.mood, DayPlans.weatherConditions, DayPlans.totalPlannedMinutes, DayPlans.totalCompletedMinutes, DayPlans.completionPercentage, DayPlans.createdAt, DayPlans.updatedAt FROM DayPlans WHERE id = ?""",
        mapper, 1) {
      bindString(0, planId)
    }

    override fun toString(): String = "DayPlans.sq:getDayPlanById"
  }

  private inner class GetDayPlanByDateQuery<out T : Any>(
    public val planDate: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("DayPlans", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("DayPlans", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_074_965_298,
        """SELECT DayPlans.id, DayPlans.date, DayPlans.name, DayPlans.status, DayPlans.reflection, DayPlans.energyLevel, DayPlans.mood, DayPlans.weatherConditions, DayPlans.totalPlannedMinutes, DayPlans.totalCompletedMinutes, DayPlans.completionPercentage, DayPlans.createdAt, DayPlans.updatedAt FROM DayPlans WHERE date = ? LIMIT 1""",
        mapper, 1) {
      bindLong(0, DayPlansAdapter.dateAdapter.encode(planDate))
    }

    override fun toString(): String = "DayPlans.sq:getDayPlanByDate"
  }

  private inner class GetDayPlansInRangeQuery<out T : Any>(
    public val startDate: Long,
    public val endDate: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("DayPlans", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("DayPlans", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_213_626_130, """
    |SELECT DayPlans.id, DayPlans.date, DayPlans.name, DayPlans.status, DayPlans.reflection, DayPlans.energyLevel, DayPlans.mood, DayPlans.weatherConditions, DayPlans.totalPlannedMinutes, DayPlans.totalCompletedMinutes, DayPlans.completionPercentage, DayPlans.createdAt, DayPlans.updatedAt FROM DayPlans
    |WHERE date BETWEEN ? AND ?
    |ORDER BY date ASC
    """.trimMargin(), mapper, 2) {
      bindLong(0, DayPlansAdapter.dateAdapter.encode(startDate))
      bindLong(1, DayPlansAdapter.dateAdapter.encode(endDate))
    }

    override fun toString(): String = "DayPlans.sq:getDayPlansInRange"
  }
}
