package com.romankozak.forwardappmobile.shared.features.projects.logs

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class ProjectExecutionLogsQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAllByProjectId(projectId: String, mapper: (
    id: String,
    projectId: String,
    timestamp: Long,
    type: String,
    description: String,
    details: String?,
  ) -> T): Query<T> = SelectAllByProjectIdQuery(projectId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)
    )
  }

  public fun selectAllByProjectId(projectId: String): Query<ProjectExecutionLogs> =
      selectAllByProjectId(projectId) { id, projectId_, timestamp, type, description, details ->
    ProjectExecutionLogs(
      id,
      projectId_,
      timestamp,
      type,
      description,
      details
    )
  }

  public fun insertProjectExecutionLog(
    id: String,
    projectId: String,
    timestamp: Long,
    type: String,
    description: String,
    details: String?,
  ) {
    driver.execute(-1_808_793_217, """
        |INSERT OR REPLACE INTO ProjectExecutionLogs (
        |    id, projectId, timestamp, type, description, details
        |) VALUES (
        |    ?, ?, ?, ?, ?, ?
        |)
        """.trimMargin(), 6) {
          bindString(0, id)
          bindString(1, projectId)
          bindLong(2, timestamp)
          bindString(3, type)
          bindString(4, description)
          bindString(5, details)
        }
    notifyQueries(-1_808_793_217) { emit ->
      emit("ProjectExecutionLogs")
    }
  }

  public fun deleteProjectExecutionLog(id: String) {
    driver.execute(-1_567_906_483, """DELETE FROM ProjectExecutionLogs WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(-1_567_906_483) { emit ->
      emit("ProjectExecutionLogs")
    }
  }

  public fun updateProjectExecutionLog(
    description: String,
    details: String?,
    id: String,
  ) {
    driver.execute(-768_724_625, """
        |UPDATE ProjectExecutionLogs
        |SET description = ?,
        |    details = ?
        |WHERE id = ?
        """.trimMargin(), 3) {
          bindString(0, description)
          bindString(1, details)
          bindString(2, id)
        }
    notifyQueries(-768_724_625) { emit ->
      emit("ProjectExecutionLogs")
    }
  }

  private inner class SelectAllByProjectIdQuery<out T : Any>(
    public val projectId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ProjectExecutionLogs", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ProjectExecutionLogs", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(193_946_917, """
    |SELECT ProjectExecutionLogs.id, ProjectExecutionLogs.projectId, ProjectExecutionLogs.timestamp, ProjectExecutionLogs.type, ProjectExecutionLogs.description, ProjectExecutionLogs.details FROM ProjectExecutionLogs
    |WHERE projectId = ?
    |ORDER BY timestamp DESC
    """.trimMargin(), mapper, 1) {
      bindString(0, projectId)
    }

    override fun toString(): String = "ProjectExecutionLogs.sq:selectAllByProjectId"
  }
}
