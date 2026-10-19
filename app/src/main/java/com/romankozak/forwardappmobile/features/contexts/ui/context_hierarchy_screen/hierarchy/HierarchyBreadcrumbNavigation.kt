package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.hierarchy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BreadcrumbNavigation(
    breadcrumbs: List<BreadcrumbItem>,
    onNavigate: (BreadcrumbItem) -> Unit,
    onClearNavigation: () -> Unit,
    onFocusedListMenuClick: (String) -> Unit,
    onOpenAsProject: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    showRootWhenEmpty: Boolean = false,
    rootLabel: String = "All",
    wrap: Boolean = false,
) {
    @Suppress("UNUSED_VARIABLE")
    val unusedCallbacks = onFocusedListMenuClick to onOpenAsProject
    if (breadcrumbs.isEmpty() && !showRootWhenEmpty) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        if (wrap) {
            // Keep section titles visible alongside occurrence ancestors.
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                RootBreadcrumb(rootLabel, breadcrumbs.isNotEmpty(), onClearNavigation, padded = true)
                breadcrumbs.forEachIndexed { index, item ->
                    BreadcrumbChip(item, index == breadcrumbs.lastIndex, onNavigate, bounded = true)
                }
            }
        } else {
            val lazyRowState = rememberLazyListState()
            LaunchedEffect(breadcrumbs) {
                // Index zero is root; the last item is the current location.
                if (breadcrumbs.isNotEmpty()) lazyRowState.animateScrollToItem(breadcrumbs.size)
            }
            LazyRow(
                state = lazyRowState,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item { RootBreadcrumb(rootLabel, breadcrumbs.isNotEmpty(), onClearNavigation) }
                itemsIndexed(breadcrumbs) { index, item ->
                    BreadcrumbChip(item, index == breadcrumbs.lastIndex, onNavigate)
                }
            }
        }
    }
}

@Composable
private fun RootBreadcrumb(label: String, hasChildren: Boolean, onClick: () -> Unit, padded: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.clickable(onClick = onClick).padding(vertical = if (padded) 4.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Home, contentDescription = label,
                modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        if (hasChildren) BreadcrumbSeparator()
    }
}

@Composable
private fun BreadcrumbChip(item: BreadcrumbItem, isCurrent: Boolean, onNavigate: (BreadcrumbItem) -> Unit,
    bounded: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = (if (bounded) Modifier.widthIn(max = 240.dp) else Modifier)
                .clickable(enabled = !isCurrent) { onNavigate(item) },
            shape = RoundedCornerShape(16.dp),
            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        ) {
            Text(
                text = item.name,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!isCurrent) BreadcrumbSeparator()
    }
}

@Composable
private fun BreadcrumbSeparator() {
    Icon(Icons.Outlined.ChevronRight, contentDescription = null,
        modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

