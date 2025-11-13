package com.romankozak.forwardappmobile.ui.screens.mainscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenTopAppBar(
    projectCount: Int,
    isLoading: Boolean,
    onPlaceholderAction: () -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NeonTitle("Projects")
                    if (BuildConfig.DEBUG) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ) {
                            Text(
                                text = "Debug",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = { isMenuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Додаткові дії")
            }
            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Пошук (скоро)") },
                    onClick = {
                        onPlaceholderAction()
                        isMenuExpanded = false
                    },
                )
                DropdownMenuItem(
                    text = { Text("Синхронізація (скоро)") },
                    onClick = {
                        onPlaceholderAction()
                        isMenuExpanded = false
                    },
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Налаштування (скоро)") },
                    onClick = {
                        onPlaceholderAction()
                        isMenuExpanded = false
                    },
                )
                DropdownMenuItem(
                    text = { Text("Про застосунок") },
                    onClick = {
                        onPlaceholderAction()
                        isMenuExpanded = false
                    },
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.primary,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        modifier = Modifier.shadow(4.dp),
    )
}

@Composable
private fun NeonTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val textColor = MaterialTheme.colorScheme.primary

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style =
                MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
            color = textColor,
        )
    }
}
