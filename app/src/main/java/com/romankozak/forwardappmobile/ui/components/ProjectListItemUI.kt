


package com.romankozak.forwardappmobile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.shared.data.database.models.Project

@Composable
fun ProjectRow(
    list: Project,
    level: Int,
    hasChildren: Boolean,
    onListClick: (String) -> Unit,
    onToggleExpanded: (list: Project) -> Unit,
    onMenuRequested: (list: Project) -> Unit,
    isCurrentlyDragging: Boolean,
    isHovered: Boolean,
    isDraggingDown: Boolean,
    isHighlighted: Boolean,
    showFocusButton: Boolean,
    onFocusRequested: (list: Project) -> Unit,
    modifier: Modifier = Modifier,
    displayName: AnnotatedString? = null,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else Color.Transparent,
        animationSpec = tween(durationMillis = 500),
        label = "Highlight Animation",
    )

    val indentation = (level * 24).dp

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(backgroundColor),
    ) {
        if (isHovered && !isDraggingDown && !isCurrentlyDragging) {
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = indentation))
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onListClick(list.id) }
                    .alpha(if (isCurrentlyDragging) 0.6f else 1f)
                    .padding(start = indentation)
                    .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (hasChildren) {
                    IconButton(onClick = { onToggleExpanded(list) }) {
                        Icon(
                            imageVector = if (list.isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = "Згорнути/Розгорнути",
                        )
                    }
                }
            }

            
            Box(modifier = Modifier.weight(1f)) {
                var textDidOverflow by remember(displayName) { mutableStateOf(false) }

                if (textDidOverflow) {
                    
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Назва задовга: ${displayName?.text ?: list.name}",
                        modifier = Modifier.padding(start = 4.dp),
                    )
                } else {
                    
                    Text(
                        text = displayName ?: AnnotatedString(list.name),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        style = MaterialTheme.typography.bodyLarge,
                        onTextLayout = { textLayoutResult ->
                            
                            textDidOverflow = textLayoutResult.didOverflowWidth
                        },
                    )
                }
            }
            

            AnimatedVisibility(visible = showFocusButton, enter = fadeIn(), exit = fadeOut()) {
                IconButton(onClick = { onFocusRequested(list) }) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = "Сфокусуватися",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            IconButton(onClick = { onMenuRequested(list) }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Дії зі списком")
            }
        }

        if (isHovered && isDraggingDown && !isCurrentlyDragging) {
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = indentation))
        }
    }
}
