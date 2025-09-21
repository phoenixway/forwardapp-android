package com.romankozak.forwardappmobile.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {
    // Тут можна додавати ViewModel-специфічні provides методи якщо потрібно
}