package com.romankozak.forwardappmobile.di

import android.app.Application
import com.romankozak.forwardappmobile.ui.screens.mainscreen.MainScreenViewModel
import com.romankozak.forwardappmobile.ui.screens.mainscreen.state.DialogStateManager
import com.romankozak.forwardappmobile.ui.screens.mainscreen.state.PlanningModeManager
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@Component
abstract class AppComponent(
    @get:Provides val application: Application,
) {
    val planningModeManager: PlanningModeManager
        @Provides get() = PlanningModeManager()

    val dialogStateManager: DialogStateManager
        @Provides get() = DialogStateManager()

    val mainScreenViewModel: MainScreenViewModel
        @Provides get() = MainScreenViewModel()
}
