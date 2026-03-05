package com.romankozak.forwardappmobile.features.attachments.specific_types.notedocument

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.navigateOrFallback
import com.romankozak.forwardappmobile.ui.common.editor.UniversalEditorScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder

private data class TypedWikiLink(
    val type: String,
    val targetId: String,
    val label: String?,
)

private fun parseTypedWikiLink(raw: String): TypedWikiLink? {
    val match = Regex("""^(doc|ctx|music|checklist):([^|]+)(?:\|(.+))?$""", RegexOption.IGNORE_CASE).matchEntire(raw.trim()) ?: return null
    return TypedWikiLink(
        type = match.groupValues[1].lowercase(),
        targetId = match.groupValues[2].trim(),
        label = match.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }?.trim(),
    )
}

@Composable
fun NoteDocumentEditorScreen(
    navController: NavController,
    navigationManager: EnhancedNavigationManager? = null,
    startEdit: Boolean = false,
    viewModel: NoteDocumentEditorViewModel = hiltViewModel(),
) {
    val backStackEntry = navController.currentBackStackEntry
    val documentId: String? =
        backStackEntry?.arguments?.getString("documentId")
            ?: backStackEntry?.arguments?.getString("listId")

    val focusRequester = remember { FocusRequester() }
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(documentId) {
        if (documentId == null) {
            delay(300)
            focusRequester.requestFocus()
            val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    LaunchedEffect(documentId) { documentId?.let { viewModel.loadDocument(it) } }

    UniversalEditorScreen(
        title = "Нотатка",
        onSave = { content, cursorPosition ->
            viewModel.saveDocument(content, cursorPosition)
            navController.previousBackStackEntry?.savedStateHandle?.set("refresh_needed", true)
            navController.popBackStack()
        },
        onAutoSave = { content, cursorPosition ->
            viewModel.saveDocument(content, cursorPosition)
        },
        onNavigateBack = { navController.popBackStack() },
        onWikiLinkClick = { link ->
            coroutineScope.launch {
                when {
                    link.startsWith("#") -> {
                        val encoded = URLEncoder.encode(link, "UTF-8")
                        navigationManager.navigateOrFallback(
                            navController = navController,
                            target = NavTarget.GlobalSearch(query = encoded),
                            recordInHistory = true,
                        )
                    }
                    link.startsWith("@") -> {
                        val contextName = link.removePrefix("@").trim()
                        val contextId = viewModel.findContextIdByName(contextName)
                        if (contextId != null) {
                            navigationManager.navigateOrFallback(
                                navController = navController,
                                target = NavTarget.ContextDetail(contextId = contextId),
                                recordInHistory = true,
                            )
                        } else {
                            val encoded = URLEncoder.encode(link, "UTF-8")
                            navigationManager.navigateOrFallback(
                                navController = navController,
                                target = NavTarget.GlobalSearch(query = encoded),
                                recordInHistory = true,
                            )
                        }
                    }
                    else -> {
                        val typed = parseTypedWikiLink(link)
                        if (typed != null) {
                            when (typed.type) {
                                "doc" -> {
                                    navigationManager.navigateOrFallback(
                                        navController = navController,
                                        target = NavTarget.NoteDocument(id = typed.targetId, startEdit = false),
                                    )
                                }
                                "ctx" -> {
                                    navigationManager.navigateOrFallback(
                                        navController = navController,
                                        target = NavTarget.ContextDetail(contextId = typed.targetId),
                                        recordInHistory = true,
                                    )
                                }
                                "music" -> {
                                    navigationManager.navigateOrFallback(
                                        navController = navController,
                                        target = NavTarget.MusicNote(id = typed.targetId, startEdit = false),
                                    )
                                }
                                "checklist" -> {
                                    navigationManager.navigateOrFallback(
                                        navController = navController,
                                        target = NavTarget.Checklist(id = typed.targetId, contextId = null),
                                    )
                                }
                            }
                            return@launch
                        }
                        val documentId = viewModel.findDocumentIdByName(link)
                        if (documentId != null) {
                            navigationManager.navigateOrFallback(
                                navController = navController,
                                target = NavTarget.NoteDocument(id = documentId, startEdit = false),
                            )
                            return@launch
                        }
                        val musicNoteId = viewModel.findMusicNoteIdByName(link)
                        if (musicNoteId != null) {
                            navigationManager.navigateOrFallback(
                                navController = navController,
                                target = NavTarget.MusicNote(id = musicNoteId, startEdit = false),
                            )
                            return@launch
                        }
                        val checklistId = viewModel.findChecklistIdByName(link)
                        if (checklistId != null) {
                            navigationManager.navigateOrFallback(
                                navController = navController,
                                target = NavTarget.Checklist(id = checklistId, contextId = null),
                            )
                            return@launch
                        }
                        val contextId = viewModel.findContextIdByName(link)
                        if (contextId != null) {
                            navigationManager.navigateOrFallback(
                                navController = navController,
                                target = NavTarget.ContextDetail(contextId = contextId),
                                recordInHistory = true,
                            )
                            return@launch
                        }
                        viewModel.universalEditorViewModel.showError("Не зміг відкрити вкладення \"$link\"")
                    }
                }
            }
        },
        linkSuggestions = viewModel.linkSuggestions.collectAsStateWithLifecycle().value,
        contextSuggestions = viewModel.contextSuggestions.collectAsStateWithLifecycle().value,
        viewModel = viewModel.universalEditorViewModel,
        navController = navController,
        navigationManager = navigationManager,
        contentFocusRequester = focusRequester,
        startInEditMode = startEdit || documentId == null,
        foldingPersistenceKey = documentId?.let { "note_document:$it" },
    )
}
