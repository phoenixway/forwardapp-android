package com.romankozak.forwardappmobile.ui.screens.customlist.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ListFormatMode {
  BULLET,
  NUMBERED,
  CHECKLIST,
  PLAIN,
}

enum class SortMode {
  NONE,
  ALPHABETICAL,
  CREATION_DATE,
  PRIORITY,
}

data class ListToolbarState(
  val isEditing: Boolean = false,
  val formatMode: ListFormatMode = ListFormatMode.BULLET,
  val sortMode: SortMode = SortMode.NONE,
  val totalItems: Int = 0,
  val hasSelection: Boolean = false,
  val canIndent: Boolean = true,
  val canDeIndent: Boolean = false,
  val canMoveUp: Boolean = true,
  val canMoveDown: Boolean = true,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedListToolbar(
  modifier: Modifier = Modifier,
  state: ListToolbarState,
  // Block actions
  onIndentBlock: () -> Unit,
  onDeIndentBlock: () -> Unit,
  onMoveBlockUp: () -> Unit,
  onMoveBlockDown: () -> Unit,
  // Line actions
  onIndentLine: () -> Unit,
  onDeIndentLine: () -> Unit,
  onMoveLineUp: () -> Unit,
  onMoveLineDown: () -> Unit,
  onDeleteLine: () -> Unit,
  onCopyLine: () -> Unit,
  onCutLine: () -> Unit,
  onPasteLine: () -> Unit,
  // Other actions
  onToggleBullet: () -> Unit,
  onToggleNumbered: () -> Unit = {},
  onToggleChecklist: () -> Unit = {},
  onUndo: () -> Unit = {},
  onRedo: () -> Unit = {},
) {
  val haptics = LocalHapticFeedback.current

  AnimatedVisibility(
    visible = state.isEditing,
    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
  ) {
    Card(
      modifier = modifier.fillMaxWidth(),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
      Column(modifier = Modifier.padding(12.dp)) {


        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              // Форматування
              ToolbarSection(title = "Формат") {
                EnhancedToolbarButton(
                  icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                  description = "Маркери",
                  isActive = state.formatMode == ListFormatMode.BULLET,
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleBullet()
                  },
                )
                EnhancedToolbarButton(
                  icon = Icons.Filled.FormatListNumbered,
                  description = "Нумерація",
                  isActive = state.formatMode == ListFormatMode.NUMBERED,
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleNumbered()
                  },
                )
                EnhancedToolbarButton(
                  icon = Icons.Default.TaskAlt, // альтернатива для чекліста
                  description = "Чекліст",
                  isActive = state.formatMode == ListFormatMode.CHECKLIST,
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleChecklist()
                  },
                )
              }

              VerticalDivider(
                modifier = Modifier.height(40.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
              )

              // Відступи
              ToolbarSection(title = "Відступи") {
                EnhancedToolbarButton(
                  icon = Icons.AutoMirrored.Filled.FormatIndentIncrease,
                  description = "Збільшити відступ",
                  enabled = state.canIndent,
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onIndentLine()
                  },
                )
                EnhancedToolbarButton(
                  icon = Icons.AutoMirrored.Filled.FormatIndentDecrease,
                  description = "Зменшити відступ",
                  enabled = state.canDeIndent,
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDeIndentLine()
                  },
                )
              }

              VerticalDivider(
                modifier = Modifier.height(40.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
              )

              // Переміщення
              ToolbarSection(title = "Рядки") {
                EnhancedToolbarButton(
                  icon = Icons.Default.KeyboardArrowUp,
                  description = "Рядок вгору",
                  enabled = state.canMoveUp,
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onMoveLineUp()
                  },
                )
                EnhancedToolbarButton(
                  icon = Icons.Default.KeyboardArrowDown,
                  description = "Рядок вниз",
                  enabled = state.canMoveDown,
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onMoveLineDown()
                  },
                )
              }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {

              // Дії з блоками
              ToolbarSection(title = "Блоки") {
                EnhancedToolbarButton(
                  icon = Icons.AutoMirrored.Filled.FormatIndentIncrease,
                  description = "Відступ блоку",
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onIndentBlock()
                  },
                )
                EnhancedToolbarButton(
                  icon = Icons.AutoMirrored.Filled.FormatIndentDecrease,
                  description = "Зняти відступ блоку",
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDeIndentBlock()
                  },
                )
                Spacer(modifier = Modifier.width(8.dp))
                EnhancedToolbarButton(
                  icon = Icons.Default.KeyboardDoubleArrowUp,
                  description = "Блок вгору",
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onMoveBlockUp()
                  },
                )
                EnhancedToolbarButton(
                  icon = Icons.Default.KeyboardDoubleArrowDown,
                  description = "Блок вниз",
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onMoveBlockDown()
                  },
                )
              }

              VerticalDivider(
                modifier = Modifier.height(40.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
              )
              
              ToolbarSection(title = "Редагування") {
                EnhancedToolbarButton(
                  icon = Icons.Default.DeleteOutline,
                  description = "Видалити рядок",
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDeleteLine()
                  },
                )
                EnhancedToolbarButton(
                  icon = Icons.Default.ContentCopy,
                  description = "Копіювати рядок",
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCopyLine()
                  },
                )
                EnhancedToolbarButton(
                  icon = Icons.Default.ContentCut,
                  description = "Вирізати рядок",
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCutLine()
                  },
                )
                EnhancedToolbarButton(
                  icon = Icons.Default.ContentPaste,
                  description = "Вставити рядок",
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPasteLine()
                  },
                )
              }

              VerticalDivider(
                modifier = Modifier.height(40.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
              )

              // Скасування/Повтор
              ToolbarSection(title = "Історія") {
                EnhancedToolbarButton(
                  icon = Icons.AutoMirrored.Filled.Undo,
                  description = "Скасувати",
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onUndo()
                  },
                )
                EnhancedToolbarButton(
                  icon = Icons.AutoMirrored.Filled.Redo,
                  description = "Повторити",
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onRedo()
                  },
                )
              }
            }
        }
      }
    }
  }
}

@Composable
private fun ToolbarSection(title: String, content: @Composable RowScope.() -> Unit) {
  Row(
    modifier = Modifier.padding(horizontal = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(2.dp),
    verticalAlignment = Alignment.CenterVertically,
    content = content
  )
}

@Composable
private fun EnhancedToolbarButton(
  icon: ImageVector,
  description: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isActive: Boolean = false,
) {
  val animatedColor by
    animateColorAsState(
      targetValue =
        when {
          isActive -> MaterialTheme.colorScheme.primary
          enabled -> MaterialTheme.colorScheme.onSurfaceVariant
          else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        },
      animationSpec = tween(200),
      label = "button_color",
    )

  val animatedBackgroundColor by
    animateColorAsState(
      targetValue =
        if (isActive) {
          MaterialTheme.colorScheme.primaryContainer
        } else {
          Color.Transparent
        },
      animationSpec = tween(200),
      label = "button_background",
    )

  FilledIconButton(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.size(36.dp),
    colors =
      IconButtonDefaults.filledIconButtonColors(
        containerColor = animatedBackgroundColor,
        contentColor = animatedColor,
        disabledContainerColor = Color.Transparent,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
      ),
  ) {
    Icon(imageVector = icon, contentDescription = description, modifier = Modifier.size(18.dp))
  }
}