package com.itantra.app.communication

import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.DiscoveredDevice
import com.itantra.app.domain.model.TransportType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mock implementation of [CommunicationTransport] for unit tests, offline development,
 * and predictable verification of message framing, delivery, and fault handling.
 */
class MockCommunicationTransport(
    override val transportType: TransportType = TransportType.WIFI_DIRECT,
    var autoEchoAck: Boolean = true
) : CommunicationTransport {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _incomingPayloads = MutableSharedFlow<ByteArray>(replay = 1, extraBufferCapacity = 64)
    override fun observeIncomingPayloads(): Flow<ByteArray> = _incomingPayloads.asSharedFlow()

    private val sentPayloadsList = mutableListOf<ByteArray>()

    /** Test hooks for simulating network conditions. */
    var shouldFailConnection: Boolean = false
    var shouldFailSend: Boolean = false
    var sendDelayMs: Long = 0L
    var lastSentPayload: ByteArray? = null
    var sentPayloadCount: Int = 0

    fun simulateConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }

    fun getSentPayloads(): List<ByteArray> = synchronized(sentPayloadsList) {
        sentPayloadsList.toList()
    }

    fun setFailConnection(fail: Boolean) {
        shouldFailConnection = fail
    }

    fun setFailSend(fail: Boolean) {
        shouldFailSend = fail
    }

    override suspend fun startDiscovery(): Result<Unit> {
        _connectionState.value = ConnectionState.Scanning
        _discoveredDevices.value = emptyList()

        if (sendDelayMs > 0) delay(sendDelayMs)
        _discoveredDevices.value = listOf(
            DiscoveredDevice("mock-device-01", "iTantra-Peer-Alpha", transportType, 92),
            DiscoveredDevice("mock-device-02", "iTantra-Peer-Beta", transportType, 68)
        )
        return Result.success(Unit)
    }

    override suspend fun stopDiscovery(): Result<Unit> {
        if (_connectionState.value is ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Disconnected
        }
        return Result.success(Unit)
    }

    override suspend fun connect(endpoint: String): Result<Unit> {
        if (shouldFailConnection) {
            _connectionState.value = ConnectionState.Error("Mock connection rejected.")
            return Result.failure(IllegalStateException("Mock connection rejected."))
        }

        _connectionState.value = ConnectionState.Connected(
            deviceName = "Peer-$endpoint",
            deviceId = endpoint,
            transport = transportType,
            signalStrength = 85
        )
        return Result.success(Unit)
    }

    override suspend fun connect(device: DiscoveredDevice): Result<Unit> {
        return connect(device.id)
    }

    override suspend fun disconnect(): Result<Unit> {
        _connectionState.value = ConnectionState.Disconnected
        return Result.success(Unit)
    }

    override suspend fun send(payload: ByteArray): Result<Unit> {
        if (_connectionState.value !is ConnectionState.Connected) {
            if (shouldFailConnection) {
                return Result.failure(IllegalStateException("Cannot send payload while disconnected."))
            }
            _connectionState.value = ConnectionState.Connected("mock-peer", "mock-peer-01", transportType, 100)
        }

        if (shouldFailSend) {
            return Result.failure(IllegalStateException("Simulated transport transmission failure."))
        }

        lastSentPayload = payload
        sentPayloadCount++
        synchronized(sentPayloadsList) {
            sentPayloadsList.add(payload)
        }

        if (sendDelayMs > 0) delay(sendDelayMs)

        // Automatically synthesize and emit an ACK packet if requested
        if (autoEchoAck) {
            val parsedMsg = MessageSerializer.deserialize(payload).getOrNull()
            if (parsedMsg != null && parsedMsg.messageType == MessageType.CHAT) {
                val ack = TextMessage(
                    messageId = MessageIdGenerator.generateAckId(parsedMsg.messageId),
                    senderId = "mock-remote-peer",
                    receiverId = parsedMsg.senderId,
                    text = "ACK:${parsedMsg.messageId}",
                    languageCode = parsedMsg.languageCode,
                    messageType = MessageType.ACK,
                    priority = MessagePriority.NORMAL
                ).withComputedChecksum()

                val ackBytes = MessageSerializer.serialize(ack).getOrNull()
                if (ackBytes != null) {
                    _incomingPayloads.emit(ackBytes)
                }
            }
        }

        return Result.success(Unit)
    }

    /**
     * Test helper to inject simulated incoming raw packet payloads.
     */
    suspend fun emitIncomingPayload(payload: ByteArray) {
        _incomingPayloads.emit(payload)
    }
}
