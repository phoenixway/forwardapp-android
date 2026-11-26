package com.romankozak.forwardappmobile.features.sync

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

@Composable
internal fun DeviceSyncQrDialog(address: String, key: String, onDismiss: () -> Unit) {
    val bitmap = remember(address, key) { runCatching {
        BarcodeEncoder().encodeBitmap(encodeDevicePairingQr(address, key), BarcodeFormat.QR_CODE, 480, 480)
    }.getOrNull() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Підключити інший пристрій") },
        text = { Column {
            Text("Відскануйте код на іншому телефоні. Код містить адресу й ключ доступу: показуйте його лише своїм пристроям.",
                style = MaterialTheme.typography.bodySmall)
            if (bitmap != null) Surface(Modifier.padding(top = 12.dp), color = androidx.compose.ui.graphics.Color.White) {
                Image(bitmap.asImageBitmap(), contentDescription = "QR-код підключення", modifier = Modifier.size(240.dp).padding(8.dp))
            } else Text("Не вдалося створити QR-код")
        } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрити") } },
    )
}
