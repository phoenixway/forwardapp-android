package com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Boolean
import kotlin.Long
import kotlin.String
import kotlin.collections.Collection

public class NoteDocumentItemsQueries(
  driver: SqlDriver,
  private val NoteDocumentItemsAdapter: NoteDocumentItems.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getNoteDocumentItems(documentId: String, mapper: (
    id: String,
    listId: String,
    parentId: String?,
    content: String,
    isCompleted: Boolean,
    itemOrder: Long,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = GetNoteDocumentItemsQuery(documentId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3)!!,
      cursor.getBoolean(4)!!,
      NoteDocumentItemsAdapter.itemOrderAdapter.decode(cursor.getLong(5)!!),
      NoteDocumentItemsAdapter.createdAtAdapter.decode(cursor.getLong(6)!!),
      NoteDocumentItemsAdapter.updatedAtAdapter.decode(cursor.getLong(7)!!)
    )
  }

  public fun getNoteDocumentItems(documentId: String): Query<NoteDocumentItems> =
      getNoteDocumentItems(documentId) { id, listId, parentId, content, isCompleted, itemOrder,
      createdAt, updatedAt ->
    NoteDocumentItems(
      id,
      listId,
      parentId,
      content,
      isCompleted,
      itemOrder,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> getAllNoteDocumentItems(mapper: (
    id: String,
    listId: String,
    parentId: String?,
    content: String,
    isCompleted: Boolean,
    itemOrder: Long,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = Query(835_340_168, arrayOf("NoteDocumentItems"), driver,
      "NoteDocumentItems.sq", "getAllNoteDocumentItems", """
  |SELECT NoteDocumentItems.id, NoteDocumentItems.listId, NoteDocumentItems.parentId, NoteDocumentItems.content, NoteDocumentItems.isCompleted, NoteDocumentItems.itemOrder, NoteDocumentItems.createdAt, NoteDocumentItems.updatedAt FROM NoteDocumentItems
  |ORDER BY itemOrder ASC
  """.trimMargin()) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3)!!,
      cursor.getBoolean(4)!!,
      NoteDocumentItemsAdapter.itemOrderAdapter.decode(cursor.getLong(5)!!),
      NoteDocumentItemsAdapter.createdAtAdapter.decode(cursor.getLong(6)!!),
      NoteDocumentItemsAdapter.updatedAtAdapter.decode(cursor.getLong(7)!!)
    )
  }

  public fun getAllNoteDocumentItems(): Query<NoteDocumentItems> = getAllNoteDocumentItems { id,
      listId, parentId, content, isCompleted, itemOrder, createdAt, updatedAt ->
    NoteDocumentItems(
      id,
      listId,
      parentId,
      content,
      isCompleted,
      itemOrder,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> getNoteDocumentItemById(id: String, mapper: (
    id: String,
    listId: String,
    parentId: String?,
    content: String,
    isCompleted: Boolean,
    itemOrder: Long,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = GetNoteDocumentItemByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3)!!,
      cursor.getBoolean(4)!!,
      NoteDocumentItemsAdapter.itemOrderAdapter.decode(cursor.getLong(5)!!),
      NoteDocumentItemsAdapter.createdAtAdapter.decode(cursor.getLong(6)!!),
      NoteDocumentItemsAdapter.updatedAtAdapter.decode(cursor.getLong(7)!!)
    )
  }

  public fun getNoteDocumentItemById(id: String): Query<NoteDocumentItems> =
      getNoteDocumentItemById(id) { id_, listId, parentId, content, isCompleted, itemOrder,
      createdAt, updatedAt ->
    NoteDocumentItems(
      id_,
      listId,
      parentId,
      content,
      isCompleted,
      itemOrder,
      createdAt,
      updatedAt
    )
  }

  public fun insertNoteDocumentItem(
    id: String,
    documentId: String,
    parentId: String?,
    content: String,
    isCompleted: Boolean,
    itemOrder: Long,
    createdAt: Long,
    updatedAt: Long,
  ) {
    driver.execute(275_791_513, """
        |INSERT OR REPLACE INTO NoteDocumentItems(
        |    id,
        |    listId,
        |    parentId,
        |    content,
        |    isCompleted,
        |    itemOrder,
        |    createdAt,
        |    updatedAt
        |) VALUES (
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?
        |)
        """.trimMargin(), 8) {
          bindString(0, id)
          bindString(1, documentId)
          bindString(2, parentId)
          bindString(3, content)
          bindBoolean(4, isCompleted)
          bindLong(5, NoteDocumentItemsAdapter.itemOrderAdapter.encode(itemOrder))
          bindLong(6, NoteDocumentItemsAdapter.createdAtAdapter.encode(createdAt))
          bindLong(7, NoteDocumentItemsAdapter.updatedAtAdapter.encode(updatedAt))
        }
    notifyQueries(275_791_513) { emit ->
      emit("NoteDocumentItems")
    }
  }

  public fun updateNoteDocumentItem(
    documentId: String,
    parentId: String?,
    content: String,
    isCompleted: Boolean,
    itemOrder: Long,
    createdAt: Long,
    updatedAt: Long,
    id: String,
  ) {
    driver.execute(2_112_984_233, """
        |UPDATE NoteDocumentItems SET
        |    listId = ?,
        |    parentId = ?,
        |    content = ?,
        |    isCompleted = ?,
        |    itemOrder = ?,
        |    createdAt = ?,
        |    updatedAt = ?
        |WHERE id = ?
        """.trimMargin(), 8) {
          bindString(0, documentId)
          bindString(1, parentId)
          bindString(2, content)
          bindBoolean(3, isCompleted)
          bindLong(4, NoteDocumentItemsAdapter.itemOrderAdapter.encode(itemOrder))
          bindLong(5, NoteDocumentItemsAdapter.createdAtAdapter.encode(createdAt))
          bindLong(6, NoteDocumentItemsAdapter.updatedAtAdapter.encode(updatedAt))
          bindString(7, id)
        }
    notifyQueries(2_112_984_233) { emit ->
      emit("NoteDocumentItems")
    }
  }

  public fun deleteNoteDocumentItemById(id: String) {
    driver.execute(-2_017_071_107, """DELETE FROM NoteDocumentItems WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(-2_017_071_107) { emit ->
      emit("NoteDocumentItems")
    }
  }

  public fun deleteNoteDocumentItemsByDocument(documentId: String) {
    driver.execute(1_998_450_938, """DELETE FROM NoteDocumentItems WHERE listId = ?""", 1) {
          bindString(0, documentId)
        }
    notifyQueries(1_998_450_938) { emit ->
      emit("NoteDocumentItems")
    }
  }

  public fun deleteNoteDocumentItemsByIds(ids: Collection<String>) {
    val idsIndexes = createArguments(count = ids.size)
    driver.execute(null, """DELETE FROM NoteDocumentItems WHERE id IN $idsIndexes""", ids.size) {
          ids.forEachIndexed { index, ids_ ->
            bindString(index, ids_)
          }
        }
    notifyQueries(-21_641_351) { emit ->
      emit("NoteDocumentItems")
    }
  }

  public fun deleteNoteDocumentItemsByProject(projectId: String) {
    driver.execute(558_642_010, """
        |DELETE FROM NoteDocumentItems
        |WHERE listId IN (
        |    SELECT id FROM NoteDocuments WHERE projectId = ?
        |)
        """.trimMargin(), 1) {
          bindString(0, projectId)
        }
    notifyQueries(558_642_010) { emit ->
      emit("NoteDocumentItems")
    }
  }

  public fun deleteAllNoteDocumentItems() {
    driver.execute(1_977_213_213, """DELETE FROM NoteDocumentItems""", 0)
    notifyQueries(1_977_213_213) { emit ->
      emit("NoteDocumentItems")
    }
  }

  private inner class GetNoteDocumentItemsQuery<out T : Any>(
    public val documentId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("NoteDocumentItems", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("NoteDocumentItems", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_013_841_341, """
    |SELECT NoteDocumentItems.id, NoteDocumentItems.listId, NoteDocumentItems.parentId, NoteDocumentItems.content, NoteDocumentItems.isCompleted, NoteDocumentItems.itemOrder, NoteDocumentItems.createdAt, NoteDocumentItems.updatedAt FROM NoteDocumentItems
    |WHERE listId = ?
    |ORDER BY itemOrder ASC
    """.trimMargin(), mapper, 1) {
      bindString(0, documentId)
    }

    override fun toString(): String = "NoteDocumentItems.sq:getNoteDocumentItems"
  }

  private inner class GetNoteDocumentItemByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("NoteDocumentItems", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("NoteDocumentItems", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_136_023_144, """
    |SELECT NoteDocumentItems.id, NoteDocumentItems.listId, NoteDocumentItems.parentId, NoteDocumentItems.content, NoteDocumentItems.isCompleted, NoteDocumentItems.itemOrder, NoteDocumentItems.createdAt, NoteDocumentItems.updatedAt FROM NoteDocumentItems
    |WHERE id = ?
    """.trimMargin(), mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "NoteDocumentItems.sq:getNoteDocumentItemById"
  }
}
