package com.romankozak.forwardappmobile.features.mainscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.mainscreen.scopelinks.ScopeAttachmentOption
import com.romankozak.forwardappmobile.features.mainscreen.scopelinks.toScopeAttachmentOption
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STRATEGIC_ARC_TAG = "arc"

data class StrategicArcUiState(
    val projects: List<Context> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class StrategicArcViewModel
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
        private val settingsRepository: SettingsRepository,
        private val attachmentsRepository: AttachmentsRepository,
    ) : ViewModel() {
        val uiState: StateFlow<StrategicArcUiState> =
            contextRepository.getAllContextsFlow()
                .map { projects ->
                    val arcProjects =
                        projects.filter {
                            it.tags?.contains("arc") == true
                        }
                    StrategicArcUiState(projects = arcProjects)
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = StrategicArcUiState(isLoading = true),
                )

        private val _attachmentOptions = MutableStateFlow<List<ScopeAttachmentOption>>(emptyList())
        val attachmentOptions: StateFlow<List<ScopeAttachmentOption>> = _attachmentOptions.asStateFlow()

        private val _linkedAttachmentIds = MutableStateFlow<List<String>>(emptyList())
        val linkedAttachmentIds: StateFlow<List<String>> = _linkedAttachmentIds.asStateFlow()

        private val _scopeContextsExpanded = MutableStateFlow(true)
        val scopeContextsExpanded: StateFlow<Boolean> = _scopeContextsExpanded.asStateFlow()

        private val _scopeAttachmentsExpanded = MutableStateFlow(true)
        val scopeAttachmentsExpanded: StateFlow<Boolean> = _scopeAttachmentsExpanded.asStateFlow()

        private val _isScopeLinksSheetVisible = MutableStateFlow(false)
        val isScopeLinksSheetVisible: StateFlow<Boolean> = _isScopeLinksSheetVisible.asStateFlow()

        init {
            attachmentsRepository.getAttachmentLibraryItems()
                .onEach { results ->
                    _attachmentOptions.value = results.mapNotNull { it.toScopeAttachmentOption() }
                }
                .launchIn(viewModelScope)

            settingsRepository.strategicArcLinkedAttachmentIdsFlow
                .onEach { ids -> _linkedAttachmentIds.value = ids.toList() }
                .launchIn(viewModelScope)

            settingsRepository.strategicArcScopeContextsExpandedFlow
                .onEach { expanded -> _scopeContextsExpanded.value = expanded }
                .launchIn(viewModelScope)

            settingsRepository.strategicArcScopeAttachmentsExpandedFlow
                .onEach { expanded -> _scopeAttachmentsExpanded.value = expanded }
                .launchIn(viewModelScope)
        }

        fun addArcLink(contextId: String) {
            viewModelScope.launch {
                updateTags(contextId, addTag = STRATEGIC_ARC_TAG)
            }
        }

        fun removeArcLink(contextId: String) {
            viewModelScope.launch {
                updateTags(contextId, removeTags = setOf(STRATEGIC_ARC_TAG))
            }
        }

        fun addAttachmentLink(attachmentId: String) {
            val next = (_linkedAttachmentIds.value + attachmentId).distinct()
            _linkedAttachmentIds.value = next
            viewModelScope.launch {
                settingsRepository.setStrategicArcLinkedAttachmentIds(next.toSet())
            }
        }

        fun removeAttachmentLink(attachmentId: String) {
            val next = _linkedAttachmentIds.value.filterNot { it == attachmentId }
            _linkedAttachmentIds.value = next
            viewModelScope.launch {
                settingsRepository.setStrategicArcLinkedAttachmentIds(next.toSet())
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
                        contextId = SystemContexts.STRATEGIC_BEACONS.raw,
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
                        contextId = SystemContexts.STRATEGIC_BEACONS.raw,
                        link = RelatedLink(type = LinkType.OBSIDIAN, target = target, displayName = display),
                    )
                addAttachmentLink(attachmentId)
            }
        }

        fun setScopeContextsExpanded(expanded: Boolean) {
            _scopeContextsExpanded.value = expanded
            viewModelScope.launch {
                settingsRepository.setStrategicArcScopeContextsExpanded(expanded)
            }
        }

        fun setScopeAttachmentsExpanded(expanded: Boolean) {
            _scopeAttachmentsExpanded.value = expanded
            viewModelScope.launch {
                settingsRepository.setStrategicArcScopeAttachmentsExpanded(expanded)
            }
        }

        fun toggleScopeLinksSheet() {
            _isScopeLinksSheetVisible.value = !_isScopeLinksSheetVisible.value
        }

        fun dismissScopeLinksSheet() {
            _isScopeLinksSheetVisible.value = false
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
