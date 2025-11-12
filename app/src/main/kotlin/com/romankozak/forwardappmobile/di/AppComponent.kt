package com.romankozak.forwardappmobile.di

import android.app.Application
import android.content.Context
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import com.romankozak.forwardappmobile.di.AndroidSingleton

@AndroidSingleton
@Component(AndroidCommonModule::class)
abstract class AppComponent {

    abstract val application: Application

    @Provides
    @ApplicationContext
    fun provideApplicationContext(): Context = application.applicationContext

    companion object
}
