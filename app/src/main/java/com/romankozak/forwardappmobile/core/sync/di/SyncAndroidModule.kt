package com.romankozak.forwardappmobile.core.sync.di

import com.romankozak.forwardappmobile.core.data.interfaces.sync.IContentProvider
import com.romankozak.forwardappmobile.core.sync.AndroidContentProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    abstract fun bindContentProvider(impl: AndroidContentProvider): IContentProvider
}
