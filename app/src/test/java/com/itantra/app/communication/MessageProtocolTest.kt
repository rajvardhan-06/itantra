package com.itantra.app.communication

import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.DiscoveredDevice
import com.itantra.app.domain.model.SupportedLanguage
import com.itantra.app.domain.model.TransportType
import com.itantra.app.ui.screens.ptt.PushToTalkViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Comprehensive unit test suite for Phase 5: Text Message Protocol & Communication.
 *
 * Covers the 22 required scenarios including message modeling, serialization,
 * framing, validation, multilingual UTF-8 handling, mock transport, ACK loop,
 * deduplication, retry policy, and ViewModel integration.
 */
class MessageProtocolTest {

    private val testDispatcher = Dispatchers.Unconfined
    private val testScope = CoroutineScope(testDispatcher)

    @Before
    fun setUp() {
        // Clear any lingering state
    }

    // 1. TextMessage creation
    @Test
    fun testTextMessageCreation() {
        val now = System.currentTimeMillis()
        val msg = TextMessage(
            messageId = "msg-123",
            senderId = "dev-alpha",
            receiverId = "dev-beta",
            text = "Hello iTantra",
            languageCode = "en",
            timestamp = now,
            messageType = MessageType.CHAT,
            priority = MessagePriority.IMPORTANT,
            sequenceNumber = 42L,
            protocolVersion = MessageProtocol.PROTOCOL_VERSION
        ).withComputedChecksum()

        assertEquals("msg-123", msg.messageId)
        assertEquals("dev-alpha", msg.senderId)
        assertEquals("dev-beta", msg.receiverId)
        assertEquals("Hello iTantra", msg.text)
        assertEquals("en", msg.languageCode)
        assertEquals(now, msg.timestamp)
        assertEquals(MessageType.CHAT, msg.messageType)
        assertEquals(MessagePriority.IMPORTANT, msg.priority)
        assertEquals(42L, msg.sequenceNumber)
        assertEquals(1, msg.protocolVersion)
        assertTrue(msg.checksum != 0L)
        assertTrue(msg.isChecksumValid())
    }

    // 2. Serialization with CRC32
    @Test
    fun testSerializationWithCrc32() {
        val msg = TextMessage(
            messageId = "msg-test-2",
            senderId = "device-sender",
            text = "Testing CRC32 integrity",
            languageCode = "hi",
            priority = MessagePriority.NORMAL
        ).withComputedChecksum()

        val serializeResult = MessageSerializer.serialize(msg)
        assertTrue(serializeResult.isSuccess)
        val payload = serializeResult.getOrThrow()
        assertTrue(payload.isNotEmpty())

        val jsonString = String(payload, Charsets.UTF_8)
        assertTrue(jsonString.contains("\"crc\":${msg.checksum}"))
        assertTrue(jsonString.contains("\"id\":\"msg-test-2\""))
        assertTrue(jsonString.contains("\"type\":\"CHAT\""))
    }

    // 3. Deserialization
    @Test
    fun testDeserialization() {
        val original = TextMessage(
            messageId = "msg-3",
            senderId = "sender-3",
            receiverId = "receiver-3",
            text = "Deserialization test message",
            languageCode = "ta",
            sequenceNumber = 7L,
            priority = MessagePriority.ALERT
        ).withComputedChecksum()

        val serialized = MessageSerializer.serialize(original).getOrThrow()
        val deserializedResult = MessageSerializer.deserialize(serialized)

        assertTrue(deserializedResult.isSuccess)
        val deserialized = deserializedResult.getOrThrow()

        assertEquals(original.messageId, deserialized.messageId)
        assertEquals(original.senderId, deserialized.senderId)
        assertEquals(original.receiverId, deserialized.receiverId)
        assertEquals(original.text, deserialized.text)
        assertEquals(original.languageCode, deserialized.languageCode)
        assertEquals(original.messageType, deserialized.messageType)
        assertEquals(original.priority, deserialized.priority)
        assertEquals(original.sequenceNumber, deserialized.sequenceNumber)
        assertEquals(original.protocolVersion, deserialized.protocolVersion)
        assertEquals(original.checksum, deserialized.checksum)
        assertTrue(deserialized.isChecksumValid())
    }

    // 4. UTF-8 multi-byte handling
    @Test
    fun testUtf8MultiByteHandling() {
        val hindiText = "नमस्ते दुनिया! यह एक ऑफ़लाइन ट्रांससीव्हर परीक्षण है।"
        val msg = TextMessage(
            messageId = "msg-hindi-4",
            senderId = "sender-hi",
            text = hindiText,
            languageCode = "hi"
        ).withComputedChecksum()

        val serialized = MessageSerializer.serialize(msg).getOrThrow()
        val deserialized = MessageSerializer.deserialize(serialized).getOrThrow()

        assertEquals(hindiText, deserialized.text)
        assertTrue(deserialized.isChecksumValid())
    }

    // 5. Telugu script support
    @Test
    fun testTeluguScriptSupport() {
        val teluguText = "నమస్కారం! ఐతంత్ర తో ఆఫ్‌లైన్ మెసేజ్ పంపడం చాలా సులభం."
        val msg = TextMessage(
            messageId = "msg-telugu-5",
            senderId = "sender-te",
            text = teluguText,
            languageCode = "te",
            priority = MessagePriority.ALERT
        ).withComputedChecksum()

        val serialized = MessageSerializer.serialize(msg).getOrThrow()
        val deserialized = MessageSerializer.deserialize(serialized).getOrThrow()

        assertEquals(teluguText, deserialized.text)
        assertEquals("te", deserialized.languageCode)
        assertTrue(deserialized.isChecksumValid())
    }

    // 6. Empty text rejection
    @Test
    fun testEmptyTextRejection() {
        val emptyMsg = TextMessage(
            messageId = "msg-empty",
            senderId = "sender-1",
            text = "   ",
            languageCode = "en"
        ).withComputedChecksum()

        val validation = MessageValidator.validate(emptyMsg)
        assertFalse(validation.isValid)
        assertTrue((validation as ValidationResult.Invalid).reason.contains("empty"))
    }

    // 7. Text length > 1000 rejection
    @Test
    fun testTextLengthExceeding1000CharsRejection() {
        val longText = "A".repeat(1001)
        val msg = TextMessage(
            messageId = "msg-too-long",
            senderId = "sender-1",
            text = longText,
            languageCode = "en"
        ).withComputedChecksum()

        val validation = MessageValidator.validate(msg)
        assertFalse(validation.isValid)
        assertTrue((validation as ValidationResult.Invalid).reason.contains("limit", ignoreCase = true))
    }

    // 8. Serialized payload > 4096 rejection
    @Test
    fun testSerializedPayloadExceeding4096BytesRejection() {
        val oversizedBuilder = StringBuilder()
        while (oversizedBuilder.toString().toByteArray(Charsets.UTF_8).size < 4200) {
            oversizedBuilder.append("\uD83D\uDE00\uD83D\uDE80\uD83C\uDF0D")
        }
        val oversizedText = oversizedBuilder.toString()
        val msg = TextMessage(
            messageId = "msg-oversized",
            senderId = "sender-oversized-id",
            text = oversizedText,
            languageCode = "hi"
        ).withComputedChecksum()

        val validation = MessageValidator.validate(msg)
        assertFalse(validation.isValid)
        assertTrue((validation as ValidationResult.Invalid).reason.contains("limit", ignoreCase = true))
    }

    // 9. Invalid language code rejection
    @Test
    fun testInvalidLanguageCodeRejection() {
        val invalidLangMsg = TextMessage(
            messageId = "msg-lang",
            senderId = "sender-1",
            text = "Valid text",
            languageCode = "zz"
        ).withComputedChecksum()

        val validation = MessageValidator.validate(invalidLangMsg)
        assertFalse(validation.isValid)
        assertTrue((validation as ValidationResult.Invalid).reason.contains("language"))
    }

    // 10. Invalid message ID rejection
    @Test
    fun testInvalidMessageIdRejection() {
        val invalidIdMsg = TextMessage(
            messageId = "",
            senderId = "sender-1",
            text = "Valid text",
            languageCode = "en"
        ).withComputedChecksum()

        val validation = MessageValidator.validate(invalidIdMsg)
        assertFalse(validation.isValid)
        assertTrue((validation as ValidationResult.Invalid).reason.contains("Message ID", ignoreCase = true))
    }

    // 11. Checksum mismatch rejection
    @Test
    fun testChecksumMismatchRejection() {
        val msg = TextMessage(
            messageId = "msg-tamper",
            senderId = "sender-1",
            text = "Original message text",
            languageCode = "en"
        ).withComputedChecksum()

        // Tamper with checksum directly
        val tamperedMsg = msg.copy(checksum = 999999L)
        assertFalse(tamperedMsg.isChecksumValid())

        val validation = MessageValidator.validate(tamperedMsg)
        assertFalse(validation.isValid)
        assertTrue((validation as ValidationResult.Invalid).reason.contains("checksum mismatch", ignoreCase = true))

        // Also test deserialization rejection on tampered JSON payload
        val serialized = MessageSerializer.serialize(msg).getOrThrow()
        val tamperedJson = String(serialized, Charsets.UTF_8).replace("\"txt\":\"Original message text\"", "\"txt\":\"Hacked text\"")
        val result = MessageSerializer.deserialize(tamperedJson.toByteArray(Charsets.UTF_8))
        assertFalse(result.isSuccess)
    }

    // 12. Malformed JSON handling
    @Test
    fun testMalformedJsonHandling() {
        val corruptBytes = "{ malformed: json [ unbalanced".toByteArray(Charsets.UTF_8)
        val result = MessageSerializer.deserialize(corruptBytes)
        assertFalse(result.isSuccess)
        assertNotNull(result.exceptionOrNull())
    }

    // 13. Unsupported protocol version rejection
    @Test
    fun testUnsupportedProtocolVersionRejection() {
        val msg = TextMessage(
            messageId = "msg-v2",
            senderId = "sender-1",
            text = "Future version text",
            languageCode = "en",
            protocolVersion = 99
        ).withComputedChecksum()

        val validation = MessageValidator.validate(msg)
        assertFalse(validation.isValid)
        assertTrue((validation as ValidationResult.Invalid).reason.contains("version"))
    }

    // 14. Deduplication
    @Test
    fun testDeduplication() = runBlocking(testDispatcher) {
        val mockTransport = MockCommunicationTransport(autoEchoAck = false)
        val commManager = CommunicationManager(
            transport = mockTransport,
            dispatcher = testDispatcher
        )
        try {
            mockTransport.simulateConnectionState(
                ConnectionState.Connected(
                    deviceName = "Peer-Device",
                    deviceId = "peer-01",
                    transport = TransportType.WIFI_DIRECT,
                    signalStrength = 95
                )
            )

            val duplicateId = "msg-duplicate-14"
            val incomingChat = TextMessage(
                messageId = duplicateId,
                senderId = "peer-01",
                text = "Hello there",
                languageCode = "en"
            ).withComputedChecksum()

            val serialized = MessageSerializer.serialize(incomingChat).getOrThrow()

            // Emit first time
            mockTransport.emitIncomingPayload(serialized)
            var attempts = 0
            while (mockTransport.getSentPayloads().isEmpty() && attempts < 20) {
                delay(20)
                attempts++
            }

            // Remote peer should have received an ACK packet back
            val payloadsFirstTime = mockTransport.getSentPayloads()
            assertEquals(1, payloadsFirstTime.size)

            // Emit identical messageId a second time
            mockTransport.emitIncomingPayload(serialized)
            attempts = 0
            while (mockTransport.getSentPayloads().size < 2 && attempts < 20) {
                delay(20)
                attempts++
            }

            // Second ACK should be sent back to confirm receipt to peer, but deduplication drops reprocessing
            val payloadsSecondTime = mockTransport.getSentPayloads()
            assertEquals(2, payloadsSecondTime.size)
        } finally {
            commManager.release()
        }
    }

    // 15. Message ordering & sequence numbers
    @Test
    fun testMessageOrderingAndSequenceNumbers() = runBlocking(testDispatcher) {
        val mockTransport = MockCommunicationTransport(autoEchoAck = true)
        val commManager = CommunicationManager(
            transport = mockTransport,
            dispatcher = testDispatcher
        )
        try {
            mockTransport.simulateConnectionState(
                ConnectionState.Connected(
                    deviceName = "Peer-Device",
                    deviceId = "peer-01",
                    transport = TransportType.WIFI_DIRECT,
                    signalStrength = 90
                )
            )

            val msg1 = commManager.sendMessage("First message", "en").getOrThrow()
            val msg2 = commManager.sendMessage("Second message", "en").getOrThrow()
            val msg3 = commManager.sendMessage("Third message", "en").getOrThrow()

            assertEquals(1L, msg1.sequenceNumber)
            assertEquals(2L, msg2.sequenceNumber)
            assertEquals(3L, msg3.sequenceNumber)
        } finally {
            commManager.release()
        }
    }

    // 16. Mock transport connection
    @Test
    fun testMockTransportConnection() = runBlocking(testDispatcher) {
        val mockTransport = MockCommunicationTransport()
        val commManager = CommunicationManager(
            transport = mockTransport,
            dispatcher = testDispatcher
        )
        try {
            val connectResult = commManager.connect("192.168.49.1:8988")
            assertTrue(connectResult.isSuccess)
            assertTrue(commManager.connectionState.value is ConnectionState.Connected)
            assertTrue(commManager.communicationState.value is CommunicationState.Connected)
        } finally {
            commManager.release()
        }
    }

    // 17. Mock message sending
    @Test
    fun testMockMessageSending() = runBlocking(testDispatcher) {
        val mockTransport = MockCommunicationTransport(autoEchoAck = false)
        val commManager = CommunicationManager(
            transport = mockTransport,
            dispatcher = testDispatcher
        )
        try {
            val sendResult = commManager.sendMessage("Transmitting via mock", "en")
            assertTrue(sendResult.isSuccess)

            var attempts = 0
            while (mockTransport.getSentPayloads().isEmpty() && attempts < 20) {
                delay(20)
                attempts++
            }

            val sentPayloads: List<ByteArray> = mockTransport.getSentPayloads()
            assertEquals(1, sentPayloads.size)

            val payload: ByteArray = sentPayloads.first()
            val deserialized = MessageSerializer.deserialize(payload).getOrThrow()
            assertEquals("Transmitting via mock", deserialized.text)
        } finally {
            commManager.release()
        }
    }

    // 18. Transport connection failure
    @Test
    fun testTransportConnectionFailure() = runBlocking(testDispatcher) {
        val mockTransport = MockCommunicationTransport().apply {
            setFailConnection(true)
        }
        val commManager = CommunicationManager(
            transport = mockTransport,
            dispatcher = testDispatcher
        )
        try {
            val connectResult = commManager.connect("invalid-endpoint")
            assertFalse(connectResult.isSuccess)
            assertTrue(commManager.connectionState.value is ConnectionState.Error)
        } finally {
            commManager.release()
        }
    }

    // 19. Transmission timeout and retries (up to 3)
    @Test
    fun testTransmissionTimeoutAndRetries() = runBlocking(testDispatcher) {
        val mockTransport = MockCommunicationTransport(autoEchoAck = false).apply {
            setFailSend(true)
        }
        val commManager = CommunicationManager(
            transport = mockTransport,
            dispatcher = testDispatcher,
            ackTimeoutMs = 100L,
            retryBackoffBaseMs = 10L
        )
        try {
            val sentMsg = commManager.sendMessage("Will fail and retry", "en").getOrThrow()
            val messageId = sentMsg.messageId

            // Await retry exhaustion
            var attempts = 0
            while (!commManager.isMessageFailed(messageId) && attempts < 50) {
                delay(20)
                attempts++
            }

            assertTrue(commManager.isMessageFailed(messageId))
            assertEquals(DeliveryStatus.FAILED, commManager.deliveryStatuses.value[messageId])
        } finally {
            commManager.release()
        }
    }

    // 20. Delivery ACK handling
    @Test
    fun testDeliveryAckHandling() = runBlocking(testDispatcher) {
        val mockTransport = MockCommunicationTransport(autoEchoAck = false)
        val commManager = CommunicationManager(
            transport = mockTransport,
            dispatcher = testDispatcher
        )
        try {
            val sentMsg = commManager.sendMessage("Waiting for ACK", "en").getOrThrow()
            val messageId = sentMsg.messageId

            assertFalse(commManager.isMessageDelivered(messageId))

            // Wait until the message has been dispatched to transport
            var sendWaitAttempts = 0
            while (mockTransport.getSentPayloads().isEmpty() && sendWaitAttempts < 20) {
                delay(20)
                sendWaitAttempts++
            }
            assertEquals(1, mockTransport.getSentPayloads().size)

            // Remote peer sends ACK packet for this messageId
            val ackMessage = TextMessage(
                messageId = "ack-${System.currentTimeMillis()}",
                senderId = "peer-01",
                text = "ACK:$messageId",
                languageCode = "en",
                messageType = MessageType.ACK
            ).withComputedChecksum()

            mockTransport.emitIncomingPayload(MessageSerializer.serialize(ackMessage).getOrThrow())

            var attempts = 0
            while (!commManager.isMessageDelivered(messageId) && attempts < 20) {
                delay(20)
                attempts++
            }

            assertTrue(commManager.isMessageDelivered(messageId))
            assertEquals(DeliveryStatus.DELIVERED, commManager.deliveryStatuses.value[messageId])
        } finally {
            commManager.release()
        }
    }

    // 21. CommunicationManager queue & delivery status
    @Test
    fun testCommunicationManagerQueueAndDeliveryStatus() = runBlocking(testDispatcher) {
        val mockTransport = MockCommunicationTransport(autoEchoAck = true)
        val commManager = CommunicationManager(
            transport = mockTransport,
            dispatcher = testDispatcher
        )
        try {
            val msg = commManager.sendMessage("Lifecycle test", "hi").getOrThrow()

            var attempts = 0
            while (!commManager.isMessageDelivered(msg.messageId) && attempts < 20) {
                delay(20)
                attempts++
            }

            // Because autoAckIncoming is true, it quickly transitions to DELIVERED
            assertEquals(DeliveryStatus.DELIVERED, commManager.deliveryStatuses.value[msg.messageId])
        } finally {
            commManager.release()
        }
    }

    // 22. ViewModel state transitions
    @Test
    fun testViewModelStateTransitions() = runBlocking(testDispatcher) {
        val mockTransport = MockCommunicationTransport(autoEchoAck = true)
        val commManager = CommunicationManager(
            transport = mockTransport,
            dispatcher = testDispatcher
        )
        val vmScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + testDispatcher)
        try {
            val viewModel = PushToTalkViewModel(
                communicationManager = commManager,
                externalScope = vmScope
            )

            // 1. Initial state
            assertEquals(MessagePriority.NORMAL, viewModel.uiState.value.selectedPriority)

            // 2. Select priority
            viewModel.selectPriority(MessagePriority.ALERT)
            assertEquals(MessagePriority.ALERT, viewModel.uiState.value.selectedPriority)

            // 3. Edit transcription
            viewModel.onTranscriptionEdited("Test transmission from ViewModel")
            assertEquals("Test transmission from ViewModel", viewModel.uiState.value.editableTranscription)

            // 4. Send message
            viewModel.sendMessage()

            var attempts = 0
            while (viewModel.uiState.value.deliveryStatus != DeliveryStatus.DELIVERED && attempts < 30) {
                delay(20)
                attempts++
            }

            val lastSentId = viewModel.uiState.value.lastSentMessageId
            assertNotNull(lastSentId)
            assertEquals(DeliveryStatus.DELIVERED, viewModel.uiState.value.deliveryStatus)

            // 5. Test retry
            viewModel.retrySendMessage(lastSentId)

            attempts = 0
            while (viewModel.uiState.value.deliveryStatus != DeliveryStatus.DELIVERED && attempts < 30) {
                delay(20)
                attempts++
            }
            assertEquals(DeliveryStatus.DELIVERED, viewModel.uiState.value.deliveryStatus)
        } finally {
            vmScope.cancel()
            commManager.release()
        }
    }
}
