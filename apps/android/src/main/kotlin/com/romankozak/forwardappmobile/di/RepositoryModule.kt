package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.projects.core.data.repository.ProjectRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.repository.ProjectRepository
import kotlinx.coroutines.CoroutineDispatcher
import me.tatarka.inject.annotations.Provides

interface RepositoryModule {

    @Provides
    @AndroidSingleton
    fun provideProjectRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ProjectRepository = ProjectRepositoryImpl(database, ioDispatcher)
}
