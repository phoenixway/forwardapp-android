package com.romankozak.forwardappmobile.features.globalsearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityClass
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind

internal enum class MagicBoxResultType(
    val label: String,
    private val entityClass: CanonicalEntityClass? = null,
) {
    All("Усі типи"),
    Attachments("Вкладення"),
    Orientations("Орієнтири", CanonicalEntityClass.ORIENTATION),
    Aspects("Аспекти", CanonicalEntityClass.ASPECT),
    Workspaces("Робочі простори", CanonicalEntityClass.WORKSPACE),
    ;

    fun matches(entity: CanonicalEntitySummary, kind: OrientationKind?): Boolean = when (this) {
        All -> true
        Orientations -> entity.ref.entityClass == entityClass && (kind == null || entity.orientationKind == kind)
        else -> entity.ref.entityClass == entityClass
    }
}

@Composable
internal fun MagicBoxResultFilters(
    type: MagicBoxResultType,
    onTypeChange: (MagicBoxResultType) -> Unit,
    orientationKind: OrientationKind?,
    onOrientationKindChange: (OrientationKind?) -> Unit,
    attachmentsAvailable: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ResultFilterMenu(
            selected = type,
            options = MagicBoxResultType.entries.filter { attachmentsAvailable || it != MagicBoxResultType.Attachments },
            label = { it.label },
            onSelect = onTypeChange,
            modifier = Modifier.weight(1f),
        )
        if (type == MagicBoxResultType.Orientations) {
            ResultFilterMenu(
                selected = orientationKind,
                options = listOf(null) + OrientationKind.entries,
                label = { it?.searchFilterLabel() ?: "Усі підтипи" },
                onSelect = onOrientationKindChange,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun <T> ResultFilterMenu(
    selected: T,
    options: List<T>,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(label(selected), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

private fun OrientationKind.searchFilterLabel(): String = when (this) {
    OrientationKind.MAIN_BEACON -> "Головний маяк"
    OrientationKind.MAIN_BEACON_GROUP -> "Група головних маяків"
    OrientationKind.GOAL -> "Ціль"
    OrientationKind.DIRECTION -> "Напрям"
    OrientationKind.INTENT -> "Намір"
    OrientationKind.MILESTONE -> "Віха"
    OrientationKind.ONGOING_STANDARD -> "Постійний стандарт"
    OrientationKind.OPPORTUNITY -> "Можливість"
    OrientationKind.THEME -> "Тема"
    OrientationKind.QUEST -> "Квест"
    OrientationKind.DAY_THEME -> "Тема дня"
    OrientationKind.ARC_QUEST -> "Квест арки"
    OrientationKind.PROJECT -> "Проєкт"
}
