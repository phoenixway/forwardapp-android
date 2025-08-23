package com.romankozak.forwardappmobile.ui.shared

import androidx.lifecycle.ViewModel

/**
 * Ця ViewModel слугує як надійна шина для передачі одноразових результатів між екранами.
 * Вона живе довше за окремі екрани, тому надійно зберігає результат.
 */
class NavigationResultViewModel : ViewModel() {

    private val results = mutableMapOf<String, Any?>()

    /**
     * Встановлює результат за певним ключем.
     */
    fun <T> setResult(key: String, value: T) {
        results[key] = value
    }

    /**
     * Отримує результат за ключем і одразу ж його "споживає" (видаляє),
     * щоб він не був оброблений повторно.
     */
    fun <T> consumeResult(key: String): T? {
        // Ми використовуємо .remove() щоб одночасно отримати і видалити значення
        @Suppress("UNCHECKED_CAST")
        return results.remove(key) as? T
    }
}