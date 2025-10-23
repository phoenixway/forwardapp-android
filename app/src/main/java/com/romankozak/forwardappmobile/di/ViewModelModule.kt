package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases.PlanningSearchAdapter
import com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases.PlanningSettingsProvider
import com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases.SearchUseCase
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
