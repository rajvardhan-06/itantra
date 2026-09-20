package com.itantra.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.itantra.app.R
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.ui.components.ConnectionStatusBadge
import com.itantra.app.ui.components.EmptyStateView
import com.itantra.app.ui.components.ItantraCard
import com.itantra.app.ui.components.MessageItemCard
import com.itantra.app.ui.components.OfflineModeBadge
import com.itantra.app.ui.components.SectionHeader
import com.itantra.app.ui.navigation.Screen
import com.itantra.app.ui.theme.ItantraDimens
import com.itantra.app.ui.theme.StatusOnline

import com.itantra.app.ui.components.DemoModeBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateTo: (Screen) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text  = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text  = stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                top    = paddingValues.calculateTopPadding() + ItantraDimens.SpacingSm,
                bottom = paddingValues.calculateBottomPadding() + ItantraDimens.SpacingLg,
                start  = ItantraDimens.ScreenPaddingHorizontal,
                end    = ItantraDimens.ScreenPaddingHorizontal
            ),
            verticalArrangement = Arrangement.spacedBy(ItantraDimens.SpacingMd)
        ) {
            // ── Status row ────────────────────────────────────────────────
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ItantraDimens.SpacingSm),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    OfflineModeBadge()
                    ConnectionStatusBadge(connectionState = uiState.connectionState)
                    if (uiState.isDemoMode) {
                        DemoModeBadge()
                    }
                }
            }

            // ── SIH Demo Simulation Controls (when in Demo Mode) ─────────
            if (uiState.isDemoMode) {
                item {
                    ItantraCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "SIH Demonstration Controls",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Loopback Mode",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = uiState.lastDemoAction ?: "Simulate incoming peer messages through full framing, CRC32 check, ACK, and TTS playback.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { viewModel.simulateIncomingMessage(0) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Hindi Peer", style = MaterialTheme.typography.labelSmall)
                                }
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { viewModel.simulateIncomingMessage(1) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Bengali Peer", style = MaterialTheme.typography.labelSmall)
                                }
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { viewModel.simulateIncomingMessage(2) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("English Peer", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // ── Connection Status Card ────────────────────────────────────
            item {
                ConnectionInfoCard(
                    connectionState = uiState.connectionState,
                    onClick = { onNavigateTo(Screen.Connection) }
                )
            }

            // ── Hero Communication CTA ────────────────────────────────────
            item {
                CommunicationHeroCard(
                    languageName = uiState.activeLanguage.nativeName,
                    languageDisplay = uiState.activeLanguage.displayName,
                    onClick = { onNavigateTo(Screen.PushToTalk) }
                )
            }

            // ── Quick Actions Grid ────────────────────────────────────────
            item {
                SectionHeader(title = stringResource(R.string.home_quick_actions))
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ItantraDimens.SpacingMd),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickActionCard(
                        icon    = Icons.Filled.Language,
                        label   = "Models",
                        onClick = { onNavigateTo(Screen.Language) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        icon    = Icons.Filled.Wifi,
                        label   = "Connect",
                        onClick = { onNavigateTo(Screen.Connection) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        icon    = Icons.AutoMirrored.Filled.Chat,
                        label   = "History",
                        onClick = { onNavigateTo(Screen.History) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Summary Stats ─────────────────────────────────────────────
            if (uiState.totalMessageCount > 0) {
                item {
                    ItantraCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(value = "${uiState.totalMessageCount}", label = "Messages")
                            StatItem(value = uiState.activeLanguage.code.uppercase(), label = "Language")
                            StatItem(
                                value = if (uiState.connectionState is ConnectionState.Connected) "Active" else "—",
                                label = "Link"
                            )
                        }
                    }
                }
            }

            // ── Recent Transmissions ──────────────────────────────────────
            item {
                SectionHeader(title = stringResource(R.string.home_recent_title))
            }

            if (uiState.recentMessages.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        title = stringResource(R.string.home_no_recent),
                        description = "Start a Push-to-Talk session to transmit your first message.",
                        actionLabel = "Start Communication",
                        onAction = { onNavigateTo(Screen.PushToTalk) }
                    )
                }
            } else {
                items(uiState.recentMessages, key = { it.id }) { message ->
                    MessageItemCard(message = message)
                }
            }
        }
    }
}

// ── Connection Info Card ───────────────────────────────────────────────────

@Composable
private fun ConnectionInfoCard(
    connectionState: ConnectionState,
    onClick: () -> Unit
) {
    ItantraCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(ItantraDimens.RadiusMedium))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(ItantraDimens.IconMd)
                )
            }
            Spacer(Modifier.width(ItantraDimens.SpacingMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (connectionState) {
                        is ConnectionState.Connected  -> "Connected to ${connectionState.deviceName}"
                        is ConnectionState.Connecting  -> "Connecting to ${connectionState.deviceName}…"
                        is ConnectionState.Scanning    -> "Scanning for devices…"
                        is ConnectionState.Error       -> "Connection error"
                        ConnectionState.Disconnected   -> "No device connected"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (connectionState) {
                        is ConnectionState.Connected  -> "${connectionState.transport.name.replace('_', ' ')}  ·  ${connectionState.signalStrength}% signal"
                        is ConnectionState.Error       -> connectionState.reason
                        else                           -> "Tap to manage connections"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (connectionState is ConnectionState.Connected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(StatusOnline)
                )
            }
        }
    }
}

// ── Communication Hero Card ────────────────────────────────────────────────

@Composable
private fun CommunicationHeroCard(
    languageName: String,
    languageDisplay: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ItantraDimens.RadiusXl))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                )
            )
            .clickable { onClick() }
            .padding(ItantraDimens.CardPaddingLarge)
            .semantics { contentDescription = "Start push to talk communication in $languageDisplay" }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(ItantraDimens.RadiusLarge))
                    .background(Color.White.copy(alpha = 0.18f))
            ) {
                Icon(
                    imageVector        = Icons.Filled.Mic,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(ItantraDimens.IconLg)
                )
            }
            Spacer(Modifier.width(ItantraDimens.SpacingLg))
            Column {
                Text(
                    text  = stringResource(R.string.home_ptt_quick_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(ItantraDimens.SpacingXs))
                Text(
                    text  = stringResource(R.string.home_ptt_quick_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(ItantraDimens.SpacingSm))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(ItantraDimens.RadiusPill))
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text  = "$languageName  ·  $languageDisplay",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── Quick Action Card ──────────────────────────────────────────────────────

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(ItantraDimens.RadiusLarge))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .clickable { onClick() }
            .padding(vertical = ItantraDimens.SpacingLg, horizontal = ItantraDimens.SpacingSm)
            .semantics { contentDescription = "$label action" }
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(ItantraDimens.IconLg)
        )
        Spacer(Modifier.height(ItantraDimens.SpacingSm))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Stat Item ──────────────────────────────────────────────────────────────

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
