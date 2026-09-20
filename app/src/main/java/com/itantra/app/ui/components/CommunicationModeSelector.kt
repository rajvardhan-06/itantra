package com.itantra.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.itantra.app.domain.model.CommunicationMode
import com.itantra.app.ui.theme.ItantraDimens
import com.itantra.app.ui.theme.StatusWarning

/**
 * Reusable Material 3 component for selecting application communication modes.
 *
 * Clearly indicates supported modes vs. planned/in-design modes, preventing
 * unsupported modes (like full-duplex cellular Phone Mode) from appearing functional.
 */
@Composable
fun CommunicationModeSelector(
    selectedMode: CommunicationMode,
    onModeSelected: (CommunicationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var unsupportedNoticeMode by remember { mutableStateOf<CommunicationMode?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CommunicationMode.entries.forEach { mode ->
            val isSelected = mode == selectedMode
            val icon: ImageVector = when (mode) {
                CommunicationMode.PUSH_TO_TALK -> Icons.Filled.Mic
                CommunicationMode.TEXT_ONLY -> Icons.AutoMirrored.Filled.Chat
                CommunicationMode.VOICE_ASSISTED -> Icons.Filled.RecordVoiceOver
                CommunicationMode.PHONE_MODE -> Icons.Filled.Phone
            }

            Card(
                shape = RoundedCornerShape(ItantraDimens.RadiusMedium),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(ItantraDimens.RadiusMedium)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable {
                        if (mode.isSupported) {
                            onModeSelected(mode)
                            unsupportedNoticeMode = null
                        } else {
                            unsupportedNoticeMode = mode
                        }
                    }
                    .semantics {
                        role = Role.RadioButton
                        this.selected = isSelected
                    }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (!mode.isSupported) {
                                    StatusBadge(
                                        label = "IN DESIGN",
                                        color = StatusWarning
                                    )
                                }
                            }
                        }
                        RadioButton(
                            selected = isSelected,
                            enabled = mode.isSupported,
                            onClick = {
                                if (mode.isSupported) {
                                    onModeSelected(mode)
                                    unsupportedNoticeMode = null
                                } else {
                                    unsupportedNoticeMode = mode
                                }
                            }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = mode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Explanatory banner when user taps an unsupported / in-design mode
        if (unsupportedNoticeMode != null) {
            Card(
                shape = RoundedCornerShape(ItantraDimens.RadiusMedium),
                colors = CardDefaults.cardColors(
                    containerColor = StatusWarning.copy(alpha = 0.12f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = StatusWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Mode Unavailable — Architectural Notice",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Phone Mode requires continuous full-duplex VoIP streaming, dynamic acoustic echo cancellation (AEC), and jitter buffers over raw Wi-Fi/Bluetooth channels. To maintain honest evaluation, iTantra provides low-bandwidth PTT micro-transmissions instead of simulating fake cellular calls.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
