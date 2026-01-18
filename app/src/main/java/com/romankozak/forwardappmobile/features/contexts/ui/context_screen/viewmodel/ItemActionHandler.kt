package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.viewmodel

import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.BacklogViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.GoalActionDialogState
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.GoalActionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.features.contexts.data.models.Goal
import com.romankozak.forwardappmobile.features.contexts.data.models.LinkType
import com.romankozak.forwardappmobile.features.contexts.data.models.BacklogItemContent
import com.romankozak.forwardappmobile.features.contexts.data.models.RelatedLink

class ItemActionHandler
@Inject
constructor(
    private val projectRepository: ProjectRepository,
    private val goalRepository: com.romankozak.forwardappmobile.data.repository.GoalRepository,
    private val recentItemsRepository: RecentItemsRepository,
    val scope: CoroutineScope,
    private val projectIdFlow: StateFlow<String>,
    private val resultListener: ResultListener,
) {
    interface ResultListener : BaseHandlerResultListener {
        fun isSelectionModeActive(): Boolean

        fun toggleSelection(itemId: String)

        fun requestAttachmentShare(item: BacklogItemContent)
    }

    private var recentlyDeletedItems: List<BacklogItemContent>? = null

    private val _goalActionDialogState = MutableStateFlow<GoalActionDialogState>(GoalActionDialogState.Hidden)
    val goalActionDialogState = _goalActionDialogState.asStateFlow()

    private val _showGoalTransportMenu = MutableStateFlow(false)
    val showGoalTransportMenu = _showGoalTransportMenu.asStateFlow()

    private val _itemForTransportMenu = MutableStateFlow<BacklogItemContent?>(null)
    val itemForTransportMenu = _itemForTransportMenu.asStateFlow()

    private val _onCopyContentToClipboard = MutableStateFlow<() -> Unit>({  })
    val onCopyContentToClipboard = _onCopyContentToClipboard.asStateFlow()

    fun onItemClick(item: BacklogItemContent) {
        if (item is BacklogItemContent.GoalItem) {
            // Одразу відкриваємо редагування цілі по тапу
            resultListener.requestNavigation("goal_settings_screen/${item.goal.id}")
            return
        }

        if (resultListener.isSelectionModeActive()) {
            resultListener.toggleSelection(item.backlogItem.id)
        } else {
            scope.launch {
                when (item) {
                    is BacklogItemContent.NoteItem -> recentItemsRepository.logNoteAccess(item.note)
                    is BacklogItemContent.NoteDocumentItem -> recentItemsRepository.logNoteDocumentAccess(item.document)
                    is BacklogItemContent.SublistItem -> {
                        projectRepository.getProjectById(item.project.id)?.let {
                            recentItemsRepository.logProjectAccess(it)
                        }
                    }
                    is BacklogItemContent.LinkItem -> {
                        if (item.link.linkData.type == LinkType.OBSIDIAN) {
                            recentItemsRepository.logObsidianLinkAccess(item.link.linkData)
                        }
                    }
                    is BacklogItemContent.ChecklistItem -> recentItemsRepository.logChecklistAccess(item.checklist)
                    else -> {}
                }
            }

            val currentProjectId = projectIdFlow.value
            when (item) {
                is BacklogItemContent.GoalItem ->
                    resultListener.requestNavigation(
                        "goal_settings_screen/${item.goal.id}",
                    )
                is BacklogItemContent.SublistItem ->
                    resultListener.requestNavigation("goal_detail_screen/${item.project.id}")
                is BacklogItemContent.LinkItem ->
                    resultListener.requestNavigation(BacklogViewModel.HANDLE_LINK_CLICK_ROUTE + "/${item.link.linkData.target}")
                is BacklogItemContent.NoteItem ->
                    resultListener.showSnackbar("Застарілі нотатки недоступні для редагування", null)
                is BacklogItemContent.NoteDocumentItem ->
                    resultListener.requestNavigation("note_document_screen/${item.document.id}")
                is BacklogItemContent.ChecklistItem ->
                    resultListener.requestNavigation("checklist_screen?checklistId=${item.checklist.id}")
            }
        }
    }

    fun deleteItem(item: BacklogItemContent) {
        scope.launch {
            recentlyDeletedItems = listOf(item)
            val currentProjectId = projectIdFlow.value
            val isAttachment =
                item is BacklogItemContent.LinkItem ||
                        item is BacklogItemContent.NoteDocumentItem ||
                        item is BacklogItemContent.ChecklistItem
            if (isAttachment) {
                projectRepository.unlinkAttachmentFromProject(currentProjectId, item.backlogItem.id)
                resultListener.forceRefresh()
                resultListener.showSnackbar("Вкладення видалено з проєкту", null)
            } else {
                projectRepository.deleteListItems(currentProjectId, listOf(item.backlogItem.id))
                resultListener.showSnackbar("Елемент видалено", "Скасувати")
            }
        }
    }

    fun shareAttachmentToProject(item: BacklogItemContent) {
        resultListener.requestAttachmentShare(item)
    }

    fun onGoalActionInitiated(item: BacklogItemContent) {
        _goalActionDialogState.value = GoalActionDialogState.AwaitingActionChoice(item)
    }

    fun onDismissGoalActionDialogs() {
        _goalActionDialogState.value = GoalActionDialogState.Hidden
    }

    fun onGoalActionSelected(
        actionType: GoalActionType,
        item: BacklogItemContent,
    ) {

        onItemActionSelected(actionType, item)
    }

    fun toggleGoalCompletedWithState(
        goal: Goal,
        isChecked: Boolean,
    ) {
        scope.launch {
            val updatedGoal = goal.copy(completed = isChecked, updatedAt = System.currentTimeMillis())
            goalRepository.updateGoal(updatedGoal)
            delay(100)
            resultListener.forceRefresh()
        }
    }

    fun copyContentRequest(content: BacklogItemContent) {
        scope.launch {
            val (message, text) =
                when (content) {
                    is BacklogItemContent.GoalItem -> Pair("Текст скопійовано", content.goal.text)
                    is BacklogItemContent.LinkItem -> {
                        val linkText = content.link.linkData.displayName ?: content.link.linkData.target
                        Pair("Посилання скопійовано", linkText)
                    }
                    is BacklogItemContent.SublistItem -> Pair("Назва проекту скопійована", content.project.name)
                    is BacklogItemContent.NoteItem -> Pair("Текст нотатки скопійовано", content.note.content)
                    is BacklogItemContent.NoteDocumentItem -> Pair("Назва списку скопійована", content.document.name)
                    is BacklogItemContent.ChecklistItem -> Pair("Назва чекліста скопійована", content.checklist.name)
                }

            resultListener.copyToClipboard(text)
            resultListener.showSnackbar(message, null)
        }
    }

    fun onGoalTransportInitiated(
        item: BacklogItemContent,
        onCopyContentToClipboard: () -> Unit,
    ) {
        _onCopyContentToClipboard.value = onCopyContentToClipboard
        if (item is BacklogItemContent.GoalItem || item is BacklogItemContent.SublistItem) {
            _itemForTransportMenu.value = item
            _showGoalTransportMenu.value = true
        } else {
            resultListener.showSnackbar("Транспорт доступний тільки для цілей та під-проектів", null)
        }
    }

    fun onDismissGoalTransportMenu() {
        _showGoalTransportMenu.value = false
        _itemForTransportMenu.value = null
    }

    fun onRelatedLinkClick(link: RelatedLink) {
        resultListener.requestNavigation(BacklogViewModel.HANDLE_LINK_CLICK_ROUTE + "/${link.target}")
    }


    fun onItemActionSelected(
        actionType: GoalActionType,
        item: BacklogItemContent,
    ) {
        when (item) {
            is BacklogItemContent.GoalItem -> {
                resultListener.setPendingAction(
                    actionType,
                    itemIds = setOf(item.backlogItem.id),
                    goalIds = setOf(item.goal.id),
                )
            }
            is BacklogItemContent.SublistItem -> {
                when (actionType) {
                    GoalActionType.CreateInstance -> {
                        resultListener.showSnackbar("Дія 'Створити посилання' недоступна для під-проектів.", null)
                        return
                    }
                    else -> {
                        resultListener.setPendingAction(
                            actionType,
                            itemIds = setOf(item.backlogItem.id),
                            goalIds = setOf(item.project.id),
                        )
                    }
                }
            }
            else -> {
                resultListener.showSnackbar("Ця дія недоступна для даного типу елемента.", null)
                return
            }
        }
        onDismissGoalActionDialogs()
    }

    fun undoDelete() {
        scope.launch {
            recentlyDeletedItems?.let { itemsToRestore ->
                val listItemsToRestore = itemsToRestore.map { it.backlogItem }
                projectRepository.restoreListItems(listItemsToRestore)
                resultListener.forceRefresh()
            }
            recentlyDeletedItems = null
        }
    }
}
