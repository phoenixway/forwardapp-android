package com.romankozak.forwardappmobile.core.navigation.capability

import com.romankozak.forwardappmobile.core.navigation.NavigationDispatcher
import javax.inject.Inject

class NavigationDispatcherNavigator
    @Inject
    constructor(
        private val dispatcher: NavigationDispatcher,
    ) : Navigator {
        override fun navigateTo(screenId: ScreenId) {
            dispatcher.navigate(screenId.raw)
        }
    }
