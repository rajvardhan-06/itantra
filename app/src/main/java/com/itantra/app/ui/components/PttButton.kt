package com.itantra.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.itantra.app.R
import com.itantra.app.ui.theme.StatusError

/**
 * Large circular Push-to-Talk button with animated pulse rings when [isRecording].
 * Accessible with screen reader role, state description, and disabled state styling.
 *
 * @param isRecording  Whether the microphone is actively recording.
 * @param onPress      Called when the user presses the button (start recording).
 * @param onRelease    Called when the user releases the button (stop recording).
 * @param size         Diameter of the button.
 * @param enabled      When false, the button is visually dimmed and non-interactive.
 */
@Composable
fun PttButton(
    isRecording: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    enabled: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ptt_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.32f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue  = 0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val buttonDescription = if (isRecording) {
        "Recording in progress. Release to stop recording"
    } else {
        "Hold to record audio, release to transcribe"
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .semantics {
                role = Role.Button
                contentDescription = buttonDescription
                stateDescription = if (isRecording) "Recording" else if (enabled) "Ready" else "Disabled"
                if (!enabled) disabled()
            }
            .alpha(if (enabled) 1f else 0.45f)
    ) {
        // Outer pulse ring — only visible when recording
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(size * 1.55f)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        StatusError.copy(alpha = pulseAlpha * 0.4f)
                    )
            )
            Box(
                modifier = Modifier
                    .size(size * 1.28f)
                    .scale(pulseScale * 0.92f)
                    .clip(CircleShape)
                    .background(
                        StatusError.copy(alpha = pulseAlpha * 0.3f)
                    )
            )
        }

        // Main button circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    if (isRecording) {
                        Brush.radialGradient(
                            colors = listOf(StatusError, StatusError.copy(alpha = 0.82f))
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            )
                        )
                    }
                )
                .border(
                    width = if (isRecording) 3.dp else 2.dp,
                    color = if (isRecording) StatusError.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    shape = CircleShape
                )
                .pointerInput(enabled) {
                    if (enabled) {
                        detectTapGestures(
                            onPress = {
                                onPress()
                                tryAwaitRelease()
                                onRelease()
                            }
                        )
                    }
                }
        ) {
            Icon(
                imageVector        = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = null, // Handled by outer box semantics
                tint               = Color.White,
                modifier           = Modifier.size(size * 0.42f)
            )
        }
    }
}
