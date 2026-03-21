package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.viewmodel

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.InboxRepository
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
        private val inboxRepository: InboxRepository,
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

            fun openGoalInlineEditor(goal: Goal)

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

        private val _showAttachmentPasteDialog = MutableStateFlow(false)
        val showAttachmentPasteDialog = _showAttachmentPasteDialog.asStateFlow()

        private val _canPasteIntoCurrentBacklog = MutableStateFlow(false)
        val canPasteIntoCurrentBacklog = _canPasteIntoCurrentBacklog.asStateFlow()

        private val _canPasteIntoCurrentDirection = MutableStateFlow(false)
        val canPasteIntoCurrentDirection = _canPasteIntoCurrentDirection.asStateFlow()

        private val _canPasteIntoCurrentAttachments = MutableStateFlow(false)
        val canPasteIntoCurrentAttachments = _canPasteIntoCurrentAttachments.asStateFlow()

        private val _canPasteIntoCurrentInbox = MutableStateFlow(false)
        val canPasteIntoCurrentInbox = _canPasteIntoCurrentInbox.asStateFlow()

        private var pendingPasteTargetViewMode: ContextViewMode? = null
        private var pendingIncludeAttachmentsForPaste: Boolean = false
        private var pendingAddSourceContextLinkForGoalLinks: Boolean = false

        init {
            scope.launch {
                combine(projectIdFlow, backlogClipboardUseCase.clipboardPayload) { contextId, _ ->
                    backlogClipboardUseCase.canPasteIntoBacklog(contextId)
                }.collect { canPaste ->
                    _canPasteIntoCurrentBacklog.value = canPaste
                }
            }
            scope.launch {
                combine(projectIdFlow, backlogClipboardUseCase.clipboardPayload) { contextId, _ ->
                    backlogClipboardUseCase.canPasteIntoDirection(contextId)
                }.collect { canPaste ->
                    _canPasteIntoCurrentDirection.value = canPaste
                }
            }
            scope.launch {
                combine(projectIdFlow, backlogClipboardUseCase.clipboardPayload) { contextId, _ ->
                    backlogClipboardUseCase.canPasteIntoAttachments(contextId)
                }.collect { canPaste ->
                    _canPasteIntoCurrentAttachments.value = canPaste
                }
            }
            scope.launch {
                combine(projectIdFlow, backlogClipboardUseCase.clipboardPayload) { contextId, _ ->
                    backlogClipboardUseCase.canPasteIntoInbox(contextId)
                }.collect { canPaste ->
                    _canPasteIntoCurrentInbox.value = canPaste
                }
            }
        }

        fun onItemClick(item: BacklogItemContent) {
            if (resultListener.isSelectionModeActive()) {
                resultListener.toggleSelection(item.backlogItem.id)
            } else {
                if (item is BacklogItemContent.GoalItem) {
                    resultListener.openGoalInlineEditor(item.goal)
                    return
                }

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
                        is BacklogItemContent.MusicNoteItem -> {}
                        else -> {}
                    }
                }

                val currentProjectId = projectIdFlow.value
                when (item) {
                    is BacklogItemContent.ContextLinkItem ->
                        resultListener.requestNavigation("goal_detail_screen/${item.project.id}")
                    is BacklogItemContent.LinkItem ->
                        resultListener.requestNavigation(ContextScreenViewModel.HANDLE_LINK_CLICK_ROUTE + "/${item.link.linkData.target}")
                    is BacklogItemContent.NoteItem ->
                        resultListener.showSnackbar("Застарілі нотатки недоступні для редагування", null)
                    is BacklogItemContent.NoteDocumentItem ->
                        resultListener.requestNavigation("note_document_screen/${item.document.id}")
                    is BacklogItemContent.MusicNoteItem ->
                        resultListener.requestNavigation("music_note_screen/${item.musicNote.id}")
                    is BacklogItemContent.ChecklistItem ->
                        resultListener.requestNavigation("checklist_screen?checklistId=${item.checklist.id}")
                    is BacklogItemContent.GoalItem -> Unit
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
                        item is BacklogItemContent.MusicNoteItem ||
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
                        is BacklogItemContent.MusicNoteItem -> Pair("Назва нот скопійована", content.musicNote.name)
                        is BacklogItemContent.ChecklistItem -> Pair("Назва чекліста скопійована", content.checklist.name)
                    }

                resultListener.copyToClipboard(text)
                resultListener.showSnackbar(message, null)
            }
        }

        fun onGoalTransportInitiated(item: BacklogItemContent) {
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
            pendingPasteTargetViewMode = null
            pendingIncludeAttachmentsForPaste = false
            pendingAddSourceContextLinkForGoalLinks = false
        }

        fun onDismissAttachmentPasteDialog() {
            _showAttachmentPasteDialog.value = false
            pendingPasteTargetViewMode = null
            pendingIncludeAttachmentsForPaste = false
            pendingAddSourceContextLinkForGoalLinks = false
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

        fun onTransportPasteRequested(targetViewMode: ContextViewMode) {
            if (!backlogClipboardUseCase.hasPayload()) {
                resultListener.showSnackbar("Буфер порожній", null)
                return
            }
            onDismissGoalTransportMenu()
            pendingPasteTargetViewMode = targetViewMode
            if (
                (targetViewMode == ContextViewMode.BACKLOG || targetViewMode == ContextViewMode.DIRECTION) &&
                backlogClipboardUseCase.hasAttachmentRefsInClipboard()
            ) {
                _showAttachmentPasteDialog.value = true
                return
            }
            continuePaste(targetViewMode = targetViewMode, includeAttachments = targetViewMode == ContextViewMode.CONNECTIONS)
        }

        fun onAttachmentPasteDecision(includeAttachments: Boolean) {
            val target = pendingPasteTargetViewMode
            _showAttachmentPasteDialog.value = false
            if (target == null) return
            continuePaste(targetViewMode = target, includeAttachments = includeAttachments || target == ContextViewMode.CONNECTIONS)
        }

        private fun continuePaste(
            targetViewMode: ContextViewMode,
            includeAttachments: Boolean,
        ) {
            when (targetViewMode) {
                ContextViewMode.BACKLOG -> {
                    pendingIncludeAttachmentsForPaste = includeAttachments
                    if (backlogClipboardUseCase.isCopyOperation() && backlogClipboardUseCase.copyPayloadHasGoals()) {
                        _showPasteModeDialog.value = true
                    } else {
                        pasteIntoCurrentBacklog(mode = BacklogPasteMode.AS_LINK, includeAttachments = includeAttachments)
                        pendingPasteTargetViewMode = null
                    }
                }

                ContextViewMode.DIRECTION -> {
                    pasteIntoCurrentDirection(includeAttachments = includeAttachments)
                    pendingPasteTargetViewMode = null
                }

                ContextViewMode.CONNECTIONS -> {
                    pasteIntoCurrentAttachments()
                    pendingPasteTargetViewMode = null
                }

                ContextViewMode.INBOX -> {
                    pasteIntoCurrentInbox()
                    pendingPasteTargetViewMode = null
                }

                else -> {
                    pendingPasteTargetViewMode = null
                    resultListener.showSnackbar("Вставка підтримується лише у беклозі, інбоксі, напрямку або вкладеннях", null)
                }
            }
        }

        private fun pasteIntoCurrentInbox() {
            val targetContextId = projectIdFlow.value
            if (targetContextId.isBlank()) {
                resultListener.showSnackbar("Контекст не визначено", null)
                return
            }
            scope.launch {
                val insertedCount = backlogClipboardUseCase.pasteIntoInbox(targetContextId)
                if (insertedCount > 0) {
                    resultListener.forceRefresh()
                    resultListener.showSnackbar("Додано записів в інбокс: $insertedCount", null)
                } else {
                    resultListener.showSnackbar("Немає валідних елементів для вставки", null)
                }
            }
        }

        fun onPasteModeSelected(
            mode: BacklogPasteMode,
            addSourceContextLink: Boolean,
        ) {
            _showPasteModeDialog.value = false
            pendingAddSourceContextLinkForGoalLinks = addSourceContextLink
            pasteIntoCurrentBacklog(
                mode = mode,
                includeAttachments = pendingIncludeAttachmentsForPaste,
                addSourceContextLinkForGoalLinks = pendingAddSourceContextLinkForGoalLinks,
            )
            pendingPasteTargetViewMode = null
            pendingIncludeAttachmentsForPaste = false
            pendingAddSourceContextLinkForGoalLinks = false
        }

        private fun pasteIntoCurrentBacklog(
            mode: BacklogPasteMode,
            includeAttachments: Boolean,
            addSourceContextLinkForGoalLinks: Boolean = false,
        ) {
            scope.launch {
                val report =
                    backlogClipboardUseCase.pasteBacklogGoals(
                        targetContextId = projectIdFlow.value,
                        mode = mode,
                        includeAttachments = includeAttachments,
                        addSourceContextLinkForGoalLinks = addSourceContextLinkForGoalLinks,
                    )
                resultListener.showSnackbar(report.toUserMessage(), null)
                resultListener.forceRefresh()
            }
        }

        private fun pasteIntoCurrentDirection(includeAttachments: Boolean) {
            scope.launch {
                val report =
                    backlogClipboardUseCase.pasteIntoDirection(
                        targetContextId = projectIdFlow.value,
                        includeAttachments = includeAttachments,
                    )
                resultListener.showSnackbar(report.toUserMessage(), null)
                resultListener.forceRefresh()
            }
        }

        private fun pasteIntoCurrentAttachments() {
            scope.launch {
                val report = backlogClipboardUseCase.pasteIntoAttachments(targetContextId = projectIdFlow.value)
                resultListener.showSnackbar(report.toUserMessage(), null)
                resultListener.forceRefresh()
            }
        }

        fun copyAttachmentItem(item: BacklogItemContent) {
            if (!isAttachment(item)) {
                resultListener.showSnackbar("Для копіювання вибери вкладення", null)
                return
            }
            copyAttachmentById(item.backlogItem.id)
        }

        fun copyAttachmentById(attachmentId: String) {
            if (attachmentId.isBlank()) {
                resultListener.showSnackbar("Для копіювання вибери вкладення", null)
                return
            }
            backlogClipboardUseCase.copyAttachmentItems(
                sourceContextId = projectIdFlow.value,
                listItemIds = listOf(attachmentId),
            )
            resultListener.showSnackbar("Скопійовано вкладення. Перейди в цільовий список і натисни Вставити", null)
        }

        fun cutAttachmentItem(item: BacklogItemContent) {
            if (!isAttachment(item)) {
                resultListener.showSnackbar("Для вирізання вибери вкладення", null)
                return
            }
            cutAttachmentById(item.backlogItem.id)
        }

        fun cutAttachmentById(attachmentId: String) {
            if (attachmentId.isBlank()) {
                resultListener.showSnackbar("Для вирізання вибери вкладення", null)
                return
            }
            backlogClipboardUseCase.cutAttachmentItems(
                sourceContextId = projectIdFlow.value,
                listItemIds = listOf(attachmentId),
            )
            resultListener.showSnackbar("Вирізано вкладення. Перейди в цільовий список і натисни Вставити", null)
        }

        private fun isAttachment(item: BacklogItemContent): Boolean =
            item is BacklogItemContent.LinkItem ||
                item is BacklogItemContent.NoteDocumentItem ||
                item is BacklogItemContent.MusicNoteItem ||
                item is BacklogItemContent.ChecklistItem

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
