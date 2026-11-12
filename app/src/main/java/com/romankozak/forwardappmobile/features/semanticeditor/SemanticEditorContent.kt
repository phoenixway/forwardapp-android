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
import com.romankozak.forwardappmobile.features.planning.OrientationPlanningContent
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass
import kotlinx.coroutines.launch

internal enum class SemanticEditorTab(val title: String) {
    MAIN("Основне"), SEMANTICS("Семантика"), WORKSPACES("Workspaces"), PLANNING("Planning")
}

@Composable
internal fun SemanticEditorContent(
    model: SemanticEntityEditorModel,
    padding: PaddingValues,
    onNameChange: (String) -> Unit,
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
    planning: @Composable () -> Unit = { OrientationPlanningContent(model.id) },
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
            SemanticEditorTab.PLANNING -> planning()
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
        val tabs = SemanticEditorTab.entries.filter {
            it != SemanticEditorTab.PLANNING || model.entityClass == SemanticEntityClass.ORIENTATION
        }
        val pager = rememberPagerState { tabs.size }
        val scope = rememberCoroutineScope()
        val pages = rememberSaveableStateHolder()
        val focus = LocalFocusManager.current
        Column(modifier.fillMaxSize()) {
            SemanticEditorIdentityHeader(model)
            ScrollableTabRow(selectedTabIndex = pager.currentPage, edgePadding = 0.dp) {
                tabs.forEachIndexed { index, item ->
                    Tab(selected = index == pager.currentPage, onClick = {
                        focus.clearFocus()
                        scope.launch { pager.scrollToPage(index) }
                    },
                        text = { Text(item.title, style = MaterialTheme.typography.labelLarge, maxLines = 1) })
                }
            }
            model.errorMessage?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth(),
                key = { tabs[it].name }) { page ->
                val pageTab = tabs[page]
                pages.SaveableStateProvider(pageTab.name) {
                    val pageModifier = if (pageTab == SemanticEditorTab.PLANNING) Modifier.fillMaxSize()
                    else Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
                    Column(pageModifier,
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        content(pageTab)
                    }
                }
            }
        }
    }
}
