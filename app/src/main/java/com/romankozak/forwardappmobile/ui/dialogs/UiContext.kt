package com.romankozak.forwardappmobile.ui.dialogs

import java.util.UUID

data class UiContext(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tag: String,
    val emoji: String,
    val isReserved: Boolean,
)
