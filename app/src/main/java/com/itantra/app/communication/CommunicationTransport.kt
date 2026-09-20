package com.itantra.app.communication

import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.DiscoveredDevice
import com.itantra.app.domain.model.TransportType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Replaceable abstraction for underlying peer-to-peer communication transports (Wi-Fi, Bluetooth, Mock).
 *
 * Transports operate purely at the byte payload layer with length framing,
 * unaware of high-level speech or transcription concepts.
 */
interface CommunicationTransport {

    /** The physical or simulated medium implemented by this transport. */
    val transportType: TransportType

    /** Reactive state flow representing connection status. */
    val connectionState: StateFlow<ConnectionState>

    /** Reactive state flow representing peers found during an active scan. */
    val discoveredDevices: StateFlow<List<DiscoveredDevice>>

    /**
     * Flow of raw incoming framed message payloads received from the connected peer.
     */
    fun observeIncomingPayloads(): Flow<ByteArray>

    /** Convenience alias matching standard transport specification. */
    fun observeIncomingMessages(): Flow<ByteArray> = observeIncomingPayloads()

    /** Convenience alias matching standard transport specification. */
    fun connectionState(): StateFlow<ConnectionState> = connectionState

    /**
     * Starts discovering nearby devices on this transport medium.
     */
    suspend fun startDiscovery(): Result<Unit>

    /**
     * Stops active peer discovery.
     */
    suspend fun stopDiscovery(): Result<Unit>

    /**
     * Establishes a connection to a specific remote endpoint (e.g., `192.168.49.1:8988` or Bluetooth MAC).
     */
    suspend fun connect(endpoint: String): Result<Unit>

    /**
     * Establishes a connection to a discovered device descriptor.
     */
    suspend fun connect(device: DiscoveredDevice): Result<Unit>

    /**
     * Disconnects from the currently connected peer and closes sockets/streams.
     */
    suspend fun disconnect(): Result<Unit>

    /**
     * Sends a raw byte payload across the active transport connection.
     */
    suspend fun send(payload: ByteArray): Result<Unit>
}
