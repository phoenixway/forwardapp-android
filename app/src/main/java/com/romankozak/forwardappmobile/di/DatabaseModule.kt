package com.romankozak.forwardappmobile.di

import android.content.Context
import com.romankozak.forwardappmobile.shared.database.DatabaseDriverFactory
import com.romankozak.forwardappmobile.shared.database.createForwardAppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideForwardAppDatabase(
        @ApplicationContext context: Context
    ): ForwardAppDatabase {
        return createForwardAppDatabase(DatabaseDriverFactory(context))
    }
}
