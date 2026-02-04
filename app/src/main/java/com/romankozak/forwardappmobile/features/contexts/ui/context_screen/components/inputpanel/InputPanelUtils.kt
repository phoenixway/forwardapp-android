package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.core.config.FeatureToggles
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.core.theme.LocalInputPanelColors
import com.romankozak.forwardappmobile.domain.ner.ReminderParseResult
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Button
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Controller
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenuItem
import kotlinx.coroutines.delay

// ------------------- STATE ---------------------

internal data class PanelColors(
    val containerColor: Color,
    val contentColor: Color,
    val accentColor: Color,
    val inputFieldColor: Color,
)

data class NavPanelState(
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val menuExpanded: Boolean,
    val currentView: ContextViewMode,
    val isProjectManagementEnabled: Boolean,
    val enableInbox: Boolean,
    val enableLog: Boolean,
    val enableArtifact: Boolean,
    val enableBacklog: Boolean,
    val enableDashboard: Boolean,
    val enableAttachments: Boolean,
    val inputMode: InputMode,
)

data class NavPanelActions(
    val onBackClick: () -> Unit,
    val onForwardClick: () -> Unit,
    val onShowProjectHierarchy: () -> Unit,
    val onNavigateHome: () -> Unit,
    val onRecentsClick: () -> Unit,
    val onCloseSearch: () -> Unit,
    val onViewChange: (ContextViewMode) -> Unit,
    val onInputModeSelected: (InputMode) -> Unit,
    val onMenuExpandedChange: (Boolean) -> Unit,
    val onAddProjectToDayPlan: () -> Unit,
    val menuActions: OptionsMenuActions,
)

data class OptionsMenuActions(
    val onEditList: () -> Unit,
    val onToggleProjectManagement: () -> Unit,
    val onStartTrackingCurrentProject: () -> Unit,
    val onShareList: () -> Unit,
    val onImportFromMarkdown: () -> Unit,
    val onExportToMarkdown: () -> Unit,
    val onImportBacklogFromMarkdown: () -> Unit,
    val onExportBacklogToMarkdown: () -> Unit,
    val onExportProjectState: () -> Unit,
    val onDeleteList: () -> Unit,
    val onSetReminder: () -> Unit,
    val onShowDisplayPropertiesClick: () -> Unit,
)

// ------------------- VIEW TOGGLE ---------------------

@Composable
internal fun ViewModeToggle(
    currentView: ContextViewMode,
    isProjectManagementEnabled: Boolean,
    onViewChange: (ContextViewMode) -> Unit,
    onInputModeSelected: (InputMode) -> Unit,
    contentColor: Color,
    holdMenuController: HoldMenu2Controller,
    enableInbox: Boolean = true,
    enableLog: Boolean = true,
    enableArtifact: Boolean = true,
    enableBacklog: Boolean = true,
    enableDashboard: Boolean = true,
    enableAttachments: Boolean = true,
) {
    val availableViews =
        remember(isProjectManagementEnabled, enableInbox, enableLog, enableArtifact, enableBacklog, enableDashboard, enableAttachments) {
            ContextViewMode.values()
                .filter {
                    when (it) {
                        ContextViewMode.INBOX -> enableInbox
                        ContextViewMode.ADVANCED -> isProjectManagementEnabled && enableLog
                        ContextViewMode.ATTACHMENTS -> enableAttachments
                        ContextViewMode.BACKLOG -> enableBacklog
                        ContextViewMode.DASHBOARD -> enableDashboard
                        else -> true
                    }
                }
                .sortedBy {
                    when (it) {
                        ContextViewMode.DASHBOARD -> 0
                        ContextViewMode.BACKLOG -> 1
                        ContextViewMode.INBOX -> 2
                        ContextViewMode.ADVANCED -> 3
                        ContextViewMode.ATTACHMENTS -> 4
                    }
                }
                .reversed()
        }

    val menuItems =
        remember(availableViews) {
            availableViews.map { viewMode ->
                HoldMenuItem(
                    label =
                        when (viewMode) {
                            ContextViewMode.DASHBOARD -> "Dashboard"
                            ContextViewMode.BACKLOG -> "Backlog"
                            ContextViewMode.INBOX -> "Inbox"
                            ContextViewMode.ADVANCED -> "Advanced"
                            ContextViewMode.ATTACHMENTS -> "Attachments"
                        },
                    icon =
                        when (viewMode) {
                            ContextViewMode.BACKLOG -> Icons.AutoMirrored.Outlined.ListAlt
                            ContextViewMode.INBOX -> Icons.AutoMirrored.Outlined.Notes
                            ContextViewMode.ADVANCED -> Icons.Outlined.Dashboard
                            ContextViewMode.ATTACHMENTS -> Icons.Default.Attachment
                            ContextViewMode.DASHBOARD -> Icons.Outlined.ViewModule
                        },
                )
            }
        }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = contentColor.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.1f)),
    ) {
        Row(modifier = Modifier.height(36.dp), verticalAlignment = Alignment.CenterVertically) {
            HoldMenu2Button(
                items = menuItems,
                controller = holdMenuController,
                longPressDuration = 250,
                onSelect = { index ->
                    val selectedViewMode = availableViews[index]
                    onViewChange(selectedViewMode)
                    val newMode =
                        when (selectedViewMode) {
                            ContextViewMode.INBOX, ContextViewMode.ADVANCED -> InputMode.AddQuickRecord
                            ContextViewMode.DASHBOARD -> InputMode.AddGoal
                            else -> InputMode.AddGoal
                        }
                    onInputModeSelected(newMode)
                },
                modifier = Modifier.size(40.dp).padding(2.dp),
            ) {
                val currentIcon =
                    when (currentView) {
                        ContextViewMode.DASHBOARD -> Icons.Outlined.ViewModule
                        ContextViewMode.BACKLOG -> Icons.AutoMirrored.Outlined.ListAlt
                        ContextViewMode.INBOX -> Icons.AutoMirrored.Outlined.Notes
                        ContextViewMode.ADVANCED -> Icons.Outlined.Dashboard
                        ContextViewMode.ATTACHMENTS -> Icons.Default.Attachment
                    }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = currentIcon,
                        contentDescription = "Change View Mode",
                        modifier = Modifier.size(18.dp),
                        tint = contentColor,
                    )
                }
            }
        }
    }
}

// ------------------- MENU ---------------------

internal data class MenuItem(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val isVisible: Boolean = true,
    val isDestructive: Boolean = false,
)

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
                if (state.inputMode == InputMode.SearchInList) {
                    actions.onCloseSearch()
                } else {
                    actions.onMenuExpandedChange(true)
                }
            },
            modifier = Modifier.size(40.dp),
        ) {
            AnimatedContent(
                targetState = state.inputMode,
                transitionSpec = {
                    (slideInHorizontally { it / 2 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 2 } + fadeOut())
                },
                label = "OptionsMenuIconAnimation",
            ) { mode ->
                val icon =
                    when (mode) {
                        InputMode.SearchInList -> Icons.Default.Close
                        else -> Icons.Default.MoreVert
                    }
                Icon(
                    imageVector = icon,
                    contentDescription =
                        if (mode == InputMode.SearchInList) {
                            "Закрити пошук"
                        } else {
                            stringResource(R.string.more_options)
                        },
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (state.menuExpanded) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { actions.onMenuExpandedChange(false) },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                val menu = actions.menuActions
                val editListText = stringResource(R.string.edit_list)
                val shareListText = stringResource(R.string.share_list)
                val deleteListText = stringResource(R.string.delete_list)

                val menuItems =
                    remember(state.currentView, state.isProjectManagementEnabled) {
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
                    modifier = Modifier.navigationBarsPadding(),
                ) {
                    items(menuItems.filter { it.isVisible }) { item ->
                        val color =
                            if (item.isDestructive) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        Column(
                            modifier = Modifier.clickable { item.onClick() }.padding(8.dp),
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
}