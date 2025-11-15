package com.romankozak.forwardappmobile.shared.features.aichat

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Boolean
import kotlin.Long
import kotlin.String

public class ConversationsQueries(
  driver: SqlDriver,
  private val ConversationsAdapter: Conversations.Adapter,
  private val ChatMessagesAdapter: ChatMessages.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getAllConversations(mapper: (
    id: Long,
    title: String,
    creationTimestamp: Long,
    folderId: Long?,
  ) -> T): Query<T> = Query(-1_245_746_009, arrayOf("Conversations"), driver, "Conversations.sq",
      "getAllConversations", """
  |SELECT Conversations.id, Conversations.title, Conversations.creationTimestamp, Conversations.folderId FROM Conversations
  |ORDER BY creationTimestamp DESC
  """.trimMargin()) { cursor ->
    mapper(
      ConversationsAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1)!!,
      ConversationsAdapter.creationTimestampAdapter.decode(cursor.getLong(2)!!),
      cursor.getLong(3)
    )
  }

  public fun getAllConversations(): Query<Conversations> = getAllConversations { id, title,
      creationTimestamp, folderId ->
    Conversations(
      id,
      title,
      creationTimestamp,
      folderId
    )
  }

  public fun <T : Any> getConversationsByFolder(folderId: Long?, mapper: (
    id: Long,
    title: String,
    creationTimestamp: Long,
    folderId: Long?,
  ) -> T): Query<T> = GetConversationsByFolderQuery(folderId) { cursor ->
    mapper(
      ConversationsAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1)!!,
      ConversationsAdapter.creationTimestampAdapter.decode(cursor.getLong(2)!!),
      cursor.getLong(3)
    )
  }

  public fun getConversationsByFolder(folderId: Long?): Query<Conversations> =
      getConversationsByFolder(folderId) { id, title, creationTimestamp, folderId_ ->
    Conversations(
      id,
      title,
      creationTimestamp,
      folderId_
    )
  }

  public fun <T : Any> getConversationsWithoutFolder(mapper: (
    id: Long,
    title: String,
    creationTimestamp: Long,
    folderId: Long?,
  ) -> T): Query<T> = Query(-1_123_963_042, arrayOf("Conversations"), driver, "Conversations.sq",
      "getConversationsWithoutFolder", """
  |SELECT Conversations.id, Conversations.title, Conversations.creationTimestamp, Conversations.folderId FROM Conversations
  |WHERE folderId IS NULL
  |ORDER BY creationTimestamp DESC
  """.trimMargin()) { cursor ->
    mapper(
      ConversationsAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1)!!,
      ConversationsAdapter.creationTimestampAdapter.decode(cursor.getLong(2)!!),
      cursor.getLong(3)
    )
  }

  public fun getConversationsWithoutFolder(): Query<Conversations> = getConversationsWithoutFolder {
      id, title, creationTimestamp, folderId ->
    Conversations(
      id,
      title,
      creationTimestamp,
      folderId
    )
  }

  public fun <T : Any> getConversationById(id: Long, mapper: (
    id: Long,
    title: String,
    creationTimestamp: Long,
    folderId: Long?,
  ) -> T): Query<T> = GetConversationByIdQuery(id) { cursor ->
    mapper(
      ConversationsAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1)!!,
      ConversationsAdapter.creationTimestampAdapter.decode(cursor.getLong(2)!!),
      cursor.getLong(3)
    )
  }

  public fun getConversationById(id: Long): Query<Conversations> = getConversationById(id) { id_,
      title, creationTimestamp, folderId ->
    Conversations(
      id_,
      title,
      creationTimestamp,
      folderId
    )
  }

  public fun getLastInsertedConversationId(): ExecutableQuery<Long> = Query(-1_738_030_876, driver,
      "Conversations.sq", "getLastInsertedConversationId", "SELECT last_insert_rowid()") { cursor ->
    cursor.getLong(0)!!
  }

  public fun <T : Any> getConversationsWithLastMessage(mapper: (
    id: Long,
    title: String,
    creationTimestamp: Long,
    folderId: Long?,
    lastMessageId: Long?,
    lastMessageText: String?,
    lastMessageIsFromUser: Boolean?,
    lastMessageIsError: Boolean?,
    lastMessageTimestamp: Long?,
    lastMessageIsStreaming: Boolean?,
  ) -> T): Query<T> = Query(383_216_115, arrayOf("Conversations", "ChatMessages"), driver,
      "Conversations.sq", "getConversationsWithLastMessage", """
  |SELECT
  |    C.id,
  |    C.title,
  |    C.creationTimestamp,
  |    C.folderId,
  |    LM.id AS lastMessageId,
  |    LM.text AS lastMessageText,
  |    LM.isFromUser AS lastMessageIsFromUser,
  |    LM.isError AS lastMessageIsError,
  |    LM.timestamp AS lastMessageTimestamp,
  |    LM.isStreaming AS lastMessageIsStreaming
  |FROM Conversations AS C
  |LEFT JOIN ChatMessages AS LM ON LM.id = (
  |    SELECT id FROM ChatMessages
  |    WHERE conversationId = C.id
  |    ORDER BY timestamp DESC
  |    LIMIT 1
  |)
  |ORDER BY C.creationTimestamp DESC
  """.trimMargin()) { cursor ->
    mapper(
      ConversationsAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1)!!,
      ConversationsAdapter.creationTimestampAdapter.decode(cursor.getLong(2)!!),
      cursor.getLong(3),
      cursor.getLong(4)?.let { ChatMessagesAdapter.idAdapter.decode(it) },
      cursor.getString(5),
      cursor.getBoolean(6),
      cursor.getBoolean(7),
      cursor.getLong(8)?.let { ChatMessagesAdapter.timestampAdapter.decode(it) },
      cursor.getBoolean(9)
    )
  }

  public fun getConversationsWithLastMessage(): Query<GetConversationsWithLastMessage> =
      getConversationsWithLastMessage { id, title, creationTimestamp, folderId, lastMessageId,
      lastMessageText, lastMessageIsFromUser, lastMessageIsError, lastMessageTimestamp,
      lastMessageIsStreaming ->
    GetConversationsWithLastMessage(
      id,
      title,
      creationTimestamp,
      folderId,
      lastMessageId,
      lastMessageText,
      lastMessageIsFromUser,
      lastMessageIsError,
      lastMessageTimestamp,
      lastMessageIsStreaming
    )
  }

  public fun <T : Any> getConversationsWithLastMessageByFolder(folderId: Long?, mapper: (
    id: Long,
    title: String,
    creationTimestamp: Long,
    folderId: Long?,
    lastMessageId: Long?,
    lastMessageText: String?,
    lastMessageIsFromUser: Boolean?,
    lastMessageIsError: Boolean?,
    lastMessageTimestamp: Long?,
    lastMessageIsStreaming: Boolean?,
  ) -> T): Query<T> = GetConversationsWithLastMessageByFolderQuery(folderId) { cursor ->
    mapper(
      ConversationsAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1)!!,
      ConversationsAdapter.creationTimestampAdapter.decode(cursor.getLong(2)!!),
      cursor.getLong(3),
      cursor.getLong(4)?.let { ChatMessagesAdapter.idAdapter.decode(it) },
      cursor.getString(5),
      cursor.getBoolean(6),
      cursor.getBoolean(7),
      cursor.getLong(8)?.let { ChatMessagesAdapter.timestampAdapter.decode(it) },
      cursor.getBoolean(9)
    )
  }

  public fun getConversationsWithLastMessageByFolder(folderId: Long?):
      Query<GetConversationsWithLastMessageByFolder> =
      getConversationsWithLastMessageByFolder(folderId) { id, title, creationTimestamp, folderId_,
      lastMessageId, lastMessageText, lastMessageIsFromUser, lastMessageIsError,
      lastMessageTimestamp, lastMessageIsStreaming ->
    GetConversationsWithLastMessageByFolder(
      id,
      title,
      creationTimestamp,
      folderId_,
      lastMessageId,
      lastMessageText,
      lastMessageIsFromUser,
      lastMessageIsError,
      lastMessageTimestamp,
      lastMessageIsStreaming
    )
  }

  public fun <T : Any> getConversationsWithLastMessageWithoutFolder(mapper: (
    id: Long,
    title: String,
    creationTimestamp: Long,
    folderId: Long?,
    lastMessageId: Long?,
    lastMessageText: String?,
    lastMessageIsFromUser: Boolean?,
    lastMessageIsError: Boolean?,
    lastMessageTimestamp: Long?,
    lastMessageIsStreaming: Boolean?,
  ) -> T): Query<T> = Query(-2_126_046_173, arrayOf("Conversations", "ChatMessages"), driver,
      "Conversations.sq", "getConversationsWithLastMessageWithoutFolder", """
  |SELECT
  |    C.id,
  |    C.title,
  |    C.creationTimestamp,
  |    C.folderId,
  |    LM.id AS lastMessageId,
  |    LM.text AS lastMessageText,
  |    LM.isFromUser AS lastMessageIsFromUser,
  |    LM.isError AS lastMessageIsError,
  |    LM.timestamp AS lastMessageTimestamp,
  |    LM.isStreaming AS lastMessageIsStreaming
  |FROM Conversations AS C
  |LEFT JOIN ChatMessages AS LM ON LM.id = (
  |    SELECT id FROM ChatMessages
  |    WHERE conversationId = C.id
  |    ORDER BY timestamp DESC
  |    LIMIT 1
  |)
  |WHERE C.folderId IS NULL
  |ORDER BY C.creationTimestamp DESC
  """.trimMargin()) { cursor ->
    mapper(
      ConversationsAdapter.idAdapter.decode(cursor.getLong(0)!!),
      cursor.getString(1)!!,
      ConversationsAdapter.creationTimestampAdapter.decode(cursor.getLong(2)!!),
      cursor.getLong(3),
      cursor.getLong(4)?.let { ChatMessagesAdapter.idAdapter.decode(it) },
      cursor.getString(5),
      cursor.getBoolean(6),
      cursor.getBoolean(7),
      cursor.getLong(8)?.let { ChatMessagesAdapter.timestampAdapter.decode(it) },
      cursor.getBoolean(9)
    )
  }

  public fun getConversationsWithLastMessageWithoutFolder():
      Query<GetConversationsWithLastMessageWithoutFolder> =
      getConversationsWithLastMessageWithoutFolder { id, title, creationTimestamp, folderId,
      lastMessageId, lastMessageText, lastMessageIsFromUser, lastMessageIsError,
      lastMessageTimestamp, lastMessageIsStreaming ->
    GetConversationsWithLastMessageWithoutFolder(
      id,
      title,
      creationTimestamp,
      folderId,
      lastMessageId,
      lastMessageText,
      lastMessageIsFromUser,
      lastMessageIsError,
      lastMessageTimestamp,
      lastMessageIsStreaming
    )
  }

  public fun insertConversation(
    title: String,
    creationTimestamp: Long,
    folderId: Long?,
  ) {
    driver.execute(-1_829_187_430, """
        |INSERT INTO Conversations(
        |    title,
        |    creationTimestamp,
        |    folderId
        |) VALUES (
        |    ?,
        |    ?,
        |    ?
        |)
        """.trimMargin(), 3) {
          bindString(0, title)
          bindLong(1, ConversationsAdapter.creationTimestampAdapter.encode(creationTimestamp))
          bindLong(2, folderId)
        }
    notifyQueries(-1_829_187_430) { emit ->
      emit("Conversations")
    }
  }

  public fun updateConversation(
    title: String,
    folderId: Long?,
    id: Long,
  ) {
    driver.execute(-1_774_735_702, """
        |UPDATE Conversations SET
        |    title = ?,
        |    folderId = ?
        |WHERE id = ?
        """.trimMargin(), 3) {
          bindString(0, title)
          bindLong(1, folderId)
          bindLong(2, ConversationsAdapter.idAdapter.encode(id))
        }
    notifyQueries(-1_774_735_702) { emit ->
      emit("Conversations")
    }
  }

  public fun deleteConversationById(id: Long) {
    driver.execute(980_443_774, """
        |DELETE FROM Conversations
        |WHERE id = ?
        """.trimMargin(), 1) {
          bindLong(0, ConversationsAdapter.idAdapter.encode(id))
        }
    notifyQueries(980_443_774) { emit ->
      emit("ChatMessages")
      emit("Conversations")
    }
  }

  public fun deleteAllConversations() {
    driver.execute(1_928_234_936, """DELETE FROM Conversations""", 0)
    notifyQueries(1_928_234_936) { emit ->
      emit("ChatMessages")
      emit("Conversations")
    }
  }

  private inner class GetConversationsByFolderQuery<out T : Any>(
    public val folderId: Long?,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Conversations", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Conversations", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null, """
    |SELECT Conversations.id, Conversations.title, Conversations.creationTimestamp, Conversations.folderId FROM Conversations
    |WHERE folderId ${ if (folderId == null) "IS" else "=" } ?
    |ORDER BY creationTimestamp DESC
    """.trimMargin(), mapper, 1) {
      bindLong(0, folderId)
    }

    override fun toString(): String = "Conversations.sq:getConversationsByFolder"
  }

  private inner class GetConversationByIdQuery<out T : Any>(
    public val id: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Conversations", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Conversations", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(2_101_430_125, """
    |SELECT Conversations.id, Conversations.title, Conversations.creationTimestamp, Conversations.folderId FROM Conversations
    |WHERE id = ?
    """.trimMargin(), mapper, 1) {
      bindLong(0, ConversationsAdapter.idAdapter.encode(id))
    }

    override fun toString(): String = "Conversations.sq:getConversationById"
  }

  private inner class GetConversationsWithLastMessageByFolderQuery<out T : Any>(
    public val folderId: Long?,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Conversations", "ChatMessages", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Conversations", "ChatMessages", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null, """
    |SELECT
    |    C.id,
    |    C.title,
    |    C.creationTimestamp,
    |    C.folderId,
    |    LM.id AS lastMessageId,
    |    LM.text AS lastMessageText,
    |    LM.isFromUser AS lastMessageIsFromUser,
    |    LM.isError AS lastMessageIsError,
    |    LM.timestamp AS lastMessageTimestamp,
    |    LM.isStreaming AS lastMessageIsStreaming
    |FROM Conversations AS C
    |LEFT JOIN ChatMessages AS LM ON LM.id = (
    |    SELECT id FROM ChatMessages
    |    WHERE conversationId = C.id
    |    ORDER BY timestamp DESC
    |    LIMIT 1
    |)
    |WHERE C.folderId ${ if (folderId == null) "IS" else "=" } ?
    |ORDER BY C.creationTimestamp DESC
    """.trimMargin(), mapper, 1) {
      bindLong(0, folderId)
    }

    override fun toString(): String = "Conversations.sq:getConversationsWithLastMessageByFolder"
  }
}
