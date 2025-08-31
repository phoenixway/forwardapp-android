// File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/globalsearch/GlobalSearchScreen.kt

package com.romankozak.forwardappmobile.ui.screens.globalsearch

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.data.database.models.GlobalSearchResultItem
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.ui.screens.globalsearch.components.LinkSearchResultItem
import com.romankozak.forwardappmobile.ui.screens.globalsearch.components.SearchResultItem
import com.romankozak.forwardappmobile.ui.screens.globalsearch.components.SublistSearchResultItem
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    navController: NavController,
    viewModel: GlobalSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val query = viewModel.query
    val obsidianVaultName by viewModel.obsidianVaultName.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Результати для: \"$query\"",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.results.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Нічого не знайдено.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.results, key = { result ->
                    when (result) {
                        is GlobalSearchResultItem.GoalItem -> "goal_${result.searchResult.goal.id}_${result.searchResult.listId}"
                        is GlobalSearchResultItem.LinkItem -> "link_${result.searchResult.link.id}_${result.searchResult.listId}"
                        is GlobalSearchResultItem.SublistItem -> "sublist_${result.searchResult.sublist.id}_${result.searchResult.parentListId}"
                    }
                }) { result ->
                    when (result) {
                        is GlobalSearchResultItem.GoalItem -> {
                            SearchResultItem(
                                result = result.searchResult,
                                onClick = {
                                    val route = "goal_detail_screen/${result.searchResult.listId}?goalId=${result.searchResult.goal.id}"
                                    Log.d("HighlightDebug", "[1. NAV] Navigating with route: $route")
                                    navController.navigate(route)
                                }
                            )
                        }
                        is GlobalSearchResultItem.LinkItem -> {
                            val searchResult = result.searchResult
                            val linkData = searchResult.link.linkData
                            LinkSearchResultItem(
                                result = searchResult,
                                onClick = {
                                    val route = "goal_detail_screen/${searchResult.listId}?itemIdToHighlight=${searchResult.listItemId}"
                                    Log.d("HighlightDebug", "[1. NAV] Navigating to item in context: $route")
                                    navController.navigate(route)
                                },
                                onGoToTargetList = {
                                    val route = "goal_detail_screen/${linkData.target}"
                                    Log.d("HighlightDebug", "[1. NAV] Navigating to target list: $route")
                                    navController.navigate(route)
                                },
                                onOpenInObsidian = {
                                    handleRelatedLinkClick(
                                        link = linkData,
                                        obsidianVaultName = obsidianVaultName,
                                        context = context
                                    )
                                },
                                // ADDED: Passing the handler for the new button
                                onOpenUrl = {
                                    handleRelatedLinkClick(
                                        link = linkData,
                                        obsidianVaultName = obsidianVaultName,
                                        context = context
                                    )
                                }
                            )
                        }
                        is GlobalSearchResultItem.SublistItem -> {
                            SublistSearchResultItem(
                                result = result.searchResult,
                                onClick = {
                                    val route = "goal_detail_screen/${result.searchResult.sublist.id}"
                                    Log.d("HighlightDebug", "[1. NAV] Navigating with route: $route")
                                    navController.navigate(route)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Handles clicks on links that should be opened by external applications (web browser, Obsidian).
 */
private fun handleRelatedLinkClick(link: RelatedLink, obsidianVaultName: String, context: Context) {
    try {
        when (link.type) {
            LinkType.URL -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.target))
                context.startActivity(intent)
            }
            LinkType.OBSIDIAN -> {
                if (obsidianVaultName.isNotBlank()) {
                    val encodedVault = URLEncoder.encode(obsidianVaultName, "UTF-8")
                    val encodedFile = URLEncoder.encode(link.target, "UTF-8")
                    val obsidianUri = "obsidian://open?vault=$encodedVault&file=$encodedFile"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(obsidianUri))
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "Obsidian vault name is not set.", Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                // This handler is not for GOAL_LIST or NOTE types, which are handled internally.
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open link.", Toast.LENGTH_LONG).show()
    }
}