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

public class ChatMessagesQueries(
  driver: SqlDriver,
  private val ChatMessagesAdapter: ChatMessages.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> getMessagesForConversation(conversationId: Long, mapper: (
    id: Long,
    conversationId: Long,
    text: String,
    isFromUser: Boolean,
    isError: Boolean,
    timestamp: Long,
    isStreaming: Boolean,
  ) -> T): Query<T> = GetMessagesForConversationQuery(conversationId) { cursor ->
    mapper(
      ChatMessagesAdapter.idAdapter.decode(cursor.getLong(0)!!),
      ChatMessagesAdapter.conversationIdAdapter.decode(cursor.getLong(1)!!),
      cursor.getString(2)!!,
      cursor.getBoolean(3)!!,
      cursor.getBoolean(4)!!,
      ChatMessagesAdapter.timestampAdapter.decode(cursor.getLong(5)!!),
      cursor.getBoolean(6)!!
    )
  }

  public fun getMessagesForConversation(conversationId: Long): Query<ChatMessages> =
      getMessagesForConversation(conversationId) { id, conversationId_, text, isFromUser, isError,
      timestamp, isStreaming ->
    ChatMessages(
      id,
      conversationId_,
      text,
      isFromUser,
      isError,
      timestamp,
      isStreaming
    )
  }

  public fun <T : Any> getLastAssistantMessage(conversationId: Long, mapper: (
    id: Long,
    conversationId: Long,
    text: String,
    isFromUser: Boolean,
    isError: Boolean,
    timestamp: Long,
    isStreaming: Boolean,
  ) -> T): Query<T> = GetLastAssistantMessageQuery(conversationId) { cursor ->
    mapper(
      ChatMessagesAdapter.idAdapter.decode(cursor.getLong(0)!!),
      ChatMessagesAdapter.conversationIdAdapter.decode(cursor.getLong(1)!!),
      cursor.getString(2)!!,
      cursor.getBoolean(3)!!,
      cursor.getBoolean(4)!!,
      ChatMessagesAdapter.timestampAdapter.decode(cursor.getLong(5)!!),
      cursor.getBoolean(6)!!
    )
  }

  public fun getLastAssistantMessage(conversationId: Long): Query<ChatMessages> =
      getLastAssistantMessage(conversationId) { id, conversationId_, text, isFromUser, isError,
      timestamp, isStreaming ->
    ChatMessages(
      id,
      conversationId_,
      text,
      isFromUser,
      isError,
      timestamp,
      isStreaming
    )
  }

  public fun <T : Any> getMessageById(messageId: Long, mapper: (
    id: Long,
    conversationId: Long,
    text: String,
    isFromUser: Boolean,
    isError: Boolean,
    timestamp: Long,
    isStreaming: Boolean,
  ) -> T): Query<T> = GetMessageByIdQuery(messageId) { cursor ->
    mapper(
      ChatMessagesAdapter.idAdapter.decode(cursor.getLong(0)!!),
      ChatMessagesAdapter.conversationIdAdapter.decode(cursor.getLong(1)!!),
      cursor.getString(2)!!,
      cursor.getBoolean(3)!!,
      cursor.getBoolean(4)!!,
      ChatMessagesAdapter.timestampAdapter.decode(cursor.getLong(5)!!),
      cursor.getBoolean(6)!!
    )
  }

  public fun getMessageById(messageId: Long): Query<ChatMessages> = getMessageById(messageId) { id,
      conversationId, text, isFromUser, isError, timestamp, isStreaming ->
    ChatMessages(
      id,
      conversationId,
      text,
      isFromUser,
      isError,
      timestamp,
      isStreaming
    )
  }

  public fun getLastInsertedChatMessageId(): ExecutableQuery<Long> = Query(-2_095_592_360, driver,
      "ChatMessages.sq", "getLastInsertedChatMessageId", "SELECT last_insert_rowid()") { cursor ->
    cursor.getLong(0)!!
  }

  public fun countMessagesForConversation(conversationId: Long): Query<Long> =
      CountMessagesForConversationQuery(conversationId) { cursor ->
    cursor.getLong(0)!!
  }

  public fun insertChatMessage(
    conversationId: Long,
    text: String,
    isFromUser: Boolean,
    isError: Boolean,
    timestamp: Long,
    isStreaming: Boolean,
  ) {
    driver.execute(880_185_636, """
        |INSERT INTO ChatMessages(
        |    conversationId,
        |    text,
        |    isFromUser,
        |    isError,
        |    timestamp,
        |    isStreaming
        |) VALUES (
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?,
        |    ?
        |)
        """.trimMargin(), 6) {
          bindLong(0, ChatMessagesAdapter.conversationIdAdapter.encode(conversationId))
          bindString(1, text)
          bindBoolean(2, isFromUser)
          bindBoolean(3, isError)
          bindLong(4, ChatMessagesAdapter.timestampAdapter.encode(timestamp))
          bindBoolean(5, isStreaming)
        }
    notifyQueries(880_185_636) { emit ->
      emit("ChatMessages")
    }
  }

  public fun deleteMessageById(messageId: Long) {
    driver.execute(-986_823_204, """
        |DELETE FROM ChatMessages
        |WHERE id = ?
        """.trimMargin(), 1) {
          bindLong(0, ChatMessagesAdapter.idAdapter.encode(messageId))
        }
    notifyQueries(-986_823_204) { emit ->
      emit("ChatMessages")
    }
  }

  public fun deleteMessagesForConversation(conversationId: Long) {
    driver.execute(-943_906_333, """
        |DELETE FROM ChatMessages
        |WHERE conversationId = ?
        """.trimMargin(), 1) {
          bindLong(0, ChatMessagesAdapter.conversationIdAdapter.encode(conversationId))
        }
    notifyQueries(-943_906_333) { emit ->
      emit("ChatMessages")
    }
  }

  public fun deleteAllMessages() {
    driver.execute(-401_440_912, """DELETE FROM ChatMessages""", 0)
    notifyQueries(-401_440_912) { emit ->
      emit("ChatMessages")
    }
  }

  private inner class GetMessagesForConversationQuery<out T : Any>(
    public val conversationId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ChatMessages", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ChatMessages", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_222_603_652, """
    |SELECT ChatMessages.id, ChatMessages.conversationId, ChatMessages.text, ChatMessages.isFromUser, ChatMessages.isError, ChatMessages.timestamp, ChatMessages.isStreaming FROM ChatMessages
    |WHERE conversationId = ?
    |ORDER BY timestamp ASC
    """.trimMargin(), mapper, 1) {
      bindLong(0, ChatMessagesAdapter.conversationIdAdapter.encode(conversationId))
    }

    override fun toString(): String = "ChatMessages.sq:getMessagesForConversation"
  }

  private inner class GetLastAssistantMessageQuery<out T : Any>(
    public val conversationId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ChatMessages", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ChatMessages", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(93_326_339, """
    |SELECT ChatMessages.id, ChatMessages.conversationId, ChatMessages.text, ChatMessages.isFromUser, ChatMessages.isError, ChatMessages.timestamp, ChatMessages.isStreaming FROM ChatMessages
    |WHERE conversationId = ?
    |  AND isFromUser = 0
    |ORDER BY timestamp DESC
    |LIMIT 1
    """.trimMargin(), mapper, 1) {
      bindLong(0, ChatMessagesAdapter.conversationIdAdapter.encode(conversationId))
    }

    override fun toString(): String = "ChatMessages.sq:getLastAssistantMessage"
  }

  private inner class GetMessageByIdQuery<out T : Any>(
    public val messageId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ChatMessages", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ChatMessages", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-114_160_139, """
    |SELECT ChatMessages.id, ChatMessages.conversationId, ChatMessages.text, ChatMessages.isFromUser, ChatMessages.isError, ChatMessages.timestamp, ChatMessages.isStreaming FROM ChatMessages
    |WHERE id = ?
    """.trimMargin(), mapper, 1) {
      bindLong(0, ChatMessagesAdapter.idAdapter.encode(messageId))
    }

    override fun toString(): String = "ChatMessages.sq:getMessageById"
  }

  private inner class CountMessagesForConversationQuery<out T : Any>(
    public val conversationId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ChatMessages", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ChatMessages", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-2_120_563_165, """
    |SELECT COUNT(*) FROM ChatMessages
    |WHERE conversationId = ?
    """.trimMargin(), mapper, 1) {
      bindLong(0, ChatMessagesAdapter.conversationIdAdapter.encode(conversationId))
    }

    override fun toString(): String = "ChatMessages.sq:countMessagesForConversation"
  }
}
