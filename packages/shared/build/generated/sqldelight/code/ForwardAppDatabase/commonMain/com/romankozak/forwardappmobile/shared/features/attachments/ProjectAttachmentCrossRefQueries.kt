package com.romankozak.forwardappmobile.shared.features.attachments

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class ProjectAttachmentCrossRefQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> getLinksForProject(projectId: String, mapper: (
    projectId: String,
    attachmentId: String,
    attachmentOrder: Long,
  ) -> T): Query<T> = GetLinksForProjectQuery(projectId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2)!!
    )
  }

  public fun getLinksForProject(projectId: String): Query<ProjectAttachmentCrossRef> =
      getLinksForProject(projectId) { projectId_, attachmentId, attachmentOrder ->
    ProjectAttachmentCrossRef(
      projectId_,
      attachmentId,
      attachmentOrder
    )
  }

  public fun <T : Any> getAllProjectAttachmentLinks(mapper: (
    projectId: String,
    attachmentId: String,
    attachmentOrder: Long,
  ) -> T): Query<T> = Query(1_249_704_289, arrayOf("ProjectAttachmentCrossRef"), driver,
      "ProjectAttachmentCrossRef.sq", "getAllProjectAttachmentLinks",
      "SELECT ProjectAttachmentCrossRef.projectId, ProjectAttachmentCrossRef.attachmentId, ProjectAttachmentCrossRef.attachmentOrder FROM ProjectAttachmentCrossRef") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2)!!
    )
  }

  public fun getAllProjectAttachmentLinks(): Query<ProjectAttachmentCrossRef> =
      getAllProjectAttachmentLinks { projectId, attachmentId, attachmentOrder ->
    ProjectAttachmentCrossRef(
      projectId,
      attachmentId,
      attachmentOrder
    )
  }

  public fun insertProjectAttachmentLink(
    projectId: String,
    attachmentId: String,
    attachmentOrder: Long,
  ) {
    driver.execute(1_800_304_068, """
        |INSERT OR IGNORE INTO ProjectAttachmentCrossRef(projectId, attachmentId, attachmentOrder)
        |VALUES (?, ?, ?)
        """.trimMargin(), 3) {
          bindString(0, projectId)
          bindString(1, attachmentId)
          bindLong(2, attachmentOrder)
        }
    notifyQueries(1_800_304_068) { emit ->
      emit("ProjectAttachmentCrossRef")
    }
  }

  public fun deleteProjectAttachmentLink(projectId: String, attachmentId: String) {
    driver.execute(1_364_221_458, """
        |DELETE FROM ProjectAttachmentCrossRef
        |WHERE projectId = ? AND attachmentId = ?
        """.trimMargin(), 2) {
          bindString(0, projectId)
          bindString(1, attachmentId)
        }
    notifyQueries(1_364_221_458) { emit ->
      emit("ProjectAttachmentCrossRef")
    }
  }

  public fun updateAttachmentOrder(
    attachmentOrder: Long,
    projectId: String,
    attachmentId: String,
  ) {
    driver.execute(-1_271_942_935, """
        |UPDATE ProjectAttachmentCrossRef
        |SET attachmentOrder = ?
        |WHERE projectId = ? AND attachmentId = ?
        """.trimMargin(), 3) {
          bindLong(0, attachmentOrder)
          bindString(1, projectId)
          bindString(2, attachmentId)
        }
    notifyQueries(-1_271_942_935) { emit ->
      emit("ProjectAttachmentCrossRef")
    }
  }

  private inner class GetLinksForProjectQuery<out T : Any>(
    public val projectId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ProjectAttachmentCrossRef", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ProjectAttachmentCrossRef", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_257_959_628,
        """SELECT ProjectAttachmentCrossRef.projectId, ProjectAttachmentCrossRef.attachmentId, ProjectAttachmentCrossRef.attachmentOrder FROM ProjectAttachmentCrossRef WHERE projectId = ? ORDER BY attachmentOrder ASC""",
        mapper, 1) {
      bindString(0, projectId)
    }

    override fun toString(): String = "ProjectAttachmentCrossRef.sq:getLinksForProject"
  }
}
