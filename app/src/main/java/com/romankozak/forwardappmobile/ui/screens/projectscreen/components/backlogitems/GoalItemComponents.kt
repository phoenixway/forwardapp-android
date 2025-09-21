package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ListAlt
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.data.database.models.ScoringStatus

@Composable
fun RelatedLinkChip(
    link: RelatedLink,
    onClick: () -> Unit,
) {
    val icon =
        when (link.type) {
            LinkType.PROJECT -> Icons.Default.ListAlt
            LinkType.URL -> Icons.Default.Link
            LinkType.OBSIDIAN -> Icons.Default.Book
            null -> Icons.Default.BrokenImage // Or any other default icon
            else -> Icons.Default.BrokenImage

        }
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(imageVector = icon, contentDescription = link.type?.name ?: "Link", modifier = Modifier.size(14.dp))
            Text(
                text = link.displayName ?: link.target,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun EnhancedScoreStatusBadge(
    scoringStatus: ScoringStatus,
    displayScore: Int,
) {
    when (scoringStatus) {
        ScoringStatus.ASSESSED -> {
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
        ScoringStatus.IMPOSSIBLE_TO_ASSESS -> {
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
        ScoringStatus.NOT_ASSESSED -> {
        }
    }
}