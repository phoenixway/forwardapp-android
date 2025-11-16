package com.romankozak.forwardappmobile.di

import androidx.lifecycle.ViewModelProvider
import me.tatarka.inject.annotations.Provides

interface ViewModelFactoryModule {
    @Provides
    fun provideViewModelFactory(
        factory: InjectedViewModelFactory
    ): ViewModelProvider.Factory = factory
}
