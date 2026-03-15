package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode

private data class OptionsMenuLabels(
    val shareListText: String,
    val deleteListText: String,
    val focusToggleText: String,
    val focusToggleIcon: ImageVector,
)

private data class OptionsMenuHandlers(
    val onEditList: () -> Unit,
    val onAddProjectToDayPlan: () -> Unit,
    val onStartTrackingCurrentProject: () -> Unit,
    val onShareList: () -> Unit,
    val onImportFromMarkdown: () -> Unit,
    val onExportToMarkdown: () -> Unit,
    val onImportBacklogFromMarkdown: () -> Unit,
    val onExportBacklogToMarkdown: () -> Unit,
    val onExportProjectState: () -> Unit,
    val onToggleFocusContext: () -> Unit,
    val onSetReminder: () -> Unit,
    val onDeleteList: () -> Unit,
    val onDismiss: () -> Unit,
)

/**
 * Опис елемента меню.
 */

/**
 * Меню опцій проекту (нижній Sheet).
 * Винесено в окрему функцію для чистоти коду.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OptionsMenu(
    state: NavPanelState,
    actions: NavPanelActions,
    contentColor: Color,
) {
    Box {
        IconButton(
            onClick = {
                actions.onMenuExpandedChange(true)
            },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.more_options),
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
        }

        if (state.menuExpanded) {
            val sheetState = rememberModalBottomSheetState()

            ModalBottomSheet(
                onDismissRequest = { actions.onMenuExpandedChange(false) },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                dragHandle = { BottomSheetDefaults.DragHandle() },
            ) {
                OptionsMenuSheetContent(
                    state = state,
                    actions = actions,
                )
            }
        }
    }
}

@Composable
private fun OptionsMenuSheetContent(
    state: NavPanelState,
    actions: NavPanelActions,
) {
    val menuItems = buildMenuItems(state = state, actions = actions)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier =
            Modifier
                .navigationBarsPadding()
                .fillMaxWidth(),
    ) {
        items(menuItems.filter { it.isVisible }) { item ->
            OptionsMenuItem(item)
        }
    }
}

@Composable
private fun buildMenuItems(
    state: NavPanelState,
    actions: NavPanelActions,
): List<MenuItem> {
    val labels = buildOptionsMenuLabels(state)
    val menu = actions.menuActions
    val handlers =
        OptionsMenuHandlers(
            onEditList = { menu.onEditList() },
            onAddProjectToDayPlan = { actions.onAddProjectToDayPlan() },
            onStartTrackingCurrentProject = { menu.onStartTrackingCurrentProject() },
            onShareList = { menu.onShareList() },
            onImportFromMarkdown = { menu.onImportFromMarkdown() },
            onExportToMarkdown = { menu.onExportToMarkdown() },
            onImportBacklogFromMarkdown = { menu.onImportBacklogFromMarkdown() },
            onExportBacklogToMarkdown = { menu.onExportBacklogToMarkdown() },
            onExportProjectState = { menu.onExportProjectState() },
            onToggleFocusContext = { menu.onToggleFocusContext() },
            onSetReminder = { menu.onSetReminder() },
            onDeleteList = { menu.onDeleteList() },
            onDismiss = { actions.onMenuExpandedChange(false) },
        )

    return buildList {
        addAll(buildPrimaryMenuItems(labels = labels, handlers = handlers))
        addAll(
            buildMarkdownMenuItems(
                currentView = state.currentView,
                handlers = handlers,
            ),
        )
        addAll(
            buildSecondaryMenuItems(
                isProjectManagementEnabled = state.isProjectManagementEnabled,
                labels = labels,
                handlers = handlers,
            ),
        )
    }
}

@Composable
private fun buildOptionsMenuLabels(state: NavPanelState): OptionsMenuLabels {
    val shareListText = stringResource(R.string.share_list)
    val deleteListText = stringResource(R.string.delete_list)
    val focusToggleText =
        if (state.isCurrentContextFocused) {
            "Зняти фокус з контексту"
        } else {
            "Додати контекст у фокус"
        }
    val focusToggleIcon =
        if (state.isCurrentContextFocused) {
            Icons.Outlined.VisibilityOff
        } else {
            Icons.Outlined.CenterFocusStrong
        }
    return OptionsMenuLabels(
        shareListText = shareListText,
        deleteListText = deleteListText,
        focusToggleText = focusToggleText,
        focusToggleIcon = focusToggleIcon,
    )
}

private fun buildPrimaryMenuItems(
    labels: OptionsMenuLabels,
    handlers: OptionsMenuHandlers,
): List<MenuItem> =
    listOf(
        MenuItem(
            text = "Properties",
            icon = Icons.Default.Edit,
            onClick = {
                handlers.onEditList()
                handlers.onDismiss()
            },
        ),
        MenuItem(
            text = "Додати до плану на сьогодні",
            icon = Icons.Outlined.EventAvailable,
            onClick = {
                handlers.onAddProjectToDayPlan()
                handlers.onDismiss()
            },
        ),
        MenuItem(
            text = "Start tracking current project",
            icon = Icons.Outlined.PlayCircle,
            onClick = {
                handlers.onStartTrackingCurrentProject()
                handlers.onDismiss()
            },
        ),
        MenuItem(
            text = labels.shareListText,
            icon = Icons.Default.Share,
            onClick = {
                handlers.onShareList()
                handlers.onDismiss()
            },
        ),
    )

private fun buildMarkdownMenuItems(
    currentView: ContextViewMode,
    handlers: OptionsMenuHandlers,
): List<MenuItem> =
    listOf(
        MenuItem(
            text = "Імпортувати з Markdown",
            icon = Icons.Default.Upload,
            onClick = {
                handlers.onImportFromMarkdown()
                handlers.onDismiss()
            },
            isVisible = currentView == ContextViewMode.INBOX,
        ),
        MenuItem(
            text = "Експортувати в Markdown",
            icon = Icons.Default.Download,
            onClick = {
                handlers.onExportToMarkdown()
                handlers.onDismiss()
            },
            isVisible = currentView == ContextViewMode.INBOX,
        ),
        MenuItem(
            text = "Імпортувати беклог з Markdown",
            icon = Icons.Default.Upload,
            onClick = {
                handlers.onImportBacklogFromMarkdown()
                handlers.onDismiss()
            },
            isVisible = currentView == ContextViewMode.BACKLOG,
        ),
        MenuItem(
            text = "Експортувати беклог в Markdown",
            icon = Icons.Default.Download,
            onClick = {
                handlers.onExportBacklogToMarkdown()
                handlers.onDismiss()
            },
            isVisible = currentView == ContextViewMode.BACKLOG,
        ),
    )

private fun buildSecondaryMenuItems(
    isProjectManagementEnabled: Boolean,
    labels: OptionsMenuLabels,
    handlers: OptionsMenuHandlers,
): List<MenuItem> =
    listOf(
        MenuItem(
            text = "Експортувати історію і стан",
            icon = Icons.Outlined.Assessment,
            onClick = {
                handlers.onExportProjectState()
                handlers.onDismiss()
            },
            isVisible = isProjectManagementEnabled,
        ),
        MenuItem(
            text = labels.focusToggleText,
            icon = labels.focusToggleIcon,
            onClick = {
                handlers.onToggleFocusContext()
                handlers.onDismiss()
            },
        ),
        MenuItem(
            text = "Встановити нагадування",
            icon = Icons.Outlined.Alarm,
            onClick = {
                handlers.onSetReminder()
                handlers.onDismiss()
            },
        ),
        MenuItem(
            text = labels.deleteListText,
            icon = Icons.Outlined.Delete,
            onClick = {
                handlers.onDeleteList()
                handlers.onDismiss()
            },
            isDestructive = true,
        ),
    )

@Composable
private fun OptionsMenuItem(item: MenuItem) {
    val itemColor =
        if (item.isDestructive) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Column(
        modifier =
            Modifier
                .clickable { item.onClick() }
                .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.text,
            tint = itemColor,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.text,
            textAlign = TextAlign.Center,
            color = itemColor,
            fontSize = 12.sp,
            lineHeight = 14.sp,
        )
    }
}
