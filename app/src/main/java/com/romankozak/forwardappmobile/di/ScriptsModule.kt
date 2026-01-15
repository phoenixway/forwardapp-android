package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.domain.scripts.LuaScriptRunner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScriptsModule {
    @Provides
    @Singleton
    fun provideLuaScriptRunner(): LuaScriptRunner = LuaScriptRunner()
}
