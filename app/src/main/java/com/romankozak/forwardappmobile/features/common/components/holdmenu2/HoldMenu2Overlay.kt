package com.romankozak.forwardappmobile.features.common.components.holdmenu2

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
@Composable
fun HoldMenu2Overlay(
    controller: HoldMenu2Controller,
    modifier: Modifier = Modifier
) {
    val state = controller.state

    AnimatedVisibility(
        visible = state.isOpen && state.items.isNotEmpty(),
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(100)),
    ) {
        Log.e("HOLDMENU2", "📍 Overlay: rendering popup")

        Box(modifier = modifier.fillMaxSize()) {

            // --- BACKDROP (blur only background, not popup) ---
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f))
                    .blur(18.dp)
            )

            // --- POPUP ON TOP ---
            HoldMenu2Popup(state)
        }
    }
}
