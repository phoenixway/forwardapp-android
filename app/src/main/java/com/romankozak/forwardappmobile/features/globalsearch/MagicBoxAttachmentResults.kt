package com.romankozak.forwardappmobile.features.globalsearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalAttachmentSearchResult
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.repository.SearchRepository
import com.romankozak.forwardappmobile.features.entitybrowser.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

internal fun attachmentSearchDestination(result: GlobalAttachmentSearchResult): NavTarget? = when (result.attachmentType) {
    BacklogItemTypeValues.NOTE_DOCUMENT -> NavTarget.NoteDocument(id = result.entityId)
    BacklogItemTypeValues.CHECKLIST -> NavTarget.Checklist(id = result.entityId)
    BacklogItemTypeValues.MUSIC_NOTE -> NavTarget.MusicNote(id = result.entityId)
    BacklogItemTypeValues.SCRIPT -> NavTarget.ScriptEditor(contextId = result.ownerContextId, scriptId = result.entityId)
    else -> null
}

private fun attachmentSearchLabel(type: String): String = when (type) {
    BacklogItemTypeValues.NOTE_DOCUMENT -> "Нотатка"
    BacklogItemTypeValues.CHECKLIST -> "Список"
    BacklogItemTypeValues.MUSIC_NOTE -> "Музичні ноти"
    BacklogItemTypeValues.SCRIPT -> "Скрипт"
    else -> "Вкладення"
}

data class MagicBoxAttachmentState(
    val input: String = "",
    val results: List<GlobalSearchResultItem.AttachmentItem> = emptyList(),
    val error: String? = null,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class MagicBoxAttachmentViewModel @Inject constructor(search: SearchRepository) : ViewModel() {
    private val input = MutableStateFlow("")
    fun setInput(value: String) { input.value = value }
    val state = input.debounce(180).flatMapLatest { query ->
        if (query.isBlank() || hasCanonicalQueryOperators(query)) flowOf(MagicBoxAttachmentState(query))
        else search.observeAttachmentSearch(query).map { rows ->
            MagicBoxAttachmentState(query, rows.filter { attachmentSearchDestination(it.searchResult) != null })
        }.catch { emit(MagicBoxAttachmentState(query, error = "Не вдалося завантажити вкладення")) }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MagicBoxAttachmentState())
}

/** One scroll surface, separate typed identities; canonical predicates remain with their own engine. */
@Composable
internal fun MagicBoxCombinedResultList(
    entities: List<CanonicalEntitySummary>,
    attachments: List<GlobalSearchResultItem.AttachmentItem>,
    onEntity: (CanonicalEntityRef) -> Unit,
    onAttachment: (GlobalSearchResultItem.AttachmentItem) -> Unit,
    showAttachments: Boolean,
    attachmentsLoading: Boolean = false,
    attachmentError: String? = null,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        if (entities.isNotEmpty()) {
            item { Text("Сутності · ${entities.size}", Modifier.padding(12.dp), style = MaterialTheme.typography.titleSmall) }
            items(entities, key = { "canonical:${it.ref.entityClass}:${it.ref.id}" }) { entity ->
                CanonicalEntityRow(entity, onClick = { onEntity(entity.ref) })
            }
        }
        if (showAttachments) {
            if (attachmentsLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(12.dp)) }
            attachmentError?.let { error -> item { Text(error, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error) } }
            if (attachments.isNotEmpty()) {
                item { Text("Вкладення · ${attachments.size}", Modifier.padding(12.dp), style = MaterialTheme.typography.titleSmall) }
                items(attachments, key = { "attachment:${it.uniqueId}" }) { item ->
                    val record = item.searchResult
                    ListItem(
                        headlineContent = { Text(record.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(listOfNotNull(attachmentSearchLabel(record.attachmentType), record.contextName)
                            .joinToString(" · "), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        leadingContent = { Icon(Icons.Default.Description, null) },
                        modifier = Modifier.clickable { onAttachment(item) },
                    )
                }
            }
        }
        if (entities.isEmpty() && (!showAttachments || (attachments.isEmpty() && !attachmentsLoading && attachmentError == null))) {
            item { Text("Нічого не знайдено", Modifier.padding(24.dp)) }
        }
    }
}
