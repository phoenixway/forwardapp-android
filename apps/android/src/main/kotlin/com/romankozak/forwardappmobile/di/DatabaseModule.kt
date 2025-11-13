package com.romankozak.forwardappmobile.di

import android.app.Application
import com.romankozak.forwardappmobile.shared.database.DatabaseDriverFactory
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.createForwardAppDatabase
import me.tatarka.inject.annotations.Provides

interface DatabaseModule {

    @Provides
    @AndroidSingleton
    fun provideDatabaseDriverFactory(
        application: Application,
    ): DatabaseDriverFactory = DatabaseDriverFactory(application)

    @Provides
    @AndroidSingleton
    fun provideForwardAppDatabase(
        driverFactory: DatabaseDriverFactory,
    ): ForwardAppDatabase = createForwardAppDatabase(driverFactory.createDriver())
}
