package com.itantra.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.itantra.app.ui.theme.StatusError

/**
 * Safety confirmation dialog displayed before transmitting an EMERGENCY-priority broadcast.
 *
 * Prevents accidental emergency triggers and clearly discloses that priority scheduling
 * does not guarantee delivery over ad-hoc wireless links without cellular infrastructure.
 */
@Composable
fun EmergencyConfirmationDialog(
    messageSnippet: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = StatusError,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Confirm Emergency Alert",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "You are about to transmit a high-priority emergency broadcast:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "\"$messageSnippet\"",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Important Note: Emergency priority moves this packet to the head of the local queue and triggers automatic text-to-speech announcement on receiving devices. However, offline ad-hoc wireless links cannot guarantee 100% delivery if receivers are out of radio range.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusError,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Confirm & Transmit")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
