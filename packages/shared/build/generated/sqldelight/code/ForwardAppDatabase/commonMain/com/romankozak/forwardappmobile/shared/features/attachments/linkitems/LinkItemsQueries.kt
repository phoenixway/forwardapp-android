package com.romankozak.forwardappmobile.shared.features.attachments.linkitems

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.`data`.models.RelatedLink
import com.romankozak.forwardappmobile.shared.database.ListItems
import kotlin.Any
import kotlin.Long
import kotlin.String

public class LinkItemsQueries(
  driver: SqlDriver,
  private val LinkItemsAdapter: LinkItems.Adapter,
  private val ListItemsAdapter: ListItems.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getAllLinkItems(mapper: (
    id: String,
    linkData: RelatedLink,
    createdAt: Long,
  ) -> T): Query<T> = Query(1_208_049_427, arrayOf("LinkItems"), driver, "LinkItems.sq",
      "getAllLinkItems", """
  |SELECT LinkItems.id, LinkItems.linkData, LinkItems.createdAt FROM LinkItems
  |ORDER BY createdAt DESC
  """.trimMargin()) { cursor ->
    mapper(
      cursor.getString(0)!!,
      LinkItemsAdapter.linkDataAdapter.decode(cursor.getString(1)!!),
      LinkItemsAdapter.createdAtAdapter.decode(cursor.getLong(2)!!)
    )
  }

  public fun getAllLinkItems(): Query<LinkItems> = getAllLinkItems { id, linkData, createdAt ->
    LinkItems(
      id,
      linkData,
      createdAt
    )
  }

  public fun <T : Any> getLinkItemById(id: String, mapper: (
    id: String,
    linkData: RelatedLink,
    createdAt: Long,
  ) -> T): Query<T> = GetLinkItemByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      LinkItemsAdapter.linkDataAdapter.decode(cursor.getString(1)!!),
      LinkItemsAdapter.createdAtAdapter.decode(cursor.getLong(2)!!)
    )
  }

  public fun getLinkItemById(id: String): Query<LinkItems> = getLinkItemById(id) { id_, linkData,
      createdAt ->
    LinkItems(
      id_,
      linkData,
      createdAt
    )
  }

  public fun <T : Any> searchLinkItems(query: String, mapper: (
    id: String,
    linkData: RelatedLink,
    createdAt: Long,
    projectId: String,
    projectName: String,
    listItemId: String,
    projectPath: String?,
  ) -> T): Query<T> = SearchLinkItemsQuery(query) { cursor ->
    mapper(
      cursor.getString(0)!!,
      LinkItemsAdapter.linkDataAdapter.decode(cursor.getString(1)!!),
      LinkItemsAdapter.createdAtAdapter.decode(cursor.getLong(2)!!),
      ListItemsAdapter.projectIdAdapter.decode(cursor.getString(3)!!),
      cursor.getString(4)!!,
      ListItemsAdapter.idAdapter.decode(cursor.getString(5)!!),
      cursor.getString(6)
    )
  }

  public fun searchLinkItems(query: String): Query<SearchLinkItems> = searchLinkItems(query) { id,
      linkData, createdAt, projectId, projectName, listItemId, projectPath ->
    SearchLinkItems(
      id,
      linkData,
      createdAt,
      projectId,
      projectName,
      listItemId,
      projectPath
    )
  }

  public fun insertLinkItem(
    id: String,
    linkData: RelatedLink,
    createdAt: Long,
  ) {
    driver.execute(1_135_785_902, """
        |INSERT OR REPLACE INTO LinkItems(
        |    id,
        |    linkData,
        |    createdAt
        |) VALUES (
        |    ?,
        |    ?,
        |    ?
        |)
        """.trimMargin(), 3) {
          bindString(0, id)
          bindString(1, LinkItemsAdapter.linkDataAdapter.encode(linkData))
          bindLong(2, LinkItemsAdapter.createdAtAdapter.encode(createdAt))
        }
    notifyQueries(1_135_785_902) { emit ->
      emit("LinkItems")
    }
  }

  public fun deleteLinkItemById(id: String) {
    driver.execute(695_330_962, """
        |DELETE FROM LinkItems
        |WHERE id = ?
        """.trimMargin(), 1) {
          bindString(0, id)
        }
    notifyQueries(695_330_962) { emit ->
      emit("LinkItems")
    }
  }

  public fun deleteAllLinkItems() {
    driver.execute(2_109_342_776, """DELETE FROM LinkItems""", 0)
    notifyQueries(2_109_342_776) { emit ->
      emit("LinkItems")
    }
  }

  private inner class GetLinkItemByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("LinkItems", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("LinkItems", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-205_962_387, """
    |SELECT LinkItems.id, LinkItems.linkData, LinkItems.createdAt FROM LinkItems
    |WHERE id = ?
    """.trimMargin(), mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "LinkItems.sq:getLinkItemById"
  }

  private inner class SearchLinkItemsQuery<out T : Any>(
    public val query: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Projects", "LinkItems", "ListItems", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Projects", "LinkItems", "ListItems", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(912_357_494, """
    |WITH RECURSIVE path_cte(id, name, path) AS (
    |    SELECT id, name, name AS path
    |    FROM Projects
    |    WHERE parentId IS NULL
    |    UNION ALL
    |    SELECT p.id, p.name, path_cte.path || ' / ' || p.name
    |    FROM Projects AS p
    |    JOIN path_cte ON p.parentId = path_cte.id
    |)
    |SELECT
    |    LI.id,
    |    LI.linkData,
    |    LI.createdAt,
    |    ListItems.projectId,
    |    Projects.name AS projectName,
    |    ListItems.id AS listItemId,
    |    path_cte.path AS projectPath
    |FROM LinkItems AS LI
    |JOIN ListItems ON ListItems.entityId = LI.id
    |JOIN Projects ON Projects.id = ListItems.projectId
    |LEFT JOIN path_cte ON path_cte.id = Projects.id
    |WHERE ListItems.itemType = 'LINK_ITEM'
    |  AND LI.linkData LIKE ?
    |ORDER BY LI.createdAt DESC
    """.trimMargin(), mapper, 1) {
      bindString(0, query)
    }

    override fun toString(): String = "LinkItems.sq:searchLinkItems"
  }
}
