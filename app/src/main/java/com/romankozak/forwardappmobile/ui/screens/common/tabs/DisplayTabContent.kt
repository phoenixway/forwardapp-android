package com.romankozak.forwardappmobile.ui.screens.common.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DisplayTabContent(
    showCheckboxes: Boolean,
    onShowCheckboxesChange: (Boolean) -> Unit,
    isAdvancedModeEnabled: Boolean,
    onAdvancedModeChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show checkboxes")
            Switch(
                checked = showCheckboxes,
                onCheckedChange = onShowCheckboxesChange
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Advanced project mode")
            Switch(
                checked = isAdvancedModeEnabled,
                onCheckedChange = onAdvancedModeChange
            )
        }
    }
}
