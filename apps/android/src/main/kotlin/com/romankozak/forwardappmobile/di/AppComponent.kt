package com.romankozak.forwardappmobile.di

import android.app.Application
import android.content.Context
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope
import com.romankozak.forwardappmobile.features.mainscreen.MainScreenViewModel

@Scope
annotation class AndroidSingleton

@AndroidSingleton
@Component
abstract class AppComponent(
    // Передаємо Application як параметр компонента
    @get:Provides val application: Application,
) : DatabaseModule,
    RepositoryModule,
    DispatcherModule,
    com.romankozak.forwardappmobile.features.mainscreen.di.MainScreenModule,
    com.romankozak.forwardappmobile.shared.features.aichat.di.AiChatModule {

    @Provides
    @ApplicationContext
    fun provideApplicationContext(): Context = application.applicationContext

    // Entry points / factories
    abstract val mainScreenViewModel: MainScreenViewModel

    companion object
}
