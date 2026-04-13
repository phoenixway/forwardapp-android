package com.romankozak.forwardappmobile.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.romankozak.forwardappmobile.desktop.app.ForwardDesktopApp

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "ForwardApp Desktop",
        ) {
            ForwardDesktopApp()
        }
    }
