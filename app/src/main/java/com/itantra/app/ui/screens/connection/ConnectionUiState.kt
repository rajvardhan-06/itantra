package com.itantra.app.ui.screens.connection

import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.DiscoveredDevice
import com.itantra.app.domain.model.TransportType

data class ConnectionUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val discoveredDevices: List<DiscoveredDevice> = emptyList(),
    val selectedTransport: TransportType = TransportType.WIFI_DIRECT,
    val isScanning: Boolean = false,
    val isHostMode: Boolean = false,
    val isDemoMode: Boolean = true,
    val isFaultInjected: Boolean = false,
    val lastDemoAction: String? = null,
    val errorMessage: String? = null
)
