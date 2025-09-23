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
        // Статистика та індикатор режиму
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Статистика
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ListAlt,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "${state.totalItems} пунктів",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          // Індикатор поточного режиму форматування
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.padding(horizontal = 4.dp),
          ) {
            Text(
              text =
                when (state.formatMode) {
                  ListFormatMode.BULLET -> "Маркери"
                  ListFormatMode.NUMBERED -> "Нумерація"
                  ListFormatMode.CHECKLIST -> "Чекліст"
                  ListFormatMode.PLAIN -> "Простий"
                },
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }

        HorizontalDivider(
          modifier = Modifier.padding(vertical = 8.dp),
          color = MaterialTheme.colorScheme.outlineVariant,
        )

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
          ToolbarSection(title = "Переміщення") {
            EnhancedToolbarButton(
              icon = Icons.Default.KeyboardArrowUp,
              description = "Перемістити вгору",
              enabled = state.canMoveUp,
              onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onMoveLineUp()
              },
            )
            EnhancedToolbarButton(
              icon = Icons.Default.KeyboardArrowDown,
              description = "Перемістити вниз",
              enabled = state.canMoveDown,
              onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onMoveLineDown()
              },
            )
          }

          VerticalDivider(
            modifier = Modifier.height(40.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
          )

          // Дії з блоками
          ToolbarSection(title = "Блоки") {
            EnhancedToolbarButton(
              icon = Icons.Default.ExpandLess,
              description = "Блок вгору",
              onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onMoveBlockUp()
              },
            )
            EnhancedToolbarButton(
              icon = Icons.Default.ExpandMore,
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

@Composable
private fun ToolbarSection(title: String, content: @Composable RowScope.() -> Unit) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.padding(horizontal = 4.dp),
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 10.sp,
      modifier = Modifier.padding(bottom = 4.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), content = content)
  }
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
