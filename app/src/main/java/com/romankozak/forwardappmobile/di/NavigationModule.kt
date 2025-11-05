
package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.features.projects.data.ProjectRepository
import com.romankozak.forwardappmobile.ui.navigation.ClearAndNavigateHomeUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NavigationModule {
    @Provides
    @Singleton
    fun provideClearAndNavigateHomeUseCase(
        projectRepository: ProjectRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ClearAndNavigateHomeUseCase {
        return ClearAndNavigateHomeUseCase(
            projectRepository = projectRepository,
            ioDispatcher = ioDispatcher,
        )
    }
}
