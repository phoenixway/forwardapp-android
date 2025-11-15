package com.romankozak.forwardappmobile.shared.features.aichat

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class ConversationFoldersQueries(
  driver: SqlDriver,
  private val ConversationFoldersAdapter: ConversationFolders.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getConversationFolders(mapper: (id: Long, name: String) -> T): Query<T> =
      Query(1_092_047_164, arrayOf("ConversationFolders"), driver, "ConversationFolders.sq",
      "getConversationFolders",
      "SELECT ConversationFolders.id, ConversationFolders.name FROM ConversationFolders ORDER BY name") {
      cursor ->
    mapper(
      ConversationFoldersAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1)!!
    )
  }

  public fun getConversationFolders(): Query<ConversationFolders> = getConversationFolders { id,
      name ->
    ConversationFolders(
      id,
      name
    )
  }

  public fun lastConversationFolderRowId(): ExecutableQuery<Long> = Query(61_735_454, driver,
      "ConversationFolders.sq", "lastConversationFolderRowId", "SELECT last_insert_rowid() AS id") {
      cursor ->
    cursor.getLong(0)!!
  }

  public fun insertConversationFolder(name: String) {
    driver.execute(686_702_106, """INSERT INTO ConversationFolders(name) VALUES (?)""", 1) {
          bindString(0, name)
        }
    notifyQueries(686_702_106) { emit ->
      emit("ConversationFolders")
    }
  }

  public fun updateConversationFolderName(name: String, id: Long) {
    driver.execute(-1_673_586_219, """UPDATE ConversationFolders SET name = ? WHERE id = ?""", 2) {
          bindString(0, name)
          bindLong(1, ConversationFoldersAdapter.idAdapter.encode(id))
        }
    notifyQueries(-1_673_586_219) { emit ->
      emit("ConversationFolders")
    }
  }

  public fun deleteConversationFolder(id: Long) {
    driver.execute(-1_245_190_004, """DELETE FROM ConversationFolders WHERE id = ?""", 1) {
          bindLong(0, ConversationFoldersAdapter.idAdapter.encode(id))
        }
    notifyQueries(-1_245_190_004) { emit ->
      emit("ConversationFolders")
    }
  }

  public fun deleteAllConversationFolders() {
    driver.execute(1_982_890_140, """DELETE FROM ConversationFolders""", 0)
    notifyQueries(1_982_890_140) { emit ->
      emit("ConversationFolders")
    }
  }
}
