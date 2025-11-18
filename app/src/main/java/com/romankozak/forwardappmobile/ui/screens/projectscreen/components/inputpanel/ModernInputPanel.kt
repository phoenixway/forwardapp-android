package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.inputpanel

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
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.List
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
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
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.domain.ner.ReminderParseResult
import com.romankozak.forwardappmobile.ui.theme.LocalInputPanelColors
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Button
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Controller
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenuItem
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Dashboard
import kotlinx.coroutines.delay

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
  holdMenuController: HoldMenu2Controller,
) {
    val availableViews = remember(isProjectManagementEnabled) {
        ProjectViewMode.values().filter {
            it != ProjectViewMode.ADVANCED || isProjectManagementEnabled
        }
    }

    val menuItems = remember(availableViews) {
        availableViews.map { viewMode ->
            HoldMenuItem(
                label = viewMode.name.replaceFirstChar { it.titlecase() },
                icon = when (viewMode) {
                    ProjectViewMode.BACKLOG -> Icons.AutoMirrored.Outlined.ListAlt
                    ProjectViewMode.INBOX -> Icons.AutoMirrored.Outlined.Notes
                    ProjectViewMode.ADVANCED -> Icons.Outlined.Dashboard
                    ProjectViewMode.ATTACHMENTS -> Icons.Default.Attachment
                }
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
            onSelect = { index ->
                val selectedViewMode = availableViews[index]
                onViewChange(selectedViewMode)
                val newMode = when (selectedViewMode) {
                    ProjectViewMode.INBOX, ProjectViewMode.ADVANCED -> InputMode.AddQuickRecord
                    else -> InputMode.AddGoal
                }
                onInputModeSelected(newMode)
            },
            modifier = Modifier.size(40.dp).padding(2.dp)
        ) {
            val currentIcon = when (currentView) {
                ProjectViewMode.BACKLOG -> Icons.AutoMirrored.Outlined.ListAlt
                ProjectViewMode.INBOX -> Icons.AutoMirrored.Outlined.Notes
                ProjectViewMode.ADVANCED -> Icons.Outlined.Dashboard
                ProjectViewMode.ATTACHMENTS -> Icons.Default.Attachment
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
                isVisible = state.currentView == ProjectViewMode.INBOX,
              ),
              MenuItem(
                "Експортувати в Markdown",
                Icons.Default.Download,
                {
                  menu.onExportToMarkdown()
                  actions.onMenuExpandedChange(false)
                },
                isVisible = state.currentView == ProjectViewMode.INBOX,
              ),
              MenuItem(
                "Імпортувати беклог з Markdown",
                Icons.Default.Upload,
                {
                  menu.onImportBacklogFromMarkdown()
                  actions.onMenuExpandedChange(false)
                },
                isVisible = state.currentView == ProjectViewMode.BACKLOG,
              ),
              MenuItem(
                "Експортувати беклог в Markdown",
                Icons.Default.Download,
                {
                  menu.onExportBacklogToMarkdown()
                  actions.onMenuExpandedChange(false)
                },
                isVisible = state.currentView == ProjectViewMode.BACKLOG,
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
  holdMenuController: HoldMenu2Controller,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        val baseWidth = if (state.isProjectManagementEnabled) 380.dp else 320.dp
        val showReveal = availableWidth > baseWidth
        val showRecents = availableWidth > (baseWidth - 40.dp)

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
                                ViewModeToggle(
                                    currentView = state.currentView,
                                    isProjectManagementEnabled = state.isProjectManagementEnabled,
                                    onViewChange = actions.onViewChange,
                                    onInputModeSelected = actions.onInputModeSelected,
                                    contentColor = contentColor,
                                    holdMenuController = holdMenuController,
                                )
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
  holdMenuController: HoldMenu2Controller,
  inputValue: TextFieldValue,
  inputMode: InputMode,
  onValueChange: (TextFieldValue) -> Unit,
  onSubmit: () -> Unit,
  onInputModeSelected: (InputMode) -> Unit,
  onRecentsClick: () -> Unit,
  onAddListLinkClick: () -> Unit,
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
  reminderParseResult: ReminderParseResult?,
  onClearReminder: () -> Unit,
  isNerActive: Boolean,
  onStartTrackingCurrentProject: () -> Unit,
  isProjectManagementEnabled: Boolean,
  onToggleProjectManagement: () -> Unit,
  onAddProjectToDayPlan: () -> Unit,
  onRevealInExplorer: () -> Unit,
  onCloseSearch: () -> Unit,
  onAddMilestone: () -> Unit,
  onShowCreateNoteDocumentDialog: () -> Unit,
  onCreateChecklist: () -> Unit,
  onShowDisplayPropertiesClick: () -> Unit,
  suggestions: List<String>,
  onSuggestionClick: (String) -> Unit,
) {
  val state =
    NavPanelState(
      canGoBack = canGoBack,
      canGoForward = canGoForward,
      menuExpanded = menuExpanded,
      currentView = currentView,
      isProjectManagementEnabled = isProjectManagementEnabled,
      inputMode = inputMode,
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
        if (isProjectManagementEnabled && currentView == ProjectViewMode.ADVANCED) InputMode.AddMilestone else null,
        InputMode.SearchGlobal,
        InputMode.SearchInList,
      )
    }

  var dragOffset by remember { mutableFloatStateOf(0f) }
  var isPressed by remember { mutableStateOf(false) }
  var showModeMenu by remember { mutableStateOf(false) }
  var animationDirection by remember { mutableIntStateOf(1) }

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
      InputMode.AddProjectLog ->
        PanelColors(
          containerColor = inputPanelColors.addProjectLog.backgroundColor,
          contentColor = inputPanelColors.addProjectLog.textColor,
          accentColor = inputPanelColors.addProjectLog.textColor,
          inputFieldColor = inputPanelColors.addProjectLog.inputFieldColor,
        )
      InputMode.AddMilestone ->
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
    modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
    shape = RoundedCornerShape(28.dp),
    shadowElevation = 0.dp,
    tonalElevation = 0.dp,
    color = animatedContainerColor,
    border = BorderStroke(1.dp, panelColors.contentColor.copy(alpha = 0.1f)),
  ) {
    Column {
      AnimatedVisibility(visible = suggestions.isNotEmpty()) {
        Column {
          HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = panelColors.contentColor.copy(alpha = 0.12f),
          )
          AutocompleteSuggestions(suggestions = suggestions, onSuggestionClick = onSuggestionClick)
        }
      }
      NavigationBar(state = state, actions = actions, contentColor = panelColors.contentColor, holdMenuController = holdMenuController)

      AnimatedVisibility(
        visible = reminderParseResult != null,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
      ) {
        reminderParseResult?.let {
          if (it.success) {
            ReminderChip(suggestionText = it.suggestionText ?: "", onClear = onClearReminder)
          } else {
            Text(
              text = "Не вдалося розпізнати дату/час: ${it.errorMessage}",
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
          }
        }
      }

      Row(
        modifier =
          Modifier.defaultMinSize(minHeight = 64.dp).padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom,
      ) {
        Box {
          Surface(
            onClick = { showModeMenu = true },
            shape = CircleShape,
            color = panelColors.contentColor.copy(alpha = 0.1f),
            contentColor = panelColors.contentColor,
            modifier =
              Modifier.size(48.dp).scale(buttonScale).pointerInput(inputMode) {
                detectHorizontalDragGestures(
                  onDragStart = { isPressed = true },
                  onDragEnd = {
                    isPressed = false
                    val threshold = 50f
                    when {
                      dragOffset > threshold -> {
                        animationDirection = -1
                        val prevIndex = ((currentModeIndex - 1) + modes.size) % modes.size
                        onInputModeSelected(modes[prevIndex])
                      }
                      dragOffset < -threshold -> {
                        animationDirection = 1
                        val nextIndex = (currentModeIndex + 1) % modes.size
                        onInputModeSelected(modes[nextIndex])
                      }
                    }
                    dragOffset = 0f
                  },
                ) { _, dragAmount ->
                  dragOffset += dragAmount
                }
              },
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
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
                  ) + fadeIn(animationSpec = tween(800)) togetherWith
                    slideOutOfContainer(
                      direction,
                      animationSpec =
                        spring(
                          dampingRatio = Spring.DampingRatioNoBouncy,
                          stiffness = Spring.StiffnessLow,
                        ),
                    ) + fadeOut(animationSpec = tween(850))
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
                  }
                Icon(
                  imageVector = icon,
                  contentDescription = "Magic Button",
                  modifier =
                    Modifier.size(22.dp).graphicsLayer {
                      rotationZ = if (isPressed) (dragOffset / 20f).coerceIn(-15f, 15f) else 0f
                    },
                )
              }
            }
          }

          Box(
            modifier =
              Modifier.align(Alignment.TopEnd)
                .size(8.dp)
                .background(color = panelColors.accentColor, shape = CircleShape)
                .padding(1.dp)
                .background(
                  color = panelColors.contentColor.copy(alpha = 0.3f),
                  shape = CircleShape,
                )
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

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
              Modifier.fillMaxWidth().padding(vertical = 12.dp).focusRequester(focusRequester),
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
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                  if (inputValue.text.isEmpty()) {
                    Text(
                      text =
                        when (inputMode) {
                          InputMode.AddGoal -> stringResource(R.string.hint_add_goal)
                          InputMode.AddQuickRecord -> stringResource(R.string.hint_add_quick_record)
                          InputMode.SearchInList -> stringResource(R.string.hint_search_in_list)
                          InputMode.SearchGlobal -> stringResource(R.string.hint_search_global)
                          InputMode.AddProjectLog -> "Додати коментар до проекту..."
                          InputMode.AddMilestone -> "Додати віху до проекту..."
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
                  NerIndicator(isActive = isNerActive, hasText = inputValue.text.isNotBlank())

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
                        contentDescription = stringResource(R.string.clear_input),
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
              contentDescription = stringResource(R.string.send),
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    }
  }
  if (showModeMenu) {
    InputPanelMagicActionsDialog(
      currentInputMode = inputMode,
      isProjectManagementEnabled = isProjectManagementEnabled,
      onDismiss = { showModeMenu = false },
      onInputModeSelected = onInputModeSelected,
      onAddListLinkClick = onAddListLinkClick,
      onShowAddWebLinkDialog = onShowAddWebLinkDialog,
      onShowAddObsidianLinkDialog = onShowAddObsidianLinkDialog,
      onAddListShortcutClick = onAddListShortcutClick,
      onShowCreateNoteDocumentDialog = onShowCreateNoteDocumentDialog,
      onCreateChecklist = onCreateChecklist,
    )
  }
}
