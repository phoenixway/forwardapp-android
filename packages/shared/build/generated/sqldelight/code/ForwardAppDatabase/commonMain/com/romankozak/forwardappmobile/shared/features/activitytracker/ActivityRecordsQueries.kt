package com.romankozak.forwardappmobile.shared.features.activitytracker

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.`data`.database.models.RelatedLinkList
import com.romankozak.forwardappmobile.shared.`data`.database.models.StringList
import kotlin.Any
import kotlin.Boolean
import kotlin.Long
import kotlin.String

public class ActivityRecordsQueries(
  driver: SqlDriver,
  private val ActivityRecordsAdapter: ActivityRecords.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getActivityRecordById(recordId: String, mapper: (
    id: String,
    name: String,
    description: String?,
    createdAt: Long,
    startTime: Long?,
    endTime: Long?,
    totalTimeSpentMinutes: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    isCompleted: Boolean,
    activityType: String,
    parentProjectId: String?,
  ) -> T): Query<T> = GetActivityRecordByIdQuery(recordId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      ActivityRecordsAdapter.createdAtAdapter.decode(cursor.getLong(3)!!),
      cursor.getLong(4),
      cursor.getLong(5),
      cursor.getLong(6),
      cursor.getString(7)?.let { ActivityRecordsAdapter.tagsAdapter.decode(it) },
      cursor.getString(8)?.let { ActivityRecordsAdapter.relatedLinksAdapter.decode(it) },
      cursor.getBoolean(9)!!,
      cursor.getString(10)!!,
      cursor.getString(11)
    )
  }

  public fun getActivityRecordById(recordId: String): Query<ActivityRecords> =
      getActivityRecordById(recordId) { id, name, description, createdAt, startTime, endTime,
      totalTimeSpentMinutes, tags, relatedLinks, isCompleted, activityType, parentProjectId ->
    ActivityRecords(
      id,
      name,
      description,
      createdAt,
      startTime,
      endTime,
      totalTimeSpentMinutes,
      tags,
      relatedLinks,
      isCompleted,
      activityType,
      parentProjectId
    )
  }

  public fun <T : Any> getActivityRecordsOrdered(mapper: (
    id: String,
    name: String,
    description: String?,
    createdAt: Long,
    startTime: Long?,
    endTime: Long?,
    totalTimeSpentMinutes: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    isCompleted: Boolean,
    activityType: String,
    parentProjectId: String?,
  ) -> T): Query<T> = Query(-2_116_518_918, arrayOf("ActivityRecords"), driver,
      "ActivityRecords.sq", "getActivityRecordsOrdered",
      "SELECT ActivityRecords.id, ActivityRecords.name, ActivityRecords.description, ActivityRecords.createdAt, ActivityRecords.startTime, ActivityRecords.endTime, ActivityRecords.totalTimeSpentMinutes, ActivityRecords.tags, ActivityRecords.relatedLinks, ActivityRecords.isCompleted, ActivityRecords.activityType, ActivityRecords.parentProjectId FROM ActivityRecords ORDER BY createdAt DESC") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      ActivityRecordsAdapter.createdAtAdapter.decode(cursor.getLong(3)!!),
      cursor.getLong(4),
      cursor.getLong(5),
      cursor.getLong(6),
      cursor.getString(7)?.let { ActivityRecordsAdapter.tagsAdapter.decode(it) },
      cursor.getString(8)?.let { ActivityRecordsAdapter.relatedLinksAdapter.decode(it) },
      cursor.getBoolean(9)!!,
      cursor.getString(10)!!,
      cursor.getString(11)
    )
  }

  public fun getActivityRecordsOrdered(): Query<ActivityRecords> = getActivityRecordsOrdered { id,
      name, description, createdAt, startTime, endTime, totalTimeSpentMinutes, tags, relatedLinks,
      isCompleted, activityType, parentProjectId ->
    ActivityRecords(
      id,
      name,
      description,
      createdAt,
      startTime,
      endTime,
      totalTimeSpentMinutes,
      tags,
      relatedLinks,
      isCompleted,
      activityType,
      parentProjectId
    )
  }

  public fun <T : Any> getLastOngoingActivity(mapper: (
    id: String,
    name: String,
    description: String?,
    createdAt: Long,
    startTime: Long?,
    endTime: Long?,
    totalTimeSpentMinutes: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    isCompleted: Boolean,
    activityType: String,
    parentProjectId: String?,
  ) -> T): Query<T> = Query(167_486_196, arrayOf("ActivityRecords"), driver, "ActivityRecords.sq",
      "getLastOngoingActivity",
      "SELECT ActivityRecords.id, ActivityRecords.name, ActivityRecords.description, ActivityRecords.createdAt, ActivityRecords.startTime, ActivityRecords.endTime, ActivityRecords.totalTimeSpentMinutes, ActivityRecords.tags, ActivityRecords.relatedLinks, ActivityRecords.isCompleted, ActivityRecords.activityType, ActivityRecords.parentProjectId FROM ActivityRecords WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      ActivityRecordsAdapter.createdAtAdapter.decode(cursor.getLong(3)!!),
      cursor.getLong(4),
      cursor.getLong(5),
      cursor.getLong(6),
      cursor.getString(7)?.let { ActivityRecordsAdapter.tagsAdapter.decode(it) },
      cursor.getString(8)?.let { ActivityRecordsAdapter.relatedLinksAdapter.decode(it) },
      cursor.getBoolean(9)!!,
      cursor.getString(10)!!,
      cursor.getString(11)
    )
  }

  public fun getLastOngoingActivity(): Query<ActivityRecords> = getLastOngoingActivity { id, name,
      description, createdAt, startTime, endTime, totalTimeSpentMinutes, tags, relatedLinks,
      isCompleted, activityType, parentProjectId ->
    ActivityRecords(
      id,
      name,
      description,
      createdAt,
      startTime,
      endTime,
      totalTimeSpentMinutes,
      tags,
      relatedLinks,
      isCompleted,
      activityType,
      parentProjectId
    )
  }

  public fun <T : Any> getLastOngoingActivityForProject(projectId: String?, mapper: (
    id: String,
    name: String,
    description: String?,
    createdAt: Long,
    startTime: Long?,
    endTime: Long?,
    totalTimeSpentMinutes: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    isCompleted: Boolean,
    activityType: String,
    parentProjectId: String?,
  ) -> T): Query<T> = GetLastOngoingActivityForProjectQuery(projectId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      ActivityRecordsAdapter.createdAtAdapter.decode(cursor.getLong(3)!!),
      cursor.getLong(4),
      cursor.getLong(5),
      cursor.getLong(6),
      cursor.getString(7)?.let { ActivityRecordsAdapter.tagsAdapter.decode(it) },
      cursor.getString(8)?.let { ActivityRecordsAdapter.relatedLinksAdapter.decode(it) },
      cursor.getBoolean(9)!!,
      cursor.getString(10)!!,
      cursor.getString(11)
    )
  }

  public fun getLastOngoingActivityForProject(projectId: String?): Query<ActivityRecords> =
      getLastOngoingActivityForProject(projectId) { id, name, description, createdAt, startTime,
      endTime, totalTimeSpentMinutes, tags, relatedLinks, isCompleted, activityType,
      parentProjectId ->
    ActivityRecords(
      id,
      name,
      description,
      createdAt,
      startTime,
      endTime,
      totalTimeSpentMinutes,
      tags,
      relatedLinks,
      isCompleted,
      activityType,
      parentProjectId
    )
  }

  public fun <T : Any> searchActivityRecordsFts(query: String, mapper: (
    id: String,
    name: String,
    description: String?,
    createdAt: Long,
    startTime: Long?,
    endTime: Long?,
    totalTimeSpentMinutes: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    isCompleted: Boolean,
    activityType: String,
    parentProjectId: String?,
  ) -> T): Query<T> = SearchActivityRecordsFtsQuery(query) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      ActivityRecordsAdapter.createdAtAdapter.decode(cursor.getLong(3)!!),
      cursor.getLong(4),
      cursor.getLong(5),
      cursor.getLong(6),
      cursor.getString(7)?.let { ActivityRecordsAdapter.tagsAdapter.decode(it) },
      cursor.getString(8)?.let { ActivityRecordsAdapter.relatedLinksAdapter.decode(it) },
      cursor.getBoolean(9)!!,
      cursor.getString(10)!!,
      cursor.getString(11)
    )
  }

  public fun searchActivityRecordsFts(query: String): Query<ActivityRecords> =
      searchActivityRecordsFts(query) { id, name, description, createdAt, startTime, endTime,
      totalTimeSpentMinutes, tags, relatedLinks, isCompleted, activityType, parentProjectId ->
    ActivityRecords(
      id,
      name,
      description,
      createdAt,
      startTime,
      endTime,
      totalTimeSpentMinutes,
      tags,
      relatedLinks,
      isCompleted,
      activityType,
      parentProjectId
    )
  }

  public fun <T : Any> searchActivityRecordsFallback(query: String, mapper: (
    id: String,
    name: String,
    description: String?,
    createdAt: Long,
    startTime: Long?,
    endTime: Long?,
    totalTimeSpentMinutes: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    isCompleted: Boolean,
    activityType: String,
    parentProjectId: String?,
  ) -> T): Query<T> = SearchActivityRecordsFallbackQuery(query) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      ActivityRecordsAdapter.createdAtAdapter.decode(cursor.getLong(3)!!),
      cursor.getLong(4),
      cursor.getLong(5),
      cursor.getLong(6),
      cursor.getString(7)?.let { ActivityRecordsAdapter.tagsAdapter.decode(it) },
      cursor.getString(8)?.let { ActivityRecordsAdapter.relatedLinksAdapter.decode(it) },
      cursor.getBoolean(9)!!,
      cursor.getString(10)!!,
      cursor.getString(11)
    )
  }

  public fun searchActivityRecordsFallback(query: String): Query<ActivityRecords> =
      searchActivityRecordsFallback(query) { id, name, description, createdAt, startTime, endTime,
      totalTimeSpentMinutes, tags, relatedLinks, isCompleted, activityType, parentProjectId ->
    ActivityRecords(
      id,
      name,
      description,
      createdAt,
      startTime,
      endTime,
      totalTimeSpentMinutes,
      tags,
      relatedLinks,
      isCompleted,
      activityType,
      parentProjectId
    )
  }

  public fun insertActivityRecord(
    id: String,
    name: String,
    description: String?,
    createdAt: Long,
    startTime: Long?,
    endTime: Long?,
    totalTimeSpentMinutes: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    isCompleted: Boolean,
    activityType: String,
    parentProjectId: String?,
  ) {
    driver.execute(-1_144_664_881, """
        |INSERT OR REPLACE INTO ActivityRecords(
        |    id,
        |    name,
        |    description,
        |    createdAt,
        |    startTime,
        |    endTime,
        |    totalTimeSpentMinutes,
        |    tags,
        |    relatedLinks,
        |    isCompleted,
        |    activityType,
        |    parentProjectId
        |) VALUES (
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?
        |)
        """.trimMargin(), 12) {
          bindString(0, id)
          bindString(1, name)
          bindString(2, description)
          bindLong(3, ActivityRecordsAdapter.createdAtAdapter.encode(createdAt))
          bindLong(4, startTime)
          bindLong(5, endTime)
          bindLong(6, totalTimeSpentMinutes)
          bindString(7, tags?.let { ActivityRecordsAdapter.tagsAdapter.encode(it) })
          bindString(8, relatedLinks?.let { ActivityRecordsAdapter.relatedLinksAdapter.encode(it) })
          bindBoolean(9, isCompleted)
          bindString(10, activityType)
          bindString(11, parentProjectId)
        }
    notifyQueries(-1_144_664_881) { emit ->
      emit("ActivityRecords")
      emit("ActivityRecordsFts")
    }
  }

  public fun updateActivityRecord(
    name: String,
    description: String?,
    createdAt: Long,
    startTime: Long?,
    endTime: Long?,
    totalTimeSpentMinutes: Long?,
    tags: StringList?,
    relatedLinks: RelatedLinkList?,
    isCompleted: Boolean,
    activityType: String,
    parentProjectId: String?,
    id: String,
  ) {
    driver.execute(-356_161_825, """
        |UPDATE ActivityRecords SET
        |    name = ?,
        |    description = ?,
        |    createdAt = ?,
        |    startTime = ?,
        |    endTime = ?,
        |    totalTimeSpentMinutes = ?,
        |    tags = ?,
        |    relatedLinks = ?,
        |    isCompleted = ?,
        |    activityType = ?,
        |    parentProjectId = ?
        |WHERE id = ?
        """.trimMargin(), 12) {
          bindString(0, name)
          bindString(1, description)
          bindLong(2, ActivityRecordsAdapter.createdAtAdapter.encode(createdAt))
          bindLong(3, startTime)
          bindLong(4, endTime)
          bindLong(5, totalTimeSpentMinutes)
          bindString(6, tags?.let { ActivityRecordsAdapter.tagsAdapter.encode(it) })
          bindString(7, relatedLinks?.let { ActivityRecordsAdapter.relatedLinksAdapter.encode(it) })
          bindBoolean(8, isCompleted)
          bindString(9, activityType)
          bindString(10, parentProjectId)
          bindString(11, id)
        }
    notifyQueries(-356_161_825) { emit ->
      emit("ActivityRecords")
      emit("ActivityRecordsFts")
    }
  }

  public fun deleteActivityRecord(recordId: String) {
    driver.execute(434_798_657, """DELETE FROM ActivityRecords WHERE id = ?""", 1) {
          bindString(0, recordId)
        }
    notifyQueries(434_798_657) { emit ->
      emit("ActivityRecords")
      emit("ActivityRecordsFts")
    }
  }

  public fun deleteAllActivityRecords() {
    driver.execute(-938_295_341, """DELETE FROM ActivityRecords""", 0)
    notifyQueries(-938_295_341) { emit ->
      emit("ActivityRecords")
      emit("ActivityRecordsFts")
    }
  }

  private inner class GetActivityRecordByIdQuery<out T : Any>(
    public val recordId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ActivityRecords", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ActivityRecords", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(335_106_930,
        """SELECT ActivityRecords.id, ActivityRecords.name, ActivityRecords.description, ActivityRecords.createdAt, ActivityRecords.startTime, ActivityRecords.endTime, ActivityRecords.totalTimeSpentMinutes, ActivityRecords.tags, ActivityRecords.relatedLinks, ActivityRecords.isCompleted, ActivityRecords.activityType, ActivityRecords.parentProjectId FROM ActivityRecords WHERE id = ?""",
        mapper, 1) {
      bindString(0, recordId)
    }

    override fun toString(): String = "ActivityRecords.sq:getActivityRecordById"
  }

  private inner class GetLastOngoingActivityForProjectQuery<out T : Any>(
    public val projectId: String?,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ActivityRecords", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ActivityRecords", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null, """
    |SELECT ActivityRecords.id, ActivityRecords.name, ActivityRecords.description, ActivityRecords.createdAt, ActivityRecords.startTime, ActivityRecords.endTime, ActivityRecords.totalTimeSpentMinutes, ActivityRecords.tags, ActivityRecords.relatedLinks, ActivityRecords.isCompleted, ActivityRecords.activityType, ActivityRecords.parentProjectId FROM ActivityRecords
    |WHERE parentProjectId ${ if (projectId == null) "IS" else "=" } ? AND endTime IS NULL
    |ORDER BY startTime DESC
    |LIMIT 1
    """.trimMargin(), mapper, 1) {
      bindString(0, projectId)
    }

    override fun toString(): String = "ActivityRecords.sq:getLastOngoingActivityForProject"
  }

  private inner class SearchActivityRecordsFtsQuery<out T : Any>(
    public val query: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ActivityRecords", "ActivityRecordsFts", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ActivityRecords", "ActivityRecordsFts", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-875_524_496, """
    |SELECT ActivityRecords.id, ActivityRecords.name, ActivityRecords.description, ActivityRecords.createdAt, ActivityRecords.startTime, ActivityRecords.endTime, ActivityRecords.totalTimeSpentMinutes, ActivityRecords.tags, ActivityRecords.relatedLinks, ActivityRecords.isCompleted, ActivityRecords.activityType, ActivityRecords.parentProjectId
    |FROM ActivityRecords
    |JOIN ActivityRecordsFts ON ActivityRecords.id = ActivityRecordsFts.id
    |WHERE ActivityRecordsFts MATCH ?
    |ORDER BY ActivityRecords.createdAt DESC
    """.trimMargin(), mapper, 1) {
      bindString(0, query)
    }

    override fun toString(): String = "ActivityRecords.sq:searchActivityRecordsFts"
  }

  private inner class SearchActivityRecordsFallbackQuery<out T : Any>(
    public val query: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ActivityRecords", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ActivityRecords", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(848_204_983, """
    |SELECT ActivityRecords.id, ActivityRecords.name, ActivityRecords.description, ActivityRecords.createdAt, ActivityRecords.startTime, ActivityRecords.endTime, ActivityRecords.totalTimeSpentMinutes, ActivityRecords.tags, ActivityRecords.relatedLinks, ActivityRecords.isCompleted, ActivityRecords.activityType, ActivityRecords.parentProjectId
    |FROM ActivityRecords
    |WHERE name LIKE '%' || ? || '%' OR description LIKE '%' || ? || '%'
    |ORDER BY createdAt DESC
    """.trimMargin(), mapper, 2) {
      bindString(0, query)
      bindString(1, query)
    }

    override fun toString(): String = "ActivityRecords.sq:searchActivityRecordsFallback"
  }
}
