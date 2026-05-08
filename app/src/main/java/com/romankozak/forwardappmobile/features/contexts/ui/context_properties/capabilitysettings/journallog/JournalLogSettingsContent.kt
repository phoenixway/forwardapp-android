package com.romankozak.forwardappmobile.features.contexts.ui.context_properties.capabilitysettings.journallog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun JournalLogSettingsContent(contextId: String) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Journal Log", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Окремий текстовий журнал для контексту. Кожен рядок може починатися зі спец-маркера і зберігається автоматично.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MarkerHint("-  елемент списку або дія")
        MarkerHint("!  важливе")
        MarkerHint("!! критично")
        MarkerHint(">  рішення")
        MarkerHint("?  питання або вибір")
        MarkerHint("*  будь-який інший маркер теж відобразиться")
        Text(
            text = "Натисни на заголовок у Journal Log view, щоб редагувати title.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Context: $contextId",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun MarkerHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}
