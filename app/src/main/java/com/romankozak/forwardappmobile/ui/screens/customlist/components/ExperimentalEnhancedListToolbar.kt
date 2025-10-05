package com.romankozak.forwardappmobile.ui.screens.customlist.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
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

enum class CommandGroup { РЕДАГУВАННЯ, СПИСКИ, ВСТАВКА, ФОРМАТУВАННЯ }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentalEnhancedListToolbar(
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
  onUndo: () -> Unit = {},
  onRedo: () -> Unit = {},
  onToggleVisibility: () -> Unit = {},
  onInsertDateTime: () -> Unit = {},
  onInsertTime: () -> Unit = {},
  onH1: () -> Unit = {},
  onH2: () -> Unit = {},
  onH3: () -> Unit = {},
  onBold: () -> Unit = {},
  onItalic: () -> Unit = {},
  onInsertSeparator: () -> Unit = {},
) {
  var selectedTab by remember { mutableStateOf(CommandGroup.РЕДАГУВАННЯ) }
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
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .height(24.dp)
            .fillMaxWidth()
            .clickable(onClick = onToggleVisibility),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Сховати тулбар",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
          )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScrollableTabRow(selectedTabIndex = selectedTab.ordinal, edgePadding = 0.dp) {
                CommandGroup.values().forEach { group ->
                    Tab(
                        modifier = Modifier.height(36.dp),
                        selected = selectedTab == group,
                        onClick = { selectedTab = group },
                        text = { Text(text = group.name, fontSize = 11.sp) }
                    )
                }
            }

            Crossfade(targetState = selectedTab) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    when (it) {
                        CommandGroup.РЕДАГУВАННЯ -> {
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
                        }
                        CommandGroup.СПИСКИ -> {
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
                            }
                            VerticalDivider(
                                modifier = Modifier.height(40.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
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
                            VerticalDivider(
                                modifier = Modifier.height(40.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
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
                        }
                        CommandGroup.ВСТАВКА -> {
                            ToolbarSection(title = "Вставка") {
                                EnhancedToolbarButton(
                                  icon = Icons.Default.DateRange,
                                  description = "Вставити дату і час",
                                  onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onInsertDateTime()
                                  },
                                )
                                EnhancedToolbarButton(
                                  icon = Icons.Default.AccessTime,
                                  description = "Вставити час",
                                  onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onInsertTime()
                                  },
                                )
                                EnhancedToolbarButton(
                                  icon = Icons.Default.HorizontalRule,
                                  description = "Вставити роздільник",
                                  onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onInsertSeparator()
                                  },
                                )
                            }
                        }
                        CommandGroup.ФОРМАТУВАННЯ -> {
                            ToolbarSection(title = "Заголовки") {
                                EnhancedToolbarButton(
                                  icon = Icons.Default.HMobiledata,
                                  description = "H1",
                                  onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onH1()
                                  },
                                )
                                EnhancedToolbarButton(
                                  icon = Icons.Default.HPlusMobiledata,
                                  description = "H2",
                                  onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onH2()
                                  },
                                )
                                 EnhancedToolbarButton(
                                  icon = Icons.Default.HPlusMobiledata,
                                  description = "H3",
                                  onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onH3()
                                  },
                                )
                            }
                            VerticalDivider(
                                modifier = Modifier.height(40.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            ToolbarSection(title = "Стиль") {
                                EnhancedToolbarButton(
                                  icon = Icons.Default.FormatBold,
                                  description = "Жирний",
                                  onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onBold()
                                  },
                                )
                                EnhancedToolbarButton(
                                  icon = Icons.Default.FormatItalic,
                                  description = "Курсив",
                                  onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onItalic()
                                  },
                                )
                            }
                        }
                    }
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