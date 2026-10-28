package com.romankozak.forwardappmobile.features.planning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.romankozak.forwardappmobile.data.planning.strategic.*
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentId
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@HiltViewModel
class StrategicCompositionCarryViewModel @Inject constructor(val coordinator: StrategicCompositionCarryCoordinator) : ViewModel()

@Composable
fun StrategicCompositionCarryDialog(questId: String, title: String, onDismiss: () -> Unit,
    onCarried: (StrategicQuestCarryForwardResult) -> Unit) {
    val model: StrategicCompositionCarryViewModel = hiltViewModel(key = "strategic-carry:$questId")
    var options by remember(questId) { mutableStateOf<List<StrategicCarryComponent>?>(null) }
    var selected by rememberSaveable(questId) { mutableStateOf<List<String>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(questId) {
        try {
            options = model.coordinator.components(questId)
            if (selected == null) selected = defaultStrategicCarrySelection(options.orEmpty()).map { it.value }
        } catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) { error = failure.message }
    }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("У поточну арку: $title") }, text = {
        Column {
            Text("Нова участь; попередня арка залишиться без змін.")
            Row {
                Checkbox(checked = true, onCheckedChange = null)
                Text(title, Modifier.padding(top = 12.dp))
            }
            if (options.orEmpty().isNotEmpty()) Text("Components", style = MaterialTheme.typography.titleSmall)
            if (options == null && error == null) CircularProgressIndicator()
            LazyColumn(Modifier.heightIn(max = 320.dp)) {
                items(options.orEmpty(), key = { it.id.value }) { item ->
                    Row {
                        Checkbox(checked = item.id.value in selected.orEmpty(), enabled = !busy, onCheckedChange = { checked ->
                            selected = if (checked) selected.orEmpty() + item.id.value else selected.orEmpty() - item.id.value
                        })
                        Text(item.title + if (item.completed) " · Завершено" else "", Modifier.padding(top = 12.dp))
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        TextButton(enabled = !busy && options != null, onClick = {
            if (!busy) {
                busy = true; error = null
                scope.launch {
                    try { onCarried(model.coordinator.carry(questId, YearMonth.now().toString(), selected.orEmpty().mapTo(hashSetOf()) { PlanningCommitmentId(it) })); onDismiss() }
                    catch (cancel: CancellationException) { throw cancel }
                    catch (failure: Exception) { error = failure.message }
                    finally { busy = false }
                }
            }
        }) { Text(if (busy) "Зберігаємо…" else "Перенести участь") }
    }, dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Скасувати") } })
}
