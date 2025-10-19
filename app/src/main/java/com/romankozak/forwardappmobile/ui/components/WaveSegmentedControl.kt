package com.romankozak.forwardappmobile.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

data class SegmentedTab(
    val title: String,
    val icon: ImageVector
)

 @Composable
fun WaveSegmentedControl(
    tabs: List<SegmentedTab>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Сегментований контрол
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTabIndex == index
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.surface
                            else
                                Color.Transparent
                        )
                        .clickable { onTabSelected(index) }
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            modifier = Modifier.size(20.dp),
                            tint = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = expandHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) + fadeIn(),
                            exit = shrinkHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) + fadeOut()
                        ) {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        
        // Анімована хвиля
        val animatedPhase by animateFloatAsState(
            targetValue = selectedTabIndex.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "wave_phase"
        )
        
        val primaryColor = MaterialTheme.colorScheme.primary
        
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            val width = size.width
            val height = size.height
            val baseY = height * 0.5f
            val amplitude = 12f
            val frequency = 0.015f
            
            // Створюємо шлях для хвилі
            val wavePath = Path().apply {
                moveTo(0f, height)
                lineTo(0f, baseY)
                
                // Малюємо синусоїду
                var x = 0f
                while (x <= width) {
                    val wave = sin(x * frequency + animatedPhase * PI.toFloat()) * amplitude
                    lineTo(x, baseY + wave)
                    x += 3f
                }
                
                lineTo(width, height)
                close()
            }
            
            // Малюємо заповнену хвилю
            drawPath(
                path = wavePath,
                color = primaryColor.copy(alpha = 0.12f),
                style = Fill
            )
            
            // Додаємо лінію хвилі зверху (більш насичена)
            val topWavePath = Path().apply {
                var x = 0f
                moveTo(0f, baseY)
                while (x <= width) {
                    val wave = sin(x * frequency + animatedPhase * PI.toFloat()) * amplitude
                    lineTo(x, baseY + wave)
                    x += 3f
                }
            }
            
            drawPath(
                path = topWavePath,
                color = primaryColor.copy(alpha = 0.3f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
            
            // Додаємо "частинки" що рухаються по хвилі
            for (i in 0..4) {
                val particleX = (width / 5f) * i + (animatedPhase * 20f)
                val particleWave = sin(particleX * frequency + animatedPhase * PI.toFloat()) * amplitude
                
                drawCircle(
                    color = primaryColor.copy(alpha = 0.4f),
                    radius = 4f,
                    center = Offset(particleX % width, baseY + particleWave)
                )
            }
        }
        
        // Анімований контент
        AnimatedContent(
            targetState = selectedTabIndex,
            transitionSpec = {
                (fadeIn(animationSpec = tween(400)) +
                    slideInVertically(
                        animationSpec = tween(400),
                        initialOffsetY = { it / 8 }
                    )).togetherWith(
                    fadeOut(animationSpec = tween(200)) +
                        slideOutVertically(
                            animationSpec = tween(200),
                            targetOffsetY = { -it / 8 }
                        )
                )
            },
            label = "content_animation"
        ) { tabIndex ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                content(tabIndex)
            }
        }
    }
}