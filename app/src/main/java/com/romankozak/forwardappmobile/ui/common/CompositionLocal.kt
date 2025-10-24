package com.romankozak.forwardappmobile.ui.common

import androidx.compose.runtime.compositionLocalOf

val LocalContextUtils = compositionLocalOf<ContextUtils> { error("No ContextUtils provided") }
