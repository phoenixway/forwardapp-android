package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.shared.database.*
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Singleton
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Scope
import me.tatarka.inject.annotations.Tag

interface CommonModule {

    @Provides @Singleton
    fun provideDatabaseDriverFactory(): DatabaseDriverFactory =
        DatabaseDriverFactory()

    @Provides @Singleton
    fun provideDatabase(factory: DatabaseDriverFactory): ForwardAppDatabase =
        createForwardAppDatabase(factory.createDriver())
}
