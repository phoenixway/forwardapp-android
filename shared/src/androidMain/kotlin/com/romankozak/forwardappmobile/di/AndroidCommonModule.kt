package com.romankozak.forwardappmobile.di

import android.content.Context
import com.romankozak.forwardappmobile.shared.database.*
import me.tatarka.inject.annotations.*

interface AndroidCommonModule : CommonModule {

    @Provides @Singleton
    override fun provideDatabaseDriverFactory(): DatabaseDriverFactory =
        DatabaseDriverFactory()

    @Provides @Singleton
    override fun provideDatabase(factory: DatabaseDriverFactory): ForwardAppDatabase =
        createForwardAppDatabase(factory)
}