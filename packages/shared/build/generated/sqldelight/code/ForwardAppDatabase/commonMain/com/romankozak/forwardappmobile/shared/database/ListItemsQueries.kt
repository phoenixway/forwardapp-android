package com.romankozak.forwardappmobile.shared.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String
import kotlin.collections.Collection

public class ListItemsQueries(
  driver: SqlDriver,
  private val ListItemsAdapter: ListItems.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getItemsForProject(projectId: String, mapper: (
    id: String,
    projectId: String,
    itemOrder: Long,
    entityId: String?,
    itemType: String?,
  ) -> T): Query<T> = GetItemsForProjectQuery(projectId) { cursor ->
    mapper(
      ListItemsAdapter.idAdapter.decode(cursor.getString(0)!!),
      ListItemsAdapter.projectIdAdapter.decode(cursor.getString(1)!!),
      ListItemsAdapter.itemOrderAdapter.decode(cursor.getLong(2)!!),
      cursor.getString(3),
      cursor.getString(4)
    )
  }

  public fun getItemsForProject(projectId: String): Query<ListItems> =
      getItemsForProject(projectId) { id, projectId_, itemOrder, entityId, itemType ->
    ListItems(
      id,
      projectId_,
      itemOrder,
      entityId,
      itemType
    )
  }

  public fun <T : Any> getAll(mapper: (
    id: String,
    projectId: String,
    itemOrder: Long,
    entityId: String?,
    itemType: String?,
  ) -> T): Query<T> = Query(1_824_012_115, arrayOf("ListItems"), driver, "ListItems.sq", "getAll",
      "SELECT ListItems.id, ListItems.projectId, ListItems.itemOrder, ListItems.entityId, ListItems.itemType FROM ListItems") {
      cursor ->
    mapper(
      ListItemsAdapter.idAdapter.decode(cursor.getString(0)!!),
      ListItemsAdapter.projectIdAdapter.decode(cursor.getString(1)!!),
      ListItemsAdapter.itemOrderAdapter.decode(cursor.getLong(2)!!),
      cursor.getString(3),
      cursor.getString(4)
    )
  }

  public fun getAll(): Query<ListItems> = getAll { id, projectId, itemOrder, entityId, itemType ->
    ListItems(
      id,
      projectId,
      itemOrder,
      entityId,
      itemType
    )
  }

  public fun getLinkCount(entityId: String?, projectId: String): Query<Long> =
      GetLinkCountQuery(entityId, projectId) { cursor ->
    cursor.getLong(0)!!
  }

  public fun <T : Any> getItemsForProjectSyncForDebug(projectId: String, mapper: (
    id: String,
    projectId: String,
    itemOrder: Long,
    entityId: String?,
    itemType: String?,
  ) -> T): Query<T> = GetItemsForProjectSyncForDebugQuery(projectId) { cursor ->
    mapper(
      ListItemsAdapter.idAdapter.decode(cursor.getString(0)!!),
      ListItemsAdapter.projectIdAdapter.decode(cursor.getString(1)!!),
      ListItemsAdapter.itemOrderAdapter.decode(cursor.getLong(2)!!),
      cursor.getString(3),
      cursor.getString(4)
    )
  }

  public fun getItemsForProjectSyncForDebug(projectId: String): Query<ListItems> =
      getItemsForProjectSyncForDebug(projectId) { id, projectId_, itemOrder, entityId, itemType ->
    ListItems(
      id,
      projectId_,
      itemOrder,
      entityId,
      itemType
    )
  }

  public fun <T : Any> getGoalIdsForProject(projectId: String, mapper: (entityId: String?) -> T):
      Query<T> = GetGoalIdsForProjectQuery(projectId) { cursor ->
    mapper(
      cursor.getString(0)
    )
  }

  public fun getGoalIdsForProject(projectId: String): Query<GetGoalIdsForProject> =
      getGoalIdsForProject(projectId) { entityId ->
    GetGoalIdsForProject(
      entityId
    )
  }

  public fun <T : Any> getListItemByEntityId(entityId: String?, mapper: (
    id: String,
    projectId: String,
    itemOrder: Long,
    entityId: String?,
    itemType: String?,
  ) -> T): Query<T> = GetListItemByEntityIdQuery(entityId) { cursor ->
    mapper(
      ListItemsAdapter.idAdapter.decode(cursor.getString(0)!!),
      ListItemsAdapter.projectIdAdapter.decode(cursor.getString(1)!!),
      ListItemsAdapter.itemOrderAdapter.decode(cursor.getLong(2)!!),
      cursor.getString(3),
      cursor.getString(4)
    )
  }

  public fun getListItemByEntityId(entityId: String?): Query<ListItems> =
      getListItemByEntityId(entityId) { id, projectId, itemOrder, entityId_, itemType ->
    ListItems(
      id,
      projectId,
      itemOrder,
      entityId_,
      itemType
    )
  }

  public fun findProjectIdForGoal(goalId: String?): Query<String> =
      FindProjectIdForGoalQuery(goalId) { cursor ->
    ListItemsAdapter.projectIdAdapter.decode(cursor.getString(0)!!)
  }

  public fun insertItem(
    id: String,
    projectId: String,
    itemOrder: Long,
    entityId: String?,
    itemType: String?,
  ) {
    driver.execute(-1_902_049_708, """
        |INSERT OR REPLACE INTO ListItems(id, projectId, itemOrder, entityId, itemType)
        |VALUES (?, ?, ?, ?, ?)
        """.trimMargin(), 5) {
          bindString(0, ListItemsAdapter.idAdapter.encode(id))
          bindString(1, ListItemsAdapter.projectIdAdapter.encode(projectId))
          bindLong(2, ListItemsAdapter.itemOrderAdapter.encode(itemOrder))
          bindString(3, entityId)
          bindString(4, itemType)
        }
    notifyQueries(-1_902_049_708) { emit ->
      emit("ListItems")
    }
  }

  public fun updateItem(
    projectId: String,
    itemOrder: Long,
    entityId: String?,
    itemType: String?,
    id: String,
  ) {
    driver.execute(1_130_820_708, """
        |UPDATE ListItems SET
        |    projectId = ?,
        |    itemOrder = ?,
        |    entityId = ?,
        |    itemType = ?
        |WHERE id = ?
        """.trimMargin(), 5) {
          bindString(0, ListItemsAdapter.projectIdAdapter.encode(projectId))
          bindLong(1, ListItemsAdapter.itemOrderAdapter.encode(itemOrder))
          bindString(2, entityId)
          bindString(3, itemType)
          bindString(4, ListItemsAdapter.idAdapter.encode(id))
        }
    notifyQueries(1_130_820_708) { emit ->
      emit("ListItems")
    }
  }

  public fun deleteItemsByIds(itemIds: Collection<String>) {
    val itemIdsIndexes = createArguments(count = itemIds.size)
    driver.execute(null, """DELETE FROM ListItems WHERE id IN $itemIdsIndexes""", itemIds.size) {
          itemIds.forEachIndexed { index, itemIds_ ->
            bindString(index, ListItemsAdapter.idAdapter.encode(itemIds_))
          }
        }
    notifyQueries(1_933_816_308) { emit ->
      emit("ListItems")
    }
  }

  public fun deleteItemsForProjects(projectIds: Collection<String>) {
    val projectIdsIndexes = createArguments(count = projectIds.size)
    driver.execute(null, """DELETE FROM ListItems WHERE projectId IN $projectIdsIndexes""",
        projectIds.size) {
          projectIds.forEachIndexed { index, projectIds_ ->
            bindString(index, ListItemsAdapter.projectIdAdapter.encode(projectIds_))
          }
        }
    notifyQueries(1_208_412_470) { emit ->
      emit("ListItems")
    }
  }

  public fun deleteLinkByEntityAndProject(entityId: String?, projectId: String) {
    driver.execute(null,
        """DELETE FROM ListItems WHERE entityId ${ if (entityId == null) "IS" else "=" } ? AND projectId = ?""",
        2) {
          bindString(0, entityId)
          bindString(1, ListItemsAdapter.projectIdAdapter.encode(projectId))
        }
    notifyQueries(1_330_125_833) { emit ->
      emit("ListItems")
    }
  }

  public fun updateListItemProjectIds(targetProjectId: String, itemIds: Collection<String>) {
    val itemIdsIndexes = createArguments(count = itemIds.size)
    driver.execute(null, """UPDATE ListItems SET projectId = ? WHERE id IN $itemIdsIndexes""", 1 +
        itemIds.size) {
          bindString(0, ListItemsAdapter.projectIdAdapter.encode(targetProjectId))
          itemIds.forEachIndexed { index, itemIds_ ->
            bindString(index + 1, ListItemsAdapter.idAdapter.encode(itemIds_))
          }
        }
    notifyQueries(549_360_673) { emit ->
      emit("ListItems")
    }
  }

  public fun deleteAll() {
    driver.execute(-1_421_082_098, """DELETE FROM ListItems""", 0)
    notifyQueries(-1_421_082_098) { emit ->
      emit("ListItems")
    }
  }

  public fun deleteItemByEntityId(entityId: String?) {
    driver.execute(null,
        """DELETE FROM ListItems WHERE entityId ${ if (entityId == null) "IS" else "=" } ?""", 1) {
          bindString(0, entityId)
        }
    notifyQueries(-1_533_581_637) { emit ->
      emit("ListItems")
    }
  }

  public fun insertListItem(
    id: String,
    projectId: String,
    itemOrder: Long,
    entityId: String?,
    itemType: String?,
  ) {
    driver.execute(-1_183_439_982, """
        |INSERT OR REPLACE INTO ListItems (
        |    id,
        |    projectId,
        |    itemOrder,
        |    entityId,
        |    itemType
        |) VALUES (
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?
        |)
        """.trimMargin(), 5) {
          bindString(0, ListItemsAdapter.idAdapter.encode(id))
          bindString(1, ListItemsAdapter.projectIdAdapter.encode(projectId))
          bindLong(2, ListItemsAdapter.itemOrderAdapter.encode(itemOrder))
          bindString(3, entityId)
          bindString(4, itemType)
        }
    notifyQueries(-1_183_439_982) { emit ->
      emit("ListItems")
    }
  }

  private inner class GetItemsForProjectQuery<out T : Any>(
    public val projectId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ListItems", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ListItems", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_107_498_846,
        """SELECT ListItems.id, ListItems.projectId, ListItems.itemOrder, ListItems.entityId, ListItems.itemType FROM ListItems WHERE projectId = ? ORDER BY itemOrder ASC, id ASC""",
        mapper, 1) {
      bindString(0, ListItemsAdapter.projectIdAdapter.encode(projectId))
    }

    override fun toString(): String = "ListItems.sq:getItemsForProject"
  }

  private inner class GetLinkCountQuery<out T : Any>(
    public val entityId: String?,
    public val projectId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ListItems", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ListItems", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null,
        """SELECT COUNT(*) FROM ListItems WHERE entityId ${ if (entityId == null) "IS" else "=" } ? AND projectId = ?""",
        mapper, 2) {
      bindString(0, entityId)
      bindString(1, ListItemsAdapter.projectIdAdapter.encode(projectId))
    }

    override fun toString(): String = "ListItems.sq:getLinkCount"
  }

  private inner class GetItemsForProjectSyncForDebugQuery<out T : Any>(
    public val projectId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ListItems", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ListItems", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_688_355_705,
        """SELECT ListItems.id, ListItems.projectId, ListItems.itemOrder, ListItems.entityId, ListItems.itemType FROM ListItems WHERE projectId = ? ORDER BY itemOrder ASC, id ASC""",
        mapper, 1) {
      bindString(0, ListItemsAdapter.projectIdAdapter.encode(projectId))
    }

    override fun toString(): String = "ListItems.sq:getItemsForProjectSyncForDebug"
  }

  private inner class GetGoalIdsForProjectQuery<out T : Any>(
    public val projectId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ListItems", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ListItems", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_764_662_073,
        """SELECT entityId FROM ListItems WHERE projectId = ? AND itemType = 'GOAL'""", mapper, 1) {
      bindString(0, ListItemsAdapter.projectIdAdapter.encode(projectId))
    }

    override fun toString(): String = "ListItems.sq:getGoalIdsForProject"
  }

  private inner class GetListItemByEntityIdQuery<out T : Any>(
    public val entityId: String?,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ListItems", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ListItems", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null,
        """SELECT ListItems.id, ListItems.projectId, ListItems.itemOrder, ListItems.entityId, ListItems.itemType FROM ListItems WHERE entityId ${ if (entityId == null) "IS" else "=" } ? LIMIT 1""",
        mapper, 1) {
      bindString(0, entityId)
    }

    override fun toString(): String = "ListItems.sq:getListItemByEntityId"
  }

  private inner class FindProjectIdForGoalQuery<out T : Any>(
    public val goalId: String?,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ListItems", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ListItems", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null,
        """SELECT projectId FROM ListItems WHERE entityId ${ if (goalId == null) "IS" else "=" } ? LIMIT 1""",
        mapper, 1) {
      bindString(0, goalId)
    }

    override fun toString(): String = "ListItems.sq:findProjectIdForGoal"
  }
}
