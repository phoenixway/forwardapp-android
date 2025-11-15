package com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class LegacyNotesQueries(
  driver: SqlDriver,
  private val LegacyNotesAdapter: LegacyNotes.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getLegacyNotesByProject(projectId: String, mapper: (
    id: String,
    projectId: String,
    title: String,
    content: String?,
    createdAt: Long,
    updatedAt: Long?,
  ) -> T): Query<T> = GetLegacyNotesByProjectQuery(projectId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      LegacyNotesAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      cursor.getLong(5)
    )
  }

  public fun getLegacyNotesByProject(projectId: String): Query<LegacyNotes> =
      getLegacyNotesByProject(projectId) { id, projectId_, title, content, createdAt, updatedAt ->
    LegacyNotes(
      id,
      projectId_,
      title,
      content,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> getAllLegacyNotes(mapper: (
    id: String,
    projectId: String,
    title: String,
    content: String?,
    createdAt: Long,
    updatedAt: Long?,
  ) -> T): Query<T> = Query(11_054_358, arrayOf("LegacyNotes"), driver, "LegacyNotes.sq",
      "getAllLegacyNotes", """
  |SELECT LegacyNotes.id, LegacyNotes.projectId, LegacyNotes.title, LegacyNotes.content, LegacyNotes.createdAt, LegacyNotes.updatedAt FROM LegacyNotes
  |ORDER BY createdAt DESC
  """.trimMargin()) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      LegacyNotesAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      cursor.getLong(5)
    )
  }

  public fun getAllLegacyNotes(): Query<LegacyNotes> = getAllLegacyNotes { id, projectId, title,
      content, createdAt, updatedAt ->
    LegacyNotes(
      id,
      projectId,
      title,
      content,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> getLegacyNoteById(id: String, mapper: (
    id: String,
    projectId: String,
    title: String,
    content: String?,
    createdAt: Long,
    updatedAt: Long?,
  ) -> T): Query<T> = GetLegacyNoteByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      LegacyNotesAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      cursor.getLong(5)
    )
  }

  public fun getLegacyNoteById(id: String): Query<LegacyNotes> = getLegacyNoteById(id) { id_,
      projectId, title, content, createdAt, updatedAt ->
    LegacyNotes(
      id_,
      projectId,
      title,
      content,
      createdAt,
      updatedAt
    )
  }

  public fun insertLegacyNote(
    id: String,
    projectId: String,
    title: String,
    content: String?,
    createdAt: Long,
    updatedAt: Long?,
  ) {
    driver.execute(-1_966_685_749, """
        |INSERT OR REPLACE INTO LegacyNotes(
        |    id, projectId, title, content, createdAt, updatedAt
        |) VALUES (
        |    ?, ?, ?, ?, ?, ?
        |)
        """.trimMargin(), 6) {
          bindString(0, id)
          bindString(1, projectId)
          bindString(2, title)
          bindString(3, content)
          bindLong(4, LegacyNotesAdapter.createdAtAdapter.encode(createdAt))
          bindLong(5, updatedAt)
        }
    notifyQueries(-1_966_685_749) { emit ->
      emit("LegacyNotes")
    }
  }

  public fun updateLegacyNote(
    title: String,
    content: String?,
    updatedAt: Long?,
    id: String,
  ) {
    driver.execute(826_663_899, """
        |UPDATE LegacyNotes SET
        |    title = ?,
        |    content = ?,
        |    updatedAt = ?
        |WHERE id = ?
        """.trimMargin(), 4) {
          bindString(0, title)
          bindString(1, content)
          bindLong(2, updatedAt)
          bindString(3, id)
        }
    notifyQueries(826_663_899) { emit ->
      emit("LegacyNotes")
    }
  }

  public fun deleteLegacyNoteById(id: String) {
    driver.execute(-1_058_052_177, """DELETE FROM LegacyNotes WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(-1_058_052_177) { emit ->
      emit("LegacyNotes")
    }
  }

  public fun deleteLegacyNotesByProject(projectId: String) {
    driver.execute(-447_373_940, """DELETE FROM LegacyNotes WHERE projectId = ?""", 1) {
          bindString(0, projectId)
        }
    notifyQueries(-447_373_940) { emit ->
      emit("LegacyNotes")
    }
  }

  public fun deleteAllLegacyNotes() {
    driver.execute(1_069_130_233, """DELETE FROM LegacyNotes""", 0)
    notifyQueries(1_069_130_233) { emit ->
      emit("LegacyNotes")
    }
  }

  private inner class GetLegacyNotesByProjectQuery<out T : Any>(
    public val projectId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("LegacyNotes", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("LegacyNotes", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-643_490_071, """
    |SELECT LegacyNotes.id, LegacyNotes.projectId, LegacyNotes.title, LegacyNotes.content, LegacyNotes.createdAt, LegacyNotes.updatedAt FROM LegacyNotes
    |WHERE projectId = ?
    |ORDER BY createdAt DESC
    """.trimMargin(), mapper, 1) {
      bindString(0, projectId)
    }

    override fun toString(): String = "LegacyNotes.sq:getLegacyNotesByProject"
  }

  private inner class GetLegacyNoteByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("LegacyNotes", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("LegacyNotes", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-2_116_128_052,
        """SELECT LegacyNotes.id, LegacyNotes.projectId, LegacyNotes.title, LegacyNotes.content, LegacyNotes.createdAt, LegacyNotes.updatedAt FROM LegacyNotes WHERE id = ?""",
        mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "LegacyNotes.sq:getLegacyNoteById"
  }
}
