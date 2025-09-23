package com.romankozak.forwardappmobile.ui.screens.customlist

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.screens.customlist.components.EnhancedListToolbar

enum class ScreenMode {
  CREATE,
  EDIT_EXISTING,
  VIEW,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedCustomListScreen(
  navController: NavController,
  viewModel: UnifiedCustomListViewModel = hiltViewModel(),
) {
  // Колір TopAppBar, який буде під прозорим рядком стану
  val topBarContainerColor = MaterialTheme.colorScheme.surfaceContainer
  val view = LocalView.current
  val isDarkTheme = isSystemInDarkTheme()

  if (!view.isInEditMode) {
    // LaunchedEffect для налаштування Edge-to-Edge. Виконується один раз.
    LaunchedEffect(Unit) {
      val window = (view.context as Activity).window
      // Дозволяє додатку малювати під системними панелями
      WindowCompat.setDecorFitsSystemWindows(window, false)

      // Робимо системні панелі прозорими
      window.statusBarColor = Color.Transparent.toArgb()
      window.navigationBarColor = Color.Transparent.toArgb()

      // Налаштовуємо колір іконок системних панелей для контрасту
      val insetsController = WindowCompat.getInsetsController(window, view)
      // Іконки рядка стану: світлі на темному фоні, темні на світлому
      insetsController.isAppearanceLightStatusBars = topBarContainerColor.luminance() > 0.5
      // Іконки панелі навігації: залежать від теми
      insetsController.isAppearanceLightNavigationBars = !isDarkTheme
    }
  }

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var screenMode by remember {
    mutableStateOf(if (viewModel.isNewList) ScreenMode.CREATE else ScreenMode.VIEW)
  }

  val keyboardController = LocalSoftwareKeyboardController.current
  val titleFocusRequester = remember { FocusRequester() }
  val contentFocusRequester = remember { FocusRequester() }

  val animatedBackgroundColor by
    animateColorAsState(
      targetValue =
        when (screenMode) {
          ScreenMode.VIEW -> MaterialTheme.colorScheme.surface
          else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
      animationSpec = tween(300),
      label = "background_color",
    )

  LaunchedEffect(screenMode) { viewModel.onToggleEditMode(screenMode != ScreenMode.VIEW) }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event: UnifiedCustomListEvent ->
      when (event) {
        is UnifiedCustomListEvent.NavigateBack -> {
          navController.previousBackStackEntry?.savedStateHandle?.set("refresh_needed", true)
          navController.popBackStack()
        }
        is UnifiedCustomListEvent.ShowError -> {}
        is UnifiedCustomListEvent.ShowSuccess -> {}
        is UnifiedCustomListEvent.AutoSaved -> {}
      }
    }
  }

  LaunchedEffect(screenMode) {
    when (screenMode) {
      ScreenMode.CREATE -> titleFocusRequester.requestFocus()
      ScreenMode.EDIT_EXISTING -> contentFocusRequester.requestFocus()
      ScreenMode.VIEW -> keyboardController?.hide()
    }
  }

  Scaffold(
    // Прибираємо Modifier.safeDrawingPadding() звідси
    modifier = Modifier.background(animatedBackgroundColor),
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      EnhancedTopAppBar(
        screenMode = screenMode,
        title = uiState.title,
        totalItems = uiState.toolbarState.totalItems,
        isLoading = uiState.isLoading,
        onNavigateBack = { navController.popBackStack() },
        onEdit = { screenMode = ScreenMode.EDIT_EXISTING },
        onSave = {
          viewModel.onSave()
          screenMode = ScreenMode.VIEW
        },
      )
    },
    bottomBar = {
      AnimatedVisibility(
        // Додаємо відступ для панелі навігації
        modifier = Modifier.navigationBarsPadding(),
        visible = screenMode != ScreenMode.VIEW,
        enter =
          slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
          ) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(),
      ) {
        EnhancedListToolbar(
          state = uiState.toolbarState,
          onIndentBlock = viewModel::onIndentBlock,
          onDeIndentBlock = viewModel::onDeIndentBlock,
          onMoveBlockUp = viewModel::onMoveBlockUp,
          onMoveBlockDown = viewModel::onMoveBlockDown,
          onIndentLine = viewModel::onIndentLine,
          onDeIndentLine = viewModel::onDeIndentLine,
          onMoveLineUp = viewModel::onMoveLineUp,
          onMoveLineDown = viewModel::onMoveLineDown,
          onDeleteLine = viewModel::onDeleteLine,
          onCopyLine = viewModel::onCopyLine,
          onCutLine = viewModel::onCutLine,
          onPasteLine = viewModel::onPasteLine,
          onToggleBullet = viewModel::onToggleBullet,
          onToggleNumbered = viewModel::onToggleNumbered,
          onToggleChecklist = viewModel::onToggleChecklist,
          onUndo = viewModel::onUndo,
          onRedo = viewModel::onRedo,
        )
      }
    },
    floatingActionButton = {
      AnimatedVisibility(
        visible = screenMode == ScreenMode.CREATE,
        enter =
          scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
            fadeIn(),
        exit = scaleOut(animationSpec = tween(200)) + fadeOut(),
      ) {
        ExtendedFloatingActionButton(
          onClick = viewModel::onSave,
          icon = { Icon(Icons.Default.Add, contentDescription = "Створити список") },
          text = { Text("Створити") },
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          // Додаємо відступ для панелі навігації
          modifier = Modifier.navigationBarsPadding().clip(RoundedCornerShape(16.dp)),
        )
      }
    },
  ) { paddingValues ->
    Box(
      modifier = Modifier.padding(paddingValues).fillMaxSize().background(animatedBackgroundColor)
    ) {
      when (screenMode) {
        ScreenMode.CREATE,
        ScreenMode.EDIT_EXISTING -> {
          CreateEditContent(
            uiState = uiState,
            viewModel = viewModel,
            titleFocusRequester = titleFocusRequester,
            contentFocusRequester = contentFocusRequester,
          )
        }
        ScreenMode.VIEW -> {
          ViewContent(
            uiState = uiState,
            viewModel = viewModel,
            onContentClick = { screenMode = ScreenMode.EDIT_EXISTING },
          )
        }
      }
      if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Card(
            colors =
              CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
              ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
          ) {
            Row(
              modifier = Modifier.padding(24.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
              Text(text = "Збереження...", style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class) // -> Corrected
@Composable
private fun EnhancedTopAppBar(
  screenMode: ScreenMode,
  title: String,
  totalItems: Int,
  isLoading: Boolean,
  onNavigateBack: () -> Unit,
  onEdit: () -> Unit,
  onSave: () -> Unit,
) {
  Card(
    // 1. Прибираємо відступ звідси. Тепер Card малюється під системним рядком.
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
    TopAppBar(
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(32.dp),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ListAlt,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.fillMaxSize().padding(6.dp),
            )
          }
          Column {
            Text(
              text =
                when (screenMode) {
                  ScreenMode.CREATE -> "Новий список"
                  ScreenMode.EDIT_EXISTING -> "Редагування"
                  ScreenMode.VIEW -> title.ifEmpty { "Список" }
                },
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.SemiBold,
            )
            if (screenMode == ScreenMode.VIEW && totalItems > 0) {
              Text(
                text = "$totalItems пунктів",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      },
      navigationIcon = {
        IconButton(onClick = onNavigateBack) {
          Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Назад",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      },
      actions = {
        when (screenMode) {
          ScreenMode.VIEW -> {
            IconButton(onClick = onEdit) {
              Icon(
                Icons.Default.Edit,
                contentDescription = "Редагувати",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
          ScreenMode.EDIT_EXISTING -> {
            FilledTonalIconButton(onClick = onSave, enabled = !isLoading) {
              if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
              } else {
                Icon(Icons.Default.Check, contentDescription = "Зберегти")
              }
            }
          }
          ScreenMode.CREATE -> {}
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
      // 2. Додаємо insets сюди. Тепер вміст TopAppBar має правильний відступ.
      windowInsets = WindowInsets.statusBars,
    )
  }
}

@Composable
private fun CreateEditContent(
  uiState: UnifiedCustomListUiState,
  viewModel: UnifiedCustomListViewModel,
  titleFocusRequester: FocusRequester,
  contentFocusRequester: FocusRequester,
) {
  Column(modifier = Modifier.fillMaxSize().imePadding().navigationBarsPadding()) {
    Card(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      colors =
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
      BasicTextField(
        value = uiState.title,
        onValueChange = viewModel::onTitleChange,
        modifier = Modifier.fillMaxWidth().padding(20.dp).focusRequester(titleFocusRequester),
        textStyle =
          TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          ),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
          if (uiState.title.isEmpty()) {
            Text(
              text = "Enter list title...",
              style =
                TextStyle(
                  fontSize = 24.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                ),
            )
          }
          innerTextField()
        },
      )
    }

    Card(
      modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp, vertical = 2.dp),
      colors =
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
      val textColor = MaterialTheme.colorScheme.onSurface
      BasicTextField(
        value = uiState.content,
        onValueChange = { newValue ->
          val oldValue = uiState.content
          if (
            newValue.text.length > oldValue.text.length &&
              newValue.text.count { it == '\n' } > oldValue.text.count { it == '\n' }
          ) {
            viewModel.onEnter(newValue)
          } else {
            viewModel.onContentChange(newValue)
          }
        },
        modifier = Modifier.fillMaxSize().focusRequester(contentFocusRequester),
        textStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, color = textColor),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        visualTransformation = ListVisualTransformation(uiState.collapsedLines, textColor),
        decorationBox = { innerTextField ->
          Row(Modifier.padding(vertical = 16.dp)) {
            Gutter(
              lines = uiState.content.text.lines(),
              collapsedLines = uiState.collapsedLines,
              onToggleFold = viewModel::onToggleFold,
            )
            Box(modifier = Modifier.padding(start = 16.dp)) { innerTextField() }
          }
        },
      )
    }
  }
}

@Composable
private fun Gutter(lines: List<String>, collapsedLines: Set<Int>, onToggleFold: (Int) -> Unit) {
  val focusManager = LocalFocusManager.current
  Column(modifier = Modifier.width(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    lines.forEachIndexed { index, line ->
      val indent = line.takeWhile { it.isWhitespace() }.length
      val nextIndent =
        if (index + 1 < lines.size) lines[index + 1].takeWhile { it.isWhitespace() }.length else -1
      val isParent = nextIndent > indent && line.isNotBlank()

      Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
        if (isParent) {
          val isCollapsed = collapsedLines.contains(index)
          val icon =
            if (isCollapsed) Icons.Default.ChevronRight else Icons.Default.KeyboardArrowDown
          Icon(
            imageVector = icon,
            contentDescription = if (isCollapsed) "Expand" else "Collapse",
            modifier =
              Modifier.size(16.dp).clickable {
                focusManager.clearFocus()
                onToggleFold(index)
              },
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

private class ListVisualTransformation(
  private val collapsedLines: Set<Int>,
  private val textColor: Color,
) : VisualTransformation {
  override fun filter(text: AnnotatedString): TransformedText {
    val originalText = text.text
    val lines = originalText.lines()
    val visibleLines = mutableListOf<IndexedValue<String>>()

    var i = 0
    while (i < lines.size) {
      val line = lines[i]
      visibleLines.add(IndexedValue(i, line))
      if (collapsedLines.contains(i)) {
        val indent = line.takeWhile { it.isWhitespace() }.length
        i++
        while (
          i < lines.size &&
            (lines[i].isBlank() || lines[i].takeWhile { it.isWhitespace() }.length > indent)
        ) {
          i++
        }
      } else {
        i++
      }
    }

    val transformedText = buildAnnotatedString {
      visibleLines.forEachIndexed { visibleIndex, (_, line) ->
        withStyle(style = SpanStyle(color = textColor)) { append(line) }
        if (visibleIndex < visibleLines.size - 1) {
          append("\n")
        }
      }
    }

    val offsetMapping =
      object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
          if (offset <= 0) return 0
          val prefix = originalText.substring(0, offset)
          val parts = prefix.lines()
          val originalLineIndex = parts.size - 1
          val charInLine = parts.lastOrNull()?.length ?: 0

          var transformedLineStart = 0
          var found = false
          for (v in visibleLines) {
            if (v.index == originalLineIndex) {
              found = true
              break
            }
            transformedLineStart += v.value.length + 1
          }
          if (!found) return transformedText.length
          return (transformedLineStart + charInLine).coerceIn(0, transformedText.length)
        }

        override fun transformedToOriginal(offset: Int): Int {
          if (offset <= 0) return 0
          val prefix = transformedText.substring(0, offset)
          val parts = prefix.lines()
          val transformedLineIndex = parts.size - 1
          val charInLine = parts.lastOrNull()?.length ?: 0
          if (transformedLineIndex >= visibleLines.size) return originalText.length
          val originalLineIndex = visibleLines[transformedLineIndex].index
          val originalLineStart = lines.take(originalLineIndex).sumOf { it.length + 1 }
          return (originalLineStart + charInLine).coerceIn(0, originalText.length)
        }
      }

    return TransformedText(transformedText, offsetMapping)
  }
}

@Composable
private fun ViewContent(
  uiState: UnifiedCustomListUiState,
  viewModel: UnifiedCustomListViewModel,
  onContentClick: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      shape = RoundedCornerShape(16.dp),
    ) {
      Text(
        text = uiState.title.ifEmpty { "Без назви" },
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.padding(24.dp),
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Card(
      modifier = Modifier.fillMaxWidth().clickable { onContentClick() },
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      shape = RoundedCornerShape(12.dp),
    ) {
      if (uiState.content.text.isBlank()) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(48.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Icon(
            imageVector = Icons.Default.EditNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp),
          )
          Text(
            text = "Список порожній",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
          )
          Text(
            text = "Натисніть щоб додати елементи",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
          )
        }
      } else {
        val annotatedString = buildAnnotatedString {
          val lines = uiState.content.text.lines()
          var i = 0
          while (i < lines.size) {
            val line = lines[i]
            val indent = line.takeWhile { it.isWhitespace() }.length
            val isCollapsed = uiState.collapsedLines.contains(i)

            withStyle(
              style =
                SpanStyle(background = if (isCollapsed) Color.LightGray else Color.Transparent)
            ) {
              append(line)
            }
            append("\n")

            if (isCollapsed) {
              i++
              while (
                i < lines.size &&
                  (lines[i].isBlank() || lines[i].takeWhile { it.isWhitespace() }.length > indent)
              ) {
                i++
              }
            } else {
              i++
            }
          }
        }
        Text(
          text = annotatedString,
          modifier = Modifier.padding(16.dp),
          style = TextStyle(lineHeight = 20.sp),
        )
      }
    }
    // Додатковий відступ знизу, щоб контент не ховався за панеллю навігації
    Spacer(modifier = Modifier.navigationBarsPadding())
  }
}
