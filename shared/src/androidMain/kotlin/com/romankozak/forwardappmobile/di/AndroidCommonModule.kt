package com.romankozak.forwardappmobile.di

import android.content.Context
import com.romankozak.forwardappmobile.shared.database.*
import com.romankozak.forwardappmobile.shared.core.data.database.DatabaseDriverFactory
import me.tatarka.inject.annotations.Provides

interface AndroidCommonModule : CommonModule {

    @Provides
    fun provideDatabaseDriverFactory(
        @ApplicationContext context: Context
    ): DatabaseDriverFactory = DatabaseDriverFactory(context)

    @Provides
    override fun provideDatabase(
        factory: DatabaseDriverFactory
    ): ForwardAppDatabase = createForwardAppDatabase(factory.createDriver())
}
