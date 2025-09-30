package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.ui.common.editor.CustomListDataSource
import com.romankozak.forwardappmobile.ui.common.editor.InboxItemDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object EditorModule {

    @Provides
    fun provideCustomListDataSource(projectRepository: ProjectRepository): CustomListDataSource {
        return CustomListDataSource(projectRepository)
    }

    @Provides
    fun provideInboxItemDataSource(projectRepository: ProjectRepository): InboxItemDataSource {
        return InboxItemDataSource(projectRepository)
    }
}
