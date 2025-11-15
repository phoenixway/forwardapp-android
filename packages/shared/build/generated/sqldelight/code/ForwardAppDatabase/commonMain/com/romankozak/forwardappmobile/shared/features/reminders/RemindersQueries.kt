package com.romankozak.forwardappmobile.shared.features.reminders

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class RemindersQueries(
  driver: SqlDriver,
  private val RemindersAdapter: Reminders.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getAllReminders(mapper: (
    id: String,
    entityId: String,
    entityType: String,
    reminderTime: Long,
    status: String,
    creationTime: Long,
    snoozeUntil: Long?,
  ) -> T): Query<T> = Query(-1_716_301_350, arrayOf("Reminders"), driver, "Reminders.sq",
      "getAllReminders",
      "SELECT Reminders.id, Reminders.entityId, Reminders.entityType, Reminders.reminderTime, Reminders.status, Reminders.creationTime, Reminders.snoozeUntil FROM Reminders ORDER BY reminderTime DESC") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      RemindersAdapter.reminderTimeAdapter.decode(cursor.getLong(3)!!),
      cursor.getString(4)!!,
      RemindersAdapter.creationTimeAdapter.decode(cursor.getLong(5)!!),
      cursor.getLong(6)
    )
  }

  public fun getAllReminders(): Query<Reminders> = getAllReminders { id, entityId, entityType,
      reminderTime, status, creationTime, snoozeUntil ->
    Reminders(
      id,
      entityId,
      entityType,
      reminderTime,
      status,
      creationTime,
      snoozeUntil
    )
  }

  public fun <T : Any> getReminderById(reminderId: String, mapper: (
    id: String,
    entityId: String,
    entityType: String,
    reminderTime: Long,
    status: String,
    creationTime: Long,
    snoozeUntil: Long?,
  ) -> T): Query<T> = GetReminderByIdQuery(reminderId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      RemindersAdapter.reminderTimeAdapter.decode(cursor.getLong(3)!!),
      cursor.getString(4)!!,
      RemindersAdapter.creationTimeAdapter.decode(cursor.getLong(5)!!),
      cursor.getLong(6)
    )
  }

  public fun getReminderById(reminderId: String): Query<Reminders> = getReminderById(reminderId) {
      id, entityId, entityType, reminderTime, status, creationTime, snoozeUntil ->
    Reminders(
      id,
      entityId,
      entityType,
      reminderTime,
      status,
      creationTime,
      snoozeUntil
    )
  }

  public fun <T : Any> getRemindersForEntity(entityId: String, mapper: (
    id: String,
    entityId: String,
    entityType: String,
    reminderTime: Long,
    status: String,
    creationTime: Long,
    snoozeUntil: Long?,
  ) -> T): Query<T> = GetRemindersForEntityQuery(entityId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      RemindersAdapter.reminderTimeAdapter.decode(cursor.getLong(3)!!),
      cursor.getString(4)!!,
      RemindersAdapter.creationTimeAdapter.decode(cursor.getLong(5)!!),
      cursor.getLong(6)
    )
  }

  public fun getRemindersForEntity(entityId: String): Query<Reminders> =
      getRemindersForEntity(entityId) { id, entityId_, entityType, reminderTime, status,
      creationTime, snoozeUntil ->
    Reminders(
      id,
      entityId_,
      entityType,
      reminderTime,
      status,
      creationTime,
      snoozeUntil
    )
  }

  public fun insertReminder(
    id: String,
    entityId: String,
    entityType: String,
    reminderTime: Long,
    status: String,
    creationTime: Long,
    snoozeUntil: Long?,
  ) {
    driver.execute(-2_145_136_633, """
        |INSERT OR REPLACE INTO Reminders(
        |    id,
        |    entityId,
        |    entityType,
        |    reminderTime,
        |    status,
        |    creationTime,
        |    snoozeUntil
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
          bindString(1, entityId)
          bindString(2, entityType)
          bindLong(3, RemindersAdapter.reminderTimeAdapter.encode(reminderTime))
          bindString(4, status)
          bindLong(5, RemindersAdapter.creationTimeAdapter.encode(creationTime))
          bindLong(6, snoozeUntil)
        }
    notifyQueries(-2_145_136_633) { emit ->
      emit("Reminders")
    }
  }

  public fun updateReminder(
    entityId: String,
    entityType: String,
    reminderTime: Long,
    status: String,
    creationTime: Long,
    snoozeUntil: Long?,
    id: String,
  ) {
    driver.execute(1_696_871_959, """
        |UPDATE Reminders SET
        |    entityId = ?,
        |    entityType = ?,
        |    reminderTime = ?,
        |    status = ?,
        |    creationTime = ?,
        |    snoozeUntil = ?
        |WHERE id = ?
        """.trimMargin(), 7) {
          bindString(0, entityId)
          bindString(1, entityType)
          bindLong(2, RemindersAdapter.reminderTimeAdapter.encode(reminderTime))
          bindString(3, status)
          bindLong(4, RemindersAdapter.creationTimeAdapter.encode(creationTime))
          bindLong(5, snoozeUntil)
          bindString(6, id)
        }
    notifyQueries(1_696_871_959) { emit ->
      emit("Reminders")
    }
  }

  public fun deleteReminder(reminderId: String) {
    driver.execute(-1_226_276_615, """DELETE FROM Reminders WHERE id = ?""", 1) {
          bindString(0, reminderId)
        }
    notifyQueries(-1_226_276_615) { emit ->
      emit("Reminders")
    }
  }

  public fun deleteRemindersByEntity(entityId: String) {
    driver.execute(-1_044_985_900, """DELETE FROM Reminders WHERE entityId = ?""", 1) {
          bindString(0, entityId)
        }
    notifyQueries(-1_044_985_900) { emit ->
      emit("Reminders")
    }
  }

  public fun clearReminders() {
    driver.execute(1_038_168_112, """DELETE FROM Reminders""", 0)
    notifyQueries(1_038_168_112) { emit ->
      emit("Reminders")
    }
  }

  private inner class GetReminderByIdQuery<out T : Any>(
    public val reminderId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Reminders", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Reminders", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(349_577_118,
        """SELECT Reminders.id, Reminders.entityId, Reminders.entityType, Reminders.reminderTime, Reminders.status, Reminders.creationTime, Reminders.snoozeUntil FROM Reminders WHERE id = ?""",
        mapper, 1) {
      bindString(0, reminderId)
    }

    override fun toString(): String = "Reminders.sq:getReminderById"
  }

  private inner class GetRemindersForEntityQuery<out T : Any>(
    public val entityId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Reminders", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Reminders", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(672_051_589,
        """SELECT Reminders.id, Reminders.entityId, Reminders.entityType, Reminders.reminderTime, Reminders.status, Reminders.creationTime, Reminders.snoozeUntil FROM Reminders WHERE entityId = ? ORDER BY reminderTime DESC""",
        mapper, 1) {
      bindString(0, entityId)
    }

    override fun toString(): String = "Reminders.sq:getRemindersForEntity"
  }
}
