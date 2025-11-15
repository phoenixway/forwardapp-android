package com.romankozak.forwardappmobile.shared.features.projects.views.inbox

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class InboxRecordsQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> getInboxRecords(projectId: String, mapper: (
    id: String,
    projectId: String,
    text: String,
    createdAt: Long,
    itemOrder: Long,
  ) -> T): Query<T> = GetInboxRecordsQuery(projectId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!
    )
  }

  public fun getInboxRecords(projectId: String): Query<InboxRecords> = getInboxRecords(projectId) {
      id, projectId_, text, createdAt, itemOrder ->
    InboxRecords(
      id,
      projectId_,
      text,
      createdAt,
      itemOrder
    )
  }

  public fun <T : Any> getInboxRecordById(id: String, mapper: (
    id: String,
    projectId: String,
    text: String,
    createdAt: Long,
    itemOrder: Long,
  ) -> T): Query<T> = GetInboxRecordByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!
    )
  }

  public fun getInboxRecordById(id: String): Query<InboxRecords> = getInboxRecordById(id) { id_,
      projectId, text, createdAt, itemOrder ->
    InboxRecords(
      id_,
      projectId,
      text,
      createdAt,
      itemOrder
    )
  }

  public fun insertInboxRecord(
    id: String,
    projectId: String,
    text: String,
    createdAt: Long,
    itemOrder: Long,
  ) {
    driver.execute(166_919_942, """
        |INSERT OR REPLACE INTO InboxRecords(
        |    id, projectId, text, createdAt, itemOrder
        |) VALUES (
        |    ?, ?, ?, ?, ?
        |)
        """.trimMargin(), 5) {
          bindString(0, id)
          bindString(1, projectId)
          bindString(2, text)
          bindLong(3, createdAt)
          bindLong(4, itemOrder)
        }
    notifyQueries(166_919_942) { emit ->
      emit("InboxRecords")
    }
  }

  public fun updateInboxRecord(
    projectId: String,
    text: String,
    createdAt: Long,
    itemOrder: Long,
    id: String,
  ) {
    driver.execute(861_413_110, """
        |UPDATE InboxRecords SET
        |    projectId = ?,
        |    text = ?,
        |    createdAt = ?,
        |    itemOrder = ?
        |WHERE id = ?
        """.trimMargin(), 5) {
          bindString(0, projectId)
          bindString(1, text)
          bindLong(2, createdAt)
          bindLong(3, itemOrder)
          bindString(4, id)
        }
    notifyQueries(861_413_110) { emit ->
      emit("InboxRecords")
    }
  }

  public fun deleteInboxRecordById(id: String) {
    driver.execute(826_382_022, """DELETE FROM InboxRecords WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(826_382_022) { emit ->
      emit("InboxRecords")
    }
  }

  public fun deleteAllInboxRecords() {
    driver.execute(2_063_208_698, """DELETE FROM InboxRecords""", 0)
    notifyQueries(2_063_208_698) { emit ->
      emit("InboxRecords")
    }
  }

  public fun deleteInboxRecordsByProject(projectId: String) {
    driver.execute(1_438_113_635, """DELETE FROM InboxRecords WHERE projectId = ?""", 1) {
          bindString(0, projectId)
        }
    notifyQueries(1_438_113_635) { emit ->
      emit("InboxRecords")
    }
  }

  private inner class GetInboxRecordsQuery<out T : Any>(
    public val projectId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("InboxRecords", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("InboxRecords", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_608_786_342,
        """SELECT InboxRecords.id, InboxRecords.projectId, InboxRecords.text, InboxRecords.createdAt, InboxRecords.itemOrder FROM InboxRecords WHERE projectId = ? ORDER BY itemOrder DESC, createdAt DESC""",
        mapper, 1) {
      bindString(0, projectId)
    }

    override fun toString(): String = "InboxRecords.sq:getInboxRecords"
  }

  private inner class GetInboxRecordByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("InboxRecords", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("InboxRecords", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(184_800_427,
        """SELECT InboxRecords.id, InboxRecords.projectId, InboxRecords.text, InboxRecords.createdAt, InboxRecords.itemOrder FROM InboxRecords WHERE id = ?""",
        mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "InboxRecords.sq:getInboxRecordById"
  }
}
