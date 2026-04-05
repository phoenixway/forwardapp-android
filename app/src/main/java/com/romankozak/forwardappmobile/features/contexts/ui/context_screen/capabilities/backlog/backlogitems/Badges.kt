package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.backlogitems

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.core.data.models.entities.ScoringStatusValues

@Composable
fun EnhancedScoreStatusBadge(
    scoringStatus: String,
    displayScore: Int,
) {
    when (scoringStatus) {
        ScoringStatusValues.ASSESSED -> {
            if (displayScore > 0) {
                val animatedColor by animateColorAsState(
                    targetValue =
                        when {
                            displayScore >= 80 -> MaterialTheme.colorScheme.tertiary
                            displayScore >= 60 -> MaterialTheme.colorScheme.secondary
                            displayScore >= 40 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.error
                        },
                    label = "score_color",
                )

                var isVisible by remember { mutableStateOf(value = false) }

                LaunchedEffect(Unit) {
                    isVisible = true
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter =
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        ) + fadeIn(),
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = animatedColor.copy(alpha = 0.15f),
                        border = BorderStroke(0.6.dp, animatedColor.copy(alpha = 0.3f)),
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Оцінка: $displayScore з 100"
                            }.height(24.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ElectricBolt,
                                    contentDescription = null,
                                    tint = animatedColor,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = "$displayScore/100",
                                    style =
                                        MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.1.sp,
                                        ),
                                    color = animatedColor,
                                )
                            }
                        }
                }
            }
        }
        ScoringStatusValues.IMPOSSIBLE_TO_ASSESS -> {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
                modifier =
                    Modifier
                        .semantics {
                            contentDescription = "Неможливо оцінити"
                        }
                        .height(24.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
        ScoringStatusValues.NOT_ASSESSED -> {
        }
    }
}

@Composable
fun RelativeSizeBadge(relativeSize: Int) {
    if (relativeSize <= 0) return

    val tint = MaterialTheme.colorScheme.secondary
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = tint.copy(alpha = 0.12f),
        border = BorderStroke(0.6.dp, tint.copy(alpha = 0.22f)),
        modifier =
            Modifier.semantics {
                contentDescription = "Відносний розмір: $relativeSize з 5"
            }.height(24.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DiamondBadgeGlyph(
                tint = tint,
                modifier = Modifier.size(8.dp),
            )
            Text(
                text = "$relativeSize/5",
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.1.sp,
                    ),
                color = tint,
            )
        }
    }
}

@Composable
private fun DiamondBadgeGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.rotate(45f),
        shape = RoundedCornerShape(2.dp),
        color = tint.copy(alpha = 0.92f),
        border = BorderStroke(0.5.dp, tint.copy(alpha = 0.7f)),
    ) {}
}
