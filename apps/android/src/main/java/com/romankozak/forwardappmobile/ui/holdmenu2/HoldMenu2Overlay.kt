package com.romankozak.forwardappmobile.ui.holdmenu2

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun HoldMenu2Overlay(
    controller: HoldMenu2Controller,
    modifier: Modifier = Modifier
) {
    val state = controller.state

    if (!state.isOpen || state.items.isEmpty()) {
        return
    }

    Log.e("HOLDMENU2", "📍 Overlay: rendering popup")

    Box(
        modifier = modifier.background(Color.Black.copy(alpha = 0.4f))
    ) {
        HoldMenu2Popup(state)
    }
}