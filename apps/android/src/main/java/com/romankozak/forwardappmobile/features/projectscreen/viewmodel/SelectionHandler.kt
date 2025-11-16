package com.romankozak.forwardappmobile.features.projectscreen.viewmodel

import com.romankozak.forwardappmobile.shared.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.shared.data.repository.GoalRepository
import com.romankozak.forwardappmobile.shared.data.repository.ProjectRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

interface SelectionHandlerResultListener {
  fun updateSelectionState(selectedIds: Set<String>)
}

@Inject
class SelectionHandler(
  private val projectRepository: ProjectRepository,
  private val goalRepository: GoalRepository,
  private val scope: CoroutineScope,
  private val projectIdFlow: StateFlow<String>,
  private val listContent: StateFlow<List<ListItemContent>>,
  private val listener: SelectionHandlerResultListener,
) {
  fun onSelectAll() {
    val allIds = listContent.value.map { it.listItem.id }.toSet()
    listener.updateSelectionState(allIds)
  }

  fun onDeselectAll() {
    listener.updateSelectionState(emptySet())
  }

  fun onDeleteSelected(selectedIds: Set<String>) {
    scope.launch(Dispatchers.IO) {
      val itemsToDelete = listContent.value.filter { it.listItem.id in selectedIds }
      val goalsToDelete = itemsToDelete.filterIsInstance<ListItemContent.GoalItem>().map { it.goal.id }
      val subprojectsToDelete = itemsToDelete.filterIsInstance<ListItemContent.SublistItem>().map { it.project }
      val linksToDelete = itemsToDelete.filterIsInstance<ListItemContent.LinkItem>().map { it.link.id }
      val notesToDelete = itemsToDelete.filterIsInstance<ListItemContent.NoteItem>().map { it.note.id }
      val noteDocumentsToDelete = itemsToDelete.filterIsInstance<ListItemContent.NoteDocumentItem>().map { it.document.id }
      val checklistsToDelete = itemsToDelete.filterIsInstance<ListItemContent.ChecklistItem>().map { it.checklist.id }

      if (goalsToDelete.isNotEmpty()) {
        goalRepository.deleteGoals(goalsToDelete)
      }
      if (subprojectsToDelete.isNotEmpty()) {
        projectRepository.deleteProjectsAndSubProjects(subprojectsToDelete)
      }
      if (linksToDelete.isNotEmpty()) {
        projectRepository.deleteLinks(linksToDelete)
      }
      if (notesToDelete.isNotEmpty()) {
        projectRepository.deleteNotes(notesToDelete)
      }
      if (noteDocumentsToDelete.isNotEmpty()) {
        projectRepository.deleteNoteDocuments(noteDocumentsToDelete)
      }
      if (checklistsToDelete.isNotEmpty()) {
        projectRepository.deleteChecklists(checklistsToDelete)
      }
      withContext(Dispatchers.Main) {
        listener.updateSelectionState(emptySet())
      }
    }
  }

  fun clearSelection() {
    listener.updateSelectionState(emptySet())
  }
}
