package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode

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
                if (state.inputMode == InputMode.SearchInList || state.inputMode == InputMode.SearchGlobal) {
                    actions.onCloseSearch()
                } else {
                    actions.onMenuExpandedChange(true)
                }
            },
            modifier = Modifier.size(40.dp),
        ) {
            val isSearchMode = state.inputMode == InputMode.SearchInList || state.inputMode == InputMode.SearchGlobal
            Icon(
                imageVector = if (isSearchMode) Icons.Default.Close else Icons.Default.MoreVert,
                contentDescription = if (isSearchMode) "Закрити пошук" else stringResource(R.string.more_options),
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
        }

        if (state.menuExpanded) {
            val sheetState = rememberModalBottomSheetState()

            val shareListText = stringResource(R.string.share_list)
            val deleteListText = stringResource(R.string.delete_list)
            val editListText = stringResource(R.string.edit_list)

            ModalBottomSheet(
                onDismissRequest = { actions.onMenuExpandedChange(false) },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                dragHandle = { BottomSheetDefaults.DragHandle() },
            ) {
                val menu = actions.menuActions

                val menuItems =
                    remember(state.currentView, state.isProjectManagementEnabled, state.activeCapabilities) {
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
                                shareListText,
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
                                if (state.isCurrentContextFocused) "Зняти фокус з контексту" else "Додати контекст у фокус",
                                if (state.isCurrentContextFocused) Icons.Outlined.VisibilityOff else Icons.Outlined.CenterFocusStrong,
                                {
                                    menu.onToggleFocusContext()
                                    actions.onMenuExpandedChange(false)
                                },
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
                                deleteListText,
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
                    modifier =
                        Modifier
                            .navigationBarsPadding()
                            .fillMaxWidth(),
                ) {
                    items(menuItems.filter { it.isVisible }) { item ->
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
                }
            }
        }
    }
}
