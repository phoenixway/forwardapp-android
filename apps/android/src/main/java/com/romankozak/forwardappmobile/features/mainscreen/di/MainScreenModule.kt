package com.romankozak.forwardappmobile.features.mainscreen.di

import com.romankozak.forwardappmobile.features.mainscreen.MainScreenViewModel
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.repository.ProjectRepository
import com.romankozak.forwardappmobile.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import me.tatarka.inject.annotations.Provides

interface MainScreenModule {
    @Provides
    fun provideMainScreenViewModel(
        projectRepository: ProjectRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): MainScreenViewModel = MainScreenViewModel(
        projectRepository = projectRepository,
        ioDispatcher = ioDispatcher,
    )
}
