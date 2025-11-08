package com.romankozak.forwardappmobile.di

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
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
        val driver = AndroidSqliteDriver(
            schema = ForwardAppDatabase.Schema,
            context = context,
            name = "forwardapp.db"
        )
        return ForwardAppDatabase(driver)
    }
}
