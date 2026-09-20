package com.itantra.app.ui.screens.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.itantra.app.R
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.DiscoveredDevice
import com.itantra.app.domain.model.TransportType
import com.itantra.app.ui.components.ConnectionStatusBadge
import com.itantra.app.ui.theme.StatusError
import com.itantra.app.ui.theme.StatusOnline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(viewModel: ConnectionViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = stringResource(R.string.connection_title),
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (uiState.isDemoMode) {
                        com.itantra.app.ui.components.DemoModeBadge(
                            modifier = Modifier.padding(end = 12.dp)
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
                top    = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp,
                start  = 16.dp,
                end    = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── SIH Demo & Reliability Controls ───────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SIH Demo Mode (Loopback)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (uiState.isDemoMode) "Simulated peer active for judging" else "Live Wi-Fi / Bluetooth hardware",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isDemoMode,
                            onCheckedChange = { viewModel.setDemoMode(it) }
                        )
                    }

                    if (uiState.isDemoMode) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Reliability & Fault Simulation",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (uiState.lastDemoAction != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = uiState.lastDemoAction!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.injectCorruptedPacket() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Inject Bad CRC", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { viewModel.toggleFaultInjection() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.isFaultInjected) StatusError else MaterialTheme.colorScheme.errorContainer,
                                    contentColor = if (uiState.isFaultInjected) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onErrorContainer
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (uiState.isFaultInjected) "Clear Fault" else "Inject Fault", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { viewModel.resetDemoSession() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reset Demo Session & Sockets", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // ── Connection status card ────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(16.dp)
                ) {
                    Text(
                        text  = "Current Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    ConnectionStatusBadge(connectionState = uiState.connectionState)
                    if (uiState.connectionState is ConnectionState.Connected) {
                        val conn = uiState.connectionState as ConnectionState.Connected
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text  = "Device: ${conn.deviceName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text  = "Transport: ${conn.transport.name.replace('_', ' ')}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text  = "Signal: ${conn.signalStrength}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (conn.signalStrength > 60) StatusOnline else StatusError
                        )
                    }
                    if (uiState.errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text  = uiState.errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusError
                        )
                    }
                }
            }

            // ── Transport mode selector ───────────────────────────────────
            item {
                Text(
                    text  = "Transport Mode",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    TransportToggle(
                        label     = stringResource(R.string.connection_wifi_direct),
                        icon      = Icons.Filled.Wifi,
                        selected  = uiState.selectedTransport == TransportType.WIFI_DIRECT,
                        onClick   = { viewModel.selectTransport(TransportType.WIFI_DIRECT) },
                        modifier  = Modifier.weight(1f)
                    )
                    TransportToggle(
                        label     = stringResource(R.string.connection_bluetooth),
                        icon      = Icons.Filled.Bluetooth,
                        selected  = uiState.selectedTransport == TransportType.BLUETOOTH,
                        onClick   = { viewModel.selectTransport(TransportType.BLUETOOTH) },
                        modifier  = Modifier.weight(1f)
                    )
                }
            }

            // ── Phase 5: Host Mode (Server Socket) Toggle ─────────────────
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.isHostMode) stringResource(R.string.comm_host_mode) else stringResource(R.string.comm_client_mode),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (uiState.isHostMode) stringResource(R.string.comm_server_listening, 8988) else "Connect to nearby peer or server",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isHostMode,
                        onCheckedChange = { viewModel.toggleHostMode() }
                    )
                }
            }

            // ── Scan / disconnect controls ────────────────────────────────
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    if (uiState.connectionState is ConnectionState.Connected) {
                        OutlinedButton(
                            onClick  = { viewModel.disconnect() },
                            modifier = Modifier.fillMaxWidth(),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = StatusError)
                        ) {
                            Text(stringResource(R.string.connection_disconnect))
                        }
                    } else {
                        Button(
                            onClick  = {
                                if (uiState.isScanning) viewModel.stopScan() else viewModel.startScan()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isScanning) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.connection_scanning))
                            } else {
                                Text(stringResource(R.string.connection_scan))
                            }
                        }
                    }
                }
            }

            // ── Discovered devices ────────────────────────────────────────
            if (uiState.discoveredDevices.isNotEmpty()) {
                item {
                    Text(
                        text  = "Nearby Devices",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(uiState.discoveredDevices, key = { it.id }) { device ->
                    DeviceCard(device = device, onConnect = { viewModel.connectToDevice(device) })
                }
            } else if (!uiState.isScanning && uiState.connectionState is ConnectionState.Disconnected) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth().height(80.dp)
                    ) {
                        Text(
                            text  = stringResource(R.string.connection_no_devices),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransportToggle(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                  else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 14.dp)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier           = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelMedium,
            color      = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun DeviceCard(device: DiscoveredDevice, onConnect: () -> Unit) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Text(
                text       = device.name,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text  = "${device.transport.name.replace('_', ' ')}  ·  ${device.signalStrength}% signal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = onConnect,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(stringResource(R.string.connection_connect), style = MaterialTheme.typography.labelMedium)
        }
    }
}
