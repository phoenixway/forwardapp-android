package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
import com.romankozak.forwardappmobile.core.theme.InputPanelColors

/**
 * Внутрішня модель для опису елемента меню опцій.
 */
internal data class MenuItem(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val isVisible: Boolean = true,
    val isDestructive: Boolean = false,
)

/**
 * Логіка вибору кольорової схеми панелі залежно від режиму введення.
 */
internal fun getPanelColors(
    mode: InputMode,
    theme: InputPanelColors
): PanelColors {
    val style = when (mode) {
        InputMode.AddGoal -> theme.addGoal
        InputMode.AddQuickRecord -> theme.addQuickRecord
        InputMode.SearchInList -> theme.searchInList
        InputMode.SearchGlobal -> theme.searchGlobal
        InputMode.AddProjectLog, InputMode.AddMilestone -> theme.addProjectLog
    }
    return PanelColors(
        containerColor = style.backgroundColor,
        contentColor = style.textColor,
        accentColor = style.textColor,
        inputFieldColor = style.inputFieldColor
    )
}

/**
 * Нижнє меню (BottomSheet) з додатковими діями для проекту.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OptionsMenu(
    state: NavPanelState,
    actions: NavPanelActions,
    contentColor: Color,
) {
    if (state.menuExpanded) {
        ModalBottomSheet(
            onDismissRequest = { actions.onMenuExpandedChange(false) },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            val menu = actions.menuActions
            
            val menuItems = remember(state.currentView, state.isProjectManagementEnabled) {
                listOf(
                    MenuItem(
                        "Project Properties",
                        Icons.Default.Edit,
                        {
                            menu.onEditList()
                            actions.onMenuExpandedChange(false)
                        },
                    ),
                    MenuItem(
                        "Додати до плану на сьогодні",
                        Icons.Outlined.EventAvailable,
                        {
                            actions.onAddProjectToDayPlan()
                            actions.onMenuExpandedChange(false)
                        },
                    ),
                    MenuItem(
                        "Start tracking current project",
                        Icons.Outlined.PlayCircle,
                        {
                            menu.onStartTrackingCurrentProject()
                            actions.onMenuExpandedChange(false)
                        },
                    ),
                    MenuItem(
                        stringResource(R.string.share_list),
                        Icons.Default.Share,
                        {
                            menu.onShareList()
                            actions.onMenuExpandedChange(false)
                        },
                    ),
                    MenuItem(
                        "Імпортувати з Markdown",
                        Icons.Default.Upload,
                        {
                            menu.onImportFromMarkdown()
                            actions.onMenuExpandedChange(false)
                        },
                        isVisible = state.currentView == ContextViewMode.INBOX,
                    ),
                    MenuItem(
                        "Експортувати в Markdown",
                        Icons.Default.Download,
                        {
                            menu.onExportToMarkdown()
                            actions.onMenuExpandedChange(false)
                        },
                        isVisible = state.currentView == ContextViewMode.INBOX,
                    ),
                    MenuItem(
                        "Імпортувати беклог з Markdown",
                        Icons.Default.Upload,
                        {
                            menu.onImportBacklogFromMarkdown()
                            actions.onMenuExpandedChange(false)
                        },
                        isVisible = state.currentView == ContextViewMode.BACKLOG,
                    ),
                    MenuItem(
                        "Експортувати беклог в Markdown",
                        Icons.Default.Download,
                        {
                            menu.onExportBacklogToMarkdown()
                            actions.onMenuExpandedChange(false)
                        },
                        isVisible = state.currentView == ContextViewMode.BACKLOG,
                    ),
                    MenuItem(
                        "Експортувати історію і стан",
                        Icons.Outlined.Assessment,
                        {
                            menu.onExportProjectState()
                            actions.onMenuExpandedChange(false)
                        },
                        isVisible = state.isProjectManagementEnabled,
                    ),
                    MenuItem(
                        "Встановити нагадування",
                        Icons.Outlined.Alarm,
                        {
                            menu.onSetReminder()
                            actions.onMenuExpandedChange(false)
                        },
                    ),
                    MenuItem(
                        stringResource(R.string.delete_list),
                        Icons.Outlined.Delete,
                        {
                            menu.onDeleteList()
                            actions.onMenuExpandedChange(false)
                        },
                        isDestructive = true,
                    ),
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .navigationBarsPadding()
                    .fillMaxWidth(),
            ) {
                items(menuItems.filter { it.isVisible }) { item ->
                    val color = if (item.isDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    Column(
                        modifier = Modifier
                            .clickable { item.onClick() }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = item.text,
                            tint = color,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            item.text,
                            textAlign = TextAlign.Center,
                            color = color,
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                        )
                    }
                }
            }
        }
    }
}
