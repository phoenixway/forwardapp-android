package com.romankozak.forwardappmobile.shared.features.aichat.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.database.createTestDatabase
import com.romankozak.forwardappmobile.shared.data.database.createTestDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.createForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.aichat.domain.model.ConversationWithLastMessage
import com.romankozak.forwardappmobile.shared.features.aichat.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: ChatRepository

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        database = createForwardAppDatabase(driver)
        repository = ChatRepositoryImpl(database, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `createConversation inserts data`() = runTest {
        val conversationId = repository.createConversation(title = "New Chat", folderId = null)

        val conversations = repository.observeConversations().first()
        assertEquals(1, conversations.size)
        assertEquals("New Chat", conversations.first().title)
        assertEquals(conversationId, conversations.first().id)
    }

    @Test
    fun `insertMessage emits via observeMessages`() = runTest {
        val conversationId = repository.createConversation("Thread", null)
        repository.insertMessage(
            conversationId = conversationId,
            text = "Hello",
            isFromUser = true,
            isError = false,
            timestamp = 100,
            isStreaming = false,
        )

        val messages = repository.observeMessages(conversationId).first()
        assertEquals(1, messages.size)
        assertEquals("Hello", messages.first().text)

        val count = repository.observeMessageCount(conversationId).first()
        assertEquals(1L, count)
    }

    @Test
    fun `observeAllConversationsWithLastMessage returns latest message`() = runTest {
        val conversationId = repository.createConversation("Thread", null)
        repository.insertMessage(conversationId, "First", true, false, 10, false)
        repository.insertMessage(conversationId, "Second", false, false, 20, false)

        val conversations = repository.observeAllConversationsWithLastMessage().first()
        assertEquals(1, conversations.size)
        val lastMessage = conversations.first().lastMessage
        assertNotNull(lastMessage)
        assertEquals("Second", lastMessage.text)
        assertTrue(!lastMessage.isFromUser)
    }
}
