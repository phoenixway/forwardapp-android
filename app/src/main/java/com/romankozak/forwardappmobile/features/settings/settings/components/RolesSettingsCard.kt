package com.romankozak.forwardappmobile.features.settings.settings.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.settings.settings.SettingsUiState
import com.romankozak.forwardappmobile.features.settings.settings.utils.getFolderName
import com.romankozak.forwardappmobile.features.settings.settings.utils.getFileName

@Composable
fun RolesSettingsCard(
    state: SettingsUiState,
    onFolderSelected: (Uri, Context) -> Unit,
) {
    val context = LocalContext.current

    val folderPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
            onResult = { uri: Uri? ->
                uri?.let {
                    onFolderSelected(it, context)
                }
            },
        )

    SettingsCard(
        title = "Chat Roles",
        icon = Icons.Default.Face,
    ) {
        Text("Select a folder containing your role files (.md or .txt).")
        Spacer(modifier = Modifier.height(8.dp))
        FileSelector(
            label = "Roles Folder",
            selectedFileUri = state.rolesFolderUri,
            onSelectClick = { folderPickerLauncher.launch(null) },
            context = context,
            isFolder = true,
        )
    }
}

@Composable
fun FileSelector(
    label: String,
    selectedFileUri: String,
    onSelectClick: () -> Unit,
    context: Context,
    isFolder: Boolean = false,
) {
    val displayName = remember(selectedFileUri) {
        if (selectedFileUri.isNotBlank()) {
            try {
                val uri = Uri.parse(selectedFileUri)
                if (isFolder) getFolderName(uri, context) else getFileName(uri, context)
            } catch (e: Exception) {
                "Invalid URI"
            }
        } else {
            "Not selected"
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(onClick = onSelectClick) {
            Text(if (isFolder) "Select Folder" else "Select File")
        }
    }
}
