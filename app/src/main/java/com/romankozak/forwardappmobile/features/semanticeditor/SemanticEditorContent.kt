package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.planning.SemanticPlanningEntry
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass
import kotlinx.coroutines.launch

internal enum class SemanticEditorTab(val title: String) {
    MAIN("Основне"), SEMANTICS("Семантика"), WORKSPACES("Workspaces")
}

@Composable
internal fun SemanticEditorContent(
    model: SemanticEntityEditorModel,
    padding: PaddingValues,
    onNameChange: (String) -> Unit,
    onOpenPlanning: () -> Unit,
    onAddSemantic: () -> Unit,
    onChooseAspectParent: () -> Unit,
    onMakeAspectRoot: () -> Unit,
    onOpenSemantic: (String) -> Unit,
    onRemoveAspect: (String) -> Unit,
    onAddWorkspace: () -> Unit,
    onRemoveWorkspace: (String) -> Unit,
    onOpenWorkspace: (WorkspaceBindingItem) -> Unit,
    onRemoveOrientation: (String, Long) -> Unit,
    onCreateWorkspace: () -> Unit = {},
    workspaceCreationBusy: Boolean = false,
    tags: @Composable () -> Unit = { SemanticTagsSection(model) },
) {
    SemanticEditorTabs(model, Modifier.padding(padding)) { tab ->
        when (tab) {
            SemanticEditorTab.MAIN -> {
                SemanticEditorGroup("Основні дані") {
                    OutlinedTextField(
                        value = model.name,
                        onValueChange = onNameChange,
                        label = { Text("Назва") },
                        singleLine = true,
                        enabled = !model.isSaving,
                        isError = model.name.isBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("Зміни назви зберігаються кнопкою вгорі") },
                    )
                }
                tags()
                if (model.entityClass == SemanticEntityClass.ORIENTATION) {
                    SemanticEditorGroup("Участь у плануванні") {
                        SemanticPlanningEntry(CanonicalEntityRef.Orientation(model.id), onOpenPlanning)
                    }
                }
            }
            SemanticEditorTab.SEMANTICS -> {
                SemanticEditorHint("Класифікація та зв’язки", "Прибирання зв’язку не видаляє жодну із сутностей.")
                if (model.entityClass == SemanticEntityClass.ASPECT) {
                    AspectParentSection(model, onChooseAspectParent, onMakeAspectRoot, onOpenSemantic)
                    AspectChildrenSection(model, onOpenSemantic)
                    HorizontalDivider()
                }
                SemanticAssociationsSection(model, onAddSemantic, onRemoveAspect, onRemoveOrientation, onOpenSemantic)
            }
            SemanticEditorTab.WORKSPACES -> {
                SemanticWorkspaceCreationCard(model, workspaceCreationBusy, onCreateWorkspace)
                WorkspaceBindingsSection(model, onAddWorkspace, onRemoveWorkspace, onOpenWorkspace,
                    addLabel = "Пов’язати")
            }
        }
    }
}

/** Presentation state only. Drafts and mutations stay with the existing editor authorities. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun SemanticEditorTabs(
    model: SemanticEntityEditorModel,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(SemanticEditorTab) -> Unit,
) {
    key(model.id) {
        val pager = rememberPagerState { SemanticEditorTab.entries.size }
        val scope = rememberCoroutineScope()
        val pages = rememberSaveableStateHolder()
        val focus = LocalFocusManager.current
        Column(modifier.fillMaxSize()) {
            SemanticEditorIdentityHeader(model)
            ScrollableTabRow(selectedTabIndex = pager.currentPage, edgePadding = 0.dp) {
                SemanticEditorTab.entries.forEach { item ->
                    Tab(selected = item.ordinal == pager.currentPage, onClick = {
                        focus.clearFocus()
                        scope.launch { pager.scrollToPage(item.ordinal) }
                    },
                        text = { Text(item.title, style = MaterialTheme.typography.labelLarge, maxLines = 1) })
                }
            }
            model.errorMessage?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth(),
                key = { SemanticEditorTab.entries[it].name }) { page ->
                val pageTab = SemanticEditorTab.entries[page]
                pages.SaveableStateProvider(pageTab.name) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        content(pageTab)
                    }
                }
            }
        }
    }
}
