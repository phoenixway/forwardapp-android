package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.viewmodel

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.features.contexts.domain.clipboard.BacklogClipboardUseCase
import com.romankozak.forwardappmobile.features.contexts.domain.clipboard.BacklogPasteMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.ContextScreenViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.GoalActionDialogState
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.GoalActionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

class ItemActionHandler
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
        private val goalRepository: com.romankozak.forwardappmobile.data.repository.GoalRepository,
        private val recentItemsRepository: RecentItemsRepository,
        private val backlogClipboardUseCase: BacklogClipboardUseCase,
        val scope: CoroutineScope,
        private val projectIdFlow: StateFlow<String>,
        private val resultListener: ResultListener,
    ) {
        interface ResultListener : BaseHandlerResultListener {
            fun isSelectionModeActive(): Boolean

            fun toggleSelection(itemId: String)

            fun requestAttachmentShare(item: BacklogItemContent)

            fun requestNavigation(route: String)

            fun setPendingAction(
                actionType: GoalActionType,
                itemIds: Set<String>,
                goalIds: Set<String>,
            )
        }

        private var recentlyDeletedItems: List<BacklogItemContent>? = null

        private val _goalActionDialogState = MutableStateFlow<GoalActionDialogState>(GoalActionDialogState.Hidden)
        val goalActionDialogState = _goalActionDialogState.asStateFlow()

        private val _showGoalTransportMenu = MutableStateFlow(false)
        val showGoalTransportMenu = _showGoalTransportMenu.asStateFlow()

        private val _itemForTransportMenu = MutableStateFlow<BacklogItemContent?>(null)
        val itemForTransportMenu = _itemForTransportMenu.asStateFlow()

        private val _showPasteModeDialog = MutableStateFlow(false)
        val showPasteModeDialog = _showPasteModeDialog.asStateFlow()

        private val _canPasteIntoCurrentBacklog = MutableStateFlow(false)
        val canPasteIntoCurrentBacklog = _canPasteIntoCurrentBacklog.asStateFlow()

        init {
            scope.launch {
                combine(projectIdFlow, backlogClipboardUseCase.clipboardPayload) { contextId, _ ->
                    backlogClipboardUseCase.canPasteIntoBacklog(contextId)
                }.collect { canPaste ->
                    _canPasteIntoCurrentBacklog.value = canPaste
                }
            }
        }

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
                        is BacklogItemContent.ContextLinkItem -> {
                            contextRepository.getContextById(item.project.id)?.let {
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
                    is BacklogItemContent.ContextLinkItem ->
                        resultListener.requestNavigation("goal_detail_screen/${item.project.id}")
                    is BacklogItemContent.LinkItem ->
                        resultListener.requestNavigation(ContextScreenViewModel.HANDLE_LINK_CLICK_ROUTE + "/${item.link.linkData.target}")
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
                    contextRepository.unlinkAttachmentFromContext(currentProjectId, item.backlogItem.id)
                    resultListener.forceRefresh()
                    resultListener.showSnackbar("Вкладення видалено з проєкту", null)
                } else {
                    contextRepository.deleteListItemsFromContext(currentProjectId, listOf(item.backlogItem.id))
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
                        is BacklogItemContent.ContextLinkItem -> Pair("Назва проекту скопійована", content.project.name)
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
        ) {
            if (item is BacklogItemContent.GoalItem || item is BacklogItemContent.ContextLinkItem) {
                _itemForTransportMenu.value = item
                _showGoalTransportMenu.value = true
            } else {
                resultListener.showSnackbar("Транспорт доступний тільки для цілей і посилань на контексти", null)
            }
        }

        fun onDismissGoalTransportMenu() {
            _showGoalTransportMenu.value = false
            _itemForTransportMenu.value = null
        }

        fun onDismissPasteModeDialog() {
            _showPasteModeDialog.value = false
        }

        fun onTransportCopyRequested() {
            when (val item = _itemForTransportMenu.value) {
                is BacklogItemContent.GoalItem ->
                    backlogClipboardUseCase.copyBacklogGoals(
                        sourceContextId = projectIdFlow.value,
                        goalIds = listOf(item.goal.id),
                    )

                is BacklogItemContent.ContextLinkItem ->
                    backlogClipboardUseCase.copyBacklogContextLinks(
                        sourceContextId = projectIdFlow.value,
                        contextIds = listOf(item.project.id),
                    )

                else -> {
                    resultListener.showSnackbar("Для копіювання вибери ціль або посилання на контекст", null)
                    return
                }
            }
            onDismissGoalTransportMenu()
            resultListener.showSnackbar("Скопійовано. Перейди в цільовий беклог і натисни Вставити", null)
        }

        fun onTransportCutRequested() {
            val item = _itemForTransportMenu.value
            val backlogItemId = item?.backlogItem?.id
            if (backlogItemId.isNullOrBlank()) {
                resultListener.showSnackbar("Для вирізання вибери ціль або посилання на контекст", null)
                return
            }
            backlogClipboardUseCase.cutBacklogGoals(
                sourceContextId = projectIdFlow.value,
                listItemIds = listOf(backlogItemId),
            )
            onDismissGoalTransportMenu()
            resultListener.showSnackbar("Вирізано. Перейди в цільовий беклог і натисни Вставити", null)
        }

        fun onTransportPasteRequested() {
            if (!backlogClipboardUseCase.hasPayload()) {
                resultListener.showSnackbar("Буфер порожній", null)
                return
            }
            onDismissGoalTransportMenu()
            if (backlogClipboardUseCase.isCopyOperation() && backlogClipboardUseCase.copyPayloadHasGoals()) {
                _showPasteModeDialog.value = true
            } else {
                pasteIntoCurrentBacklog(BacklogPasteMode.AS_LINK)
            }
        }

        fun onPasteModeSelected(mode: BacklogPasteMode) {
            _showPasteModeDialog.value = false
            pasteIntoCurrentBacklog(mode)
        }

        private fun pasteIntoCurrentBacklog(mode: BacklogPasteMode) {
            scope.launch {
                val report =
                    backlogClipboardUseCase.pasteBacklogGoals(
                        targetContextId = projectIdFlow.value,
                        mode = mode,
                    )
                resultListener.showSnackbar(report.toUserMessage(), null)
                resultListener.forceRefresh()
            }
        }

        fun onRelatedLinkClick(link: RelatedLink) {
            resultListener.requestNavigation(ContextScreenViewModel.HANDLE_LINK_CLICK_ROUTE + "/${link.target}")
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
                is BacklogItemContent.ContextLinkItem -> {
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
                    contextRepository.restoreListItems(listItemsToRestore)
                    resultListener.forceRefresh()
                }
                recentlyDeletedItems = null
            }
        }
    }
