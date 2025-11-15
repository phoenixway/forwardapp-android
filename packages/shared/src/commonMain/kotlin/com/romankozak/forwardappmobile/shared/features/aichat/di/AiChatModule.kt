package com.romankozak.forwardappmobile.shared.features.aichat.di

import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.aichat.data.repository.ChatRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.aichat.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineDispatcher
import me.tatarka.inject.annotations.Provides

interface AiChatModule {
    @Provides
    fun provideChatRepository(
        db: ForwardAppDatabase,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): ChatRepository = ChatRepositoryImpl(db, dispatcher)
}
