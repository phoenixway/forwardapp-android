package com.romankozak.forwardappmobile.features.context_lab.domain

import com.romankozak.forwardappmobile.core.context.*
import com.romankozak.forwardappmobile.core.capability.CapabilitySet
import com.romankozak.forwardappmobile.core.navigation.capability.Navigator
import com.romankozak.forwardappmobile.core.navigation.capability.ViewResolver
import com.romankozak.forwardappmobile.features.context_lab.ContextLabController
import javax.inject.Inject
import android.util.Log

private const val TAG = "SwitchContextUseCase"

class SwitchContextUseCase @Inject constructor(
    private val labController: ContextLabController,
    private val systemController: ContextController, //
    private val viewResolver: ViewResolver, //
    private val navigator: Navigator // Ваш інтерфейс навігації
) {
    fun execute(contextId: ContextId) {
        try {
            Log.d(TAG, "Executing context switch for contextId: ${contextId.raw}")

            // 1. Пошук контексту в лабораторії
            val context = labController.getAllContexts().find { it.id == contextId }
                ?: error("Context with id ${contextId.raw} not found")
            Log.d(TAG, "Found context: ${context.role.label}")

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
            Log.d(TAG, "Updated systemController with new state")

            // 4. Синхронізація стану в лабораторії
            labController.activate(contextId)
            Log.d(TAG, "Activated context in labController")

            // 5. Автоматична навігація на стартовий екран
            val startViewId = newState.views.start
            Log.d(TAG, "Start viewId: ${startViewId.raw}")

            val screenId = viewResolver.resolve(startViewId)
            Log.d(TAG, "Resolved screenId: ${screenId.raw}")

            navigator.navigateTo(screenId)
            Log.d(TAG, "Successfully navigated to screenId: ${screenId.raw}")
        } catch (e: Exception) {
            Log.e(TAG, "Error during context switch for contextId: ${contextId.raw}", e)
            throw e // Re-throw the exception to ensure the app still crashes as before
        }
    }
}