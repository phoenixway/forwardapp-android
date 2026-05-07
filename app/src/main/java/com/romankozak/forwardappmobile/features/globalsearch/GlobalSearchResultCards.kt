package com.romankozak.forwardappmobile.features.globalsearch

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import java.util.Locale

internal data class SearchResultCardSpec(
    val presentation: ResultTypePresentation,
    val isSelected: Boolean,
    val query: String,
    val title: String,
    val subtitle: String?,
    val supporting: String?,
    val onClick: () -> Unit,
    val secondaryActionIcon: ImageVector?,
    val secondaryActionDescription: String?,
    val onSecondaryAction: (() -> Unit)?,
)

internal data class ResultTypePresentation(
    val label: String,
    val icon: ImageVector,
    val tone: ResultBadgeTone,
)

internal enum class ResultBadgeTone {
    Primary,
    Secondary,
    Tertiary,
    Surface,
}

@Composable
internal fun SearchResultGroupHeader(
    presentation: ResultTypePresentation,
    count: Int,
    isExpanded: Boolean = true,
    onToggle: (() -> Unit)? = null,
) {
    val (container, content) = resultBadgeColors(presentation.tone)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = onToggle != null) { onToggle?.invoke() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = presentation.icon,
                contentDescription = presentation.label,
                tint = content,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = presentation.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = container,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = content,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (onToggle != null) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Згорнути секцію" else "Розгорнути секцію",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
internal fun UnifiedSearchResultCard(spec: SearchResultCardSpec) {
    val selectedScale by animateFloatAsState(
        targetValue = if (spec.isSelected) 1.01f else 1f,
        animationSpec = tween(durationMillis = 170),
        label = "selected_result_scale",
    )

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .graphicsLayer(scaleX = selectedScale, scaleY = selectedScale),
        onClick = spec.onClick,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (spec.isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
        shape = RoundedCornerShape(16.dp),
        border =
            BorderStroke(
                if (spec.isSelected) 1.35.dp else 1.dp,
                if (spec.isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                },
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            SearchResultTextContent(spec = spec)
            SearchResultSecondaryAction(spec = spec)
        }
    }
}

@Composable
internal fun HighlightedText(
    text: String,
    query: String,
    style: TextStyle,
    color: Color,
    maxLines: Int,
) {
    val annotated =
        remember(text, query) {
            if (query.isBlank()) return@remember buildAnnotatedString { append(text) }
            val loweredText = text.lowercase(Locale.getDefault())
            val loweredQuery = query.lowercase(Locale.getDefault())
            val start = loweredText.indexOf(loweredQuery)
            if (start < 0) {
                buildAnnotatedString { append(text) }
            } else {
                val end = start + query.length
                buildAnnotatedString {
                    append(text.substring(0, start))
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(start, end))
                    }
                    append(text.substring(end))
                }
            }
        }

    Text(
        text = annotated,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun ResultTypeBadge(
    presentation: ResultTypePresentation,
    modifier: Modifier = Modifier,
) {
    val (container, content) = resultBadgeColors(presentation.tone)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = container,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = presentation.icon,
                contentDescription = presentation.label,
                tint = content,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = presentation.label,
                style = MaterialTheme.typography.labelSmall,
                color = content,
            )
        }
    }
}

@Composable
internal fun resultBadgeColors(tone: ResultBadgeTone): Pair<Color, Color> =
    when (tone) {
        ResultBadgeTone.Primary ->
            MaterialTheme.colorScheme.primaryContainer to
                MaterialTheme.colorScheme.onPrimaryContainer
        ResultBadgeTone.Secondary ->
            MaterialTheme.colorScheme.secondaryContainer to
                MaterialTheme.colorScheme.onSecondaryContainer
        ResultBadgeTone.Tertiary ->
            MaterialTheme.colorScheme.tertiaryContainer to
                MaterialTheme.colorScheme.onTertiaryContainer
        ResultBadgeTone.Surface ->
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f) to
                MaterialTheme.colorScheme.onSurfaceVariant
    }

@Composable
internal fun ResultsCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 4.dp,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun SearchResultLeadingBadge(
    presentation: ResultTypePresentation,
    containerColor: Color,
    contentColor: Color,
) {
    Box(
        modifier =
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = presentation.icon,
            contentDescription = presentation.label,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun RowScope.SearchResultTextContent(spec: SearchResultCardSpec) {
    Column(modifier = Modifier.weight(1f)) {
        HighlightedText(
            text = spec.title,
            query = spec.query,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
        )
        if (!spec.subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            HighlightedText(
                text = spec.subtitle,
                query = spec.query,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        if (!spec.supporting.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            HighlightedText(
                text = spec.supporting,
                query = spec.query,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        ResultTypeBadge(
            presentation = spec.presentation,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

@Composable
private fun SearchResultSecondaryAction(spec: SearchResultCardSpec) {
    if (spec.secondaryActionIcon != null && spec.onSecondaryAction != null) {
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = spec.onSecondaryAction,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = spec.secondaryActionIcon,
                contentDescription = spec.secondaryActionDescription ?: "Action",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
