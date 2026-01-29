package com.romankozak.forwardappmobile.core.di

import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import com.romankozak.forwardappmobile.sync.AttachmentsRepositoryImpl
import com.romankozak.forwardappmobile.sync.SyncApi
import com.romankozak.forwardappmobile.sync.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncInternalModule {
    @Provides
    @Singleton
    fun provideAttachmentsRepository(): AttachmentsRepository = NoOpAttachmentsRepository()

    @Provides
    @Singleton
    fun provideSyncApi(): SyncApi = NoOpSyncRepository()
}