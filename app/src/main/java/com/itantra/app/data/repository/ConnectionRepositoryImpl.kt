package com.itantra.app.data.repository

import com.itantra.app.communication.CommunicationManager
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.DiscoveredDevice
import com.itantra.app.domain.model.TransportType
import com.itantra.app.domain.repository.ConnectionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementation of [ConnectionRepository] that coordinates device discovery and connections.
 * Can delegate directly to an underlying [CommunicationManager] or operate with simulated peers.
 */
class ConnectionRepositoryImpl(
    private val communicationManager: CommunicationManager? = null
) : ConnectionRepository {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())

    override fun observeConnectionState(): Flow<ConnectionState> {
        return communicationManager?.connectionState ?: _connectionState.asStateFlow()
    }

    override fun observeDiscoveredDevices(): Flow<List<DiscoveredDevice>> {
        return communicationManager?.discoveredDevices ?: _discoveredDevices.asStateFlow()
    }

    override suspend fun startScan(transport: TransportType) {
        if (communicationManager != null) {
            communicationManager.startDiscovery()
        } else {
            _connectionState.value = ConnectionState.Scanning
            _discoveredDevices.value = emptyList()

            delay(700L)
            _discoveredDevices.value = listOf(
                DiscoveredDevice("device-01", "iTantra-Alpha", transport, 89),
                DiscoveredDevice("device-02", "iTantra-Beta", transport, 61),
            )
            delay(900L)
            _discoveredDevices.value = _discoveredDevices.value + listOf(
                DiscoveredDevice("device-03", "iTantra-Gamma", transport, 38),
            )

            delay(2500L)
            if (_connectionState.value is ConnectionState.Scanning) {
                _connectionState.value = ConnectionState.Disconnected
            }
        }
    }

    override suspend fun stopScan() {
        if (communicationManager != null) {
            communicationManager.stopDiscovery()
        } else {
            if (_connectionState.value is ConnectionState.Scanning) {
                _connectionState.value = ConnectionState.Disconnected
            }
        }
    }

    override suspend fun connectToDevice(device: DiscoveredDevice) {
        if (communicationManager != null) {
            communicationManager.connect(device)
        } else {
            _connectionState.value = ConnectionState.Connecting(device.name)
            delay(1200L)

            _connectionState.value = ConnectionState.Connected(
                deviceName = device.name,
                deviceId = device.id,
                transport = device.transport,
                signalStrength = device.signalStrength
            )
        }
    }

    override suspend fun disconnect() {
        if (communicationManager != null) {
            communicationManager.disconnect()
        } else {
            delay(300L)
            _connectionState.value = ConnectionState.Disconnected
            _discoveredDevices.value = emptyList()
        }
    }
}
