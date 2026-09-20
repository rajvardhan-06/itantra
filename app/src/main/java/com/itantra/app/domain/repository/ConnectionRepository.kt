package com.itantra.app.domain.repository

import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.DiscoveredDevice
import com.itantra.app.domain.model.TransportType
import kotlinx.coroutines.flow.Flow

/**
 * Contract for managing device discovery and connections.
 * Concrete implementations will wrap Wi-Fi Direct or Bluetooth LE APIs.
 */
interface ConnectionRepository {
    /** Stream of the current connection state. */
    fun observeConnectionState(): Flow<ConnectionState>

    /** Stream of discovered nearby devices during an active scan. */
    fun observeDiscoveredDevices(): Flow<List<DiscoveredDevice>>

    /** Begin scanning for nearby devices on the given transport. */
    suspend fun startScan(transport: TransportType)

    /** Stop any ongoing scan. */
    suspend fun stopScan()

    /** Initiate a connection to the given discovered device. */
    suspend fun connectToDevice(device: DiscoveredDevice)

    /** Terminate the current connection. */
    suspend fun disconnect()
}
