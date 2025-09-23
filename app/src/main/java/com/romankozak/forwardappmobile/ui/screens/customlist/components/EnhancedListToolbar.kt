package com.romankozak.forwardappmobile.ui.screens.customlist.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
  val hasSelection: Boolean = false,
  val selectedCount: Int = 0,
  val totalItems: Int = 0,
  val canUndo: Boolean = false,
  val isSelectAllMode: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedListToolbar(
  state: ListToolbarState,
  onToggleEdit: () -> Unit,
  onAddItem: () -> Unit,
  onFormatChange: (ListFormatMode) -> Unit,
  onSortChange: (SortMode) -> Unit,
  onSelectAll: () -> Unit,
  onClearSelection: () -> Unit,
  onDeleteSelected: () -> Unit,
  onUndo: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showFormatMenu by remember { mutableStateOf(false) }
  var showSortMenu by remember { mutableStateOf(false) }
  var showMoreMenu by remember { mutableStateOf(false) }

  Card(
    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (state.isEditing) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
          else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = if (state.isEditing) 4.dp else 2.dp),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      // Main toolbar row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Left side - Mode indicator and stats
        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Mode indicator
          Surface(
            shape = CircleShape,
            color =
              if (state.isEditing) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(32.dp),
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.clickable { onToggleEdit() }.padding(8.dp),
            ) {
              Icon(
                imageVector = if (state.isEditing) Icons.Default.Check else Icons.Default.Edit,
                contentDescription = if (state.isEditing) "Finish editing" else "Start editing",
                tint =
                  if (state.isEditing) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
              )
            }
          }

          // Stats and selection info
          Column {
            if (state.hasSelection) {
              Text(
                text = "${state.selectedCount} selected",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
              )
            } else {
              Text(
                text = "${state.totalItems} items",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            if (state.isEditing) {
              Text(
                text = "Editing mode",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
              )
            }
          }
        }

        // Right side - Action buttons
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (state.isEditing) {
            EditModeActions(
              state = state,
              onAddItem = onAddItem,
              onSelectAll = onSelectAll,
              onClearSelection = onClearSelection,
              onDeleteSelected = onDeleteSelected,
              onUndo = onUndo,
            )
          }

          // Format menu
          Box {
            IconButton(onClick = { showFormatMenu = true }) {
              Icon(
                imageVector =
                  when (state.formatMode) {
                    ListFormatMode.BULLET -> Icons.Default.FormatListBulleted
                    ListFormatMode.NUMBERED -> Icons.Default.FormatListNumbered
                    ListFormatMode.CHECKLIST -> Icons.Default.CheckBox
                    ListFormatMode.PLAIN -> Icons.Default.FormatAlignLeft
                  },
                contentDescription = "Change format",
                tint = MaterialTheme.colorScheme.primary,
              )
            }

            FormatDropdownMenu(
              expanded = showFormatMenu,
              onDismiss = { showFormatMenu = false },
              currentMode = state.formatMode,
              onFormatChange = { format ->
                onFormatChange(format)
                showFormatMenu = false
              },
            )
          }

          // Sort menu
          Box {
            IconButton(onClick = { showSortMenu = true }) {
              Icon(
                imageVector = Icons.Default.Sort,
                contentDescription = "Sort options",
                tint =
                  if (state.sortMode != SortMode.NONE) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            SortDropdownMenu(
              expanded = showSortMenu,
              onDismiss = { showSortMenu = false },
              currentMode = state.sortMode,
              onSortChange = { sort ->
                onSortChange(sort)
                showSortMenu = false
              },
            )
          }

          // More options menu
          Box {
            IconButton(onClick = { showMoreMenu = true }) {
              Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            MoreOptionsMenu(expanded = showMoreMenu, onDismiss = { showMoreMenu = false })
          }
        }
      }

      // Secondary toolbar for active selections
      AnimatedVisibility(
        visible = state.hasSelection,
        enter = fadeIn() + expandHorizontally(),
        exit = fadeOut() + shrinkHorizontally(),
      ) {
        Column {
          Spacer(modifier = Modifier.height(8.dp))
          SelectionToolbar(
            selectedCount = state.selectedCount,
            totalItems = state.totalItems,
            onSelectAll = onSelectAll,
            onClearSelection = onClearSelection,
            onDeleteSelected = onDeleteSelected,
          )
        }
      }
    }
  }
}

@Composable
private fun EditModeActions(
  state: ListToolbarState,
  onAddItem: () -> Unit,
  onSelectAll: () -> Unit,
  onClearSelection: () -> Unit,
  onDeleteSelected: () -> Unit,
  onUndo: () -> Unit,
) {
  // Undo button
  AnimatedVisibility(
    visible = state.canUndo,
    enter = fadeIn() + expandHorizontally(),
    exit = fadeOut() + shrinkHorizontally(),
  ) {
    Row {
      IconButton(onClick = onUndo) {
        Icon(
          imageVector = Icons.Default.Undo,
          contentDescription = "Undo",
          tint = MaterialTheme.colorScheme.primary,
        )
      }
      Spacer(modifier = Modifier.width(4.dp))
    }
  }

  // Add item button
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.size(32.dp),
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.clickable { onAddItem() }) {
      Icon(
        imageVector = Icons.Default.Add,
        contentDescription = "Add item",
        tint = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.size(16.dp),
      )
    }
  }
}

@Composable
private fun SelectionToolbar(
  selectedCount: Int,
  totalItems: Int,
  onSelectAll: () -> Unit,
  onClearSelection: () -> Unit,
  onDeleteSelected: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
    shape = RoundedCornerShape(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "$selectedCount of $totalItems selected",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )

        if (selectedCount < totalItems) {
          Text(
            text = "Select all",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onSelectAll() },
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = onClearSelection) {
          Icon(
            imageVector = Icons.Default.CheckBoxOutlineBlank,
            contentDescription = "Clear selection",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        IconButton(onClick = onDeleteSelected) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete selected",
            tint = MaterialTheme.colorScheme.error,
          )
        }
      }
    }
  }
}

@Composable
private fun FormatDropdownMenu(
  expanded: Boolean,
  onDismiss: () -> Unit,
  currentMode: ListFormatMode,
  onFormatChange: (ListFormatMode) -> Unit,
) {
  DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
    FormatMenuItem(
      icon = Icons.Default.FormatListBulleted,
      text = "Bullet List",
      isSelected = currentMode == ListFormatMode.BULLET,
      onClick = { onFormatChange(ListFormatMode.BULLET) },
    )
    FormatMenuItem(
      icon = Icons.Default.FormatListNumbered,
      text = "Numbered List",
      isSelected = currentMode == ListFormatMode.NUMBERED,
      onClick = { onFormatChange(ListFormatMode.NUMBERED) },
    )
    FormatMenuItem(
      icon = Icons.Default.CheckBox,
      text = "Checklist",
      isSelected = currentMode == ListFormatMode.CHECKLIST,
      onClick = { onFormatChange(ListFormatMode.CHECKLIST) },
    )
    FormatMenuItem(
      icon = Icons.Default.FormatAlignLeft,
      text = "Plain Text",
      isSelected = currentMode == ListFormatMode.PLAIN,
      onClick = { onFormatChange(ListFormatMode.PLAIN) },
    )
  }
}

@Composable
private fun SortDropdownMenu(
  expanded: Boolean,
  onDismiss: () -> Unit,
  currentMode: SortMode,
  onSortChange: (SortMode) -> Unit,
) {
  DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
    SortMenuItem(
      text = "No Sorting",
      isSelected = currentMode == SortMode.NONE,
      onClick = { onSortChange(SortMode.NONE) },
    )
    SortMenuItem(
      text = "Alphabetical",
      isSelected = currentMode == SortMode.ALPHABETICAL,
      onClick = { onSortChange(SortMode.ALPHABETICAL) },
    )
    SortMenuItem(
      text = "Creation Date",
      isSelected = currentMode == SortMode.CREATION_DATE,
      onClick = { onSortChange(SortMode.CREATION_DATE) },
    )
    SortMenuItem(
      text = "Priority",
      isSelected = currentMode == SortMode.PRIORITY,
      onClick = { onSortChange(SortMode.PRIORITY) },
    )
  }
}

@Composable
private fun MoreOptionsMenu(expanded: Boolean, onDismiss: () -> Unit) {
  DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
    DropdownMenuItem(
      text = { Text("Export List") },
      onClick = { /* TODO */ },
      leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
    )
    DropdownMenuItem(
      text = { Text("Change Colors") },
      onClick = { /* TODO */ },
      leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
    )
  }
}

@Composable
private fun FormatMenuItem(
  icon: ImageVector,
  text: String,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  DropdownMenuItem(
    text = {
      Text(
        text,
        color =
          if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
      )
    },
    onClick = onClick,
    leadingIcon = {
      Icon(
        icon,
        contentDescription = null,
        tint =
          if (isSelected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    },
    trailingIcon =
      if (isSelected) {
        {
          Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
          )
        }
      } else null,
  )
}

@Composable
private fun SortMenuItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
  DropdownMenuItem(
    text = {
      Text(
        text,
        color =
          if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
      )
    },
    onClick = onClick,
    trailingIcon =
      if (isSelected) {
        {
          Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
          )
        }
      } else null,
  )
}
