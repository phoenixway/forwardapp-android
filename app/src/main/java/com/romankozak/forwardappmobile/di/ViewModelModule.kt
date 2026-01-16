package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.usecases.PlanningSearchAdapter
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.usecases.PlanningSettingsProvider
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.usecases.SearchUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class ViewModelModule {
  @Binds
  abstract fun bindPlanningSearchAdapter(searchUseCase: SearchUseCase): PlanningSearchAdapter

  @Binds
  abstract fun bindPlanningSettingsProvider(settingsRepository: SettingsRepository): PlanningSettingsProvider
}
