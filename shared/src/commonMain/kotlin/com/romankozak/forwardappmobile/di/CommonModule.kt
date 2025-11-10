package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.shared.database.*
import me.tatarka.inject.annotations.*

@Scope
annotation class Singleton

interface CommonModule {

    @Provides @Singleton
    fun provideDatabaseDriverFactory(): DatabaseDriverFactory =
        DatabaseDriverFactory()

    @Provides @Singleton
    fun provideDatabase(factory: DatabaseDriverFactory): ForwardAppDatabase =
        createForwardAppDatabase(factory)
}
