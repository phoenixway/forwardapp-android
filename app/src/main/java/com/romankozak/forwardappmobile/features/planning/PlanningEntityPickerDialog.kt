package com.romankozak.forwardappmobile.features.planning

import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.entitypicker.CanonicalEntityPickerScreen
import com.romankozak.forwardappmobile.features.entitypicker.CanonicalEntityPickerUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Dialog host for the existing canonical picker, with a bounded independent VM lifetime. */
@Composable
fun PlanningEntityPickerDialog(
    onDismiss: () -> Unit,
    onSelected: suspend (CanonicalEntitySummary) -> Unit,
    host: PlanningReferencesViewModel = hiltViewModel(),
) {
    val store = remember { ViewModelStore() }
    val picker = remember(host) { host.newPicker().also { store.put("picker", it) } }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    DisposableEffect(store) { onDispose { store.clear() } }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CanonicalEntityPickerScreen(onBack = { if (!busy) onDismiss() }, onResult = { selection ->
            val ref = selection.singleOrNull()
            val entity = (picker.uiState.value as? CanonicalEntityPickerUiState.Ready)?.items?.singleOrNull { it.ref == ref }
            if (!busy && entity != null) {
                busy = true
                scope.launch {
                    try { onSelected(entity); onDismiss() }
                    catch (cancel: CancellationException) { throw cancel }
                    catch (failure: Exception) { error = failure.message ?: "Не вдалося виконати дію" }
                    finally { busy = false }
                }
            }
        }, viewModel = picker)
    }
    if (busy) {
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text("Зберігаємо…") },
            text = { CircularProgressIndicator() },
            confirmButton = {},
        )
    }
    error?.let { message ->
        AlertDialog(onDismissRequest = { error = null }, title = { Text("Не вдалося зберегти") },
            text = { Text(message) }, confirmButton = { TextButton(onClick = { error = null }) { Text("Гаразд") } })
    }
}
