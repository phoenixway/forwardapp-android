package com.romankozak.forwardappmobile.features.recent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItemType
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

fun recentEntityDestination(item: RecentItem): NavTarget? = when (item.type) {
    RecentItemType.PROJECT -> NavTarget.Workspace(item.target)
    RecentItemType.NOTE -> NavTarget.LegacyNote(item.target)
    RecentItemType.NOTE_DOCUMENT -> NavTarget.NoteDocument(item.target)
    RecentItemType.CHECKLIST -> NavTarget.Checklist(id = item.target)
    RecentItemType.MUSIC_NOTE -> NavTarget.MusicNote(item.target)
    RecentItemType.SEMANTIC_ENTITY -> NavTarget.SemanticEntityEditor(item.target)
    RecentItemType.SCRIPT -> NavTarget.ScriptEditor(scriptId = item.target)
    RecentItemType.OBSIDIAN_LINK -> null // External URIs remain with their existing opener.
}

@HiltViewModel
class RecentSemanticAccessViewModel @Inject constructor(private val repository: RecentItemsRepository) : ViewModel() {
    fun record(id: String, title: String) {
        viewModelScope.launch {
            try { repository.logEntityAccess(RecentItemType.SEMANTIC_ENTITY, id, title) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Recent metadata failure must not block the semantic editor. */ }
        }
    }
}

@Composable
internal fun RecordRecentSemanticAccess(id: String, title: String, recorder: RecentSemanticAccessViewModel = hiltViewModel()) {
    LaunchedEffect(id) { recorder.record(id, title) }
}

@Composable
fun RecentEntitiesContent(onOpen: (NavTarget) -> Unit, viewModel: RecentViewModel = hiltViewModel()) {
    val items by viewModel.recentItems.collectAsStateWithLifecycle()
    RecentEntitiesList(items, onOpen)
}

@Composable
internal fun RecentEntitiesList(items: List<RecentItem>, onOpen: (NavTarget) -> Unit) {
    val visible = items.filter { recentEntityDestination(it) != null }
        .distinctBy { it.type to it.target }
    LazyColumn(Modifier.fillMaxSize()) {
        item { Text("Недавні", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium) }
        if (visible.isEmpty()) item { Text("Ще немає відкритих сутностей", Modifier.padding(16.dp)) }
        items(visible, key = { "${it.type}:${it.target}" }) { item ->
            ListItem(headlineContent = { Text(item.displayName) },
                supportingContent = { Text(when (item.type) {
                    RecentItemType.PROJECT -> "Workspace"
                    RecentItemType.SEMANTIC_ENTITY -> "Семантична сутність"
                    RecentItemType.CHECKLIST -> "Список"
                    RecentItemType.MUSIC_NOTE -> "Музичні ноти"
                    RecentItemType.SCRIPT -> "Скрипт"
                    else -> "Нотатка"
                }) }, leadingContent = { Icon(Icons.Default.History, null) },
                modifier = Modifier.clickable { recentEntityDestination(item)?.let(onOpen) })
        }
    }
}
