package com.romankozak.forwardappmobile.di

import android.content.Context
import com.romankozak.forwardappmobile.shared.database.*
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Singleton
import me.tatarka.inject.annotations.Qualifier

@Qualifier
annotation class ApplicationContext

interface AndroidCommonModule : CommonModule {

    @Provides @Singleton
    fun provideDatabaseDriverFactory(@ApplicationContext context: Context): DatabaseDriverFactory =
        DatabaseDriverFactory(context)

    @Provides @Singleton
    override fun provideDatabase(factory: DatabaseDriverFactory): ForwardAppDatabase =
        createForwardAppDatabase(factory.createDriver())
}
