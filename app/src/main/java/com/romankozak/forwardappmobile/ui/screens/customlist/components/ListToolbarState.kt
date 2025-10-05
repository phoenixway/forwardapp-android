package com.romankozak.forwardappmobile.ui.screens.customlist.components

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
      val canUndo: Boolean = false,
    val canRedo: Boolean = false
)
