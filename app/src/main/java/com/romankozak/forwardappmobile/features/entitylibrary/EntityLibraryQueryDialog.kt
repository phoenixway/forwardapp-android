package com.romankozak.forwardappmobile.features.entitylibrary

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.domain.orientation.DEFAULT_ORIENTATION_SCORING_POLICY_V1

@Composable
internal fun EntityLibraryQueryDialog(
    query: CanonicalEntityQuery,
    availableTags: List<String>,
    onDismiss: () -> Unit,
    onApply: (Set<CanonicalEntityClass>, Set<String>, Map<CanonicalScoreDimension, CanonicalScoreRange>) -> Unit,
) {
    var types by remember { mutableStateOf(query.entityTypes) }
    var tags by remember { mutableStateOf(query.tags) }
    var scores by remember { mutableStateOf(query.scoreFilters) }
    var dimension by remember { mutableStateOf(CanonicalScoreDimension.IMPORTANCE) }
    var dimensionMenu by remember { mutableStateOf(false) }
    var min by remember(dimension) { mutableStateOf(scores[dimension]?.min?.queryLabel().orEmpty()) }
    var max by remember(dimension) { mutableStateOf(scores[dimension]?.max?.queryLabel().orEmpty()) }
    var error by remember(dimension) { mutableStateOf<String?>(null) }
    var tagSearch by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Фільтри Library") }, text = {
        Column(Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Тип сутності", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CanonicalEntityClass.entries.forEach { type ->
                    FilterChip(selected = type in types, onClick = { types = if (type in types) types - type else types + type }, label = { Text(type.displayLabel()) })
                }
            }
            Text("Теги (усі вибрані)", style = MaterialTheme.typography.titleSmall)
            Text("Власні теги Orientation, Aspect та Workspace; без успадкування між сутностями.", style = MaterialTheme.typography.bodySmall)
            if (availableTags.isEmpty() && tags.isEmpty()) Text("Тегів поки немає")
            OutlinedTextField(value = tagSearch, onValueChange = { tagSearch = it }, label = { Text("Знайти тег") }, singleLine = true)
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 160.dp)) {
                items((availableTags + tags).distinct().sorted().filter { it.contains(tagSearch, ignoreCase = true) }, key = { it }) { tag ->
                    FilterChip(selected = tag in tags, onClick = { tags = if (tag in tags) tags - tag else tags + tag }, label = { Text("#$tag") })
                }
            }
            Text("Scoring", style = MaterialTheme.typography.titleSmall)
            Text("Поточна оцінка Orientation. Aspect і Workspace не успадковують її. Порожні значення не відповідають числовому фільтру.", style = MaterialTheme.typography.bodySmall)
            Box {
                TextButton(onClick = { dimensionMenu = true }) { Text(dimension.displayLabel()) }
                DropdownMenu(expanded = dimensionMenu, onDismissRequest = { dimensionMenu = false }) {
                    CanonicalScoreDimension.entries.forEach { candidate ->
                        DropdownMenuItem(text = { Text(candidate.displayLabel()) }, onClick = { dimension = candidate; dimensionMenu = false })
                    }
                }
            }
            Text(dimension.coordinateHint(), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = min, onValueChange = { min = it; error = null }, label = { Text("Мінімум ≥") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(value = max, onValueChange = { max = it; error = null }, label = { Text("Максимум ≤") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            }
            TextButton(onClick = {
                val range = parseLibraryScoreRange(min, max)
                if (range == null && (min.isNotBlank() || max.isNotBlank())) error = "Вкажіть скінченні числа; мінімум не може перевищувати максимум"
                else { scores = if (range == null) scores - dimension else scores + (dimension to range); error = null }
            }) { Text("Застосувати діапазон") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            scores.forEach { (key, value) ->
                InputChip(selected = true, onClick = { scores = scores - key; if (key == dimension) { min = ""; max = "" } }, label = { Text("${key.displayLabel()} ${value.displayLabel()} ×") })
            }
        }
    }, confirmButton = { TextButton(onClick = {
        val pending = parseLibraryScoreRange(min, max)
        if (pending == null && (min.isNotBlank() || max.isNotBlank())) error = "Невірний діапазон"
        else onApply(types, tags, if (pending == null) scores - dimension else scores + (dimension to pending))
    }) { Text("Застосувати фільтри") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } })
}

internal fun parseLibraryScoreRange(min: String, max: String): CanonicalScoreRange? {
    if (min.isBlank() && max.isBlank()) return null
    val low = min.takeIf { it.isNotBlank() }?.replace(',', '.')?.toDoubleOrNull()
    val high = max.takeIf { it.isNotBlank() }?.replace(',', '.')?.toDoubleOrNull()
    if (min.isNotBlank() && low == null || max.isNotBlank() && high == null) return null
    return try { CanonicalScoreRange(low, high) } catch (_: IllegalArgumentException) { null }
}

private fun CanonicalScoreDimension.coordinateHint(): String {
    val policy = DEFAULT_ORIENTATION_SCORING_POLICY_V1
    val axis = when (this) {
        CanonicalScoreDimension.IMPORTANCE -> policy.importance
        CanonicalScoreDimension.IMPACT -> policy.impact
        CanonicalScoreDimension.EFFORT -> policy.effort
        CanonicalScoreDimension.COST -> policy.cost
        CanonicalScoreDimension.RISK -> policy.risk
        CanonicalScoreDimension.SCORE -> return "Підсумкова оцінка: 0–100"
    }
    return "Координата policy ${policy.id} v${policy.version}: ${axis.min.toDouble().queryLabel()}–${axis.max.toDouble().queryLabel()}"
}
