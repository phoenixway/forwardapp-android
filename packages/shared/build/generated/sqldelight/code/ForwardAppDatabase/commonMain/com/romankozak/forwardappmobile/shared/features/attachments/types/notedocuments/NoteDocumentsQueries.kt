package com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class NoteDocumentsQueries(
  driver: SqlDriver,
  private val NoteDocumentsAdapter: NoteDocuments.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getNoteDocumentsForProject(projectId: String, mapper: (
    id: String,
    projectId: String,
    name: String,
    content: String?,
    createdAt: Long,
    updatedAt: Long,
    lastCursorPosition: Long,
  ) -> T): Query<T> = GetNoteDocumentsForProjectQuery(projectId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      NoteDocumentsAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      NoteDocumentsAdapter.updatedAtAdapter.decode(cursor.getLong(5)!!),
      NoteDocumentsAdapter.lastCursorPositionAdapter.decode(cursor.getLong(6)!!)
    )
  }

  public fun getNoteDocumentsForProject(projectId: String): Query<NoteDocuments> =
      getNoteDocumentsForProject(projectId) { id, projectId_, name, content, createdAt, updatedAt,
      lastCursorPosition ->
    NoteDocuments(
      id,
      projectId_,
      name,
      content,
      createdAt,
      updatedAt,
      lastCursorPosition
    )
  }

  public fun <T : Any> getAllNoteDocuments(mapper: (
    id: String,
    projectId: String,
    name: String,
    content: String?,
    createdAt: Long,
    updatedAt: Long,
    lastCursorPosition: Long,
  ) -> T): Query<T> = Query(1_146_815_784, arrayOf("NoteDocuments"), driver, "NoteDocuments.sq",
      "getAllNoteDocuments", """
  |SELECT NoteDocuments.id, NoteDocuments.projectId, NoteDocuments.name, NoteDocuments.content, NoteDocuments.createdAt, NoteDocuments.updatedAt, NoteDocuments.lastCursorPosition FROM NoteDocuments
  |ORDER BY updatedAt DESC
  """.trimMargin()) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      NoteDocumentsAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      NoteDocumentsAdapter.updatedAtAdapter.decode(cursor.getLong(5)!!),
      NoteDocumentsAdapter.lastCursorPositionAdapter.decode(cursor.getLong(6)!!)
    )
  }

  public fun getAllNoteDocuments(): Query<NoteDocuments> = getAllNoteDocuments { id, projectId,
      name, content, createdAt, updatedAt, lastCursorPosition ->
    NoteDocuments(
      id,
      projectId,
      name,
      content,
      createdAt,
      updatedAt,
      lastCursorPosition
    )
  }

  public fun <T : Any> getNoteDocumentById(id: String, mapper: (
    id: String,
    projectId: String,
    name: String,
    content: String?,
    createdAt: Long,
    updatedAt: Long,
    lastCursorPosition: Long,
  ) -> T): Query<T> = GetNoteDocumentByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      NoteDocumentsAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      NoteDocumentsAdapter.updatedAtAdapter.decode(cursor.getLong(5)!!),
      NoteDocumentsAdapter.lastCursorPositionAdapter.decode(cursor.getLong(6)!!)
    )
  }

  public fun getNoteDocumentById(id: String): Query<NoteDocuments> = getNoteDocumentById(id) { id_,
      projectId, name, content, createdAt, updatedAt, lastCursorPosition ->
    NoteDocuments(
      id_,
      projectId,
      name,
      content,
      createdAt,
      updatedAt,
      lastCursorPosition
    )
  }

  public fun insertNoteDocument(
    id: String,
    projectId: String,
    name: String,
    content: String?,
    createdAt: Long,
    updatedAt: Long,
    lastCursorPosition: Long,
  ) {
    driver.execute(741_843_961, """
        |INSERT OR REPLACE INTO NoteDocuments(
        |    id,
        |    projectId,
        |    name,
        |    content,
        |    createdAt,
        |    updatedAt,
        |    lastCursorPosition
        |) VALUES (
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?
        |)
        """.trimMargin(), 7) {
          bindString(0, id)
          bindString(1, projectId)
          bindString(2, name)
          bindString(3, content)
          bindLong(4, NoteDocumentsAdapter.createdAtAdapter.encode(createdAt))
          bindLong(5, NoteDocumentsAdapter.updatedAtAdapter.encode(updatedAt))
          bindLong(6, NoteDocumentsAdapter.lastCursorPositionAdapter.encode(lastCursorPosition))
        }
    notifyQueries(741_843_961) { emit ->
      emit("NoteDocuments")
    }
  }

  public fun updateNoteDocument(
    name: String,
    content: String?,
    updatedAt: Long,
    lastCursorPosition: Long,
    id: String,
  ) {
    driver.execute(796_295_689, """
        |UPDATE NoteDocuments SET
        |    name = ?,
        |    content = ?,
        |    updatedAt = ?,
        |    lastCursorPosition = ?
        |WHERE id = ?
        """.trimMargin(), 5) {
          bindString(0, name)
          bindString(1, content)
          bindLong(2, NoteDocumentsAdapter.updatedAtAdapter.encode(updatedAt))
          bindLong(3, NoteDocumentsAdapter.lastCursorPositionAdapter.encode(lastCursorPosition))
          bindString(4, id)
        }
    notifyQueries(796_295_689) { emit ->
      emit("NoteDocuments")
    }
  }

  public fun deleteNoteDocumentById(id: String) {
    driver.execute(-1_488_425_379, """
        |DELETE FROM NoteDocuments
        |WHERE id = ?
        """.trimMargin(), 1) {
          bindString(0, id)
        }
    notifyQueries(-1_488_425_379) { emit ->
      emit("NoteDocuments")
    }
  }

  public fun deleteNoteDocumentsByProject(projectId: String) {
    driver.execute(1_053_714_874, """
        |DELETE FROM NoteDocuments
        |WHERE projectId = ?
        """.trimMargin(), 1) {
          bindString(0, projectId)
        }
    notifyQueries(1_053_714_874) { emit ->
      emit("NoteDocuments")
    }
  }

  public fun deleteAllNoteDocuments() {
    driver.execute(-722_018_173, """DELETE FROM NoteDocuments""", 0)
    notifyQueries(-722_018_173) { emit ->
      emit("NoteDocuments")
    }
  }

  private inner class GetNoteDocumentsForProjectQuery<out T : Any>(
    public val projectId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("NoteDocuments", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("NoteDocuments", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_119_938_003, """
    |SELECT NoteDocuments.id, NoteDocuments.projectId, NoteDocuments.name, NoteDocuments.content, NoteDocuments.createdAt, NoteDocuments.updatedAt, NoteDocuments.lastCursorPosition FROM NoteDocuments
    |WHERE projectId = ?
    |ORDER BY updatedAt DESC
    """.trimMargin(), mapper, 1) {
      bindString(0, projectId)
    }

    override fun toString(): String = "NoteDocuments.sq:getNoteDocumentsForProject"
  }

  private inner class GetNoteDocumentByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("NoteDocuments", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("NoteDocuments", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(380_408_578, """
    |SELECT NoteDocuments.id, NoteDocuments.projectId, NoteDocuments.name, NoteDocuments.content, NoteDocuments.createdAt, NoteDocuments.updatedAt, NoteDocuments.lastCursorPosition FROM NoteDocuments
    |WHERE id = ?
    """.trimMargin(), mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "NoteDocuments.sq:getNoteDocumentById"
  }
}
