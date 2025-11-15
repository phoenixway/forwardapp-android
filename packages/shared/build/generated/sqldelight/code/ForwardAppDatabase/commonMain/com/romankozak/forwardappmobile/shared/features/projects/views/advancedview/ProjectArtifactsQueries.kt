package com.romankozak.forwardappmobile.shared.features.projects.views.advancedview

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class ProjectArtifactsQueries(
  driver: SqlDriver,
  private val ProjectArtifactsAdapter: ProjectArtifacts.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getArtifactForProject(projectId: String, mapper: (
    id: String,
    projectId: String,
    content: String,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = GetArtifactForProjectQuery(projectId) { cursor ->
    mapper(
      ProjectArtifactsAdapter.idAdapter.decode(cursor.getString(0)!!),
      ProjectArtifactsAdapter.projectIdAdapter.decode(cursor.getString(1)!!),
      ProjectArtifactsAdapter.contentAdapter.decode(cursor.getString(2)!!),
      ProjectArtifactsAdapter.createdAtAdapter.decode(cursor.getLong(3)!!),
      ProjectArtifactsAdapter.updatedAtAdapter.decode(cursor.getLong(4)!!)
    )
  }

  public fun getArtifactForProject(projectId: String): Query<ProjectArtifacts> =
      getArtifactForProject(projectId) { id, projectId_, content, createdAt, updatedAt ->
    ProjectArtifacts(
      id,
      projectId_,
      content,
      createdAt,
      updatedAt
    )
  }

  public fun insertProjectArtifact(
    id: String,
    projectId: String,
    content: String,
    createdAt: Long,
    updatedAt: Long,
  ) {
    driver.execute(-948_723_421, """
        |INSERT OR REPLACE INTO ProjectArtifacts (
        |    id, projectId, content, createdAt, updatedAt
        |) VALUES (
        |    ?, ?, ?, ?, ?
        |)
        """.trimMargin(), 5) {
          bindString(0, ProjectArtifactsAdapter.idAdapter.encode(id))
          bindString(1, ProjectArtifactsAdapter.projectIdAdapter.encode(projectId))
          bindString(2, ProjectArtifactsAdapter.contentAdapter.encode(content))
          bindLong(3, ProjectArtifactsAdapter.createdAtAdapter.encode(createdAt))
          bindLong(4, ProjectArtifactsAdapter.updatedAtAdapter.encode(updatedAt))
        }
    notifyQueries(-948_723_421) { emit ->
      emit("ProjectArtifacts")
    }
  }

  public fun deleteProjectArtifact(id: String) {
    driver.execute(770_006_001, """DELETE FROM ProjectArtifacts WHERE id = ?""", 1) {
          bindString(0, ProjectArtifactsAdapter.idAdapter.encode(id))
        }
    notifyQueries(770_006_001) { emit ->
      emit("ProjectArtifacts")
    }
  }

  private inner class GetArtifactForProjectQuery<out T : Any>(
    public val projectId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ProjectArtifacts", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ProjectArtifacts", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_578_450_807,
        """SELECT ProjectArtifacts.id, ProjectArtifacts.projectId, ProjectArtifacts.content, ProjectArtifacts.createdAt, ProjectArtifacts.updatedAt FROM ProjectArtifacts WHERE projectId = ? LIMIT 1""",
        mapper, 1) {
      bindString(0, ProjectArtifactsAdapter.projectIdAdapter.encode(projectId))
    }

    override fun toString(): String = "ProjectArtifacts.sq:getArtifactForProject"
  }
}
