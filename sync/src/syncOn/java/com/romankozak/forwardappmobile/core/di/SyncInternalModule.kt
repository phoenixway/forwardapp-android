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
abstract class SyncInternalModule {
    @Binds
    @Singleton
    abstract fun bindAttachmentsRepository(impl: AttachmentsRepositoryImpl): AttachmentsRepository

    @Binds
    @Singleton
    abstract fun bindSyncApi(impl: SyncRepository): SyncApi
}