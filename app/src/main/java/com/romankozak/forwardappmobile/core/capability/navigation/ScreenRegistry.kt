package com.romankozak.forwardappmobile.core.capability.navigation

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenRegistry @Inject constructor() {

    private val screens = mutableMapOf<ScreenId, ScreenFactory>()

    fun register(id: ScreenId, factory: ScreenFactory) {
        screens[id] = factory
    }

    fun get(id: ScreenId): ScreenFactory? = screens[id]
}
