package com.romankozak.forwardappmobile.features.projectscreen.viewmodel

import com.romankozak.forwardappmobile.shared.data.database.models.ChecklistEntity
import com.romankozak.forwardappmobile.shared.data.database.models.Goal
import com.romankozak.forwardappmobile.shared.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.shared.data.database.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.repository.GoalRepository
import com.romankozak.forwardappmobile.shared.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.shared.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.features.projectscreen.GoalActionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

interface ItemActionHandlerResultListener {
  fun requestNavigation(route: String)
  fun showSnackbar(message: String, action: String? = null)
  fun forceRefresh()
  fun isSelectionModeActive(): Boolean
  fun toggleSelection(itemId: String)
  fun requestAttachmentShare(item: ListItemContent)
  fun setPendingAction(actionType: GoalActionType, itemIds: Set<String>, goalIds: Set<String>)
}

@Inject
class ItemActionHandler(
  private val projectRepository: ProjectRepository,
  private val goalRepository: GoalRepository,
  private val recentItemsRepository: RecentItemsRepository,
  private val scope: CoroutineScope,
  private val projectIdFlow: StateFlow<String>,
  private val listener: ItemActionHandlerResultListener,
) {
  fun onGoalCompletedChange(goal: Goal, completed: Boolean) {
    scope.launch(Dispatchers.IO) {
      goalRepository.updateGoal(goal.copy(completed = completed))
      listener.forceRefresh()
    }
  }

  fun onGoalClick(goal: Goal) {
    listener.requestNavigation("goal_detail_screen/${goal.id}")
  }

  fun onSubprojectClick(subproject: Project) {
    listener.requestNavigation("project_detail_screen/${subproject.id}")
  }

  fun onLinkClick(link: RelatedLink) {
    listener.requestNavigation("handle_link_click/${link.target}")
  }

  fun onNoteClick(note: LegacyNoteEntity) {
    // legacy notes no longer have dedicated editor; no-op
  }

  fun onNoteDocumentClick(noteDocument: NoteDocumentEntity) {
    listener.requestNavigation("note_document_screen/${noteDocument.id}")
  }

  fun onChecklistClick(checklist: ChecklistEntity) {
    listener.requestNavigation("checklist_screen?checklistId=${checklist.id}")
  }

  fun onShareAttachment(item: ListItemContent) {
    listener.requestAttachmentShare(item)
  }

  fun onMoveItem(itemIds: Set<String>) {
    listener.setPendingAction(GoalActionType.MoveInstance, itemIds, emptySet())
  }

  fun onCopyGoal(goalIds: Set<String>) {
    listener.setPendingAction(GoalActionType.CopyGoal, emptySet(), goalIds)
  }

  fun onAddLinkToProject(goalIds: Set<String>) {
    listener.setPendingAction(GoalActionType.AddLinkToList, emptySet(), goalIds)
  }

  fun onAddListShortcut(goalIds: Set<String>) {
    listener.setPendingAction(GoalActionType.ADD_LIST_SHORTCUT, emptySet(), goalIds)
  }

  fun onToggleSelection(itemId: String) {
    listener.toggleSelection(itemId)
  }
}
