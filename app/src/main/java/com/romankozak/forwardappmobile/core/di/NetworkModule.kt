package com.romankozak.forwardappmobile.core.di

import android.content.Context
import com.google.gson.Gson
import com.romankozak.forwardappmobile.domain.network.NetworkDiscoveryManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideNetworkDiscoveryManager(
        @ApplicationContext context: Context,
    ): NetworkDiscoveryManager {
        return NetworkDiscoveryManager(context)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}
