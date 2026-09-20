package com.itantra.app.communication

import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.DiscoveredDevice
import com.itantra.app.domain.model.ItantraMessage
import com.itantra.app.domain.model.MessageDirection
import com.itantra.app.domain.model.MessagePriority as DomainPriority
import com.itantra.app.domain.model.MessageStatus
import com.itantra.app.domain.model.SupportedLanguage
import com.itantra.app.domain.repository.MessageRepository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * High-level coordinator managing transmission reliability, acknowledgement tracking,
 * message deduplication, and integration with the conversation repository.
 */
class CommunicationManager(
    private var transport: CommunicationTransport = MockCommunicationTransport(),
    private val messageRepository: MessageRepository? = null,
    private val localDeviceId: String = MessageIdGenerator.generateDeviceId(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val externalScope: CoroutineScope? = null,
    private val ackTimeoutMs: Long = MessageProtocol.ACK_TIMEOUT_MS,
    private val retryBackoffBaseMs: Long = 500L
) {

    private val managerJob = SupervisorJob()
    private val scope = externalScope ?: CoroutineScope(managerJob + dispatcher)

    fun release() {
        managerJob.cancel()
        transportListenerJob?.cancel()
        connectionObserverJob?.cancel()
    }

    private val _communicationState = MutableStateFlow<CommunicationState>(CommunicationState.Disconnected)
    val communicationState: StateFlow<CommunicationState> = _communicationState.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _deliveryStatuses = MutableStateFlow<Map<String, DeliveryStatus>>(emptyMap())
    val deliveryStatuses: StateFlow<Map<String, DeliveryStatus>> = _deliveryStatuses.asStateFlow()

    private val _diagnostics = MutableStateFlow(CommunicationDiagnostics())
    val diagnostics: StateFlow<CommunicationDiagnostics> = _diagnostics.asStateFlow()

    // Deduplication cache: messageId -> timestamp received
    private val recentReceivedMessageIds = ConcurrentHashMap<String, Long>()

    // Outgoing pending messages awaiting ACK: messageId -> PendingMessage
    private val pendingMessages = ConcurrentHashMap<String, PendingMessage>()

    // Monotonic sequence number for outgoing messages in this session
    private val sequenceNumberGenerator = AtomicLong(0L)

    private var transportListenerJob: Job? = null
    private var connectionObserverJob: Job? = null

    init {
        attachTransport(transport)
    }

    /**
     * Swaps the active transport layer (e.g., between Wi-Fi Socket, Bluetooth, and Mock).
     */
    fun setTransport(newTransport: CommunicationTransport) {
        if (transport == newTransport) return
        scope.launch(dispatcher) {
            transport.disconnect()
            transport = newTransport
            attachTransport(newTransport)
        }
    }

    fun getActiveTransport(): CommunicationTransport = transport

    private fun syncConnectionState(state: ConnectionState, transportType: com.itantra.app.domain.model.TransportType) {
        _connectionState.value = state
        _diagnostics.update { diag ->
            diag.copy(
                activeTransportType = transportType.name,
                remoteEndpoint = when (state) {
                    is ConnectionState.Connected -> "${state.deviceName} (${state.deviceId})"
                    is ConnectionState.Connecting -> "Connecting to ${state.deviceName}..."
                    is ConnectionState.Scanning -> "Scanning..."
                    is ConnectionState.Disconnected -> "Disconnected"
                    is ConnectionState.Error -> "Error: ${state.reason}"
                }
            )
        }
        when (state) {
            is ConnectionState.Connected -> {
                val device = DiscoveredDevice(
                    id = state.deviceId,
                    name = state.deviceName,
                    transport = state.transport,
                    signalStrength = state.signalStrength
                )
                _communicationState.value = CommunicationState.Connected(device)
            }
            is ConnectionState.Connecting -> {
                _communicationState.value = CommunicationState.Connecting(state.deviceName, transportType)
            }
            is ConnectionState.Scanning -> {
                _communicationState.value = CommunicationState.Discovering(transportType)
            }
            is ConnectionState.Disconnected -> {
                _communicationState.value = CommunicationState.Disconnected
            }
            is ConnectionState.Error -> {
                _communicationState.value = CommunicationState.Failed(state.reason)
            }
        }
    }

    private fun attachTransport(t: CommunicationTransport) {
        transportListenerJob?.cancel()
        connectionObserverJob?.cancel()

        syncConnectionState(t.connectionState.value, t.transportType)

        connectionObserverJob = scope.launch(dispatcher) {
            launch {
                t.connectionState.collect { state ->
                    syncConnectionState(state, t.transportType)
                }
            }

            launch {
                t.discoveredDevices.collect { devices ->
                    _discoveredDevices.value = devices
                }
            }
        }

        transportListenerJob = scope.launch(dispatcher) {
            t.observeIncomingPayloads().collect { payload ->
                handleIncomingPayload(payload)
            }
        }
    }

    suspend fun startDiscovery(): Result<Unit> {
        val res = transport.startDiscovery()
        syncConnectionState(transport.connectionState.value, transport.transportType)
        return res
    }

    suspend fun stopDiscovery(): Result<Unit> {
        val res = transport.stopDiscovery()
        syncConnectionState(transport.connectionState.value, transport.transportType)
        return res
    }

    suspend fun connect(endpoint: String): Result<Unit> {
        val res = transport.connect(endpoint)
        syncConnectionState(transport.connectionState.value, transport.transportType)
        return res
    }

    suspend fun connect(device: DiscoveredDevice): Result<Unit> {
        val res = transport.connect(device)
        syncConnectionState(transport.connectionState.value, transport.transportType)
        return res
    }

    suspend fun disconnect(): Result<Unit> {
        val res = transport.disconnect()
        syncConnectionState(transport.connectionState.value, transport.transportType)
        return res
    }

    /**
     * Constructs, validates, persists, and transmits an outgoing text message.
     */
    suspend fun sendMessage(
        text: String,
        languageCode: String,
        priority: MessagePriority = MessagePriority.NORMAL,
        receiverId: String? = null
    ): Result<TextMessage> = withContext(dispatcher) {
        val messageId = MessageIdGenerator.generateMessageId()
        val seq = sequenceNumberGenerator.incrementAndGet()
        val now = System.currentTimeMillis()

        val textMessage = TextMessage(
            messageId = messageId,
            senderId = localDeviceId,
            receiverId = receiverId,
            text = text,
            languageCode = languageCode,
            timestamp = now,
            messageType = MessageType.CHAT,
            priority = priority,
            sequenceNumber = seq,
            protocolVersion = MessageProtocol.PROTOCOL_VERSION
        ).withComputedChecksum()

        // 1. Validation
        val validation = MessageValidator.validate(textMessage)
        if (!validation.isValid) {
            val reason = (validation as ValidationResult.Invalid).reason
            return@withContext Result.failure(IllegalArgumentException("Message validation failed: $reason"))
        }

        // 2. Persist to repository as PENDING
        val domainLang = SupportedLanguage.fromCode(languageCode)
        val itantraMessage = ItantraMessage(
            id = messageId,
            content = text,
            language = domainLang,
            direction = MessageDirection.SENT,
            status = MessageStatus.PENDING,
            priority = when (priority) {
                MessagePriority.NORMAL -> DomainPriority.NORMAL
                MessagePriority.IMPORTANT -> DomainPriority.HIGH
                MessagePriority.ALERT -> DomainPriority.URGENT
            },
            timestampMs = now
        )
        messageRepository?.addMessage(itantraMessage)

        // 3. Mark PENDING and enqueue for transmission
        updateDeliveryStatus(messageId, DeliveryStatus.PENDING)
        val pending = PendingMessage(textMessage, attemptCount = 0)
        pendingMessages[messageId] = pending

        // 4. Initiate transmission attempt
        transmitPendingMessage(pending)

        Result.success(textMessage)
    }

    /**
     * Retries sending a previously failed message.
     */
    suspend fun retryMessage(messageId: String): Result<Unit> = withContext(dispatcher) {
        var pending = pendingMessages[messageId]
        if (pending == null) {
            val repoMsg = messageRepository?.observeMessages()?.firstOrNull()?.find { it.id == messageId }
            if (repoMsg != null) {
                val seq = sequenceNumberGenerator.incrementAndGet()
                val textMsg = TextMessage(
                    messageId = repoMsg.id,
                    senderId = localDeviceId,
                    text = repoMsg.content,
                    languageCode = repoMsg.language.code,
                    timestamp = System.currentTimeMillis(),
                    sequenceNumber = seq,
                    priority = when (repoMsg.priority) {
                        DomainPriority.LOW, DomainPriority.NORMAL -> MessagePriority.NORMAL
                        DomainPriority.HIGH -> MessagePriority.IMPORTANT
                        DomainPriority.URGENT -> MessagePriority.ALERT
                    }
                ).withComputedChecksum()
                pending = PendingMessage(textMsg, attemptCount = 0)
                pendingMessages[messageId] = pending
            }
        }

        if (pending == null) {
            return@withContext Result.failure(IllegalArgumentException("Message $messageId not found in pending cache."))
        }

        val freshPending = pending.copy(attemptCount = 0)
        pendingMessages[messageId] = freshPending
        updateDeliveryStatus(messageId, DeliveryStatus.PENDING)
        transmitPendingMessage(freshPending)
        Result.success(Unit)
    }

    private fun transmitPendingMessage(pending: PendingMessage) {
        scope.launch(dispatcher) {
            val message = pending.message
            val messageId = message.messageId

            val serialized = MessageSerializer.serialize(message)
            if (serialized.isFailure) {
                val ex = serialized.exceptionOrNull()
                updateDeliveryStatus(messageId, DeliveryStatus.FAILED)
                _communicationState.value = CommunicationState.Failed("Serialization error: ${ex?.message}", ex, messageId)
                messageRepository?.updateMessageStatus(messageId, MessageStatus.FAILED)
                return@launch
            }

            val payload = serialized.getOrThrow()
            var currentAttempt = pending.attemptCount

            while (currentAttempt < MessageProtocol.MAX_RETRY_ATTEMPTS && isActive) {
                if (!pendingMessages.containsKey(messageId) || isMessageDelivered(messageId)) {
                    return@launch
                }
                currentAttempt++
                updateDeliveryStatus(messageId, DeliveryStatus.SENDING)
                _communicationState.value = CommunicationState.Sending(messageId)

                val sendResult = transport.send(payload)
                if (sendResult.isSuccess) {
                    _diagnostics.update { it.copy(packetsSent = it.packetsSent + 1) }

                    if (!pendingMessages.containsKey(messageId)) {
                        // Already acknowledged during send (e.g. mock loopback)
                        return@launch
                    }

                    updateDeliveryStatus(messageId, DeliveryStatus.SENT)
                    _communicationState.value = CommunicationState.Sent(messageId)
                    messageRepository?.updateMessageStatus(messageId, MessageStatus.SENT)

                    // Await delivery ACK with timeout
                    val ackReceived = withTimeoutOrNull(ackTimeoutMs) {
                        while (isActive && pendingMessages.containsKey(messageId)) {
                            delay(10L)
                        }
                        true
                    }

                    if (ackReceived == true && !pendingMessages.containsKey(messageId)) {
                        // Successfully ACKed!
                        return@launch
                    }
                }

                // If not acknowledged, backoff before retry
                if (currentAttempt < MessageProtocol.MAX_RETRY_ATTEMPTS) {
                    val backoffMs = retryBackoffBaseMs * (1 shl (currentAttempt - 1))
                    delay(backoffMs)
                }
            }

            // Exhausted retries without ACK
            if (pendingMessages.containsKey(messageId)) {
                pendingMessages.remove(messageId)
                updateDeliveryStatus(messageId, DeliveryStatus.FAILED)
                _diagnostics.update { it.copy(deliveryFailures = it.deliveryFailures + 1) }
                _communicationState.value = CommunicationState.Failed("Delivery timeout or transmission failure.", failedMessageId = messageId)
                messageRepository?.updateMessageStatus(messageId, MessageStatus.FAILED)
            }
        }
    }

    private suspend fun handleIncomingPayload(payload: ByteArray) = withContext(dispatcher) {
        _communicationState.value = CommunicationState.Receiving
        _diagnostics.update { it.copy(packetsReceived = it.packetsReceived + 1) }

        val deserializedResult = MessageSerializer.deserialize(payload)
        if (deserializedResult.isFailure) {
            val ex = deserializedResult.exceptionOrNull()
            val isCrc = ex?.message?.contains("CRC32", ignoreCase = true) == true
            _diagnostics.update {
                it.copy(crc32Failures = if (isCrc) it.crc32Failures + 1 else it.crc32Failures)
            }
            _communicationState.value = CommunicationState.Failed("Malformed incoming packet: ${ex?.message}", ex)
            return@withContext
        }

        _diagnostics.update { it.copy(crc32Successes = it.crc32Successes + 1) }
        val message = deserializedResult.getOrThrow()

        // Validate mandatory protocol invariants
        val validation = MessageValidator.validate(message)
        if (!validation.isValid) {
            val reason = (validation as ValidationResult.Invalid).reason
            _communicationState.value = CommunicationState.Failed("Invalid incoming packet: $reason")
            return@withContext
        }

        when (message.messageType) {
            MessageType.ACK -> {
                // Parse the target messageId from text payload ("ACK:<messageId>")
                val ackPrefix = "ACK:"
                val targetId = if (message.text.startsWith(ackPrefix)) {
                    message.text.substring(ackPrefix.length)
                } else {
                    message.messageId
                }

                val originalPending = pendingMessages.remove(targetId)
                val rtt = if (originalPending != null) {
                    System.currentTimeMillis() - originalPending.message.timestamp
                } else null

                _diagnostics.update { current ->
                    current.copy(
                        acksReceived = current.acksReceived + 1,
                        deliverySuccesses = current.deliverySuccesses + 1,
                        lastRoundTripTimeMs = if (rtt != null && rtt >= 0) rtt else current.lastRoundTripTimeMs
                    )
                }

                updateDeliveryStatus(targetId, DeliveryStatus.DELIVERED)
                _communicationState.value = CommunicationState.Delivered(targetId)
                messageRepository?.updateMessageStatus(targetId, MessageStatus.DELIVERED)
            }
            MessageType.CHAT -> {
                // Check deduplication cache
                pruneDeduplicationCache()
                if (recentReceivedMessageIds.containsKey(message.messageId)) {
                    // Duplicate message received; re-send ACK and drop processing
                    sendAck(message)
                    return@withContext
                }

                recentReceivedMessageIds[message.messageId] = System.currentTimeMillis()

                // Dispatch ACK packet back to sender
                sendAck(message)

                // Persist to conversation repository
                val domainLang = SupportedLanguage.fromCode(message.languageCode)
                val itantraMessage = ItantraMessage(
                    id = message.messageId,
                    content = message.text,
                    language = domainLang,
                    direction = MessageDirection.RECEIVED,
                    status = MessageStatus.DELIVERED,
                    priority = when (message.priority) {
                        MessagePriority.NORMAL -> DomainPriority.NORMAL
                        MessagePriority.IMPORTANT -> DomainPriority.HIGH
                        MessagePriority.ALERT -> DomainPriority.URGENT
                    },
                    timestampMs = message.timestamp,
                    remoteDeviceId = message.senderId
                )
                messageRepository?.addMessage(itantraMessage)
            }
            MessageType.PING -> {
                // Respond with PONG
                sendPong(message)
            }
            MessageType.PONG, MessageType.SYSTEM -> {
                // System / keepalive packet
            }
        }
    }

    private suspend fun sendAck(original: TextMessage) {
        val ackMessage = TextMessage(
            messageId = MessageIdGenerator.generateAckId(original.messageId),
            senderId = localDeviceId,
            receiverId = original.senderId,
            text = "ACK:${original.messageId}",
            languageCode = original.languageCode,
            messageType = MessageType.ACK,
            priority = MessagePriority.NORMAL
        ).withComputedChecksum()

        val serialized = MessageSerializer.serialize(ackMessage).getOrNull() ?: return
        val res = transport.send(serialized)
        if (res.isSuccess) {
            _diagnostics.update { it.copy(packetsSent = it.packetsSent + 1, acksSent = it.acksSent + 1) }
        }
    }

    private suspend fun sendPong(original: TextMessage) {
        val pongMessage = TextMessage(
            messageId = MessageIdGenerator.generateMessageId(),
            senderId = localDeviceId,
            receiverId = original.senderId,
            text = "PONG",
            languageCode = original.languageCode,
            messageType = MessageType.PONG,
            priority = MessagePriority.NORMAL
        ).withComputedChecksum()

        val serialized = MessageSerializer.serialize(pongMessage).getOrNull() ?: return
        val res = transport.send(serialized)
        if (res.isSuccess) {
            _diagnostics.update { it.copy(packetsSent = it.packetsSent + 1) }
        }
    }

    companion object {
        const val MAX_DEDUPLICATION_CACHE_ENTRIES = 500
        const val MAX_PENDING_MESSAGES_CAP = 100
    }

    private fun updateDeliveryStatus(messageId: String, status: DeliveryStatus) {
        _deliveryStatuses.update { it + (messageId to status) }
    }

    private fun pruneDeduplicationCache() {
        val cutoff = System.currentTimeMillis() - MessageProtocol.DEDUPLICATION_CACHE_TTL_MS
        recentReceivedMessageIds.entries.removeIf { it.value < cutoff }

        // Hard capacity bound: if still exceeding max entries, evict oldest
        if (recentReceivedMessageIds.size > MAX_DEDUPLICATION_CACHE_ENTRIES) {
            val toRemove = recentReceivedMessageIds.entries
                .sortedBy { it.value }
                .take(recentReceivedMessageIds.size - MAX_DEDUPLICATION_CACHE_ENTRIES)
            for (entry in toRemove) {
                recentReceivedMessageIds.remove(entry.key)
            }
        }
    }

    fun resetDiagnostics() {
        _diagnostics.value = CommunicationDiagnostics(
            activeTransportType = transport.transportType.name,
            remoteEndpoint = when (val state = transport.connectionState.value) {
                is ConnectionState.Connected -> "${state.deviceName} (${state.deviceId})"
                is ConnectionState.Connecting -> "Connecting to ${state.deviceName}..."
                is ConnectionState.Scanning -> "Scanning..."
                is ConnectionState.Disconnected -> "Disconnected"
                is ConnectionState.Error -> "Error: ${state.reason}"
            }
        )
    }

    fun clearSession() {
        pendingMessages.clear()
        _deliveryStatuses.value = emptyMap()
        _communicationState.value = CommunicationState.Disconnected
    }

    fun isMessageDelivered(messageId: String): Boolean {
        return _deliveryStatuses.value[messageId] == DeliveryStatus.DELIVERED
    }

    fun isMessageFailed(messageId: String): Boolean {
        return _deliveryStatuses.value[messageId] == DeliveryStatus.FAILED
    }

    private data class PendingMessage(
        val message: TextMessage,
        val attemptCount: Int
    )
}
