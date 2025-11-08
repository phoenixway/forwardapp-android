package com.romankozak.forwardappmobile.di

import android.content.Context
import com.google.gson.Gson
import com.romankozak.forwardappmobile.ui.common.IconProvider
import com.romankozak.forwardappmobile.ui.common.RemoteConfigManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UiModule {

    @Provides
    @Singleton
    fun provideRemoteConfigManager(
        @ApplicationContext context: Context,
        gson: Gson
    ): RemoteConfigManager = RemoteConfigManager(context, gson)

    @Provides
    @Singleton
    fun provideIconProvider(
        remoteConfigManager: RemoteConfigManager
    ): IconProvider = IconProvider(remoteConfigManager)
}
