// File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/backlog/components/SublistItemRow.kt
package com.romankozak.forwardappmobile.ui.screens.backlog.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.ui.screens.editlist.TagChip

/**
 * A unified, reusable Composable for displaying a sublist item.
 * It combines advanced layout features like FlowRow with a flexible API.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SublistItemRow(
    sublistContent: ListItemContent.SublistItem,
    isSelected: Boolean,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Flexible endAction slot allows injecting a drag handle or other controls.
    endAction: @Composable () -> Unit = {},
) {
    val sublist = sublistContent.sublist
    val background by animateColorAsState(
        targetValue = when {
            isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = spring(),
        label = "sublist_background_color"
    )

    var isPressed by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 1.dp,
        animationSpec = spring(stiffness = 400f),
        label = "elevation"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = when {
            isHighlighted -> MaterialTheme.colorScheme.tertiary
            isSelected -> MaterialTheme.colorScheme.primary
            else -> Color.Transparent
        },
        animationSpec = tween(200),
        label = "border_color_anim"
    )

    val sublistSemantics = stringResource(R.string.sublist_item_semantics, sublist.name)
    val iconContentDescription = stringResource(id = R.string.sublist_item_icon_description)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .semantics { contentDescription = sublistSemantics },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
        border = BorderStroke(2.dp, animatedBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clickable content area is separated from the endAction area.
            Row(
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(onClick, onLongClick) {
                        detectTapGestures(
                            onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                            onLongPress = { onLongClick() },
                            onTap = { onClick() }
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading icon for visual identification.
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(start = 12.dp, end = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = iconContentDescription,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Column for title and expandable details.
                Column(
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)
                ) {
                    Text(
                        text = sublist.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val hasExtraContent = !sublist.tags.isNullOrEmpty() || !sublist.description.isNullOrBlank()

                    // Details (tags, description) are shown with a gentle animation.
                    AnimatedVisibility(
                        visible = hasExtraContent,
                        enter = slideInVertically(
                            initialOffsetY = { -it / 2 },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        ) + fadeIn(),
                    ) {
                        Column(modifier = Modifier.padding(top = 6.dp)) {
                            if (!sublist.tags.isNullOrEmpty()) {
                                // FlowRow efficiently displays tags, wrapping to new lines as needed.
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    sublist.tags.forEach { tag ->
                                        TagChip(text = "#$tag", onDismiss = {}, isDismissible = false)
                                    }
                                }
                            }
                            if (!sublist.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(if (sublist.tags.isNullOrEmpty()) 0.dp else 4.dp))
                                Text(
                                    text = sublist.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = if (isSelected) Int.MAX_VALUE else 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            // The endAction slot is placed outside the clickable area for clarity.
            Box(modifier = Modifier.align(Alignment.CenterVertically).padding(end = 4.dp)) {
                endAction()
            }
        }
    }
}