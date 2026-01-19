package com.romankozak.forwardappmobile.core.capability

import com.romankozak.forwardappmobile.core.navigation.capability.ScreenFactory
import com.romankozak.forwardappmobile.core.navigation.capability.ScreenId
import com.romankozak.forwardappmobile.domain.lifecontext.LifeContextRule

interface CapabilityRuntime {

    fun registerScreen(
        screenId: ScreenId,
        factory: ScreenFactory,
    )

    fun registerRule(
        rule: LifeContextRule,
    )
}
