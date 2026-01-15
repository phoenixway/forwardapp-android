package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.ui.navigation.DefaultNavigationDispatcher
import com.romankozak.forwardappmobile.ui.navigation.NavigationDispatcher
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
