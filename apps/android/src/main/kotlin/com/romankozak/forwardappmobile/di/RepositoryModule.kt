package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.projects.core.data.repository.ProjectRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.repository.ProjectRepository
import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.data.repository.InboxRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.domain.repository.InboxRepository
import com.romankozak.forwardappmobile.shared.features.recent.data.repository.RecentItemRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.recent.domain.repository.RecentItemRepository
import com.romankozak.forwardappmobile.shared.features.aichat.data.repository.ConversationFolderRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.aichat.domain.repository.ConversationFolderRepository
import kotlinx.coroutines.CoroutineDispatcher
import me.tatarka.inject.annotations.Provides

interface RepositoryModule {

    @Provides
    @AndroidSingleton
    fun provideProjectRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ProjectRepository = ProjectRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideRecentItemRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): RecentItemRepository = RecentItemRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideConversationFolderRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ConversationFolderRepository = ConversationFolderRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideInboxRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): InboxRepository = InboxRepositoryImpl(database, ioDispatcher)
}
