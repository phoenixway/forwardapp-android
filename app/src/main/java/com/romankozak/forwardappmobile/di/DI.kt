package com.romankozak.forwardappmobile.di

import android.content.Context
import com.romankozak.forwardappmobile.shared.di.AndroidCommonModule
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope
import com.romankozak.forwardappmobile.shared.di.AppScope
import com.romankozak.forwardappmobile.shared.di.ApplicationContext
import com.romankozak.forwardappmobile.shared.di.Singleton

@AppScope
@Component
abstract class ApplicationComponent(
    @get:Provides @get:ApplicationContext val context: Context,
) : AndroidCommonModule {
    companion object
}
