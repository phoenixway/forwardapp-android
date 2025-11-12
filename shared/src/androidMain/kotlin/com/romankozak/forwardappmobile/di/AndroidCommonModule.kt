package com.romankozak.forwardappmobile.di

import android.content.Context
import com.romankozak.forwardappmobile.shared.database.*
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Scope
import com.romankozak.forwardappmobile.di.AndroidSingleton
import com.romankozak.forwardappmobile.di.ApplicationContext // Assuming ApplicationContext is defined in Scopes.kt or Qualifiers.kt

interface AndroidCommonModule : CommonModule {

    @Provides @AndroidSingleton
    fun provideDatabaseDriverFactory(@ApplicationContext context: Context): DatabaseDriverFactory =
        DatabaseDriverFactory(context)

    @Provides @AndroidSingleton
    override fun provideDatabase(factory: DatabaseDriverFactory): ForwardAppDatabase =
        createForwardAppDatabase(factory.createDriver())
}
