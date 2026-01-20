package com.romankozak.forwardappmobile.features.context_lab.domain

import com.romankozak.forwardappmobile.core.context.*
import com.romankozak.forwardappmobile.core.capability.CapabilitySet
import com.romankozak.forwardappmobile.core.navigation.capability.Navigator
import com.romankozak.forwardappmobile.core.navigation.capability.ViewResolver
import com.romankozak.forwardappmobile.features.context_lab.ContextLabController
import javax.inject.Inject
// File: SwitchContextUseCase.kt

class SwitchContextUseCase @Inject constructor(
    private val labController: ContextLabController,
    private val systemController: ContextController, //
    private val viewResolver: ViewResolver, //
    private val navigator: Navigator // Ваш інтерфейс навігації
) {
    fun execute(contextId: ContextId) {
        // 1. Пошук контексту в лабораторії
        val context = labController.getAllContexts().find { it.id == contextId }
            ?: error("Context with id ${contextId.raw} not found")

        // 2. Створення об'єкта стану [cite: 4, 5]
        val newState = object : ContextState { //
            override val id: ContextId = context.id
            override val features: CapabilitySet = CapabilitySet(
                active = context.config.activeCapabilities
            )
            override val views: ViewSet = ViewSet( //
                available = context.config.activeViews,
                start = context.config.currentView
            )
        }

        // 3. Оновлення глобального контролера
        systemController.update { newState }

        // 4. Синхронізація стану в лабораторії
        labController.activate(contextId)

        // 5. Автоматична навігація на стартовий екран
        val startViewId = newState.views.start
        val screenId = viewResolver.resolve(startViewId) //
        navigator.navigateTo(screenId)
    }
}