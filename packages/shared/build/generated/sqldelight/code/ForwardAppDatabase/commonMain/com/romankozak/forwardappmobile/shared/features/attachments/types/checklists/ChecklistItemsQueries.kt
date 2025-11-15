package com.romankozak.forwardappmobile.shared.features.attachments.types.checklists

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Boolean
import kotlin.Long
import kotlin.String

public class ChecklistItemsQueries(
  driver: SqlDriver,
  private val ChecklistItemsAdapter: ChecklistItems.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getItemsForChecklist(checklistId: String, mapper: (
    id: String,
    checklistId: String,
    content: String,
    isChecked: Boolean,
    itemOrder: Long,
  ) -> T): Query<T> = GetItemsForChecklistQuery(checklistId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getBoolean(3)!!,
      ChecklistItemsAdapter.itemOrderAdapter.decode(cursor.getLong(4)!!)
    )
  }

  public fun getItemsForChecklist(checklistId: String): Query<ChecklistItems> =
      getItemsForChecklist(checklistId) { id, checklistId_, content, isChecked, itemOrder ->
    ChecklistItems(
      id,
      checklistId_,
      content,
      isChecked,
      itemOrder
    )
  }

  public fun <T : Any> getChecklistItemById(id: String, mapper: (
    id: String,
    checklistId: String,
    content: String,
    isChecked: Boolean,
    itemOrder: Long,
  ) -> T): Query<T> = GetChecklistItemByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getBoolean(3)!!,
      ChecklistItemsAdapter.itemOrderAdapter.decode(cursor.getLong(4)!!)
    )
  }

  public fun getChecklistItemById(id: String): Query<ChecklistItems> = getChecklistItemById(id) {
      id_, checklistId, content, isChecked, itemOrder ->
    ChecklistItems(
      id_,
      checklistId,
      content,
      isChecked,
      itemOrder
    )
  }

  public fun <T : Any> getAllChecklistItems(mapper: (
    id: String,
    checklistId: String,
    content: String,
    isChecked: Boolean,
    itemOrder: Long,
  ) -> T): Query<T> = Query(-1_564_008_051, arrayOf("ChecklistItems"), driver, "ChecklistItems.sq",
      "getAllChecklistItems",
      "SELECT ChecklistItems.id, ChecklistItems.checklistId, ChecklistItems.content, ChecklistItems.isChecked, ChecklistItems.itemOrder FROM ChecklistItems") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getBoolean(3)!!,
      ChecklistItemsAdapter.itemOrderAdapter.decode(cursor.getLong(4)!!)
    )
  }

  public fun getAllChecklistItems(): Query<ChecklistItems> = getAllChecklistItems { id, checklistId,
      content, isChecked, itemOrder ->
    ChecklistItems(
      id,
      checklistId,
      content,
      isChecked,
      itemOrder
    )
  }

  public fun insertChecklistItem(
    id: String,
    checklistId: String,
    content: String,
    isChecked: Boolean,
    itemOrder: Long,
  ) {
    driver.execute(-1_476_056_552, """
        |INSERT OR REPLACE INTO ChecklistItems(
        |    id, checklistId, content, isChecked, itemOrder
        |) VALUES (
        |    ?, ?, ?, ?, ?
        |)
        """.trimMargin(), 5) {
          bindString(0, id)
          bindString(1, checklistId)
          bindString(2, content)
          bindBoolean(3, isChecked)
          bindLong(4, ChecklistItemsAdapter.itemOrderAdapter.encode(itemOrder))
        }
    notifyQueries(-1_476_056_552) { emit ->
      emit("ChecklistItems")
    }
  }

  public fun updateChecklistItem(
    checklistId: String,
    content: String,
    isChecked: Boolean,
    itemOrder: Long,
    id: String,
  ) {
    driver.execute(211_947_016, """
        |UPDATE ChecklistItems SET
        |    checklistId = ?,
        |    content = ?,
        |    isChecked = ?,
        |    itemOrder = ?
        |WHERE id = ?
        """.trimMargin(), 5) {
          bindString(0, checklistId)
          bindString(1, content)
          bindBoolean(2, isChecked)
          bindLong(3, ChecklistItemsAdapter.itemOrderAdapter.encode(itemOrder))
          bindString(4, id)
        }
    notifyQueries(211_947_016) { emit ->
      emit("ChecklistItems")
    }
  }

  public fun deleteChecklistItemById(id: String) {
    driver.execute(-4_166_312, """DELETE FROM ChecklistItems WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(-4_166_312) { emit ->
      emit("ChecklistItems")
    }
  }

  public fun deleteChecklistItemsByChecklistId(checklistId: String) {
    driver.execute(-553_078_595, """DELETE FROM ChecklistItems WHERE checklistId = ?""", 1) {
          bindString(0, checklistId)
        }
    notifyQueries(-553_078_595) { emit ->
      emit("ChecklistItems")
    }
  }

  public fun deleteAllChecklistItems() {
    driver.execute(968_772_040, """DELETE FROM ChecklistItems""", 0)
    notifyQueries(968_772_040) { emit ->
      emit("ChecklistItems")
    }
  }

  public fun deleteChecklistItemsByProjectId(projectId: String) {
    driver.execute(-74_596_240, """
        |DELETE FROM ChecklistItems WHERE checklistId IN (
        |    SELECT id FROM Checklists WHERE projectId = ?
        |)
        """.trimMargin(), 1) {
          bindString(0, projectId)
        }
    notifyQueries(-74_596_240) { emit ->
      emit("ChecklistItems")
    }
  }

  private inner class GetItemsForChecklistQuery<out T : Any>(
    public val checklistId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ChecklistItems", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ChecklistItems", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_588_863_345,
        """SELECT ChecklistItems.id, ChecklistItems.checklistId, ChecklistItems.content, ChecklistItems.isChecked, ChecklistItems.itemOrder FROM ChecklistItems WHERE checklistId = ? ORDER BY itemOrder ASC, id ASC""",
        mapper, 1) {
      bindString(0, checklistId)
    }

    override fun toString(): String = "ChecklistItems.sq:getItemsForChecklist"
  }

  private inner class GetChecklistItemByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ChecklistItems", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ChecklistItems", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_758_020_893,
        """SELECT ChecklistItems.id, ChecklistItems.checklistId, ChecklistItems.content, ChecklistItems.isChecked, ChecklistItems.itemOrder FROM ChecklistItems WHERE id = ? LIMIT 1""",
        mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "ChecklistItems.sq:getChecklistItemById"
  }
}
