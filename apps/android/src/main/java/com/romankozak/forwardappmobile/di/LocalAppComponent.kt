package com.romankozak.forwardappmobile.di

import androidx.compose.runtime.compositionLocalOf

val LocalAppComponent = compositionLocalOf<AppComponent> { error("App component not provided") }
