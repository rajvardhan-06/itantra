package com.itantra.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.ui.theme.ItantraDimens
import com.itantra.app.ui.theme.StatusError
import com.itantra.app.ui.theme.StatusOffline
import com.itantra.app.ui.theme.StatusOnline
import com.itantra.app.ui.theme.StatusWarning

/**
 * A small pill-shaped badge showing the current connection or operational status.
 * Includes both a coloured dot and text label (no colour-only communication).
 */
@Composable
fun StatusBadge(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(ItantraDimens.RadiusPill))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .semantics { contentDescription = "Status: $label" }
    ) {
        Icon(
            imageVector = Icons.Filled.Circle,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(8.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp
        )
    }
}

/**
 * Convenience badge wired to a [ConnectionState].
 * Maps each sealed state to a user-readable label and semantic colour.
 */
@Composable
fun ConnectionStatusBadge(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (connectionState) {
        is ConnectionState.Connected   -> "Connected" to StatusOnline
        is ConnectionState.Connecting  -> "Connecting…" to StatusWarning
        is ConnectionState.Scanning    -> "Scanning…" to StatusWarning
        is ConnectionState.Error       -> "Error" to StatusError
        ConnectionState.Disconnected   -> "Offline" to StatusOffline
    }
    StatusBadge(label = label, color = color, modifier = modifier)
}

/** Simple "Offline Mode" badge displayed on the home screen. */
@Composable
fun OfflineModeBadge(modifier: Modifier = Modifier) {
    StatusBadge(
        label    = "OFFLINE MODE",
        color    = StatusOffline,
        modifier = modifier
    )
}

/** Distinct badge displayed when running on simulated/mock transports or models. */
@Composable
fun DemoModeBadge(
    modifier: Modifier = Modifier
) {
    StatusBadge(
        label    = "DEMO / MOCK MODE",
        color    = StatusWarning,
        modifier = modifier
    )
}

