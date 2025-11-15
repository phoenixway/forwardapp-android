package com.romankozak.forwardappmobile.shared.features.attachments.types.checklists

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.String

public class ChecklistsQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> getChecklistsForProject(projectId: String, mapper: (
    id: String,
    projectId: String,
    name: String,
  ) -> T): Query<T> = GetChecklistsForProjectQuery(projectId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!
    )
  }

  public fun getChecklistsForProject(projectId: String): Query<Checklists> =
      getChecklistsForProject(projectId) { id, projectId_, name ->
    Checklists(
      id,
      projectId_,
      name
    )
  }

  public fun <T : Any> getAllChecklists(mapper: (
    id: String,
    projectId: String,
    name: String,
  ) -> T): Query<T> = Query(-192_170_189, arrayOf("Checklists"), driver, "Checklists.sq",
      "getAllChecklists",
      "SELECT Checklists.id, Checklists.projectId, Checklists.name FROM Checklists") { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!
    )
  }

  public fun getAllChecklists(): Query<Checklists> = getAllChecklists { id, projectId, name ->
    Checklists(
      id,
      projectId,
      name
    )
  }

  public fun <T : Any> getChecklistById(id: String, mapper: (
    id: String,
    projectId: String,
    name: String,
  ) -> T): Query<T> = GetChecklistByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!
    )
  }

  public fun getChecklistById(id: String): Query<Checklists> = getChecklistById(id) { id_,
      projectId, name ->
    Checklists(
      id_,
      projectId,
      name
    )
  }

  public fun insertChecklist(
    id: String,
    projectId: String,
    name: String,
  ) {
    driver.execute(1_852_133_554,
        """INSERT OR REPLACE INTO Checklists(id, projectId, name) VALUES (?, ?, ?)""", 3) {
          bindString(0, id)
          bindString(1, projectId)
          bindString(2, name)
        }
    notifyQueries(1_852_133_554) { emit ->
      emit("Checklists")
    }
  }

  public fun updateChecklist(
    projectId: String,
    name: String,
    id: String,
  ) {
    driver.execute(695_315_618, """UPDATE Checklists SET projectId = ?, name = ? WHERE id = ?""", 3)
        {
          bindString(0, projectId)
          bindString(1, name)
          bindString(2, id)
        }
    notifyQueries(695_315_618) { emit ->
      emit("Checklists")
    }
  }

  public fun deleteChecklistById(id: String) {
    driver.execute(2_059_898_354, """DELETE FROM Checklists WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(2_059_898_354) { emit ->
      emit("Checklists")
    }
  }

  public fun deleteChecklistsByProjectId(projectId: String) {
    driver.execute(1_050_650_378, """DELETE FROM Checklists WHERE projectId = ?""", 1) {
          bindString(0, projectId)
        }
    notifyQueries(1_050_650_378) { emit ->
      emit("Checklists")
    }
  }

  public fun deleteAllChecklists() {
    driver.execute(1_715_082_088, """DELETE FROM Checklists""", 0)
    notifyQueries(1_715_082_088) { emit ->
      emit("Checklists")
    }
  }

  private inner class GetChecklistsForProjectQuery<out T : Any>(
    public val projectId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Checklists", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Checklists", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-637_382_984,
        """SELECT Checklists.id, Checklists.projectId, Checklists.name FROM Checklists WHERE projectId = ? ORDER BY name COLLATE NOCASE ASC""",
        mapper, 1) {
      bindString(0, projectId)
    }

    override fun toString(): String = "Checklists.sq:getChecklistsForProject"
  }

  private inner class GetChecklistByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Checklists", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Checklists", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(152_646_077,
        """SELECT Checklists.id, Checklists.projectId, Checklists.name FROM Checklists WHERE id = ? LIMIT 1""",
        mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "Checklists.sq:getChecklistById"
  }
}
