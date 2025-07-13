import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.romankozak.forwardappmobile.GoalListViewModel

@Composable
fun WifiImportDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val viewModel: GoalListViewModel = viewModel()
    val desktopAddress by viewModel.desktopAddress.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Імпорт з Wi-Fi", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Text("Введіть IP-адресу та порт десктоп-додатку:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = desktopAddress,
                    onValueChange = { viewModel.onDesktopAddressChange(it) },
                    placeholder = { Text("Напр. 192.168.1.5:8080") },
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Скасувати")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(desktopAddress) },
                        enabled = desktopAddress.isNotBlank()
                    ) {
                        Text("Отримати дані")
                    }
                }
            }
        }
    }
}