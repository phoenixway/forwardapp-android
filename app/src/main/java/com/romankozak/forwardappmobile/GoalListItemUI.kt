package com.romankozak.forwardappmobile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box // <-- ДОДАНО
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width // <-- ДОДАНО
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu // <-- ДОДАНО
import androidx.compose.material3.DropdownMenuItem // <-- ДОДАНО
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // <-- ДОДАНО
import androidx.compose.runtime.mutableStateOf // <-- ДОДАНО
import androidx.compose.runtime.remember // <-- ДОДАНО
import androidx.compose.runtime.setValue // <-- ДОДАНО
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GoalListRow(
    list: GoalList,
    level: Int,
    hasChildren: Boolean,
    onListClick: (String) -> Unit,
    onToggleExpanded: (list: GoalList) -> Unit,
    // ЗМІНЕНО: Єдина дія для запиту меню
    onMenuRequested: (list: GoalList) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (level * 24).dp)
            .combinedClickable(
                onClick = { onListClick(list.id) },
                onLongClick = { onMenuRequested(list) } // Довгий клік викликає меню
            ),
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ... (Іконка згортання/розгортання та Spacer залишаються без змін)
            if (hasChildren) {
                IconButton(onClick = { onToggleExpanded(list) }) {
                    Icon(
                        imageVector = if (list.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                        contentDescription = "Згорнути/Розгорнути"
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            Text(
                text = list.name,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Іконка меню тепер також просто викликає запит
            IconButton(onClick = { onMenuRequested(list) }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Дії зі списком")
            }
        }
    }
}