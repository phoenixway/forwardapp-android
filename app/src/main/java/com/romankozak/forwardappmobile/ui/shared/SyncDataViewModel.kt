package com.romankozak.forwardappmobile.ui.shared

import androidx.lifecycle.ViewModel

/**
 * Цей ViewModel використовується для передачі даних (у вигляді JSON-рядка)
 * між GoalListScreen та SyncScreen.
 *
 * Він працює як тимчасове сховище. GoalListViewModel записує сюди дані,
 * а SyncViewModel їх читає після навігації.
 */
class SyncDataViewModel : ViewModel() {
    // Тепер це звичайна властивість, а не статична.
    var jsonString: String? = null
}