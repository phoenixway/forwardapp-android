package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class NotesQueries(
  driver: SqlDriver,
  private val NotesAdapter: Notes.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getNotesForProject(projectId: String, mapper: (
    id: String,
    projectId: String,
    title: String,
    content: String?,
    createdAt: Long,
    updatedAt: Long?,
  ) -> T): Query<T> = GetNotesForProjectQuery(projectId) { cursor ->
    mapper(
      NotesAdapter.idAdapter.decode(cursor.getString(0)!!),
      NotesAdapter.projectIdAdapter.decode(cursor.getString(1)!!),
      NotesAdapter.titleAdapter.decode(cursor.getString(2)!!),
      cursor.getString(3),
      NotesAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      cursor.getLong(5)
    )
  }

  public fun getNotesForProject(projectId: String): Query<Notes> = getNotesForProject(projectId) {
      id, projectId_, title, content, createdAt, updatedAt ->
    Notes(
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
  ) -> T): Query<T> = Query(-1_686_232_186, arrayOf("Notes"), driver, "Notes.sq",
      "getAllLegacyNotes",
      "SELECT Notes.id, Notes.projectId, Notes.title, Notes.content, Notes.createdAt, Notes.updatedAt FROM Notes") {
      cursor ->
    mapper(
      NotesAdapter.idAdapter.decode(cursor.getString(0)!!),
      NotesAdapter.projectIdAdapter.decode(cursor.getString(1)!!),
      NotesAdapter.titleAdapter.decode(cursor.getString(2)!!),
      cursor.getString(3),
      NotesAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      cursor.getLong(5)
    )
  }

  public fun getAllLegacyNotes(): Query<Notes> = getAllLegacyNotes { id, projectId, title, content,
      createdAt, updatedAt ->
    Notes(
      id,
      projectId,
      title,
      content,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> getLegacyNoteById(noteId: String, mapper: (
    id: String,
    projectId: String,
    title: String,
    content: String?,
    createdAt: Long,
    updatedAt: Long?,
  ) -> T): Query<T> = GetLegacyNoteByIdQuery(noteId) { cursor ->
    mapper(
      NotesAdapter.idAdapter.decode(cursor.getString(0)!!),
      NotesAdapter.projectIdAdapter.decode(cursor.getString(1)!!),
      NotesAdapter.titleAdapter.decode(cursor.getString(2)!!),
      cursor.getString(3),
      NotesAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      cursor.getLong(5)
    )
  }

  public fun getLegacyNoteById(noteId: String): Query<Notes> = getLegacyNoteById(noteId) { id,
      projectId, title, content, createdAt, updatedAt ->
    Notes(
      id,
      projectId,
      title,
      content,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> searchNotesFts(query: String, mapper: (
    id: String,
    projectId: String,
    title: String,
    content: String?,
    createdAt: Long,
    updatedAt: Long?,
  ) -> T): Query<T> = SearchNotesFtsQuery(query) { cursor ->
    mapper(
      NotesAdapter.idAdapter.decode(cursor.getString(0)!!),
      NotesAdapter.projectIdAdapter.decode(cursor.getString(1)!!),
      NotesAdapter.titleAdapter.decode(cursor.getString(2)!!),
      cursor.getString(3),
      NotesAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      cursor.getLong(5)
    )
  }

  public fun searchNotesFts(query: String): Query<Notes> = searchNotesFts(query) { id, projectId,
      title, content, createdAt, updatedAt ->
    Notes(
      id,
      projectId,
      title,
      content,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> searchNotesFallback(query: String, mapper: (
    id: String,
    projectId: String,
    title: String,
    content: String?,
    createdAt: Long,
    updatedAt: Long?,
  ) -> T): Query<T> = SearchNotesFallbackQuery(query) { cursor ->
    mapper(
      NotesAdapter.idAdapter.decode(cursor.getString(0)!!),
      NotesAdapter.projectIdAdapter.decode(cursor.getString(1)!!),
      NotesAdapter.titleAdapter.decode(cursor.getString(2)!!),
      cursor.getString(3),
      NotesAdapter.createdAtAdapter.decode(cursor.getLong(4)!!),
      cursor.getLong(5)
    )
  }

  public fun searchNotesFallback(query: String): Query<Notes> = searchNotesFallback(query) { id,
      projectId, title, content, createdAt, updatedAt ->
    Notes(
      id,
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
    driver.execute(1_026_604_379, """
        |INSERT INTO Notes(id, projectId, title, content, createdAt, updatedAt)
        |VALUES (?, ?, ?, ?, ?, ?)
        """.trimMargin(), 6) {
          bindString(0, NotesAdapter.idAdapter.encode(id))
          bindString(1, NotesAdapter.projectIdAdapter.encode(projectId))
          bindString(2, NotesAdapter.titleAdapter.encode(title))
          bindString(3, content)
          bindLong(4, NotesAdapter.createdAtAdapter.encode(createdAt))
          bindLong(5, updatedAt)
        }
    notifyQueries(1_026_604_379) { emit ->
      emit("Notes")
    }
  }

  public fun updateLegacyNote(
    title: String,
    content: String?,
    updatedAt: Long?,
    id: String,
  ) {
    driver.execute(-475_013_269, """
        |UPDATE Notes SET
        |    title = ?,
        |    content = ?,
        |    updatedAt = ?
        |WHERE id = ?
        """.trimMargin(), 4) {
          bindString(0, NotesAdapter.titleAdapter.encode(title))
          bindString(1, content)
          bindLong(2, updatedAt)
          bindString(3, NotesAdapter.idAdapter.encode(id))
        }
    notifyQueries(-475_013_269) { emit ->
      emit("Notes")
    }
  }

  public fun deleteLegacyNoteById(noteId: String) {
    driver.execute(-271_508_673, """DELETE FROM Notes WHERE id = ?""", 1) {
          bindString(0, NotesAdapter.idAdapter.encode(noteId))
        }
    notifyQueries(-271_508_673) { emit ->
      emit("Notes")
    }
  }

  public fun deleteAllLegacyNotes() {
    driver.execute(1_855_673_737, """DELETE FROM Notes""", 0)
    notifyQueries(1_855_673_737) { emit ->
      emit("Notes")
    }
  }

  private inner class GetNotesForProjectQuery<out T : Any>(
    public val projectId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Notes", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Notes", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-604_390_878,
        """SELECT Notes.id, Notes.projectId, Notes.title, Notes.content, Notes.createdAt, Notes.updatedAt FROM Notes WHERE projectId = ?""",
        mapper, 1) {
      bindString(0, NotesAdapter.projectIdAdapter.encode(projectId))
    }

    override fun toString(): String = "Notes.sq:getNotesForProject"
  }

  private inner class GetLegacyNoteByIdQuery<out T : Any>(
    public val noteId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Notes", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Notes", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(481_552_700,
        """SELECT Notes.id, Notes.projectId, Notes.title, Notes.content, Notes.createdAt, Notes.updatedAt FROM Notes WHERE id = ?""",
        mapper, 1) {
      bindString(0, NotesAdapter.idAdapter.encode(noteId))
    }

    override fun toString(): String = "Notes.sq:getLegacyNoteById"
  }

  private inner class SearchNotesFtsQuery<out T : Any>(
    public val query: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Notes", "NotesFts", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Notes", "NotesFts", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(2_036_152_531, """
    |SELECT Notes.id, Notes.projectId, Notes.title, Notes.content, Notes.createdAt, Notes.updatedAt FROM Notes
    |WHERE id IN (
    |    SELECT id FROM NotesFts WHERE NotesFts MATCH ?
    |)
    """.trimMargin(), mapper, 1) {
      bindString(0, query)
    }

    override fun toString(): String = "Notes.sq:searchNotesFts"
  }

  private inner class SearchNotesFallbackQuery<out T : Any>(
    public val query: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Notes", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Notes", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-582_225_868, """
    |SELECT Notes.id, Notes.projectId, Notes.title, Notes.content, Notes.createdAt, Notes.updatedAt FROM Notes
    |WHERE title LIKE '%' || ? || '%' OR content LIKE '%' || ? || '%'
    """.trimMargin(), mapper, 2) {
      bindString(0, query)
      bindString(1, query)
    }

    override fun toString(): String = "Notes.sq:searchNotesFallback"
  }
}
