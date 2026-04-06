package com.romankozak.forwardappmobile.core.di

import com.romankozak.forwardappmobile.core.navigation.capability.InMemoryViewRegistry
import com.romankozak.forwardappmobile.core.navigation.capability.ViewRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ViewRegistryModule {
    @Provides
    @Singleton
    fun provideViewRegistry(): ViewRegistry = InMemoryViewRegistry(emptySet())
}
