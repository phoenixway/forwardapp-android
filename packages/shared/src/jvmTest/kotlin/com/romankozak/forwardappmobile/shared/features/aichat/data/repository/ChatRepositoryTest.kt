package com.romankozak.forwardappmobile.shared.features.aichat.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.database.createTestDatabase
import com.romankozak.forwardappmobile.shared.data.database.createTestDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
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
        database = createTestDatabase(driver)
        repository = ChatRepositoryImpl(database, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `insertConversation inserts data`() = runTest {
        val conversationId = repository.insertConversation(title = "New Chat", folderId = null)

        val conversations = repository.getAllConversations().first()
        assertEquals(1, conversations.size)
        assertEquals("New Chat", conversations.first().title)
        assertEquals(conversationId, conversations.first().id)
    }

    @Test
    fun `insertMessage emits via getMessagesForConversation`() = runTest {
        val conversationId = repository.insertConversation("Thread", null)
        repository.insertChatMessage(
            conversationId = conversationId,
            text = "Hello",
            isFromUser = true,
            isError = false,
            isStreaming = false,
        )

        val messages = repository.getMessagesForConversation(conversationId).first()
        assertEquals(1, messages.size)
        assertEquals("Hello", messages.first().text)

        val count = repository.countMessagesForConversation(conversationId).first()
        assertEquals(1L, count)
    }

    @Test
    fun `observeAllConversationsWithLastMessage returns latest message`() = runTest {
        val conversationId = repository.insertConversation("Thread", null)
        repository.insertChatMessage(conversationId, "First", true, false, false)
        repository.insertChatMessage(conversationId, "Second", false, false, false)

        val conversations = repository.observeAllConversationsWithLastMessage().first()
        assertEquals(1, conversations.size)
        val lastMessage = conversations.first().lastMessage
        assertNotNull(lastMessage)
        assertEquals("Second", lastMessage.text)
        assertTrue(!lastMessage.isFromUser)
    }
}
