package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.shared.database.*
import com.romankozak.forwardappmobile.shared.core.data.database.DatabaseDriverFactory

interface CommonModule {
    fun provideDatabaseDriverFactory(): DatabaseDriverFactory
    fun provideDatabase(factory: DatabaseDriverFactory): ForwardAppDatabase
}
