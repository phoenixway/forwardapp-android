package com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan

import TaskList
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.romankozak.forwardappmobile.data.database.models.DayPlan
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.ListItemType
import com.romankozak.forwardappmobile.data.database.models.ScoringStatus
import com.romankozak.forwardappmobile.data.database.models.TaskPriority
import com.romankozak.forwardappmobile.ui.common.MatrixRainView
import com.romankozak.forwardappmobile.ui.screens.activitytracker.dialogs.ReminderPickerDialog
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.tasklist.AddTaskDialog
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.tasklist.EditTaskDialog
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems.GoalItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableLazyListState

val TAG = "NAV_DEBUG" // Тег для логування

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ErrorState(error: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxSize().padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      Icons.Outlined.CloudOff,
      contentDescription = null,
      modifier = Modifier.size(64.dp),
      tint = MaterialTheme.colorScheme.error,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = "Помилка завантаження",
      style = MaterialTheme.typography.titleMedium,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
      text = error,
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = onRetry) { Text("Спробувати ще раз") }
  }
}

@Composable
private fun EmptyTasksState(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      Icons.Outlined.Checklist,
      contentDescription = null,
      modifier = Modifier.size(80.dp),
      tint = MaterialTheme.colorScheme.outline,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = "Завдань ще немає",
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = "Натисніть кнопку '+'/Додати перше завдання",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
fun CompactDayPlanHeader(
  dayPlan: DayPlan?,
  totalPoints: Int,
  onNavigateToPreviousDay: () -> Unit,
  onNavigateToNextDay: () -> Unit,
  isNextDayNavigationEnabled: Boolean,
  onSettingsClick: () -> Unit,
  modifier: Modifier = Modifier,
  containerColor: Color,
) {
  val formattedDate =
    remember(dayPlan?.date) {
      dayPlan?.date?.let { dateMillis ->
        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("uk"))
        Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).format(formatter)
      } ?: "План дня"
    }
  Surface(modifier = modifier.fillMaxWidth(), shadowElevation = 2.dp, color = containerColor) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          // REMOVED: .statusBarsPadding() - this was causing the big vertical space
          .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp) // Added top padding instead
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        IconButton(onClick = onNavigateToPreviousDay) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Попередній день",
          )
        }
        Text(
          text =
            formattedDate.replaceFirstChar {
              if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            },
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNavigateToNextDay, enabled = isNextDayNavigationEnabled) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Наступний день",
            tint =
              if (isNextDayNavigationEnabled) {
                MaterialTheme.colorScheme.onSurface
              } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
              },
          )
        }
        IconButton(onClick = onSettingsClick) {
          Icon(imageVector = Icons.Filled.Settings, contentDescription = "Налаштування")
        }
      }
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Всього балів: $totalPoints",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

private fun DayTask.toGoal(): Goal {
  return Goal(
    id = this.id,
    text = this.title,
    description = this.description,
    completed = this.completed,
    scoringStatus = ScoringStatus.NOT_ASSESSED,
    displayScore = 0,
    reminderTime = this.dueTime,
    relatedLinks = null,
    createdAt = this.createdAt,
    updatedAt = this.createdAt,
  )
}

fun DayTask.toListItem(): ListItem {
  return ListItem(
    id = this.id,
    projectId = this.projectId ?: this.dayPlanId,
    itemType = this.taskType ?: ListItemType.GOAL,
    entityId = this.entityId ?: this.goalId ?: this.id,
    order = this.order,
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DayPlanScreen(
  dayPlanId: String,
  modifier: Modifier = Modifier,
  viewModel: DayPlanViewModel = hiltViewModel(),
  onNavigateToProject: (projectId: String) -> Unit,
  onNavigateToBacklog: (task: DayTask) -> Unit,
  onNavigateToSettings: () -> Unit,
  addTaskTrigger: Int,
  navController: NavController,
) {
  val TAG = "NAV_DEBUG" // Тег для логування

  val systemUiController = rememberSystemUiController()
  val isLight = !isSystemInDarkTheme()

  val headerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)

  // ВИДАЛЯЄМО ЦЕЙ БЛОК:
  /*
  LaunchedEffect(headerColor, isLight) {
    systemUiController.setStatusBarColor(
      color = headerColor,
      darkIcons = headerColor.luminance() > 0.5f,
    )
  }
  */

  // АКТИВУЄМО ЦЕЙ БЛОК, ЩОБ ЗРОБИТИ СТАТУС-БАР ПРОЗОРИМ
  LaunchedEffect(isLight) {
    systemUiController.setSystemBarsColor(
      color = Color.Transparent,
      darkIcons = isLight,
      isNavigationBarContrastEnforced = false,
    )
  }

  val uiState by viewModel.uiState.collectAsState()
  val isAddTaskDialogOpen by viewModel.isAddTaskDialogOpen.collectAsState()
  val selectedTask by viewModel.selectedTask.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val hapticFeedback = LocalHapticFeedback.current
  val isEditTaskDialogOpen by viewModel.isEditTaskDialogOpen.collectAsState()
  var showReminderDialog by remember { mutableStateOf(false) }
  val taskToDelete by viewModel.showDeleteConfirmationDialog.collectAsState()
  val taskToEdit by viewModel.showEditConfirmationDialog.collectAsState()

  DisposableEffect(Unit) {
    onDispose {
        viewModel.clearSelectedTask()
    }
  }

  LaunchedEffect(Unit) {
    viewModel.uiEvent.collect {
        when(it) {
            is DayPlanUiEvent.NavigateToEditTask -> {
                navController.navigate("edit_task_screen/${it.taskId}")
            }
        }
    }
  }

  var showMatrixSplash by remember { mutableStateOf(true) }
  var matrixView by remember { mutableStateOf<MatrixRainView?>(null) }
  var isContentReady by remember { mutableStateOf(false) }

  // Enhanced timing with content preparation
  LaunchedEffect(Unit) {
    // Pre-load content
    delay(100)
    isContentReady = true

    // Show matrix longer, then smooth fade
    delay(600)
    matrixView?.startFadeOut()

    // Wait for fade to complete
    delay(500)
    showMatrixSplash = false
  }

  if (taskToEdit != null) {
    EditRecurringTaskDialog(
      task = taskToEdit!!,
      onDismiss = { viewModel.dismissEditConfirmationDialog() },
      onConfirmEditSingle = { viewModel.editSingleInstanceOfRecurringTask(it) },
      onConfirmEditAll = { viewModel.editAllFutureInstancesOfRecurringTask(it) },
    )
  }

  if (taskToDelete != null) {
    DeleteRecurringTaskDialog(
      task = taskToDelete!!,
      onDismiss = { viewModel.dismissDeleteConfirmationDialog() },
      onConfirmDeleteSingle = { viewModel.deleteSingleInstanceOfRecurringTask(it) },
      onConfirmDeleteAll = { viewModel.deleteAllFutureInstancesOfRecurringTask(it) },
    )
  }

  LaunchedEffect(addTaskTrigger) {
    if (addTaskTrigger > 0) {
      viewModel.openAddTaskDialog()
    }
  }

  LaunchedEffect(Unit) { viewModel.tasksUpdated.collect { viewModel.loadDataForPlan(dayPlanId) } }
  LaunchedEffect(uiState.error) {
    uiState.error?.let { error ->
      snackbarHostState.showSnackbar(message = error, duration = SnackbarDuration.Short)
      viewModel.dismissError()
    }
  }
  LaunchedEffect(dayPlanId) { viewModel.loadDataForPlan(dayPlanId) }

  Box(modifier = modifier.fillMaxSize()) {
    // Main content with conditional visibility for smoother transition
    AnimatedVisibility(
      visible = isContentReady,
      enter = fadeIn(animationSpec = tween(300, delayMillis = 400)),
      modifier = Modifier.fillMaxSize(),
    ) {
      Box(
        modifier =
          Modifier.fillMaxSize().pointerInput(Unit) {
            detectHorizontalDragGestures { _, dragAmount ->
              when {
                dragAmount < -50 && !uiState.isToday -> viewModel.navigateToNextDay()
                dragAmount > 50 -> viewModel.navigateToPreviousDay()
              }
            }
          }
      ) {
        when {
          uiState.isLoading -> LoadingState()
          uiState.error != null && uiState.tasks.isEmpty() -> {
            ErrorState(error = uiState.error!!, onRetry = { viewModel.loadDataForPlan(dayPlanId) })
          }
          else -> {
            val tasks = uiState.tasks
            val totalPoints = tasks.filter { it.completed }.sumOf { it.points }
            TaskList(
              tasks = tasks,
              dayPlan = uiState.dayPlan,
              totalPoints = totalPoints,
              onToggleTask = { taskId ->
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.toggleTaskCompletion(taskId)
              },
              onTaskLongPress = { task ->
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.selectTask(task)
              },
              onTasksReordered = { reorderedList ->
                uiState.dayPlan?.let { dayPlan ->
                  viewModel.updateTasksOrder(dayPlan.id, reorderedList)
                }
              },
              onNavigateToPreviousDay = { viewModel.navigateToPreviousDay() },
              onNavigateToNextDay = { viewModel.navigateToNextDay() },
              isNextDayNavigationEnabled = !uiState.isToday,
              onSublistClick = onNavigateToProject,
              onSettingsClick = onNavigateToSettings,
              modifier = Modifier.fillMaxSize(),
              headerContainerColor = headerColor,
            )
          }
        }

        SnackbarHost(
          hostState = snackbarHostState,
          snackbar = { snackbarData ->
            Snackbar(
              snackbarData = snackbarData,
              containerColor = MaterialTheme.colorScheme.errorContainer,
              contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
          },
          modifier = Modifier.align(Alignment.BottomCenter),
        )
      }
    }

    // Enhanced Matrix splash screen with better integration
    AnimatedVisibility(visible = showMatrixSplash, exit = fadeOut(animationSpec = tween(300))) {
      AndroidView(
        factory = { context -> MatrixRainView(context).also { view -> matrixView = view } },
        modifier = Modifier.fillMaxSize(),
      )
    }
  }

  if (isAddTaskDialogOpen) {
    AddTaskDialog(
      onDismissRequest = viewModel::dismissAddTaskDialog,
      onConfirm = { title, description, duration, priority, recurrenceRule, points ->
        viewModel.addTask(dayPlanId, title, description, duration, priority, recurrenceRule, points)
      },
      initialPriority = TaskPriority.MEDIUM,
    )
  }

  selectedTask?.let { task ->
    TaskOptionsBottomSheet(
      task = task,
      onDismiss = viewModel::clearSelectedTask,
      onEdit = { viewModel.onEditTaskClicked(task) },
      onDelete = { taskToDelete ->
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.onDeleteTaskClicked(taskToDelete)
      },
      onSetReminder = { showReminderDialog = true },
      showAddToTodayOption = !uiState.isToday,
      onAddToToday = { viewModel.copyTaskToTodaysPlan(task) },
      onShowInBacklog = { onNavigateToBacklog(task) },
      onMoveToTop = { viewModel.moveTaskToTop(task) },
      onMoveToTomorrow = { viewModel.moveTaskToTomorrow(task) },
    )
  }



  if (showReminderDialog && selectedTask != null) {
    ReminderPickerDialog(
      onDismiss = {
        showReminderDialog = false
        viewModel.clearSelectedTask()
      },
      onSetReminder = { reminderTime ->
        viewModel.setTaskReminder(selectedTask!!.id, reminderTime)
        showReminderDialog = false
        viewModel.clearSelectedTask()
      },
      onClearReminder = {
        viewModel.clearTaskReminder(selectedTask!!.id)
        showReminderDialog = false
        viewModel.clearSelectedTask()
      },
      currentReminderTime = selectedTask!!.reminderTime,
    )
  }
}

@Composable
fun EditRecurringTaskDialog(
  task: DayTask,
  onDismiss: () -> Unit,
  onConfirmEditSingle: (DayTask) -> Unit,
  onConfirmEditAll: (DayTask) -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Редагувати повторюване завдання?") },
    text = { Text("Ви хочете редагувати тільки це завдання, чи це і всі наступні?") },
    confirmButton = {
      Column {
        Button(onClick = { onConfirmEditSingle(task) }) { Text("Тільки це завдання") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onConfirmEditAll(task) }) { Text("Це і всі наступні") }
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
  )
}

@Composable
fun DeleteRecurringTaskDialog(
  task: DayTask,
  onDismiss: () -> Unit,
  onConfirmDeleteSingle: (DayTask) -> Unit,
  onConfirmDeleteAll: (DayTask) -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Видалити повторюване завдання?") },
    text = { Text("Ви хочете видалити тільки це завдання, чи це і всі наступні?") },
    confirmButton = {
      Column(modifier = Modifier.padding(8.dp)) {
        Button(onClick = { onConfirmDeleteSingle(task) }) { Text("Тільки це завдання") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onConfirmDeleteAll(task) }) { Text("Це і всі наступні") }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onDismiss) { Text("Скасувати") }
      }
    },
    dismissButton = null,
  )
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      CircularProgressIndicator(
        modifier = Modifier.size(48.dp),
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "Завантаження плану...",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskGoalItem(
  task: DayTask,
  onToggle: () -> Unit,
  onLongPress: () -> Unit,
  isDragging: Boolean = false,
  reorderableState: ReorderableLazyListState? = null,
  modifier: Modifier = Modifier,
) {
  val goalContent = ListItemContent.GoalItem(goal = task.toGoal(), listItem = task.toListItem())
  Card(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
          checked = task.completed,
          onCheckedChange = { onToggle() },
          modifier = Modifier.padding(end = 8.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
          GoalItem(
            goalContent = goalContent,
            onCheckedChange = {},
            onClick = {},
            onLongClick = onLongPress,
            isSelected = false,
            modifier = Modifier.fillMaxWidth(),
            currentTimeMillis = System.currentTimeMillis(),
          )
        }
        if (task.recurringTaskId != null) {
          Icon(
            imageVector = Icons.Filled.Repeat,
            contentDescription = "Повторюване завдання",
            modifier = Modifier.size(16.dp).padding(start = 4.dp),
            tint = MaterialTheme.colorScheme.outline,
          )
        }
      }
      if (reorderableState != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Outlined.DragHandle,
            contentDescription = "Перетягнути для зміни порядку",
            modifier = Modifier.padding(end = 8.dp).size(24.dp),
            tint = MaterialTheme.colorScheme.outline,
          )
          IconButton(onClick = onLongPress, modifier = Modifier.size(48.dp)) {
            Icon(
              Icons.Default.MoreVert,
              contentDescription = "Більше опцій",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskOptionsBottomSheet(
  task: DayTask,
  onDismiss: () -> Unit,
  onEdit: () -> Unit,
  onDelete: (DayTask) -> Unit,
  onSetReminder: () -> Unit,
  showAddToTodayOption: Boolean,
  onAddToToday: () -> Unit,
  onShowInBacklog: (DayTask) -> Unit,
  onMoveToTop: () -> Unit,
  onMoveToTomorrow: () -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column {
      ListItem(
        headlineContent = { Text("Редагувати") },
        leadingContent = { Icon(Icons.Outlined.Edit, contentDescription = null) },
        modifier = Modifier.clickable { onEdit() },
      )
      ListItem(
        headlineContent = { Text("Підняти на вершину списку") },
        leadingContent = { Icon(Icons.Outlined.VerticalAlignTop, contentDescription = null) },
        modifier = Modifier.clickable { onMoveToTop() },
      )
      ListItem(
        headlineContent = { Text("Перенести на завтра") },
        leadingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
        modifier = Modifier.clickable {
            onMoveToTomorrow()
            onDismiss()
        },
      )
      ListItem(
        headlineContent = { Text("Встановити нагадування") },
        leadingContent = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
        modifier = Modifier.clickable { onSetReminder() },
      )
      if (showAddToTodayOption) {
        ListItem(
          headlineContent = { Text("Додати в план на сьогодні") },
          leadingContent = { Icon(Icons.Outlined.Today, contentDescription = null) },
          modifier =
            Modifier.clickable {
              onAddToToday()
              onDismiss()
            },
        )
      }
      if (task.projectId != null || task.goalId != null) {
        ListItem(
          headlineContent = { Text("Показати в беклозі проекту") },
          leadingContent = { Icon(Icons.AutoMirrored.Outlined.ListAlt, contentDescription = null) },
          modifier =
            Modifier.clickable {
              Log.d(TAG, "1. КЛІК: 'Показати в беклозі'.")
              Log.d(TAG, "   - Task Title: ${task.title}")
              Log.d(TAG, "   - Task ProjectID: ${task.projectId}") // ДУЖЕ ВАЖЛИВИЙ ЛОГ
              Log.d(TAG, "   - Task GoalID: ${task.goalId}")
              Log.d(TAG, "   - Task ID: ${task.id}")

              onShowInBacklog(task)
              onDismiss()
            },
        )
      }
      HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
      ListItem(
        headlineContent = { Text("Видалити", color = MaterialTheme.colorScheme.error) },
        leadingContent = {
          Icon(
            Icons.Outlined.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
          )
        },
        modifier =
          Modifier.clickable {
            onDelete(task)
            onDismiss()
          },
      )
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
fun CompactDayPlanHeaderExtended(
  dayPlan: DayPlan?,
  totalPoints: Int,
  onNavigateToPreviousDay: () -> Unit,
  onNavigateToNextDay: () -> Unit,
  isNextDayNavigationEnabled: Boolean,
  onSettingsClick: () -> Unit,
  modifier: Modifier = Modifier,
  containerColor: Color,
) {
  val formattedDate =
    remember(dayPlan?.date) {
      dayPlan?.date?.let { dateMillis ->
        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("uk"))
        Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).format(formatter)
      } ?: "План дня"
    }
  Surface(modifier = modifier.fillMaxWidth(), shadowElevation = 2.dp, color = containerColor) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          // REMOVED: .statusBarsPadding() - this was causing the big vertical space
          .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp) // Added top padding instead
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        IconButton(onClick = onNavigateToPreviousDay) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Попередній день",
          )
        }
        Text(
          text =
            formattedDate.replaceFirstChar {
              if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            },
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNavigateToNextDay, enabled = isNextDayNavigationEnabled) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Наступний день",
            tint =
              if (isNextDayNavigationEnabled) {
                MaterialTheme.colorScheme.onSurface
              } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
              },
          )
        }
        IconButton(onClick = onSettingsClick) {
          Icon(imageVector = Icons.Filled.Settings, contentDescription = "Налаштування")
        }
      }
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Всього балів: $totalPoints",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}
