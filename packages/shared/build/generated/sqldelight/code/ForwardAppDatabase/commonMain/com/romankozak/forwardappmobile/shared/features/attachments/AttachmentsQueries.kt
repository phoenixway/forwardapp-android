package com.romankozak.forwardappmobile.shared.features.attachments

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class AttachmentsQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> getAttachmentsForProject(projectId: String, mapper: (
    id: String,
    attachmentType: String,
    entityId: String,
    ownerProjectId: String?,
    createdAt: Long,
    updatedAt: Long,
    projectId: String,
    attachmentOrder: Long,
  ) -> T): Query<T> = GetAttachmentsForProjectQuery(projectId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getString(6)!!,
      cursor.getLong(7)!!
    )
  }

  public fun getAttachmentsForProject(projectId: String): Query<GetAttachmentsForProject> =
      getAttachmentsForProject(projectId) { id, attachmentType, entityId, ownerProjectId, createdAt,
      updatedAt, projectId_, attachmentOrder ->
    GetAttachmentsForProject(
      id,
      attachmentType,
      entityId,
      ownerProjectId,
      createdAt,
      updatedAt,
      projectId_,
      attachmentOrder
    )
  }

  public fun <T : Any> findAttachmentByEntity(
    attachmentType: String,
    entityId: String,
    mapper: (
      id: String,
      attachmentType: String,
      entityId: String,
      ownerProjectId: String?,
      createdAt: Long,
      updatedAt: Long,
    ) -> T,
  ): Query<T> = FindAttachmentByEntityQuery(attachmentType, entityId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getLong(5)!!
    )
  }

  public fun findAttachmentByEntity(attachmentType: String, entityId: String): Query<Attachments> =
      findAttachmentByEntity(attachmentType, entityId) { id, attachmentType_, entityId_,
      ownerProjectId, createdAt, updatedAt ->
    Attachments(
      id,
      attachmentType_,
      entityId_,
      ownerProjectId,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> getAttachmentById(attachmentId: String, mapper: (
    id: String,
    attachmentType: String,
    entityId: String,
    ownerProjectId: String?,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = GetAttachmentByIdQuery(attachmentId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getLong(5)!!
    )
  }

  public fun getAttachmentById(attachmentId: String): Query<Attachments> =
      getAttachmentById(attachmentId) { id, attachmentType, entityId, ownerProjectId, createdAt,
      updatedAt ->
    Attachments(
      id,
      attachmentType,
      entityId,
      ownerProjectId,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> getAllAttachments(mapper: (
    id: String,
    attachmentType: String,
    entityId: String,
    ownerProjectId: String?,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = Query(-855_060_437, arrayOf("Attachments"), driver, "Attachments.sq",
      "getAllAttachments",
      "SELECT Attachments.id, Attachments.attachmentType, Attachments.entityId, Attachments.ownerProjectId, Attachments.createdAt, Attachments.updatedAt FROM Attachments") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getLong(5)!!
    )
  }

  public fun getAllAttachments(): Query<Attachments> = getAllAttachments { id, attachmentType,
      entityId, ownerProjectId, createdAt, updatedAt ->
    Attachments(
      id,
      attachmentType,
      entityId,
      ownerProjectId,
      createdAt,
      updatedAt
    )
  }

  public fun countLinksForAttachment(attachmentId: String): Query<Long> =
      CountLinksForAttachmentQuery(attachmentId) { cursor ->
    cursor.getLong(0)!!
  }

  public fun insertAttachment(
    id: String,
    attachmentType: String,
    entityId: String,
    ownerProjectId: String?,
    createdAt: Long,
    updatedAt: Long,
  ) {
    driver.execute(83_585_046, """
        |INSERT OR REPLACE INTO Attachments(
        |    id, attachmentType, entityId, ownerProjectId, createdAt, updatedAt
        |) VALUES (
        |    ?, ?, ?, ?, ?, ?
        |)
        """.trimMargin(), 6) {
          bindString(0, id)
          bindString(1, attachmentType)
          bindString(2, entityId)
          bindString(3, ownerProjectId)
          bindLong(4, createdAt)
          bindLong(5, updatedAt)
        }
    notifyQueries(83_585_046) { emit ->
      emit("Attachments")
    }
  }

  public fun deleteAttachment(attachmentId: String) {
    driver.execute(-1_655_200_632, """DELETE FROM Attachments WHERE id = ?""", 1) {
          bindString(0, attachmentId)
        }
    notifyQueries(-1_655_200_632) { emit ->
      emit("Attachments")
    }
  }

  public fun deleteAllLinksForAttachment(attachmentId: String) {
    driver.execute(1_220_579_983,
        """DELETE FROM ProjectAttachmentCrossRef WHERE attachmentId = ?""", 1) {
          bindString(0, attachmentId)
        }
    notifyQueries(1_220_579_983) { emit ->
      emit("ProjectAttachmentCrossRef")
    }
  }

  private inner class GetAttachmentsForProjectQuery<out T : Any>(
    public val projectId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Attachments", "ProjectAttachmentCrossRef", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Attachments", "ProjectAttachmentCrossRef", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_934_326_148, """
    |SELECT
    |    A.id, A.attachmentType, A.entityId, A.ownerProjectId, A.createdAt, A.updatedAt,
    |    PAC.projectId,
    |    PAC.attachmentOrder
    |FROM Attachments AS A
    |JOIN ProjectAttachmentCrossRef AS PAC ON A.id = PAC.attachmentId
    |WHERE PAC.projectId = ?
    |ORDER BY PAC.attachmentOrder ASC, A.createdAt DESC
    """.trimMargin(), mapper, 1) {
      bindString(0, projectId)
    }

    override fun toString(): String = "Attachments.sq:getAttachmentsForProject"
  }

  private inner class FindAttachmentByEntityQuery<out T : Any>(
    public val attachmentType: String,
    public val entityId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Attachments", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Attachments", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(289_822_416,
        """SELECT Attachments.id, Attachments.attachmentType, Attachments.entityId, Attachments.ownerProjectId, Attachments.createdAt, Attachments.updatedAt FROM Attachments WHERE attachmentType = ? AND entityId = ? LIMIT 1""",
        mapper, 2) {
      bindString(0, attachmentType)
      bindString(1, entityId)
    }

    override fun toString(): String = "Attachments.sq:findAttachmentByEntity"
  }

  private inner class GetAttachmentByIdQuery<out T : Any>(
    public val attachmentId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Attachments", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Attachments", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_108_956_273,
        """SELECT Attachments.id, Attachments.attachmentType, Attachments.entityId, Attachments.ownerProjectId, Attachments.createdAt, Attachments.updatedAt FROM Attachments WHERE id = ? LIMIT 1""",
        mapper, 1) {
      bindString(0, attachmentId)
    }

    override fun toString(): String = "Attachments.sq:getAttachmentById"
  }

  private inner class CountLinksForAttachmentQuery<out T : Any>(
    public val attachmentId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ProjectAttachmentCrossRef", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ProjectAttachmentCrossRef", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-891_471_256,
        """SELECT COUNT(*) FROM ProjectAttachmentCrossRef WHERE attachmentId = ?""", mapper, 1) {
      bindString(0, attachmentId)
    }

    override fun toString(): String = "Attachments.sq:countLinksForAttachment"
  }
}
