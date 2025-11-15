package com.romankozak.forwardappmobile.shared.features.recent

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Boolean
import kotlin.Long
import kotlin.String

public class RecentItemsQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> getRecentItems(limit: Long, mapper: (
    id: String,
    type: String,
    lastAccessed: Long,
    displayName: String,
    target: String,
    isPinned: Boolean,
  ) -> T): Query<T> = GetRecentItemsQuery(limit) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getBoolean(5)!!
    )
  }

  public fun getRecentItems(limit: Long): Query<RecentItems> = getRecentItems(limit) { id, type,
      lastAccessed, displayName, target, isPinned ->
    RecentItems(
      id,
      type,
      lastAccessed,
      displayName,
      target,
      isPinned
    )
  }

  public fun <T : Any> getRecentItemById(id: String, mapper: (
    id: String,
    type: String,
    lastAccessed: Long,
    displayName: String,
    target: String,
    isPinned: Boolean,
  ) -> T): Query<T> = GetRecentItemByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getBoolean(5)!!
    )
  }

  public fun getRecentItemById(id: String): Query<RecentItems> = getRecentItemById(id) { id_, type,
      lastAccessed, displayName, target, isPinned ->
    RecentItems(
      id_,
      type,
      lastAccessed,
      displayName,
      target,
      isPinned
    )
  }

  public fun insertRecentItem(
    id: String,
    type: String,
    lastAccessed: Long,
    displayName: String,
    target: String,
    isPinned: Boolean,
  ) {
    driver.execute(-1_958_167_019, """
        |INSERT OR REPLACE INTO RecentItems (
        |    id, type, lastAccessed, displayName, target, isPinned
        |) VALUES (
        |    ?, ?, ?, ?, ?, ?
        |)
        """.trimMargin(), 6) {
          bindString(0, id)
          bindString(1, type)
          bindLong(2, lastAccessed)
          bindString(3, displayName)
          bindString(4, target)
          bindBoolean(5, isPinned)
        }
    notifyQueries(-1_958_167_019) { emit ->
      emit("RecentItems")
    }
  }

  public fun deleteAllRecentItems() {
    driver.execute(34_965_437, """DELETE FROM RecentItems""", 0)
    notifyQueries(34_965_437) { emit ->
      emit("RecentItems")
    }
  }

  private inner class GetRecentItemsQuery<out T : Any>(
    public val limit: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("RecentItems", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("RecentItems", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_206_624_547, """
    |SELECT RecentItems.id, RecentItems.type, RecentItems.lastAccessed, RecentItems.displayName, RecentItems.target, RecentItems.isPinned FROM RecentItems
    |ORDER BY isPinned DESC, lastAccessed DESC
    |LIMIT ?
    """.trimMargin(), mapper, 1) {
      bindLong(0, limit)
    }

    override fun toString(): String = "RecentItems.sq:getRecentItems"
  }

  private inner class GetRecentItemByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("RecentItems", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("RecentItems", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_971_920_568,
        """SELECT RecentItems.id, RecentItems.type, RecentItems.lastAccessed, RecentItems.displayName, RecentItems.target, RecentItems.isPinned FROM RecentItems WHERE id = ?""",
        mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "RecentItems.sq:getRecentItemById"
  }
}
