package com.romankozak.forwardappmobile.features.attachments.ui.library

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.config.FeatureFlag
import com.romankozak.forwardappmobile.config.FeatureToggles
import com.romankozak.forwardappmobile.data.database.models.LinkType
import java.net.URLEncoder
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentsLibraryScreen(
    navController: NavController,
    viewModel: AttachmentsLibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (!uiState.isFeatureEnabled && !FeatureToggles.isEnabled(FeatureFlag.AttachmentsLibrary)) {
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        return
    }

    val context = LocalContext.current

    LaunchedEffect(viewModel, navController) {
        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
        savedStateHandle
            ?.getStateFlow<String?>("list_chooser_result", null)
            ?.collect { result ->
                result?.let {
                    savedStateHandle["list_chooser_result"] = null
                    viewModel.onProjectChosen(it)
                }
            }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AttachmentsLibraryEvent.NavigateToProjectChooser -> {
                    val title = URLEncoder.encode(event.title, "UTF-8")
                    navController.navigate("list_chooser_screen/$title")
                }
                is AttachmentsLibraryEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Бібліотека додатків") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Пошук за назвою, проєктом або посиланням") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            FilterRow(
                selected = uiState.filter,
                onFilterSelected = viewModel::onFilterChange,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Всього: ${uiState.matchedCount} із ${uiState.totalCount}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.items.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Нічого не знайдено",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        AttachmentCard(
                            item = item,
                            onClick = {
                                when (item.type) {
                                    AttachmentLibraryType.NOTE_DOCUMENT ->
                                        navController.navigate("note_document_screen/${item.entityId}")
                                    AttachmentLibraryType.CHECKLIST ->
                                        navController.navigate("checklist_screen?checklistId=${item.entityId}")
                                    AttachmentLibraryType.LINK -> {
                                        val linkData = item.linkData
                                        if (linkData != null) {
                                            when (linkData.type) {
                                                LinkType.PROJECT ->
                                                    navController.navigate("goal_detail_screen/${linkData.target}")
                                                LinkType.URL, null ->
                                                    openExternalLink(context, linkData.target)
                                                LinkType.OBSIDIAN ->
                                                    openExternalLink(context, linkData.target)
                                            }
                                        }
                                    }
                                }
                            },
                            onShareClick = { viewModel.onShareToProjectClick(item) },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FilterRow(
    selected: AttachmentLibraryFilter,
    onFilterSelected: (AttachmentLibraryFilter) -> Unit,
) {
    val filters = listOf(
        AttachmentLibraryFilter.All,
        AttachmentLibraryFilter.Notes,
        AttachmentLibraryFilter.Checklists,
        AttachmentLibraryFilter.Links,
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.displayName()) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AttachmentCard(
    item: AttachmentLibraryItem,
    onClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onShareClick) {
                    Icon(Icons.Filled.Share, contentDescription = "Зберегти в проєкт")
                }
            }

            item.subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(item.type.label()) },
                )

                item.ownerProject?.let { owner ->
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("Власник: ${owner.name}") },
                    )
                }

                item.projects
                    .filterNot { owner -> item.ownerProject?.id == owner.id }
                    .forEach { project ->
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(project.name) },
                        )
                    }
            }
        }
    }
}

private fun AttachmentLibraryFilter.displayName(): String =
    when (this) {
        AttachmentLibraryFilter.All -> "Усі"
        AttachmentLibraryFilter.Notes -> "Нотатки"
        AttachmentLibraryFilter.Checklists -> "Чеклісти"
        AttachmentLibraryFilter.Links -> "Посилання"
    }

private fun AttachmentLibraryType.label(): String =
    when (this) {
        AttachmentLibraryType.NOTE_DOCUMENT -> "Нотатка"
        AttachmentLibraryType.CHECKLIST -> "Чекліст"
        AttachmentLibraryType.LINK -> "Посилання"
    }

private fun openExternalLink(context: android.content.Context, target: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (exception: ActivityNotFoundException) {
        android.util.Log.e("AttachmentsLibrary", "Cannot open link: $target", exception)
    }
}
