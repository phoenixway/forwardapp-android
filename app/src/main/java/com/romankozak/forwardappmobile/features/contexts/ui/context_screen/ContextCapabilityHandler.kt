
package com.romankozak.forwardappmobile.features.contexts.ui.context_screen

import com.romankozak.forwardappmobile.core.capability.CapabilityId

/**
 * Обробник можливостей контексту
 * Відповідає за додаткову логіку роботи з capabilities
 */
class ContextCapabilityHandler {

    /**
     * Перевіряє чи дві можливості конфліктують між собою
     */
    fun areConflicting(cap1: CapabilityId, cap2: CapabilityId): Boolean {
        // Приклад: деякі можливості можуть бути взаємовиключними
        return when {
            cap1.value == "simple_view" && cap2.value == "advanced_view" -> true
            cap1.value == "readonly" && cap2.value == "editable" -> true
            else -> false
        }
    }

    /**
     * Повертає залежності для певної можливості
     */
    fun getDependencies(capabilityId: CapabilityId): Set<CapabilityId> {
        return when (capabilityId.value) {
            "advanced" -> setOf(CapabilityId("backlog"))
            "auto_link_subprojects" -> setOf(CapabilityId("backlog"))
            else -> emptySet()
        }
    }

    /**
     * Перевіряє чи всі залежності виконані
     */
    fun areDependenciesMet(
        capabilityId: CapabilityId,
        enabledCapabilities: Set<CapabilityId>
    ): Boolean {
        val dependencies = getDependencies(capabilityId)
        return dependencies.all { it in enabledCapabilities }
    }

    /**
     * Повертає опис можливості
     */
    fun getDescription(capabilityId: CapabilityId): String {
        return when (capabilityId.value) {
            "inbox" -> "Вхідні повідомлення та швидкі нотатки"
            "backlog" -> "Управління задачами та списками"
            "log" -> "Журнал подій та прогрес"
            "artifact" -> "Артефакти проєкту"
            "dashboard" -> "Дашборд з метриками"
            "attachments" -> "Вкладення та файли"
            "advanced" -> "Розширені можливості"
            "auto_link_subprojects" -> "Автоматичне зв'язування підпроєктів"
            else -> "Невідома можливість"
        }
    }

    /**
     * Повертає пріоритет можливості для відображення
     */
    fun getPriority(capabilityId: CapabilityId): Int {
        return when (capabilityId.value) {
            "backlog" -> 1
            "inbox" -> 2
            "log" -> 3
            "artifact" -> 4
            "dashboard" -> 5
            "attachments" -> 6
            "advanced" -> 7
            else -> 99
        }
    }
}
