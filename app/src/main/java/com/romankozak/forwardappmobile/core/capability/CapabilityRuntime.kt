package com.romankozak.forwardappmobile.core.capability

import com.romankozak.forwardappmobile.core.capability.navigation.ScreenFactory
import com.romankozak.forwardappmobile.core.capability.navigation.ScreenId
import com.romankozak.forwardappmobile.domain.lifecontext.LifeContextRule

interface CapabilityRuntime {
    fun registerScreen(
        screenId: ScreenId,
        factory: ScreenFactory
    )

    fun registerDi(module: CapabilityDiModule)

    fun registerRule(rule: LifeContextRule)
}
