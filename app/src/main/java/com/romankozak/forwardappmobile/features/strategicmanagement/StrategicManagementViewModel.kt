package com.romankozak.forwardappmobile.features.strategicmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.mainscreen.scopelinks.ScopeAttachmentOption
import com.romankozak.forwardappmobile.features.mainscreen.scopelinks.toScopeAttachmentOption
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STRATEGIC_TAG = "strategic"

@HiltViewModel
class StrategicManagementViewModel
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
        private val settingsRepository: SettingsRepository,
        private val attachmentsRepository: AttachmentsRepository,
    ) : ViewModel() {
        val uiState: StateFlow<StrategicManagementUiState> =
            contextRepository.getAllContextsFlow()
                .map { projects ->
                    val strategic =
                        projects.filter {
                            it.tags?.contains(STRATEGIC_TAG) == true
                        }
                    StrategicManagementUiState(dashboardProjects = strategic)
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = StrategicManagementUiState(isLoading = true),
                )

        private val _currentTab = MutableStateFlow(StrategicManagementTab.DASHBOARD)
        val currentTab = _currentTab.asStateFlow()

        private val _attachmentOptions = MutableStateFlow<List<ScopeAttachmentOption>>(emptyList())
        val attachmentOptions: StateFlow<List<ScopeAttachmentOption>> = _attachmentOptions.asStateFlow()

        private val _linkedAttachmentIds = MutableStateFlow<List<String>>(emptyList())
        val linkedAttachmentIds: StateFlow<List<String>> = _linkedAttachmentIds.asStateFlow()

        private val _scopeContextsExpanded = MutableStateFlow(true)
        val scopeContextsExpanded: StateFlow<Boolean> = _scopeContextsExpanded.asStateFlow()

        private val _scopeAttachmentsExpanded = MutableStateFlow(true)
        val scopeAttachmentsExpanded: StateFlow<Boolean> = _scopeAttachmentsExpanded.asStateFlow()
        private val _connectionsOrder = MutableStateFlow<List<String>>(emptyList())
        val connectionsOrder: StateFlow<List<String>> = _connectionsOrder.asStateFlow()

        private val _isScopeLinksSheetVisible = MutableStateFlow(false)
        val isScopeLinksSheetVisible: StateFlow<Boolean> = _isScopeLinksSheetVisible.asStateFlow()

        init {
            attachmentsRepository.getAttachmentLibraryItems()
                .onEach { results ->
                    _attachmentOptions.value = results.mapNotNull { it.toScopeAttachmentOption() }
                }
                .launchIn(viewModelScope)

            settingsRepository.strategicLinkedAttachmentIdsFlow
                .onEach { ids -> _linkedAttachmentIds.value = ids.toList() }
                .launchIn(viewModelScope)

            settingsRepository.strategicScopeContextsExpandedFlow
                .onEach { expanded -> _scopeContextsExpanded.value = expanded }
                .launchIn(viewModelScope)

            settingsRepository.strategicScopeAttachmentsExpandedFlow
                .onEach { expanded -> _scopeAttachmentsExpanded.value = expanded }
                .launchIn(viewModelScope)

            settingsRepository.strategicConnectionsOrderFlow
                .onEach { order -> _connectionsOrder.value = order }
                .launchIn(viewModelScope)
        }

        fun onTabSelected(tab: StrategicManagementTab) {
            _currentTab.value = tab
        }

        fun addStrategicLink(contextId: String) {
            viewModelScope.launch {
                updateTags(contextId, addTag = STRATEGIC_TAG)
            }
        }

        fun removeStrategicLink(contextId: String) {
            viewModelScope.launch {
                updateTags(contextId, removeTags = setOf(STRATEGIC_TAG))
            }
        }

        fun addAttachmentLink(attachmentId: String) {
            val next = (_linkedAttachmentIds.value + attachmentId).distinct()
            _linkedAttachmentIds.value = next
            viewModelScope.launch {
                settingsRepository.setStrategicLinkedAttachmentIds(next.toSet())
            }
        }

        fun removeAttachmentLink(attachmentId: String) {
            val next = _linkedAttachmentIds.value.filterNot { it == attachmentId }
            _linkedAttachmentIds.value = next
            viewModelScope.launch {
                settingsRepository.setStrategicLinkedAttachmentIds(next.toSet())
            }
        }

        fun addUrlLink(
            url: String,
            name: String,
        ) {
            val target = url.trim()
            if (target.isBlank()) return
            val display = name.trim().ifBlank { target }
            viewModelScope.launch {
                val attachmentId =
                    attachmentsRepository.createLinkAttachment(
                        contextId = SystemContexts.STRATEGIC.raw,
                        link = RelatedLink(type = LinkType.URL, target = target, displayName = display),
                    )
                addAttachmentLink(attachmentId)
            }
        }

        fun addObsidianLink(
            noteName: String,
            displayName: String,
        ) {
            val target = noteName.trim()
            if (target.isBlank()) return
            val display = displayName.trim().ifBlank { target }
            viewModelScope.launch {
                val attachmentId =
                    attachmentsRepository.createLinkAttachment(
                        contextId = SystemContexts.STRATEGIC.raw,
                        link = RelatedLink(type = LinkType.OBSIDIAN, target = target, displayName = display),
                    )
                addAttachmentLink(attachmentId)
            }
        }

        fun setScopeContextsExpanded(expanded: Boolean) {
            _scopeContextsExpanded.value = expanded
            viewModelScope.launch {
                settingsRepository.setStrategicScopeContextsExpanded(expanded)
            }
        }

        fun setScopeAttachmentsExpanded(expanded: Boolean) {
            _scopeAttachmentsExpanded.value = expanded
            viewModelScope.launch {
                settingsRepository.setStrategicScopeAttachmentsExpanded(expanded)
            }
        }

        fun toggleScopeLinksSheet() {
            _isScopeLinksSheetVisible.value = !_isScopeLinksSheetVisible.value
        }

        fun dismissScopeLinksSheet() {
            _isScopeLinksSheetVisible.value = false
        }

        fun updateConnectionsOrder(order: List<String>) {
            _connectionsOrder.value = order
            viewModelScope.launch {
                settingsRepository.setStrategicConnectionsOrder(order)
            }
        }

        private suspend fun updateTags(
            contextId: String,
            addTag: String? = null,
            removeTags: Set<String> = emptySet(),
        ) {
            val context = contextRepository.getContextById(contextId) ?: return
            val current = context.tags.orEmpty()
            val next =
                current
                    .filterNot { it in removeTags }
                    .toMutableList()
            if (addTag != null && addTag !in next) {
                next.add(addTag)
            }
            if (next != current) {
                contextRepository.updateContext(context.copy(tags = next))
            }
        }
    }
