package com.romankozak.forwardappmobile.features.projectscreen.components.inputpanel

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.features.projectscreen.models.ProjectViewMode
import com.romankozak.forwardappmobile.ui.holdmenu.HoldMenuButton
import com.romankozak.forwardappmobile.ui.holdmenu.HoldMenuState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import com.romankozak.forwardappmobile.features.projectscreen.ProjectScreenViewModel
import com.romankozak.forwardappmobile.ui.holdmenu.HoldMenuOverlay
import com.romankozak.forwardappmobile.ui.holdmenu2.HoldMenu2Button
import com.romankozak.forwardappmobile.ui.holdmenu2.HoldMenu2Controller

// TODO: Restore from theme
object LocalInputPanelColors {
    val current: InputPanelColors = darkInputPanelColors()
}

fun darkInputPanelColors(): InputPanelColors {
    return InputPanelColors(
        addGoal = PanelTheme(
            backgroundColor = Color(0xFF2E2E2E),
            textColor = Color(0xFFE0E0E0),
            inputFieldColor = Color(0xFF3C3C3C)
        ),
        addQuickRecord = PanelTheme(
            backgroundColor = Color(0xFF2E3B4E),
            textColor = Color(0xFFD0D8E8),
            inputFieldColor = Color(0xFF3A475A)
        ),
        searchInList = PanelTheme(
            backgroundColor = Color(0xFF4E3A2E),
            textColor = Color(0xFFE8D8D0),
            inputFieldColor = Color(0xFF5A463A)
        ),
        searchGlobal = PanelTheme(
            backgroundColor = Color(0xFF4E2E4E),
            textColor = Color(0xFFE8D0E8),
            inputFieldColor = Color(0xFF5A3A5A)
        ),
        addProjectLog = PanelTheme(
            backgroundColor = Color(0xFF2E4E3A),
            textColor = Color(0xFFD0E8D8),
            inputFieldColor = Color(0xFF3A5A46)
        )
    )
}

data class InputPanelColors(
    val addGoal: PanelTheme,
    val addQuickRecord: PanelTheme,
    val searchInList: PanelTheme,
    val searchGlobal: PanelTheme,
    val addProjectLog: PanelTheme
)

data class PanelTheme(
    val backgroundColor: Color,
    val textColor: Color,
    val inputFieldColor: Color
)


// ------------------- STATE ---------------------

private data class PanelColors(
  val containerColor: Color,
  val contentColor: Color,
  val accentColor: Color,
  val inputFieldColor: Color,
)

data class NavPanelState(
  val canGoBack: Boolean,
  val canGoForward: Boolean,
  val menuExpanded: Boolean,
  val currentView: ProjectViewMode,
  val isProjectManagementEnabled: Boolean,
  val inputMode: InputMode,
  val isViewModePanelVisible: Boolean,
)

data class NavPanelActions(
  val onBackClick: () -> Unit,
  val onForwardClick: () -> Unit,
  val onHomeClick: () -> Unit,
  val onRecentsClick: () -> Unit,
  val onRevealInExplorer: () -> Unit,
  val onCloseSearch: () -> Unit,
  val onViewChange: (ProjectViewMode) -> Unit,
  val onInputModeSelected: (InputMode) -> Unit,
  val onMenuExpandedChange: (Boolean) -> Unit,
  val onAddProjectToDayPlan: () -> Unit,
  val onToggleNavPanelMode: () -> Unit,
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
private fun ViewModeToggle(

    currentView: ProjectViewMode,
    isProjectManagementEnabled: Boolean,
    onViewChange: (ProjectViewMode) -> Unit,
    onInputModeSelected: (InputMode) -> Unit,
    contentColor: Color,
    onToggleNavPanelMode: () -> Unit,
    holdMenuState: MutableState<HoldMenuState>,
) {
    val availableViews =
        ProjectViewMode.values().filter {
            it != ProjectViewMode.Advanced || isProjectManagementEnabled
        }

    /*HoldMenuButtonTest(Icons.Default.MoreVert) {
        item("Edit", Icons.Default.Edit) {}
        item("Delete", Icons.Default.Delete) {}
    }*/


    /*HoldMenuButton(
        icon = when (currentView) {
            ProjectViewMode.Backlog -> Icons.Outlined.ListAlt
            ProjectViewMode.Inbox -> Icons.Outlined.Notes
            ProjectViewMode.Advanced -> Icons.Outlined.Dashboard
            ProjectViewMode.Attachments -> Icons.Default.Attachment
        },
        state = holdMenuState
    ) {
        availableViews.forEach { viewMode ->
            item(
                label = viewMode.name,
                icon = when (viewMode) {
                    ProjectViewMode.Backlog -> Icons.Outlined.ListAlt
                    ProjectViewMode.Inbox -> Icons.Outlined.Notes
                    ProjectViewMode.Advanced -> Icons.Outlined.Dashboard
                    ProjectViewMode.Attachments -> Icons.Default.Attachment
                }
            ) {
                onViewChange(viewMode)
                val newMode =
                    when (viewMode) {
                        ProjectViewMode.Inbox -> InputMode.AddQuickRecord
                        ProjectViewMode.Advanced -> InputMode.AddQuickRecord
                        else -> InputMode.AddGoal
                    }
                onInputModeSelected(newMode)
            }
        }
    }*/

    /*HoldMenuButton(
        onLongPress = { anchor, touch ->
            holdMenuState.value = holdMenuState.value.copy(
                isOpen = true,
                anchor = anchor,
                touch = touch,     // ← ТЕПЕР Є В STATE
                selectedIndex = 0
            )
        }
    ) {
        Icon(Icons.Default.MoreVert, contentDescription = null)
    }*/



}

// ------------------- MENU ---------------------

private data class MenuItem(
  val text: String,
  val icon: ImageVector,
  val onClick: () -> Unit,
  val isVisible: Boolean = true,
  val isDestructive: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionsMenu(state: NavPanelState, actions: NavPanelActions, contentColor: Color) {
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
              "More options"
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
        val editListText = "Edit list"
        val shareListText = "Share list"
        val deleteListText = "Delete list"

        val menuItems =
          remember(state.currentView, state.isProjectManagementEnabled) {
            listOf(
              MenuItem(
                editListText,
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
                "Toggle realization support",
                Icons.Outlined.Construction,
                {
                  menu.onToggleProjectManagement()
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
                isVisible = state.currentView == ProjectViewMode.Inbox,
              ),
              MenuItem(
                "Експортувати в Markdown",
                Icons.Default.Download,
                {
                  menu.onExportToMarkdown()
                  actions.onMenuExpandedChange(false)
                },
                isVisible = state.currentView == ProjectViewMode.Inbox,
              ),
              MenuItem(
                "Імпортувати беклог з Markdown",
                Icons.Default.Upload,
                {
                  menu.onImportBacklogFromMarkdown()
                  actions.onMenuExpandedChange(false)
                },
                isVisible = state.currentView == ProjectViewMode.Backlog,
              ),
              MenuItem(
                "Експортувати беклог в Markdown",
                Icons.Default.Download,
                {
                  menu.onExportBacklogToMarkdown()
                  actions.onMenuExpandedChange(false)
                },
                isVisible = state.currentView == ProjectViewMode.Backlog,
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
              if (item.isDestructive) MaterialTheme.colorScheme.error
              else MaterialTheme.colorScheme.onSurface
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

// ------------------- BACK/FORWARD ---------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BackForwardButton(state: NavPanelState, actions: NavPanelActions, contentColor: Color) {
  val shouldShowButton =
    state.inputMode != InputMode.SearchInList && (state.canGoBack || state.canGoForward)

  AnimatedVisibility(visible = shouldShowButton) {
    val haptic = LocalHapticFeedback.current
    var showForwardIcon by remember { mutableStateOf(false) }

    LaunchedEffect(showForwardIcon) {
      if (showForwardIcon) {
        delay(400L)
        showForwardIcon = false
      }
    }

    Box(
      modifier =
        Modifier.size(40.dp)
          .clip(CircleShape)
          .combinedClickable(
            enabled = state.canGoBack || state.canGoForward,
            onClick = { if (state.canGoBack) actions.onBackClick() },
            onLongClick = {
              if (state.canGoForward) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showForwardIcon = true
                actions.onForwardClick()
              }
            },
            indication = ripple(bounded = false),
            interactionSource = remember { MutableInteractionSource() },
          ),
      contentAlignment = Alignment.Center,
    ) {
      BackForwardIcon(state = state, showForwardIcon = showForwardIcon, contentColor = contentColor)

      if (state.canGoForward && !showForwardIcon) {
        AnimatedVisibility(
          visible = true,
          modifier = Modifier.align(Alignment.BottomEnd),
          enter = fadeIn() + scaleIn(),
          exit = fadeOut() + scaleOut(),
        ) {
          Box(
            modifier =
              Modifier.padding(4.dp)
                .size(6.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .border(width = 1.dp, color = contentColor.copy(alpha = 0.5f), shape = CircleShape)
          )
        }
      }
    }
  }
}

@Composable
private fun BackForwardIcon(state: NavPanelState, showForwardIcon: Boolean, contentColor: Color) {
  val iconColor by
    animateColorAsState(
      targetValue = if (state.canGoBack) contentColor else contentColor.copy(alpha = 0.3f),
      label = "backIconColor",
    )
  val iconScale by
    animateFloatAsState(targetValue = if (state.canGoBack) 1.2f else 1.0f, label = "backIconScale")

  AnimatedContent(
    targetState = showForwardIcon,
    transitionSpec = {
      (slideInHorizontally { it / 2 } + fadeIn()) togetherWith
        (slideOutHorizontally { -it / 2 } + fadeOut())
    },
    label = "BackForwardIconAnimation",
  ) { isForward ->
    Icon(
      imageVector =
        if (isForward) {
          Icons.AutoMirrored.Filled.ArrowForward
        } else {
          Icons.AutoMirrored.Filled.ArrowBack
        },
      contentDescription = "Назад (довге натискання - Вперед)",
      modifier = Modifier.size(20.dp).scale(if (isForward) 1.2f else iconScale),
      tint = if (isForward) MaterialTheme.colorScheme.primary else iconColor,
    )
  }
}

// ------------------- NAV BAR ---------------------

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
private fun NavigationBar(
    state: NavPanelState,
    actions: NavPanelActions,
    contentColor: Color,
    modifier: Modifier = Modifier,
    holdMenuState: MutableState<HoldMenuState>,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        val baseWidth = if (state.isProjectManagementEnabled) 380.dp else 320.dp
        val showReveal = !state.isViewModePanelVisible || availableWidth > baseWidth
        val showRecents = !state.isViewModePanelVisible || availableWidth > (baseWidth - 40.dp)

        Row(
            modifier = Modifier.heightIn(min = 52.dp).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // --- LEFT SIDE ---
            BackForwardButton(state, actions, contentColor)

            IconButton(onClick = actions.onHomeClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.Home,
                    "Дім",
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }

            Row {
                AnimatedVisibility(visible = showReveal) {
                    IconButton(onClick = actions.onRevealInExplorer, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Outlined.RemoveRedEye,
                            "Показати у списку",
                            tint = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                AnimatedVisibility(visible = showRecents) {
                    IconButton(onClick = actions.onRecentsClick, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Outlined.Restore,
                            "Недавні",
                            tint = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // --- RIGHT SIDE ---
            AnimatedContent(
                targetState = state.isViewModePanelVisible,
                transitionSpec = {
                    (slideInHorizontally { it / 2 } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 2 } + fadeOut())
                },
                label = "NavBarRightAnimation",
            ) { isViewMode ->
                if (isViewMode) {
                    ViewModeToggle(
                        currentView = state.currentView,
                        isProjectManagementEnabled = state.isProjectManagementEnabled,
                        onViewChange = actions.onViewChange,
                        onInputModeSelected = actions.onInputModeSelected,
                        contentColor = contentColor,
                        onToggleNavPanelMode = actions.onToggleNavPanelMode,
                        holdMenuState = holdMenuState,
                    )
                } else {
                    IconButton(onClick = actions.onToggleNavPanelMode, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowLeft,
                            contentDescription = "Перемкнути панель",
                            tint = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            OptionsMenu(state = state, actions = actions, contentColor = contentColor)
        }
    }
}

// ------------------- NER INDICATOR ---------------------

@Composable
private fun NerIndicator(isActive: Boolean, hasText: Boolean, modifier: Modifier = Modifier) {
  AnimatedVisibility(
    visible = isActive && hasText,
    modifier = modifier,
    enter = fadeIn() + scaleIn(),
    exit = fadeOut() + scaleOut(),
  ) {
    val infiniteTransition = rememberInfiniteTransition(label = "ner_indicator_transition")
    val scale by
      infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
          ),
        label = "ner_indicator_scale",
      )
    val alpha by
      infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse,
          ),
        label = "ner_indicator_alpha",
      )

    Icon(
      imageVector = Icons.Default.AutoAwesome,
      contentDescription = "Smart recognition active",
      tint = MaterialTheme.colorScheme.tertiary.copy(alpha = alpha),
      modifier = Modifier.size(18.dp).scale(scale),
    )
  }
}




@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun ModernInputPanel(
    modifier: Modifier = Modifier,
    inputValue: TextFieldValue,
    inputMode: InputMode,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    onInputModeSelected: (InputMode) -> Unit,
    onRecentsClick: () -> Unit,
    onLinkExistingProjectClick: () -> Unit,
    onShowAddWebLinkDialog: () -> Unit,
    onShowAddObsidianLinkDialog: () -> Unit,
    onAddListShortcutClick: () -> Unit,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onHomeClick: () -> Unit,
    onEditList: () -> Unit,
    onShareList: () -> Unit,
    onDeleteList: () -> Unit,
    onSetReminder: () -> Unit,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    currentView: ProjectViewMode,
    onViewChange: (ProjectViewMode) -> Unit,
    onImportFromMarkdown: () -> Unit,
    onExportToMarkdown: () -> Unit,
    onImportBacklogFromMarkdown: () -> Unit,
    onExportBacklogToMarkdown: () -> Unit,
    onExportProjectState: () -> Unit,
    reminderParseResult: Any?, // TODO: Restore ReminderParseResult
    onClearReminder: () -> Unit,
    isNerActive: Boolean,
    onStartTrackingCurrentProject: () -> Unit,
    isProjectManagementEnabled: Boolean,
    onToggleProjectManagement: () -> Unit,
    onAddProjectToDayPlan: () -> Unit,
    isViewModePanelVisible: Boolean,
    onToggleNavPanelMode: () -> Unit,
    onRevealInExplorer: () -> Unit,
    onCloseSearch: () -> Unit,
    onAddMilestone: () -> Unit,
    onShowCreateNoteDocumentDialog: () -> Unit,
    onCreateChecklist: () -> Unit,
    onShowDisplayPropertiesClick: () -> Unit,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    holdMenuState: MutableState<HoldMenuState>,
) {
    val state =
        NavPanelState(
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            menuExpanded = menuExpanded,
            currentView = currentView,
            isProjectManagementEnabled = isProjectManagementEnabled,
            inputMode = inputMode,
            isViewModePanelVisible = isViewModePanelVisible,
        )
    val actions =
        NavPanelActions(
            onBackClick = onBackClick,
            onForwardClick = onForwardClick,
            onHomeClick = onHomeClick,
            onRecentsClick = onRecentsClick,
            onRevealInExplorer = onRevealInExplorer,
            onCloseSearch = onCloseSearch,
            onViewChange = onViewChange,
            onInputModeSelected = onInputModeSelected,
            onMenuExpandedChange = onMenuExpandedChange,
            onAddProjectToDayPlan = onAddProjectToDayPlan,
            onToggleNavPanelMode = onToggleNavPanelMode,
            menuActions =
                OptionsMenuActions(
                    onEditList = onEditList,
                    onToggleProjectManagement = onToggleProjectManagement,
                    onStartTrackingCurrentProject = onStartTrackingCurrentProject,
                    onShareList = onShareList,
                    onImportFromMarkdown = onImportFromMarkdown,
                    onExportToMarkdown = onExportToMarkdown,
                    onImportBacklogFromMarkdown = onImportBacklogFromMarkdown,
                    onExportBacklogToMarkdown = onExportBacklogToMarkdown,
                    onExportProjectState = onExportProjectState,
                    onDeleteList = onDeleteList,
                    onSetReminder = onSetReminder,
                    onShowDisplayPropertiesClick = onShowDisplayPropertiesClick,
                ),
        )

    val focusRequester = remember { FocusRequester() }

    val modes =
        remember(isProjectManagementEnabled, currentView) {
            listOfNotNull(
                InputMode.AddGoal,
                InputMode.AddQuickRecord,
                if (isProjectManagementEnabled) InputMode.AddProjectLog else null,
                if (isProjectManagementEnabled && currentView == ProjectViewMode.Advanced) InputMode.AddMilestone else null,
                if (isProjectManagementEnabled && currentView == ProjectViewMode.Backlog) InputMode.AddNestedProject else null,
                InputMode.SearchGlobal,
                InputMode.SearchInList,
            )
        }

    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }
    var showModeMenu by remember { mutableStateOf(false) }

    val currentModeIndex = modes.indexOf(inputMode)

    val inputPanelColors = LocalInputPanelColors.current
    val panelColors =
        when (inputMode) {
            InputMode.AddGoal ->
                PanelColors(
                    containerColor = inputPanelColors.addGoal.backgroundColor,
                    contentColor = inputPanelColors.addGoal.textColor,
                    accentColor = inputPanelColors.addGoal.textColor,
                    inputFieldColor = inputPanelColors.addGoal.inputFieldColor,
                )
            InputMode.AddQuickRecord ->
                PanelColors(
                    containerColor = inputPanelColors.addQuickRecord.backgroundColor,
                    contentColor = inputPanelColors.addQuickRecord.textColor,
                    accentColor = inputPanelColors.addQuickRecord.textColor,
                    inputFieldColor = inputPanelColors.addQuickRecord.inputFieldColor,
                )
            InputMode.SearchInList ->
                PanelColors(
                    containerColor = inputPanelColors.searchInList.backgroundColor,
                    contentColor = inputPanelColors.searchInList.textColor,
                    accentColor = inputPanelColors.searchInList.textColor,
                    inputFieldColor = inputPanelColors.searchInList.inputFieldColor,
                )
            InputMode.SearchGlobal ->
                PanelColors(
                    containerColor = inputPanelColors.searchGlobal.backgroundColor,
                    contentColor = inputPanelColors.searchGlobal.textColor,
                    accentColor = inputPanelColors.searchGlobal.textColor,
                    inputFieldColor = inputPanelColors.searchGlobal.inputFieldColor,
                )
            InputMode.AddProjectLog,
            InputMode.AddMilestone,
            InputMode.AddNestedProject ->
                PanelColors(
                    containerColor = inputPanelColors.addProjectLog.backgroundColor,
                    contentColor = inputPanelColors.addProjectLog.textColor,
                    accentColor = inputPanelColors.addProjectLog.textColor,
                    inputFieldColor = inputPanelColors.addProjectLog.inputFieldColor,
                )
        }

    val animatedContainerColor by
    animateColorAsState(
        targetValue = panelColors.containerColor,
        animationSpec = tween(400),
        label = "panel_color_animation",
    )

    val buttonScale by
    animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec =
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "button_scale",
    )

    LaunchedEffect(inputMode) {
        if (inputMode == InputMode.SearchInList || inputMode == InputMode.SearchGlobal) {
            delay(60)
            focusRequester.requestFocus()
        }
    }

    Surface(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        color = animatedContainerColor,
        border = BorderStroke(1.dp, panelColors.contentColor.copy(alpha = 0.1f)),
    ){
        Column {
            AnimatedVisibility(visible = suggestions.isNotEmpty()) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = panelColors.contentColor.copy(alpha = 0.12f),
                    )
                    // TODO: AutocompleteSuggestions(suggestions, onSuggestionClick)
                }
            }

            NavigationBar(
                state = state,
                actions = actions,
                contentColor = panelColors.contentColor,
                holdMenuState = holdMenuState,
            )

            AnimatedVisibility(
                visible = reminderParseResult != null,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
            ) {
                // TODO: ReminderChip
            }

            Row(
                modifier =
                    Modifier.defaultMinSize(minHeight = 64.dp)
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                MagicModeSwitcher(
                    modes = modes,
                    currentMode = inputMode,
                    onTap = { showModeMenu = true },
                    onModeSelected = { mode ->
                        onInputModeSelected(mode)
                    },
                    modifier = Modifier.scale(buttonScale)
                )


                    AnimatedContent(
                        targetState = inputMode,
                        transitionSpec = {
                            val initialIndex = modes.indexOf(initialState)
                            val targetIndex = modes.indexOf(targetState)
                            val forward = targetIndex > initialIndex

                            val direction =
                                if (forward) {
                                    AnimatedContentTransitionScope.SlideDirection.Left
                                } else {
                                    AnimatedContentTransitionScope.SlideDirection.Right
                                }

                            slideIntoContainer(
                                direction,
                                animationSpec =
                                    spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessLow,
                                    ),
                            ) + fadeIn(animationSpec = tween(300)) togetherWith
                                    slideOutOfContainer(
                                        direction,
                                        animationSpec =
                                            spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessLow,
                                            ),
                                    ) + fadeOut(animationSpec = tween(250))
                        },
                        label = "mode_icon_animation",
                    ) { mode ->
                        val icon =
                            when (mode) {
                                InputMode.AddGoal -> Icons.Outlined.Add
                                InputMode.AddQuickRecord -> Icons.Outlined.Inbox
                                InputMode.SearchInList -> Icons.Outlined.Search
                                InputMode.SearchGlobal -> Icons.Outlined.TravelExplore
                                InputMode.AddProjectLog -> Icons.Outlined.PostAdd
                                InputMode.AddMilestone -> Icons.Outlined.Flag
                                InputMode.AddNestedProject -> Icons.Default.AccountTree
                            }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Magic Button",
                            modifier =
                                Modifier.size(22.dp).graphicsLayer {
                                    rotationZ =
                                        if (isPressed) (dragOffset / 20f).coerceIn(-15f, 15f) else 0f
                                },
                            tint = panelColors.contentColor,
                        )
                    }

                    // маленький індикатор вгорі
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.Top)   // вертикально
                                .padding(3.dp)
                                .size(8.dp)
                                .background(color = panelColors.accentColor, shape = CircleShape)
                    )


                Spacer(modifier = Modifier.width(8.dp))

                // 📝 TEXT FIELD
                Surface(
                    modifier =
                        Modifier.weight(1f)
                            .heightIn(max = LocalConfiguration.current.screenHeightDp.dp / 3)
                            .defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = panelColors.inputFieldColor,
                    border = BorderStroke(1.dp, panelColors.accentColor.copy(alpha = 0.3f)),
                    shadowElevation = 0.dp,
                ) {
                    BasicTextField(
                        value = inputValue,
                        onValueChange = onValueChange,
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .focusRequester(focusRequester),
                        textStyle =
                            MaterialTheme.typography.bodyLarge.copy(
                                color = panelColors.contentColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions =
                            KeyboardActions(onSend = { if (inputValue.text.isNotBlank()) onSubmit() }),
                        singleLine = false,
                        cursorBrush = SolidColor(panelColors.accentColor),
                        decorationBox = { innerTextField ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (inputValue.text.isEmpty()) {
                                        Text(
                                            text =
                                                when (inputMode) {
                                                    InputMode.AddGoal -> "Add goal"
                                                    InputMode.AddQuickRecord -> "Add quick record"
                                                    InputMode.SearchInList -> "Search in list"
                                                    InputMode.SearchGlobal -> "Search global"
                                                    InputMode.AddProjectLog -> "Додати коментар до проекту..."
                                                    InputMode.AddMilestone -> "Додати віху до проекту..."
                                                    InputMode.AddNestedProject -> "Додати вкладений проект..."
                                                },
                                            style =
                                                MaterialTheme.typography.bodyLarge.copy(
                                                    color = panelColors.contentColor.copy(alpha = 0.7f),
                                                    fontSize = 16.sp,
                                                ),
                                        )
                                    }
                                    innerTextField()
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    NerIndicator(
                                        isActive = isNerActive,
                                        hasText = inputValue.text.isNotBlank()
                                    )

                                    AnimatedVisibility(
                                        visible = inputMode == InputMode.AddNestedProject,
                                        enter = fadeIn(),
                                        exit = fadeOut(),
                                    ) {
                                        IconButton(
                                            onClick = { onLinkExistingProjectClick() },
                                            modifier = Modifier.size(24.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Link,
                                                contentDescription = "Link existing project",
                                                tint = panelColors.contentColor.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = inputValue.text.isNotBlank(),
                                        enter = fadeIn(),
                                        exit = fadeOut(),
                                    ) {
                                        IconButton(
                                            onClick = { onValueChange(TextFieldValue("")) },
                                            modifier = Modifier.size(24.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear input",
                                                tint = panelColors.contentColor.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // ✉️ SEND BUTTON
                AnimatedVisibility(
                    visible = inputValue.text.isNotBlank(),
                    enter =
                        fadeIn() +
                                scaleIn(
                                    initialScale = 0.8f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                ),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f),
                ) {
                    val sendButtonBackgroundColor = panelColors.accentColor
                    val sendIconColor =
                        remember(sendButtonBackgroundColor) {
                            val luminance = sendButtonBackgroundColor.luminance()
                            if (luminance > 0.55f) Color(0xFF1C1B1F) else Color.White
                        }

                    IconButton(
                        onClick = onSubmit,
                        modifier =
                            Modifier.size(44.dp)
                                .background(color = sendButtonBackgroundColor, shape = CircleShape),
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                contentColor = sendIconColor
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }

    // Діалог режимів (по тапу по MagicButton)
    if (showModeMenu) {
        InputPanelMagicActionsDialog(
            currentInputMode = inputMode,
            isProjectManagementEnabled = isProjectManagementEnabled,
            onDismiss = { showModeMenu = false },
            onInputModeSelected = {
                showModeMenu = false
                onInputModeSelected(it)
            },
            onLinkExistingProjectClick = onLinkExistingProjectClick,
            onShowAddWebLinkDialog = onShowAddWebLinkDialog,
            onShowAddObsidianLinkDialog = onShowAddObsidianLinkDialog,
            onAddListShortcutClick = onAddListShortcutClick,
            onShowCreateNoteDocumentDialog = onShowCreateNoteDocumentDialog,
            onCreateChecklist = onCreateChecklist,
        )
    }
}

/**
 * Детектор: TAP або горизонтальний drag (одним пальцем).
 *
 * - короткий рух + up → onTap()
 * - достатній горизонтальний drift → onDragEnd(totalDx)
 */
suspend fun PointerInputScope.detectTapOrHorizontalDrag(
    dragThresholdPx: Float = 40f,
    onTap: () -> Unit,
    onDragEnd: (totalDx: Float) -> Unit,
) {
    awaitPointerEventScope {
        while (true) {
            val down = awaitFirstDown()

            var totalDx = 0f
            var finished = false

            while (!finished) {
                val event = awaitPointerEvent()
                val change = event.changes.first { it.id == down.id }

                val dx = change.position.x - change.previousPosition.x
                totalDx += dx
                change.consume()

                if (change.changedToUp()) {
                    finished = true

                    if (kotlin.math.abs(totalDx) < dragThresholdPx) {
                        onTap()
                    } else {
                        onDragEnd(totalDx)
                    }
                }
            }
        }
    }
}



suspend fun PointerInputScope.detectTapAndDrag(
    onTap: () -> Unit,
    onHorizontalDrag: (deltaX: Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    awaitPointerEventScope {
        while (true) {

            // Wait for finger down
            val down = awaitFirstDown()
            var dragTotal = 0f
            var isDrag = false

            // Handle movements
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.first()

                val delta = change.positionChange()
                if (delta != Offset.Zero) {
                    dragTotal += delta.x
                    onHorizontalDrag(delta.x)
                    isDrag = true
                    change.consume()
                }

                // Finger up
                if (change.changedToUp()) {
                    if (!isDrag) {
                        onTap()
                    } else {
                        onDragEnd()
                    }
                    break
                }
            }
        }
    }
}


@Composable
fun DebugTouchOverlay() {
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent()
                        Log.e("DEBUG_OVERLAY", "root event = $e")
                    }
                }
            }
    )
}



@Composable
fun MagicModeSwitcher(
    modes: List<InputMode>,
    currentMode: InputMode,
    onTap: () -> Unit,
    onModeSelected: (InputMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val dragThreshold = with(density) { 24.dp.toPx() }

    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .size(48.dp)
            // 1) TAP
            .pointerInput(modes, currentMode) {
                detectTapGestures(
                    onTap = {
                        onTap()
                    }
                )
            }
            // 2) HORIZONTAL DRAG
            .pointerInput(modes, currentMode) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        accumulatedDrag = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        accumulatedDrag += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        if (kotlin.math.abs(accumulatedDrag) > dragThreshold && modes.isNotEmpty()) {
                            val currentIndex = modes.indexOf(currentMode).coerceAtLeast(0)
                            val nextIndex = if (accumulatedDrag < 0f) {
                                // свайп вліво → наступний режим
                                (currentIndex + 1) % modes.size
                            } else {
                                // свайп вправо → попередній режим
                                (currentIndex - 1 + modes.size) % modes.size
                            }
                            onModeSelected(modes[nextIndex])
                        }
                        accumulatedDrag = 0f
                    },
                    onDragCancel = {
                        accumulatedDrag = 0f
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentMode,
            transitionSpec = {
                val initialIndex = modes.indexOf(initialState)
                val targetIndex  = modes.indexOf(targetState)
                val forward = targetIndex > initialIndex

                val dir = if (forward)
                    AnimatedContentTransitionScope.SlideDirection.Left
                else
                    AnimatedContentTransitionScope.SlideDirection.Right

                slideIntoContainer(
                    dir,
                    animationSpec = tween(250)
                ) + fadeIn() togetherWith
                        slideOutOfContainer(
                            dir,
                            animationSpec = tween(250)
                        ) + fadeOut()
            },
            label = "MagicSwitcherIcon"
        ) { mode ->
            val icon = when (mode) {
                InputMode.AddGoal -> Icons.Outlined.Add
                InputMode.AddQuickRecord -> Icons.Outlined.Inbox
                InputMode.SearchInList -> Icons.Outlined.Search
                InputMode.SearchGlobal -> Icons.Outlined.TravelExplore
                InputMode.AddProjectLog -> Icons.Outlined.PostAdd
                InputMode.AddMilestone -> Icons.Outlined.Flag
                InputMode.AddNestedProject -> Icons.Default.AccountTree
            }

            Icon(
                icon,
                contentDescription = "Mode",
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
@Composable
fun MinimalInputPanel(
    inputMode: InputMode,
    onInputModeSelected: (InputMode) -> Unit,
    onButtonAnchorChanged: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFF222222), RoundedCornerShape(16.dp))
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInWindow()
                    val size = coords.size
                    val anchor = Offset(
                        pos.x + size.width / 2f,
                        pos.y + size.height / 2f
                    )
                    onButtonAnchorChanged(anchor)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "menu",
                tint = Color.White
            )
        }
    }
}

@Composable
fun MinimalInputPanelV2(
    inputMode: InputMode,
    onInputModeSelected: (InputMode) -> Unit,
    menuItems: List<String>,
    onMenuItemSelected: (Int) -> Unit,
    holdMenuController: HoldMenu2Controller,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        HoldMenu2Button(
            items = menuItems,
            onSelect = onMenuItemSelected,
            controller = holdMenuController,
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFF222222), RoundedCornerShape(16.dp))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "menu",
                    tint = Color.White
                )
            }
        }
    }
}
