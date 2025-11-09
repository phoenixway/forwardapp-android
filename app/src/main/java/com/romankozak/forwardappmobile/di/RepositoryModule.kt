package com.romankozak.forwardappmobile.di

import ProjectRepositoryImpl
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.projects.data.ProjectLocalDataSource
import com.romankozak.forwardappmobile.shared.features.projects.data.ProjectLocalDataSourceImpl
import com.romankozak.forwardappmobile.shared.features.projects.domain.ProjectRepositoryCore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideProjectLocalDataSource(
        db: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ProjectLocalDataSource = ProjectLocalDataSourceImpl(db, ioDispatcher)

    @Provides
    @Singleton
    fun provideProjectRepository(
        projectLocalDataSource: ProjectLocalDataSource,
    ): ProjectRepositoryCore = ProjectRepositoryImpl(
        projectLocalDataSource,
    )
}