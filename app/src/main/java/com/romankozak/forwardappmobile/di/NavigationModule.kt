package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.features.navigation.DefaultNavigationDispatcher
import com.romankozak.forwardappmobile.features.navigation.NavigationDispatcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationModule {

    @Binds @Singleton
    abstract fun bindNavigationDispatcher(
        impl: DefaultNavigationDispatcher
    ): NavigationDispatcher
}
