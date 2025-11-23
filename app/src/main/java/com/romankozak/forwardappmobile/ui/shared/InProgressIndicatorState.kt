package com.romankozak.forwardappmobile.ui.shared

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class InProgressIndicatorState(isInitiallyExpanded: Boolean = true) {
    var isExpanded by mutableStateOf(isInitiallyExpanded)
}