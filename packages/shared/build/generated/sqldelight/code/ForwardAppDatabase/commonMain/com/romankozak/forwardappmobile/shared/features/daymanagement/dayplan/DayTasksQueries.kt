package com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.`data`.database.models.StringList
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.TaskPriority
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.TaskStatus
import kotlin.Any
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.collections.Collection

public class DayTasksQueries(
  driver: SqlDriver,
  private val DayTasksAdapter: DayTasks.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getTasksForDayPlan(dayPlanId: String, mapper: (
    id: String,
    dayPlanId: String,
    title: String,
    description: String?,
    goalId: String?,
    projectId: String?,
    activityRecordId: String?,
    recurringTaskId: String?,
    taskType: String?,
    entityId: String?,
    order: Long,
    priority: TaskPriority,
    status: TaskStatus,
    completed: Boolean,
    scheduledTime: Long?,
    estimatedDurationMinutes: Long?,
    actualDurationMinutes: Long?,
    dueTime: Long?,
    valueImportance: Double,
    valueImpact: Double,
    effort: Double,
    cost: Double,
    risk: Double,
    location: String?,
    tags: StringList?,
    notes: String?,
    createdAt: Long,
    updatedAt: Long?,
    completedAt: Long?,
    nextOccurrenceTime: Long?,
    points: Int,
  ) -> T): Query<T> = GetTasksForDayPlanQuery(dayPlanId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      DayTasksAdapter.orderAdapter.decode(cursor.getLong(10)!!),
      DayTasksAdapter.priorityAdapter.decode(cursor.getString(11)!!),
      DayTasksAdapter.statusAdapter.decode(cursor.getString(12)!!),
      cursor.getBoolean(13)!!,
      cursor.getLong(14),
      cursor.getLong(15),
      cursor.getLong(16),
      cursor.getLong(17),
      DayTasksAdapter.valueImportanceAdapter.decode(cursor.getDouble(18)!!),
      DayTasksAdapter.valueImpactAdapter.decode(cursor.getDouble(19)!!),
      DayTasksAdapter.effortAdapter.decode(cursor.getDouble(20)!!),
      DayTasksAdapter.costAdapter.decode(cursor.getDouble(21)!!),
      DayTasksAdapter.riskAdapter.decode(cursor.getDouble(22)!!),
      cursor.getString(23),
      cursor.getString(24)?.let { DayTasksAdapter.tagsAdapter.decode(it) },
      cursor.getString(25),
      DayTasksAdapter.createdAtAdapter.decode(cursor.getLong(26)!!),
      cursor.getLong(27),
      cursor.getLong(28),
      cursor.getLong(29),
      DayTasksAdapter.pointsAdapter.decode(cursor.getLong(30)!!)
    )
  }

  public fun getTasksForDayPlan(dayPlanId: String): Query<DayTasks> =
      getTasksForDayPlan(dayPlanId) { id, dayPlanId_, title, description, goalId, projectId,
      activityRecordId, recurringTaskId, taskType, entityId, order, priority, status, completed,
      scheduledTime, estimatedDurationMinutes, actualDurationMinutes, dueTime, valueImportance,
      valueImpact, effort, cost, risk, location, tags, notes, createdAt, updatedAt, completedAt,
      nextOccurrenceTime, points ->
    DayTasks(
      id,
      dayPlanId_,
      title,
      description,
      goalId,
      projectId,
      activityRecordId,
      recurringTaskId,
      taskType,
      entityId,
      order,
      priority,
      status,
      completed,
      scheduledTime,
      estimatedDurationMinutes,
      actualDurationMinutes,
      dueTime,
      valueImportance,
      valueImpact,
      effort,
      cost,
      risk,
      location,
      tags,
      notes,
      createdAt,
      updatedAt,
      completedAt,
      nextOccurrenceTime,
      points
    )
  }

  public fun <T : Any> getTaskById(taskId: String, mapper: (
    id: String,
    dayPlanId: String,
    title: String,
    description: String?,
    goalId: String?,
    projectId: String?,
    activityRecordId: String?,
    recurringTaskId: String?,
    taskType: String?,
    entityId: String?,
    order: Long,
    priority: TaskPriority,
    status: TaskStatus,
    completed: Boolean,
    scheduledTime: Long?,
    estimatedDurationMinutes: Long?,
    actualDurationMinutes: Long?,
    dueTime: Long?,
    valueImportance: Double,
    valueImpact: Double,
    effort: Double,
    cost: Double,
    risk: Double,
    location: String?,
    tags: StringList?,
    notes: String?,
    createdAt: Long,
    updatedAt: Long?,
    completedAt: Long?,
    nextOccurrenceTime: Long?,
    points: Int,
  ) -> T): Query<T> = GetTaskByIdQuery(taskId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      DayTasksAdapter.orderAdapter.decode(cursor.getLong(10)!!),
      DayTasksAdapter.priorityAdapter.decode(cursor.getString(11)!!),
      DayTasksAdapter.statusAdapter.decode(cursor.getString(12)!!),
      cursor.getBoolean(13)!!,
      cursor.getLong(14),
      cursor.getLong(15),
      cursor.getLong(16),
      cursor.getLong(17),
      DayTasksAdapter.valueImportanceAdapter.decode(cursor.getDouble(18)!!),
      DayTasksAdapter.valueImpactAdapter.decode(cursor.getDouble(19)!!),
      DayTasksAdapter.effortAdapter.decode(cursor.getDouble(20)!!),
      DayTasksAdapter.costAdapter.decode(cursor.getDouble(21)!!),
      DayTasksAdapter.riskAdapter.decode(cursor.getDouble(22)!!),
      cursor.getString(23),
      cursor.getString(24)?.let { DayTasksAdapter.tagsAdapter.decode(it) },
      cursor.getString(25),
      DayTasksAdapter.createdAtAdapter.decode(cursor.getLong(26)!!),
      cursor.getLong(27),
      cursor.getLong(28),
      cursor.getLong(29),
      DayTasksAdapter.pointsAdapter.decode(cursor.getLong(30)!!)
    )
  }

  public fun getTaskById(taskId: String): Query<DayTasks> = getTaskById(taskId) { id, dayPlanId,
      title, description, goalId, projectId, activityRecordId, recurringTaskId, taskType, entityId,
      order, priority, status, completed, scheduledTime, estimatedDurationMinutes,
      actualDurationMinutes, dueTime, valueImportance, valueImpact, effort, cost, risk, location,
      tags, notes, createdAt, updatedAt, completedAt, nextOccurrenceTime, points ->
    DayTasks(
      id,
      dayPlanId,
      title,
      description,
      goalId,
      projectId,
      activityRecordId,
      recurringTaskId,
      taskType,
      entityId,
      order,
      priority,
      status,
      completed,
      scheduledTime,
      estimatedDurationMinutes,
      actualDurationMinutes,
      dueTime,
      valueImportance,
      valueImpact,
      effort,
      cost,
      risk,
      location,
      tags,
      notes,
      createdAt,
      updatedAt,
      completedAt,
      nextOccurrenceTime,
      points
    )
  }

  public fun <T : Any> selectByRecurringIdAndDayPlanId(
    recurringTaskId: String?,
    dayPlanId: String,
    mapper: (
      id: String,
      dayPlanId: String,
      title: String,
      description: String?,
      goalId: String?,
      projectId: String?,
      activityRecordId: String?,
      recurringTaskId: String?,
      taskType: String?,
      entityId: String?,
      order: Long,
      priority: TaskPriority,
      status: TaskStatus,
      completed: Boolean,
      scheduledTime: Long?,
      estimatedDurationMinutes: Long?,
      actualDurationMinutes: Long?,
      dueTime: Long?,
      valueImportance: Double,
      valueImpact: Double,
      effort: Double,
      cost: Double,
      risk: Double,
      location: String?,
      tags: StringList?,
      notes: String?,
      createdAt: Long,
      updatedAt: Long?,
      completedAt: Long?,
      nextOccurrenceTime: Long?,
      points: Int,
    ) -> T,
  ): Query<T> = SelectByRecurringIdAndDayPlanIdQuery(recurringTaskId, dayPlanId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      DayTasksAdapter.orderAdapter.decode(cursor.getLong(10)!!),
      DayTasksAdapter.priorityAdapter.decode(cursor.getString(11)!!),
      DayTasksAdapter.statusAdapter.decode(cursor.getString(12)!!),
      cursor.getBoolean(13)!!,
      cursor.getLong(14),
      cursor.getLong(15),
      cursor.getLong(16),
      cursor.getLong(17),
      DayTasksAdapter.valueImportanceAdapter.decode(cursor.getDouble(18)!!),
      DayTasksAdapter.valueImpactAdapter.decode(cursor.getDouble(19)!!),
      DayTasksAdapter.effortAdapter.decode(cursor.getDouble(20)!!),
      DayTasksAdapter.costAdapter.decode(cursor.getDouble(21)!!),
      DayTasksAdapter.riskAdapter.decode(cursor.getDouble(22)!!),
      cursor.getString(23),
      cursor.getString(24)?.let { DayTasksAdapter.tagsAdapter.decode(it) },
      cursor.getString(25),
      DayTasksAdapter.createdAtAdapter.decode(cursor.getLong(26)!!),
      cursor.getLong(27),
      cursor.getLong(28),
      cursor.getLong(29),
      DayTasksAdapter.pointsAdapter.decode(cursor.getLong(30)!!)
    )
  }

  public fun selectByRecurringIdAndDayPlanId(recurringTaskId: String?, dayPlanId: String):
      Query<DayTasks> = selectByRecurringIdAndDayPlanId(recurringTaskId, dayPlanId) { id,
      dayPlanId_, title, description, goalId, projectId, activityRecordId, recurringTaskId_,
      taskType, entityId, order, priority, status, completed, scheduledTime,
      estimatedDurationMinutes, actualDurationMinutes, dueTime, valueImportance, valueImpact,
      effort, cost, risk, location, tags, notes, createdAt, updatedAt, completedAt,
      nextOccurrenceTime, points ->
    DayTasks(
      id,
      dayPlanId_,
      title,
      description,
      goalId,
      projectId,
      activityRecordId,
      recurringTaskId_,
      taskType,
      entityId,
      order,
      priority,
      status,
      completed,
      scheduledTime,
      estimatedDurationMinutes,
      actualDurationMinutes,
      dueTime,
      valueImportance,
      valueImpact,
      effort,
      cost,
      risk,
      location,
      tags,
      notes,
      createdAt,
      updatedAt,
      completedAt,
      nextOccurrenceTime,
      points
    )
  }

  public fun <T : Any> selectTemplateForRecurringTask(recurringTaskId: String?, mapper: (
    id: String,
    dayPlanId: String,
    title: String,
    description: String?,
    goalId: String?,
    projectId: String?,
    activityRecordId: String?,
    recurringTaskId: String?,
    taskType: String?,
    entityId: String?,
    order: Long,
    priority: TaskPriority,
    status: TaskStatus,
    completed: Boolean,
    scheduledTime: Long?,
    estimatedDurationMinutes: Long?,
    actualDurationMinutes: Long?,
    dueTime: Long?,
    valueImportance: Double,
    valueImpact: Double,
    effort: Double,
    cost: Double,
    risk: Double,
    location: String?,
    tags: StringList?,
    notes: String?,
    createdAt: Long,
    updatedAt: Long?,
    completedAt: Long?,
    nextOccurrenceTime: Long?,
    points: Int,
  ) -> T): Query<T> = SelectTemplateForRecurringTaskQuery(recurringTaskId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      DayTasksAdapter.orderAdapter.decode(cursor.getLong(10)!!),
      DayTasksAdapter.priorityAdapter.decode(cursor.getString(11)!!),
      DayTasksAdapter.statusAdapter.decode(cursor.getString(12)!!),
      cursor.getBoolean(13)!!,
      cursor.getLong(14),
      cursor.getLong(15),
      cursor.getLong(16),
      cursor.getLong(17),
      DayTasksAdapter.valueImportanceAdapter.decode(cursor.getDouble(18)!!),
      DayTasksAdapter.valueImpactAdapter.decode(cursor.getDouble(19)!!),
      DayTasksAdapter.effortAdapter.decode(cursor.getDouble(20)!!),
      DayTasksAdapter.costAdapter.decode(cursor.getDouble(21)!!),
      DayTasksAdapter.riskAdapter.decode(cursor.getDouble(22)!!),
      cursor.getString(23),
      cursor.getString(24)?.let { DayTasksAdapter.tagsAdapter.decode(it) },
      cursor.getString(25),
      DayTasksAdapter.createdAtAdapter.decode(cursor.getLong(26)!!),
      cursor.getLong(27),
      cursor.getLong(28),
      cursor.getLong(29),
      DayTasksAdapter.pointsAdapter.decode(cursor.getLong(30)!!)
    )
  }

  public fun selectTemplateForRecurringTask(recurringTaskId: String?): Query<DayTasks> =
      selectTemplateForRecurringTask(recurringTaskId) { id, dayPlanId, title, description, goalId,
      projectId, activityRecordId, recurringTaskId_, taskType, entityId, order, priority, status,
      completed, scheduledTime, estimatedDurationMinutes, actualDurationMinutes, dueTime,
      valueImportance, valueImpact, effort, cost, risk, location, tags, notes, createdAt, updatedAt,
      completedAt, nextOccurrenceTime, points ->
    DayTasks(
      id,
      dayPlanId,
      title,
      description,
      goalId,
      projectId,
      activityRecordId,
      recurringTaskId_,
      taskType,
      entityId,
      order,
      priority,
      status,
      completed,
      scheduledTime,
      estimatedDurationMinutes,
      actualDurationMinutes,
      dueTime,
      valueImportance,
      valueImpact,
      effort,
      cost,
      risk,
      location,
      tags,
      notes,
      createdAt,
      updatedAt,
      completedAt,
      nextOccurrenceTime,
      points
    )
  }

  public fun insertDayTask(
    id: String,
    dayPlanId: String,
    title: String,
    description: String?,
    goalId: String?,
    projectId: String?,
    activityRecordId: String?,
    recurringTaskId: String?,
    taskType: String?,
    entityId: String?,
    order: Long,
    priority: TaskPriority,
    status: TaskStatus,
    completed: Boolean,
    scheduledTime: Long?,
    estimatedDurationMinutes: Long?,
    actualDurationMinutes: Long?,
    dueTime: Long?,
    valueImportance: Double,
    valueImpact: Double,
    effort: Double,
    cost: Double,
    risk: Double,
    location: String?,
    tags: StringList?,
    notes: String?,
    createdAt: Long,
    updatedAt: Long?,
    completedAt: Long?,
    nextOccurrenceTime: Long?,
    points: Int,
  ) {
    driver.execute(928_708_938, """
        |INSERT OR REPLACE INTO DayTasks(
        |    id,
        |    dayPlanId,
        |    title,
        |    description,
        |    goalId,
        |    projectId,
        |    activityRecordId,
        |    recurringTaskId,
        |    taskType,
        |    entityId,
        |    "order",
        |    priority,
        |    status,
        |    completed,
        |    scheduledTime,
        |    estimatedDurationMinutes,
        |    actualDurationMinutes,
        |    dueTime,
        |    valueImportance,
        |    valueImpact,
        |    effort,
        |    cost,
        |    risk,
        |    location,
        |    tags,
        |    notes,
        |    createdAt,
        |    updatedAt,
        |    completedAt,
        |    nextOccurrenceTime,
        |    points
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
        """.trimMargin(), 31) {
          bindString(0, id)
          bindString(1, dayPlanId)
          bindString(2, title)
          bindString(3, description)
          bindString(4, goalId)
          bindString(5, projectId)
          bindString(6, activityRecordId)
          bindString(7, recurringTaskId)
          bindString(8, taskType)
          bindString(9, entityId)
          bindLong(10, DayTasksAdapter.orderAdapter.encode(order))
          bindString(11, DayTasksAdapter.priorityAdapter.encode(priority))
          bindString(12, DayTasksAdapter.statusAdapter.encode(status))
          bindBoolean(13, completed)
          bindLong(14, scheduledTime)
          bindLong(15, estimatedDurationMinutes)
          bindLong(16, actualDurationMinutes)
          bindLong(17, dueTime)
          bindDouble(18, DayTasksAdapter.valueImportanceAdapter.encode(valueImportance))
          bindDouble(19, DayTasksAdapter.valueImpactAdapter.encode(valueImpact))
          bindDouble(20, DayTasksAdapter.effortAdapter.encode(effort))
          bindDouble(21, DayTasksAdapter.costAdapter.encode(cost))
          bindDouble(22, DayTasksAdapter.riskAdapter.encode(risk))
          bindString(23, location)
          bindString(24, tags?.let { DayTasksAdapter.tagsAdapter.encode(it) })
          bindString(25, notes)
          bindLong(26, DayTasksAdapter.createdAtAdapter.encode(createdAt))
          bindLong(27, updatedAt)
          bindLong(28, completedAt)
          bindLong(29, nextOccurrenceTime)
          bindLong(30, DayTasksAdapter.pointsAdapter.encode(points))
        }
    notifyQueries(928_708_938) { emit ->
      emit("DayTasks")
    }
  }

  public fun deleteDayTask(taskId: String) {
    driver.execute(1_235_444_248, """DELETE FROM DayTasks WHERE id = ?""", 1) {
          bindString(0, taskId)
        }
    notifyQueries(1_235_444_248) { emit ->
      emit("DayTasks")
    }
  }

  public fun deleteTasksForPlan(dayPlanId: String) {
    driver.execute(-901_616_723, """DELETE FROM DayTasks WHERE dayPlanId = ?""", 1) {
          bindString(0, dayPlanId)
        }
    notifyQueries(-901_616_723) { emit ->
      emit("DayTasks")
    }
  }

  public fun deleteTasksForDayPlanIds(recurringTaskId: String?, dayPlanIds: Collection<String>) {
    val dayPlanIdsIndexes = createArguments(count = dayPlanIds.size)
    driver.execute(null,
        """DELETE FROM DayTasks WHERE recurringTaskId ${ if (recurringTaskId == null) "IS" else "=" } ? AND dayPlanId IN $dayPlanIdsIndexes""",
        1 + dayPlanIds.size) {
          bindString(0, recurringTaskId)
          dayPlanIds.forEachIndexed { index, dayPlanIds_ ->
            bindString(index + 1, dayPlanIds_)
          }
        }
    notifyQueries(11_587_863) { emit ->
      emit("DayTasks")
    }
  }

  public fun detachFromRecurrence(updatedAt: Long?, taskId: String) {
    driver.execute(822_553_515,
        """UPDATE DayTasks SET recurringTaskId = NULL, updatedAt = ? WHERE id = ?""", 2) {
          bindLong(0, updatedAt)
          bindString(1, taskId)
        }
    notifyQueries(822_553_515) { emit ->
      emit("DayTasks")
    }
  }

  public fun updateNextOccurrenceTime(
    nextOccurrenceTime: Long?,
    updatedAt: Long?,
    taskId: String,
  ) {
    driver.execute(639_351_416,
        """UPDATE DayTasks SET nextOccurrenceTime = ?, updatedAt = ? WHERE id = ?""", 3) {
          bindLong(0, nextOccurrenceTime)
          bindLong(1, updatedAt)
          bindString(2, taskId)
        }
    notifyQueries(639_351_416) { emit ->
      emit("DayTasks")
    }
  }

  private inner class GetTasksForDayPlanQuery<out T : Any>(
    public val dayPlanId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("DayTasks", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("DayTasks", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(346_932_754,
        """SELECT DayTasks.id, DayTasks.dayPlanId, DayTasks.title, DayTasks.description, DayTasks.goalId, DayTasks.projectId, DayTasks.activityRecordId, DayTasks.recurringTaskId, DayTasks.taskType, DayTasks.entityId, DayTasks."order", DayTasks.priority, DayTasks.status, DayTasks.completed, DayTasks.scheduledTime, DayTasks.estimatedDurationMinutes, DayTasks.actualDurationMinutes, DayTasks.dueTime, DayTasks.valueImportance, DayTasks.valueImpact, DayTasks.effort, DayTasks.cost, DayTasks.risk, DayTasks.location, DayTasks.tags, DayTasks.notes, DayTasks.createdAt, DayTasks.updatedAt, DayTasks.completedAt, DayTasks.nextOccurrenceTime, DayTasks.points FROM DayTasks WHERE dayPlanId = ? ORDER BY "order" ASC""",
        mapper, 1) {
      bindString(0, dayPlanId)
    }

    override fun toString(): String = "DayTasks.sq:getTasksForDayPlan"
  }

  private inner class GetTaskByIdQuery<out T : Any>(
    public val taskId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("DayTasks", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("DayTasks", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-710_424_593,
        """SELECT DayTasks.id, DayTasks.dayPlanId, DayTasks.title, DayTasks.description, DayTasks.goalId, DayTasks.projectId, DayTasks.activityRecordId, DayTasks.recurringTaskId, DayTasks.taskType, DayTasks.entityId, DayTasks."order", DayTasks.priority, DayTasks.status, DayTasks.completed, DayTasks.scheduledTime, DayTasks.estimatedDurationMinutes, DayTasks.actualDurationMinutes, DayTasks.dueTime, DayTasks.valueImportance, DayTasks.valueImpact, DayTasks.effort, DayTasks.cost, DayTasks.risk, DayTasks.location, DayTasks.tags, DayTasks.notes, DayTasks.createdAt, DayTasks.updatedAt, DayTasks.completedAt, DayTasks.nextOccurrenceTime, DayTasks.points FROM DayTasks WHERE id = ?""",
        mapper, 1) {
      bindString(0, taskId)
    }

    override fun toString(): String = "DayTasks.sq:getTaskById"
  }

  private inner class SelectByRecurringIdAndDayPlanIdQuery<out T : Any>(
    public val recurringTaskId: String?,
    public val dayPlanId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("DayTasks", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("DayTasks", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null,
        """SELECT DayTasks.id, DayTasks.dayPlanId, DayTasks.title, DayTasks.description, DayTasks.goalId, DayTasks.projectId, DayTasks.activityRecordId, DayTasks.recurringTaskId, DayTasks.taskType, DayTasks.entityId, DayTasks."order", DayTasks.priority, DayTasks.status, DayTasks.completed, DayTasks.scheduledTime, DayTasks.estimatedDurationMinutes, DayTasks.actualDurationMinutes, DayTasks.dueTime, DayTasks.valueImportance, DayTasks.valueImpact, DayTasks.effort, DayTasks.cost, DayTasks.risk, DayTasks.location, DayTasks.tags, DayTasks.notes, DayTasks.createdAt, DayTasks.updatedAt, DayTasks.completedAt, DayTasks.nextOccurrenceTime, DayTasks.points FROM DayTasks WHERE recurringTaskId ${ if (recurringTaskId == null) "IS" else "=" } ? AND dayPlanId = ?""",
        mapper, 2) {
      bindString(0, recurringTaskId)
      bindString(1, dayPlanId)
    }

    override fun toString(): String = "DayTasks.sq:selectByRecurringIdAndDayPlanId"
  }

  private inner class SelectTemplateForRecurringTaskQuery<out T : Any>(
    public val recurringTaskId: String?,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("DayTasks", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("DayTasks", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null,
        """SELECT DayTasks.id, DayTasks.dayPlanId, DayTasks.title, DayTasks.description, DayTasks.goalId, DayTasks.projectId, DayTasks.activityRecordId, DayTasks.recurringTaskId, DayTasks.taskType, DayTasks.entityId, DayTasks."order", DayTasks.priority, DayTasks.status, DayTasks.completed, DayTasks.scheduledTime, DayTasks.estimatedDurationMinutes, DayTasks.actualDurationMinutes, DayTasks.dueTime, DayTasks.valueImportance, DayTasks.valueImpact, DayTasks.effort, DayTasks.cost, DayTasks.risk, DayTasks.location, DayTasks.tags, DayTasks.notes, DayTasks.createdAt, DayTasks.updatedAt, DayTasks.completedAt, DayTasks.nextOccurrenceTime, DayTasks.points FROM DayTasks WHERE recurringTaskId ${ if (recurringTaskId == null) "IS" else "=" } ? AND dayPlanId IS NULL""",
        mapper, 1) {
      bindString(0, recurringTaskId)
    }

    override fun toString(): String = "DayTasks.sq:selectTemplateForRecurringTask"
  }
}
