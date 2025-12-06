package com.romankozak.forwardappmobile.ui.components.header

import androidx.compose.runtime.Composable

data class FAHeaderConfig(
    val left: ( @Composable () -> Unit)? = null,
    val center: ( @Composable () -> Unit)? = null, // Added center slot
    val right: ( @Composable () -> Unit)? = null,
    val backgroundStyle: FAHeaderBackground = FAHeaderBackground.Default
)

enum class FAHeaderBackground {
    Default,
    Transparent,
    Elevated,
    Gradient,
    CommandDeck
}
