package com.romankozak.forwardappmobile.features.projectscreen.components.backlogitems

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.shared.data.models.LinkType
import com.romankozak.forwardappmobile.shared.features.reminders.domain.model.Reminder
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.database.models.ScoringStatusValues
import kotlinx.coroutines.delay


@Composable
fun EnhancedScoreStatusBadge(
    scoringStatus: String,
    displayScore: Long,
) {
    when (scoringStatus) {
        ScoringStatusValues.ASSESSED -> {
            if (displayScore > 0) {
                val animatedColor by animateColorAsState(
                    targetValue =
                    when {
                        displayScore >= 80 -> Color(0xFF4CAF50)
                        displayScore >= 60 -> Color(0xFFFF9800)
                        displayScore >= 40 -> Color(0xFFFFEB3B)
                        else -> Color(0xFFE91E63)
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
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = animatedColor,
                                modifier = Modifier.size(10.dp),
                            )
                            Text(
                                text = "$displayScore/100",
                                style =
                                MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp,
                                    fontSize = 10.sp,
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
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier =
                Modifier
                    .semantics {
                        contentDescription = "Неможливо оцінити"
                    },
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                    Modifier
                        .size(16.dp)
                        .padding(3.dp),
                )
            }
        }
        ScoringStatusValues.NOT_ASSESSED -> {
        }
    }
}
