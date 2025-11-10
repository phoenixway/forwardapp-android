package com.romankozak.forwardappmobile.di

import android.app.Application
import com.romankozak.forwardappmobile.shared.database.DatabaseDriverFactory
import com.romankozak.forwardappmobile.ui.screens.mainscreen.MainScreenViewModel
import com.romankozak.forwardappmobile.ui.screens.mainscreen.state.DialogStateManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.state.PlanningModeManager
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.di.MainDispatcher
import com.romankozak.forwardappmobile.di.DefaultDispatcher
import com.romankozak.forwardappmobile.di.AndroidCommonModule

@Singleton
@Component
abstract class AppComponent(
    @get:Provides val application: Application,
) : AndroidCommonModule {
    val planningModeManager: PlanningModeManager
        @Provides get() = PlanningModeManager()

    val dialogStateManager: DialogStateManager
        @Provides get() = DialogStateManager()

    val mainScreenViewModel: MainScreenViewModel
        @Provides get() = MainScreenViewModel()

    @Provides
    fun databaseDriverFactory(): DatabaseDriverFactory = DatabaseDriverFactory(application)

    @Provides
    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @MainDispatcher
    fun mainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @DefaultDispatcher
    fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
