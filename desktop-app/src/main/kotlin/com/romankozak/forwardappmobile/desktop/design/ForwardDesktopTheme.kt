package com.romankozak.forwardappmobile.desktop.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ForwardLightColors =
    lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF0E5A5A),
        secondary = androidx.compose.ui.graphics.Color(0xFF8E6C2F),
        tertiary = androidx.compose.ui.graphics.Color(0xFF5E4A7D),
        background = androidx.compose.ui.graphics.Color(0xFFF7F3EC),
        surface = androidx.compose.ui.graphics.Color(0xFFFDFBF7),
    )

private val ForwardDarkColors =
    darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF75D4D4),
        secondary = androidx.compose.ui.graphics.Color(0xFFF4C66E),
        tertiary = androidx.compose.ui.graphics.Color(0xFFD3BAFF),
        background = androidx.compose.ui.graphics.Color(0xFF111716),
        surface = androidx.compose.ui.graphics.Color(0xFF18201F),
    )

@Composable
fun ForwardDesktopTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ForwardLightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
