package com.itantra.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.domain.model.ItantraMessage
import com.itantra.app.domain.model.MessageDirection
import com.itantra.app.domain.model.MessagePriority
import com.itantra.app.domain.model.MessageStatus
import com.itantra.app.tts.TtsState
import com.itantra.app.ui.theme.ItantraDimens
import com.itantra.app.ui.theme.ItantraEmergency
import com.itantra.app.ui.theme.StatusError
import com.itantra.app.ui.theme.StatusOnline
import com.itantra.app.ui.theme.StatusWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

/**
 * Chat-bubble style card displaying a single [ItantraMessage].
 * Sent messages appear right-aligned; received messages appear left-aligned with TTS playback controls.
 * Supports priority highlights, long-press to copy, and full accessibility semantics.
 */
@Composable
fun MessageItemCard(
    message: ItantraMessage,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    ttsState: TtsState? = null,
    onPlayTts: (ItantraMessage) -> Unit = {},
    onStopTts: () -> Unit = {}
) {
    val isSent = message.direction == MessageDirection.SENT
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val copyMessageAction: () -> Unit = {
        clipboardManager.setText(AnnotatedString(message.content))
        Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    val bubbleBorder = when (message.priority) {
        MessagePriority.URGENT -> BorderStrokeSpec(1.5.dp, ItantraEmergency)
        MessagePriority.HIGH -> BorderStrokeSpec(1.dp, StatusWarning)
        else -> null
    }

    Row(
        horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ItantraDimens.SpacingSm, vertical = 4.dp)
    ) {
        Column(
            horizontalAlignment = if (isSent) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            // Language + priority label row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text  = message.language.code.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                when (message.priority) {
                    MessagePriority.URGENT -> {
                        Spacer(Modifier.width(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.PriorityHigh,
                                contentDescription = null,
                                tint = ItantraEmergency,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text  = "EMERGENCY",
                                style = MaterialTheme.typography.labelSmall,
                                color = ItantraEmergency,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    MessagePriority.HIGH -> {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text  = "IMPORTANT",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusWarning,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    else -> Unit
                }
            }

            // Message bubble
            val bubbleShape = RoundedCornerShape(
                topStart     = if (isSent) 16.dp else 4.dp,
                topEnd       = if (isSent) 4.dp else 16.dp,
                bottomStart  = 16.dp,
                bottomEnd    = 16.dp
            )

            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(
                        if (isSent) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .then(
                        if (bubbleBorder != null) {
                            Modifier.border(bubbleBorder.width, bubbleBorder.color, bubbleShape)
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text  = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSent) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }

            // Time + status + action row
            Spacer(Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text  = timeFormatter.format(Date(message.timestampMs)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Copy button (quick access)
                IconButton(
                    onClick = copyMessageAction,
                    modifier = Modifier
                        .size(32.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Copy message text"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                }

                if (isSent) {
                    Spacer(Modifier.width(2.dp))
                    StatusIcon(status = message.status)
                } else {
                    Spacer(Modifier.width(4.dp))
                    // TTS Audio Playback button
                    IconButton(
                        onClick = {
                            if (isPlaying) onStopTts() else onPlayTts(message)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = if (isPlaying) "Stop playing speech" else "Play speech"
                            }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (isPlaying) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Speaking…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else if (ttsState is TtsState.LanguageUnavailable) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Voice unavailable",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusWarning
                        )
                    }
                }
            }
        }
    }
}

private data class BorderStrokeSpec(val width: androidx.compose.ui.unit.Dp, val color: Color)

@Composable
private fun StatusIcon(status: MessageStatus) {
    val (icon, tint) = when (status) {
        MessageStatus.PENDING   -> Icons.Filled.HourglassEmpty to StatusWarning
        MessageStatus.SENT      -> Icons.Filled.Check to MaterialTheme.colorScheme.onSurfaceVariant
        MessageStatus.DELIVERED -> Icons.Filled.CheckCircle to StatusOnline
        MessageStatus.FAILED    -> Icons.Filled.Error to StatusError
    }
    Icon(
        imageVector = icon,
        contentDescription = "Status: ${status.name}",
        tint = tint,
        modifier = Modifier.size(14.dp)
    )
}
