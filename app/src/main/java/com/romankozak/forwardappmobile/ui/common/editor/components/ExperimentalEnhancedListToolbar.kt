package com.romankozak.forwardappmobile.ui.common.editor.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer

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
  onToggleCheckbox: () -> Unit,
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
  onToggleKeyboard: () -> Unit = {},
) {
  var selectedTab by remember { mutableStateOf(CommandGroup.РЕДАГУВАННЯ) }
  val haptics = LocalHapticFeedback.current

  Surface(
    modifier = modifier.fillMaxWidth(),
    shadowElevation = if (state.isEditing) 12.dp else 4.dp,
    tonalElevation = 2.dp,
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Top accent line
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(3.dp)
          .background(
            MaterialTheme.colorScheme.primary.copy(
              alpha = if (state.isEditing) 0.6f else 0.3f
            )
          )
      )
      
      // Drag handle with pulsing animation
      val infiniteTransition = rememberInfiniteTransition(label = "pulse")
      val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
          animation = tween(1500, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
      )
      
      Box(
        modifier = Modifier
          .height(24.dp)
          .fillMaxWidth()
          .clickable(onClick = onToggleVisibility),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .width(48.dp)
            .height(5.dp)
            .clip(RoundedCornerShape(2.5.dp))
            .background(
              MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = if (state.isEditing) 0.5f else pulseAlpha
              )
            )
        )
      }
      
      AnimatedVisibility(
        visible = state.isEditing,
        enter = expandVertically(
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
          )
        ) + fadeIn(),
        exit = shrinkVertically(
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
          )
        ) + fadeOut(),
      ) {

        Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp)
              .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Modern tab row with chips style
            Row(verticalAlignment = Alignment.CenterVertically) {
                EnhancedToolbarButton(
                    icon = Icons.Default.Keyboard,
                    description = "Toggle Keyboard",
                    onClick = onToggleKeyboard
                )
                ScrollableTabRow(
                  selectedTabIndex = selectedTab.ordinal,
                  edgePadding = 0.dp,
                  containerColor = Color.Transparent,
                  indicator = { },
                  divider = { }
                ) {
                    CommandGroup.values().forEach { group ->
                        val isSelected = selectedTab == group
                        Tab(
                            modifier = Modifier.height(40.dp),
                            selected = isSelected,
                            onClick = {
                              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                              selectedTab = group
                            },
                        ) {
                          Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) 
                              MaterialTheme.colorScheme.primaryContainer 
                            else 
                              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                          ) {
                            Text(
                              text = group.name,
                              fontSize = 12.sp,
                              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                              color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                              else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                          }
                        }
                    }
                }
            }

            // Animated content with crossfade
            Crossfade(
              targetState = selectedTab,
              animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) { tab ->
                Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .horizontalScroll(rememberScrollState())
                      .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when (tab) {
                        CommandGroup.РЕДАГУВАННЯ -> {
                            ToolbarSection(title = "Історія") {
                                EnhancedToolbarButton(
                                  icon = Icons.AutoMirrored.Filled.Undo,
                                  description = "Скасувати",
                                  enabled = state.canUndo,
                                  onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUndo()
                                  },
                                )
                                EnhancedToolbarButton(
                                  icon = Icons.AutoMirrored.Filled.Redo,
                                  description = "Повторити",
                                  enabled = state.canRedo,
                                  onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onRedo()
                                  },
                                )
                            }
                            VerticalDivider(
                                modifier = Modifier.height(36.dp).padding(horizontal = 4.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                            ToolbarSection(title = "Редагування") {
                                EnhancedToolbarButton(
                                  icon = Icons.Default.DeleteOutline,
                                  description = "Видалити рядок",
                                  onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                EnhancedToolbarButton(
                                  icon = Icons.Default.Checklist,
                                  description = "Чекбокс",
                                  onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggleCheckbox()
                                  },
                                )
                            }
                            VerticalDivider(
                                modifier = Modifier.height(36.dp).padding(horizontal = 4.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
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
                                modifier = Modifier.height(36.dp).padding(horizontal = 4.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
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
                                modifier = Modifier.height(36.dp).padding(horizontal = 4.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
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
                                modifier = Modifier.height(36.dp).padding(horizontal = 4.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
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
    modifier = Modifier.padding(horizontal = 2.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
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
  val animatedScale by animateFloatAsState(
    targetValue = if (enabled) 1f else 0.95f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "button_scale"
  )

  val animatedColor by animateColorAsState(
    targetValue = when {
      isActive -> MaterialTheme.colorScheme.primary
      enabled -> MaterialTheme.colorScheme.onSurfaceVariant
      else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    },
    animationSpec = tween(250, easing = FastOutSlowInEasing),
    label = "button_color",
  )

  val animatedBackgroundColor by animateColorAsState(
    targetValue = if (isActive) {
      MaterialTheme.colorScheme.primaryContainer
    } else {
      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    },
    animationSpec = tween(250, easing = FastOutSlowInEasing),
    label = "button_background",
  )

  FilledIconButton(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier
      .size(36.dp)
      .graphicsLayer {
        scaleX = animatedScale
        scaleY = animatedScale
      },
    shape = RoundedCornerShape(12.dp),
    colors = IconButtonDefaults.filledIconButtonColors(
      containerColor = animatedBackgroundColor,
      contentColor = animatedColor,
      disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
      disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
    ),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = description,
      modifier = Modifier.size(20.dp)
    )
  }
}