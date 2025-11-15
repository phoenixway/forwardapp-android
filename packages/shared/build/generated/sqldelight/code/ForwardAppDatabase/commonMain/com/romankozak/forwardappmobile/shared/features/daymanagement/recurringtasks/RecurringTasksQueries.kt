package com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.`data`.database.models.StringList
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.TaskPriority
import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.domain.model.RecurrenceFrequency
import kotlin.Any
import kotlin.Int
import kotlin.Long
import kotlin.String

public class RecurringTasksQueries(
  driver: SqlDriver,
  private val RecurringTasksAdapter: RecurringTasks.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getAllRecurringTasks(mapper: (
    id: String,
    title: String,
    description: String?,
    goalId: String?,
    duration: Int?,
    priority: TaskPriority,
    points: Int,
    frequency: RecurrenceFrequency,
    interval: Int,
    daysOfWeek: StringList?,
    startDate: Long,
    endDate: Long?,
  ) -> T): Query<T> = Query(852_257_115, arrayOf("RecurringTasks"), driver, "RecurringTasks.sq",
      "getAllRecurringTasks",
      "SELECT RecurringTasks.id, RecurringTasks.title, RecurringTasks.description, RecurringTasks.goalId, RecurringTasks.duration, RecurringTasks.priority, RecurringTasks.points, RecurringTasks.frequency, RecurringTasks.\"interval\", RecurringTasks.daysOfWeek, RecurringTasks.startDate, RecurringTasks.endDate FROM RecurringTasks ORDER BY startDate DESC") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      cursor.getLong(4)?.let { RecurringTasksAdapter.durationAdapter.decode(it) },
      RecurringTasksAdapter.priorityAdapter.decode(cursor.getString(5)!!),
      RecurringTasksAdapter.pointsAdapter.decode(cursor.getLong(6)!!),
      RecurringTasksAdapter.frequencyAdapter.decode(cursor.getString(7)!!),
      RecurringTasksAdapter.intervalAdapter.decode(cursor.getLong(8)!!),
      cursor.getString(9)?.let { RecurringTasksAdapter.daysOfWeekAdapter.decode(it) },
      RecurringTasksAdapter.startDateAdapter.decode(cursor.getLong(10)!!),
      cursor.getLong(11)
    )
  }

  public fun getAllRecurringTasks(): Query<RecurringTasks> = getAllRecurringTasks { id, title,
      description, goalId, duration, priority, points, frequency, interval, daysOfWeek, startDate,
      endDate ->
    RecurringTasks(
      id,
      title,
      description,
      goalId,
      duration,
      priority,
      points,
      frequency,
      interval,
      daysOfWeek,
      startDate,
      endDate
    )
  }

  public fun <T : Any> getRecurringTasksByGoal(goalId: String?, mapper: (
    id: String,
    title: String,
    description: String?,
    goalId: String?,
    duration: Int?,
    priority: TaskPriority,
    points: Int,
    frequency: RecurrenceFrequency,
    interval: Int,
    daysOfWeek: StringList?,
    startDate: Long,
    endDate: Long?,
  ) -> T): Query<T> = GetRecurringTasksByGoalQuery(goalId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      cursor.getLong(4)?.let { RecurringTasksAdapter.durationAdapter.decode(it) },
      RecurringTasksAdapter.priorityAdapter.decode(cursor.getString(5)!!),
      RecurringTasksAdapter.pointsAdapter.decode(cursor.getLong(6)!!),
      RecurringTasksAdapter.frequencyAdapter.decode(cursor.getString(7)!!),
      RecurringTasksAdapter.intervalAdapter.decode(cursor.getLong(8)!!),
      cursor.getString(9)?.let { RecurringTasksAdapter.daysOfWeekAdapter.decode(it) },
      RecurringTasksAdapter.startDateAdapter.decode(cursor.getLong(10)!!),
      cursor.getLong(11)
    )
  }

  public fun getRecurringTasksByGoal(goalId: String?): Query<RecurringTasks> =
      getRecurringTasksByGoal(goalId) { id, title, description, goalId_, duration, priority, points,
      frequency, interval, daysOfWeek, startDate, endDate ->
    RecurringTasks(
      id,
      title,
      description,
      goalId_,
      duration,
      priority,
      points,
      frequency,
      interval,
      daysOfWeek,
      startDate,
      endDate
    )
  }

  public fun <T : Any> getRecurringTaskById(id: String, mapper: (
    id: String,
    title: String,
    description: String?,
    goalId: String?,
    duration: Int?,
    priority: TaskPriority,
    points: Int,
    frequency: RecurrenceFrequency,
    interval: Int,
    daysOfWeek: StringList?,
    startDate: Long,
    endDate: Long?,
  ) -> T): Query<T> = GetRecurringTaskByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      cursor.getLong(4)?.let { RecurringTasksAdapter.durationAdapter.decode(it) },
      RecurringTasksAdapter.priorityAdapter.decode(cursor.getString(5)!!),
      RecurringTasksAdapter.pointsAdapter.decode(cursor.getLong(6)!!),
      RecurringTasksAdapter.frequencyAdapter.decode(cursor.getString(7)!!),
      RecurringTasksAdapter.intervalAdapter.decode(cursor.getLong(8)!!),
      cursor.getString(9)?.let { RecurringTasksAdapter.daysOfWeekAdapter.decode(it) },
      RecurringTasksAdapter.startDateAdapter.decode(cursor.getLong(10)!!),
      cursor.getLong(11)
    )
  }

  public fun getRecurringTaskById(id: String): Query<RecurringTasks> = getRecurringTaskById(id) {
      id_, title, description, goalId, duration, priority, points, frequency, interval, daysOfWeek,
      startDate, endDate ->
    RecurringTasks(
      id_,
      title,
      description,
      goalId,
      duration,
      priority,
      points,
      frequency,
      interval,
      daysOfWeek,
      startDate,
      endDate
    )
  }

  public fun <T : Any> searchRecurringTasksFts(query: String, mapper: (
    id: String,
    title: String,
    description: String?,
    goalId: String?,
    duration: Int?,
    priority: TaskPriority,
    points: Int,
    frequency: RecurrenceFrequency,
    interval: Int,
    daysOfWeek: StringList?,
    startDate: Long,
    endDate: Long?,
  ) -> T): Query<T> = SearchRecurringTasksFtsQuery(query) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      cursor.getLong(4)?.let { RecurringTasksAdapter.durationAdapter.decode(it) },
      RecurringTasksAdapter.priorityAdapter.decode(cursor.getString(5)!!),
      RecurringTasksAdapter.pointsAdapter.decode(cursor.getLong(6)!!),
      RecurringTasksAdapter.frequencyAdapter.decode(cursor.getString(7)!!),
      RecurringTasksAdapter.intervalAdapter.decode(cursor.getLong(8)!!),
      cursor.getString(9)?.let { RecurringTasksAdapter.daysOfWeekAdapter.decode(it) },
      RecurringTasksAdapter.startDateAdapter.decode(cursor.getLong(10)!!),
      cursor.getLong(11)
    )
  }

  public fun searchRecurringTasksFts(query: String): Query<RecurringTasks> =
      searchRecurringTasksFts(query) { id, title, description, goalId, duration, priority, points,
      frequency, interval, daysOfWeek, startDate, endDate ->
    RecurringTasks(
      id,
      title,
      description,
      goalId,
      duration,
      priority,
      points,
      frequency,
      interval,
      daysOfWeek,
      startDate,
      endDate
    )
  }

  public fun <T : Any> searchRecurringTasksFallback(query: String, mapper: (
    id: String,
    title: String,
    description: String?,
    goalId: String?,
    duration: Int?,
    priority: TaskPriority,
    points: Int,
    frequency: RecurrenceFrequency,
    interval: Int,
    daysOfWeek: StringList?,
    startDate: Long,
    endDate: Long?,
  ) -> T): Query<T> = SearchRecurringTasksFallbackQuery(query) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      cursor.getLong(4)?.let { RecurringTasksAdapter.durationAdapter.decode(it) },
      RecurringTasksAdapter.priorityAdapter.decode(cursor.getString(5)!!),
      RecurringTasksAdapter.pointsAdapter.decode(cursor.getLong(6)!!),
      RecurringTasksAdapter.frequencyAdapter.decode(cursor.getString(7)!!),
      RecurringTasksAdapter.intervalAdapter.decode(cursor.getLong(8)!!),
      cursor.getString(9)?.let { RecurringTasksAdapter.daysOfWeekAdapter.decode(it) },
      RecurringTasksAdapter.startDateAdapter.decode(cursor.getLong(10)!!),
      cursor.getLong(11)
    )
  }

  public fun searchRecurringTasksFallback(query: String): Query<RecurringTasks> =
      searchRecurringTasksFallback(query) { id, title, description, goalId, duration, priority,
      points, frequency, interval, daysOfWeek, startDate, endDate ->
    RecurringTasks(
      id,
      title,
      description,
      goalId,
      duration,
      priority,
      points,
      frequency,
      interval,
      daysOfWeek,
      startDate,
      endDate
    )
  }

  public fun insertRecurringTask(
    id: String,
    title: String,
    description: String?,
    goalId: String?,
    duration: Int?,
    priority: TaskPriority,
    points: Int,
    frequency: RecurrenceFrequency,
    interval: Int,
    daysOfWeek: StringList?,
    startDate: Long,
    endDate: Long?,
  ) {
    driver.execute(-982_470_518, """
        |INSERT OR REPLACE INTO RecurringTasks(
        |    id,
        |    title,
        |    description,
        |    goalId,
        |    duration,
        |    priority,
        |    points,
        |    frequency,
        |    "interval",
        |    daysOfWeek,
        |    startDate,
        |    endDate
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
        |    ?
        |)
        """.trimMargin(), 12) {
          bindString(0, id)
          bindString(1, title)
          bindString(2, description)
          bindString(3, goalId)
          bindLong(4, duration?.let { RecurringTasksAdapter.durationAdapter.encode(it) })
          bindString(5, RecurringTasksAdapter.priorityAdapter.encode(priority))
          bindLong(6, RecurringTasksAdapter.pointsAdapter.encode(points))
          bindString(7, RecurringTasksAdapter.frequencyAdapter.encode(frequency))
          bindLong(8, RecurringTasksAdapter.intervalAdapter.encode(interval))
          bindString(9, daysOfWeek?.let { RecurringTasksAdapter.daysOfWeekAdapter.encode(it) })
          bindLong(10, RecurringTasksAdapter.startDateAdapter.encode(startDate))
          bindLong(11, endDate)
        }
    notifyQueries(-982_470_518) { emit ->
      emit("RecurringTasks")
    }
  }

  public fun deleteRecurringTaskById(id: String) {
    driver.execute(-700_486_966, """DELETE FROM RecurringTasks WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(-700_486_966) { emit ->
      emit("RecurringTasks")
    }
  }

  public fun deleteRecurringTasksByGoal(goalId: String?) {
    driver.execute(null,
        """DELETE FROM RecurringTasks WHERE goalId ${ if (goalId == null) "IS" else "=" } ?""", 1) {
          bindString(0, goalId)
        }
    notifyQueries(45_866_117) { emit ->
      emit("RecurringTasks")
    }
  }

  public fun deleteAllRecurringTasks() {
    driver.execute(-1_483_652_984, """DELETE FROM RecurringTasks""", 0)
    notifyQueries(-1_483_652_984) { emit ->
      emit("RecurringTasks")
    }
  }

  private inner class GetRecurringTasksByGoalQuery<out T : Any>(
    public val goalId: String?,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("RecurringTasks", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("RecurringTasks", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null, """
    |SELECT RecurringTasks.id, RecurringTasks.title, RecurringTasks.description, RecurringTasks.goalId, RecurringTasks.duration, RecurringTasks.priority, RecurringTasks.points, RecurringTasks.frequency, RecurringTasks."interval", RecurringTasks.daysOfWeek, RecurringTasks.startDate, RecurringTasks.endDate FROM RecurringTasks
    |WHERE goalId ${ if (goalId == null) "IS" else "=" } ?
    |ORDER BY startDate DESC
    """.trimMargin(), mapper, 1) {
      bindString(0, goalId)
    }

    override fun toString(): String = "RecurringTasks.sq:getRecurringTasksByGoal"
  }

  private inner class GetRecurringTaskByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("RecurringTasks", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("RecurringTasks", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_635_423_133,
        """SELECT RecurringTasks.id, RecurringTasks.title, RecurringTasks.description, RecurringTasks.goalId, RecurringTasks.duration, RecurringTasks.priority, RecurringTasks.points, RecurringTasks.frequency, RecurringTasks."interval", RecurringTasks.daysOfWeek, RecurringTasks.startDate, RecurringTasks.endDate FROM RecurringTasks WHERE id = ?""",
        mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "RecurringTasks.sq:getRecurringTaskById"
  }

  private inner class SearchRecurringTasksFtsQuery<out T : Any>(
    public val query: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("RecurringTasks", "RecurringTasksFts", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("RecurringTasks", "RecurringTasksFts", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(222_785_645, """
    |SELECT RecurringTasks.id, RecurringTasks.title, RecurringTasks.description, RecurringTasks.goalId, RecurringTasks.duration, RecurringTasks.priority, RecurringTasks.points, RecurringTasks.frequency, RecurringTasks."interval", RecurringTasks.daysOfWeek, RecurringTasks.startDate, RecurringTasks.endDate FROM RecurringTasks
    |WHERE id IN (
    |    SELECT id FROM RecurringTasksFts WHERE RecurringTasksFts MATCH ?
    |)
    """.trimMargin(), mapper, 1) {
      bindString(0, query)
    }

    override fun toString(): String = "RecurringTasks.sq:searchRecurringTasksFts"
  }

  private inner class SearchRecurringTasksFallbackQuery<out T : Any>(
    public val query: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("RecurringTasks", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("RecurringTasks", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(217_475_290, """
    |SELECT RecurringTasks.id, RecurringTasks.title, RecurringTasks.description, RecurringTasks.goalId, RecurringTasks.duration, RecurringTasks.priority, RecurringTasks.points, RecurringTasks.frequency, RecurringTasks."interval", RecurringTasks.daysOfWeek, RecurringTasks.startDate, RecurringTasks.endDate FROM RecurringTasks
    |WHERE title LIKE '%' || ? || '%' OR description LIKE '%' || ? || '%'
    """.trimMargin(), mapper, 2) {
      bindString(0, query)
      bindString(1, query)
    }

    override fun toString(): String = "RecurringTasks.sq:searchRecurringTasksFallback"
  }
}
