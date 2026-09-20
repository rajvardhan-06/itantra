package com.itantra.app.demo

import com.itantra.app.communication.CommunicationManager
import com.itantra.app.communication.MessageIdGenerator
import com.itantra.app.communication.MessagePriority
import com.itantra.app.communication.MessageSerializer
import com.itantra.app.communication.MessageType
import com.itantra.app.communication.MockCommunicationTransport
import com.itantra.app.communication.TextMessage
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.TransportType
import com.itantra.app.domain.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages live demonstrations for SIH (Smart India Hackathon) and offline judges.
 *
 * Capabilities:
 * 1. Single-device loopback demo: Emulates realistic incoming multilingual messages
 *    (Hindi, Bengali, English, etc.) from a remote responder.
 * 2. Fault Injection: Demonstrates real-time packet loss, CRC corruption, and retry recovery.
 * 3. Session Reset: Reinitializes message logs and simulated transports for repeated judge walkthroughs.
 */
class DemoSimulationManager(
    private val communicationManager: CommunicationManager,
    private val mockTransport: MockCommunicationTransport,
    private val messageRepository: MessageRepository
) {

    data class DemoPreset(
        val languageCode: String,
        val languageName: String,
        val text: String,
        val senderLabel: String
    )

    companion object {
        val PRESET_MESSAGES = listOf(
            DemoPreset(
                languageCode = "hi",
                languageName = "Hindi",
                text = "बाढ़ राहत केंद्र में अतिरिक्त भोजन और पानी की आवश्यकता है। कृपया तुरंत भेजें।",
                senderLabel = "Relief Camp 04 (NDRF)"
            ),
            DemoPreset(
                languageCode = "bn",
                languageName = "Bengali",
                text = "মেডিকেল দল ইতিমধ্যেই পৌঁছেছে, আরও দুটি অ্যাম্বুলেন্স প্রয়োজন।",
                senderLabel = "Medical Team Kolkata"
            ),
            DemoPreset(
                languageCode = "en",
                languageName = "English",
                text = "Perimeter security secured. Awaiting communication relay from sector 3.",
                senderLabel = "Field HQ Charlie"
            ),
            DemoPreset(
                languageCode = "ta",
                languageName = "Tamil",
                text = "மீட்புப் பணிகள் தீவிரமாக நடைபெற்று வருகின்றன, உதவி விரைவில் வந்து சேரும்.",
                senderLabel = "Search & Rescue Unit 2"
            )
        )
    }

    private val _lastSimulatedAction = MutableStateFlow<String?>("Demo System Ready")
    val lastSimulatedAction: StateFlow<String?> = _lastSimulatedAction.asStateFlow()

    private val _isFaultInjected = MutableStateFlow(false)
    val isFaultInjected: StateFlow<Boolean> = _isFaultInjected.asStateFlow()

    /**
     * Connects the mock transport to a simulated field peer if not already connected.
     */
    suspend fun ensureMockPeerConnected(peerName: String = "iTantra-Field-Responder-01") {
        mockTransport.simulateConnectionState(
            ConnectionState.Connected(
                deviceName = peerName,
                deviceId = "sim-peer-99",
                transport = TransportType.WIFI_DIRECT,
                signalStrength = 95
            )
        )
        _lastSimulatedAction.value = "Connected to simulated peer: $peerName"
    }

    /**
     * Injects an incoming simulated peer message in the specified language.
     * This will flow through the exact same [CommunicationTransport] raw byte stream,
     * undergo framing parsing, CRC32 verification, deserialization, delivery ACK emission,
     * repository storage, and UI flow updating.
     */
    suspend fun simulateIncomingPeerMessage(presetIndex: Int = 0): Result<Unit> {
        ensureMockPeerConnected()
        val preset = PRESET_MESSAGES.getOrElse(presetIndex) { PRESET_MESSAGES.first() }

        val simulatedMessage = TextMessage(
            messageId = MessageIdGenerator.generateMessageId(),
            senderId = preset.senderLabel,
            receiverId = "local-device",
            text = preset.text,
            languageCode = preset.languageCode,
            messageType = MessageType.CHAT,
            priority = MessagePriority.ALERT
        ).withComputedChecksum()

        val serialized = MessageSerializer.serialize(simulatedMessage).getOrElse { error ->
            return Result.failure(error)
        }

        mockTransport.emitIncomingPayload(serialized)
        _lastSimulatedAction.value = "Injected incoming message (${preset.languageName})"
        return Result.success(Unit)
    }

    /**
     * Injects a corrupted packet (corrupted CRC32) to demonstrate robust rejection
     * and defense against transmission corruption.
     */
    suspend fun injectCorruptedPacket(): Result<Unit> {
        val validMessage = TextMessage(
            messageId = MessageIdGenerator.generateMessageId(),
            senderId = "sim-corruptor",
            receiverId = "local-device",
            text = "Corrupted transmission test payload",
            languageCode = "en",
            messageType = MessageType.CHAT,
            priority = MessagePriority.NORMAL
        ).withComputedChecksum()

        val bytes = MessageSerializer.serialize(validMessage).getOrElse { return Result.failure(it) }
        // Corrupt checksum in the serialized array
        if (bytes.size > 20) {
            bytes[15] = (bytes[15].toInt() xor 0xFF).toByte()
        }

        mockTransport.emitIncomingPayload(bytes)
        _lastSimulatedAction.value = "Injected corrupted CRC32 packet (expecting validator rejection)"
        return Result.success(Unit)
    }

    /**
     * Toggles simulated transport failure to show automatic timeout & failure handling.
     */
    fun toggleSimulatedTransportFailure(fail: Boolean) {
        mockTransport.setFailSend(fail)
        _isFaultInjected.value = fail
        _lastSimulatedAction.value = if (fail) {
            "Fault Injected: Simulated transmission failures active"
        } else {
            "Fault Cleared: Transmissions restored"
        }
    }

    /**
     * Resets the entire demo session for a fresh walkthrough.
     */
    suspend fun resetDemoSession() {
        communicationManager.clearSession()
        messageRepository.clearAll()
        mockTransport.setFailSend(false)
        mockTransport.setFailConnection(false)
        _isFaultInjected.value = false
        ensureMockPeerConnected()
        _lastSimulatedAction.value = "Demo session reset to clean baseline."
    }
}
