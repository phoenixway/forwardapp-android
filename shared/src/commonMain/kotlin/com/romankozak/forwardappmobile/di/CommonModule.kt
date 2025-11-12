package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.shared.database.*
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Scope
import com.romankozak.forwardappmobile.di.Singleton // Custom Singleton from Scopes.kt

interface CommonModule {

    @Provides @Singleton
    fun provideDatabaseDriverFactory(): DatabaseDriverFactory =
        DatabaseDriverFactory(platformContext = null) // Pass null for common

    @Provides @Singleton
    fun provideDatabase(factory: DatabaseDriverFactory): ForwardAppDatabase =
        createForwardAppDatabase(factory.createDriver())
}
