package com.romankozak.forwardappmobile.sync.di

import com.romankozak.forwardappmobile.sync.datasource.AttachmentsLocalDataSource
import com.romankozak.forwardappmobile.sync.datasource.MergeLocalDataSource
import com.romankozak.forwardappmobile.sync.local.AttachmentsLocalDataSourceImpl
import com.romankozak.forwardappmobile.sync.local.MergeLocalDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncDataModule {

    @Binds
    @Singleton
    abstract fun bindAttachmentsLocalDataSource(
        impl: AttachmentsLocalDataSourceImpl
    ): AttachmentsLocalDataSource

    @Binds
    @Singleton
    abstract fun bindMergeLocalDataSource(
        impl: MergeLocalDataSourceImpl
    ): MergeLocalDataSource
}
