package com.romankozak.forwardappmobile.features.daymanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayPlanViewModel
import com.romankozak.forwardappmobile.features.mainscreen.CommandDeckFabDefaults

@Composable
fun DayManagementFabMenu(
    isFabMenuExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onDismiss: () -> Unit,
    dayPlanViewModel: DayPlanViewModel,
) {
    Box(modifier = Modifier.padding(bottom = CommandDeckFabDefaults.BottomPadding)) {
        FloatingActionButton(onClick = onToggleExpanded) {
            Icon(Icons.Default.Menu, contentDescription = "Меню дій дня")
        }
        DayManagementActionsMenu(
            expanded = isFabMenuExpanded,
            onDismissRequest = onDismiss,
            dayPlanViewModel = dayPlanViewModel,
        )
    }
}

@Composable
fun DayManagementActionsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    dayPlanViewModel: DayPlanViewModel,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier =
            Modifier.background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        DropdownMenuItem(
            text = { Text("Додати задачу") },
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
            onClick = {
                onDismissRequest()
                dayPlanViewModel.openAddTaskDialog()
            },
        )
        DropdownMenuItem(
            text = { Text("Показати зв'язки") },
            leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
            onClick = {
                onDismissRequest()
                dayPlanViewModel.toggleScopeLinksSheet()
            },
        )
        DropdownMenuItem(
            text = { Text("Назад") },
            leadingIcon = {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            },
            onClick = {
                onDismissRequest()
                dayPlanViewModel.navigateToPreviousDay()
            },
        )
        DropdownMenuItem(
            text = { Text("Вперед") },
            leadingIcon = {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            },
            onClick = {
                onDismissRequest()
                dayPlanViewModel.navigateToNextDay()
            },
        )
    }
}
