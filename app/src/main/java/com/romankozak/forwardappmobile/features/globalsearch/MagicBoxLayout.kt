package com.romankozak.forwardappmobile.features.globalsearch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
internal fun MagicBoxLayout(
    palette: OmniboxModePalette,
    content: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    scrollToTop: @Composable () -> Unit = {},
) {
    Box(
        Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MaterialTheme.colorScheme.surface, palette.screenBottomTint),
                    startY = 0.1f,
                ),
            )
            .then(modifier)
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
    ) {
        Box(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(164.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                            palette.contentSurface.copy(alpha = 0.38f),
                            palette.searchSurface.copy(alpha = 0.88f),
                        ),
                    ),
                ),
        )
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Measure the bottom bar first; history and results share the remaining height.
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                content()
                Box(Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
                    scrollToTop()
                }
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                bottomBar()
            }
        }
    }
}

@Composable
internal fun MagicBoxResultsSurface(
    palette: OmniboxModePalette,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize().padding(top = 6.dp),
        shape = RoundedCornerShape(28.dp),
        color = palette.contentSurface.copy(alpha = 0.96f),
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, palette.panelOutline.copy(alpha = 0.10f)),
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
            content()
        }
    }
}
