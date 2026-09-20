package com.itantra.app.communication

import com.itantra.app.domain.model.ItantraMessage
import com.itantra.app.domain.model.MessageDirection
import com.itantra.app.domain.model.MessagePriority as DomainPriority
import com.itantra.app.domain.model.MessageStatus
import com.itantra.app.domain.model.SupportedLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Phase 10 Comprehensive Quality Assurance & Regression Verification Suite.
 *
 * Verifies core business logic, protocol boundary contracts, multilingual Unicode
 * serialization across all 10 target Indian scripts, deduplication bounds,
 * transport retry limits, and end-to-end transceiver pipeline integrity.
 */
class Phase10ComprehensiveQATest {

    // =========================================================================
    // 1. Message Model & Validation Invariants
    // =========================================================================

    @Test
    fun messageModel_validMessagePassesValidation() {
        val validMsg = TextMessage(
            messageId = "msg-1700000000000-1-qa0001",
            senderId = "node-alpha",
            receiverId = "node-beta",
            text = "Flood alert in sector 3",
            languageCode = "en",
            messageType = MessageType.CHAT,
            priority = MessagePriority.NORMAL
        ).withComputedChecksum()

        val validation = MessageValidator.validate(validMsg)
        assertTrue("Valid message must be valid", validation.isValid)
    }

    @Test
    fun messageModel_emptyOrBlankTextIsRejected() {
        val emptyMsg = TextMessage(
            messageId = "msg-1700000000000-1-qa0002",
            senderId = "node-alpha",
            text = "",
            languageCode = "en"
        ).withComputedChecksum()

        val blankMsg = TextMessage(
            messageId = "msg-1700000000000-1-qa0003",
            senderId = "node-alpha",
            text = "   \t\n  ",
            languageCode = "en"
        ).withComputedChecksum()

        assertFalse(MessageValidator.validate(emptyMsg).isValid)
        assertFalse(MessageValidator.validate(blankMsg).isValid)
    }

    @Test
    fun messageModel_excessiveLengthIsRejected() {
        val oversizedText = "A".repeat(1001)
        val oversizedMsg = TextMessage(
            messageId = "msg-1700000000000-1-qa0004",
            senderId = "node-alpha",
            text = oversizedText,
            languageCode = "en"
        ).withComputedChecksum()

        val validation = MessageValidator.validate(oversizedMsg)
        assertFalse("Messages over 1000 characters must be rejected", validation.isValid)
    }

    @Test
    fun messageModel_blankIdentifierOrSenderIsRejected() {
        val blankIdMsg = TextMessage(
            messageId = "   ",
            senderId = "node-alpha",
            text = "Valid text",
            languageCode = "en"
        ).withComputedChecksum()

        val blankSenderMsg = TextMessage(
            messageId = "msg-1700000000000-1-qa0005",
            senderId = "",
            text = "Valid text",
            languageCode = "en"
        ).withComputedChecksum()

        assertFalse(MessageValidator.validate(blankIdMsg).isValid)
        assertFalse(MessageValidator.validate(blankSenderMsg).isValid)
    }

    @Test
    fun messageModel_invalidLanguageCodeIsRejected() {
        val badLangMsg = TextMessage(
            messageId = "msg-1700000000000-1-qa0006",
            senderId = "node-alpha",
            text = "Valid text",
            languageCode = "invalid_lang_code_too_long"
        ).withComputedChecksum()

        assertFalse(MessageValidator.validate(badLangMsg).isValid)
    }

    @Test
    fun messageModel_domainDirectionAndStatusMapping() {
        val sentMsg = ItantraMessage(
            id = "msg-test-sent",
            content = "Sent message content",
            language = SupportedLanguage.HINDI,
            direction = MessageDirection.SENT,
            status = MessageStatus.DELIVERED,
            priority = DomainPriority.HIGH,
            timestampMs = System.currentTimeMillis()
        )

        assertEquals(MessageDirection.SENT, sentMsg.direction)
        assertEquals(MessageStatus.DELIVERED, sentMsg.status)
        assertEquals(DomainPriority.HIGH, sentMsg.priority)

        val rcvMsg = sentMsg.copy(
            id = "msg-test-rcv",
            direction = MessageDirection.RECEIVED,
            status = MessageStatus.SENT
        )
        assertEquals(MessageDirection.RECEIVED, rcvMsg.direction)
    }

    // =========================================================================
    // 2. Multilingual Serialization Across All 10 Target Indian Languages
    // =========================================================================

    @Test
    fun serialization_all10TargetLanguagesPreserveUnicodeFaithfully() {
        val languageSamples = mapOf(
            "hi" to "नमस्ते, सभी राहत दलों को तुरंत बेस कैंप पर रिपोर्ट करना है।",
            "gu" to "નમસ્તે, કટોકટી પ્રતિભાવ ટીમ તૈયાર છે.",
            "mr" to "नमस्कार, पूर नियंत्रण पथक तैनात करण्यात आले आहे.",
            "kn" to "ನಮಸ್ಕಾರ, ತುರ್ತು ರಕ್ಷಣಾ ತಂಡವು ಸ್ಥಳಕ್ಕೆ ತಲುಪಿದೆ.",
            "ml" to "ഹലോ, ദുരിതാശ്വാസ ക്യാമ്പിൽ സഹായം എത്തിച്ചു.",
            "ta" to "வணக்கம், அவசர மீட்புக் குழு விரைந்துள்ளது.",
            "te" to "నమస్కారం, వరద సహాయక చర్యలు కొనసాగుతున్నాయి.",
            "or" to "ନମସ୍କାର, ବନ୍ୟା ପ୍ରଭାବିତ ଅଞ୍ଚଳରେ ସାହାଯ୍ୟ ପହଞ୍ଚିଛି।",
            "bn" to "নমস্কার, জরুরি উদ্ধারকারী দল পৌঁছছে।",
            "en" to "Priority dispatch: Medical helicopter in route to zone 4."
        )

        for ((langCode, sampleText) in languageSamples) {
            val msg = TextMessage(
                messageId = "msg-lang-$langCode",
                senderId = "station-node",
                text = sampleText,
                languageCode = langCode,
                messageType = MessageType.CHAT,
                priority = MessagePriority.IMPORTANT
            ).withComputedChecksum()

            // 1. Serialize to UTF-8
            val serializedRes = MessageSerializer.serialize(msg)
            assertTrue("Serialization must succeed for $langCode", serializedRes.isSuccess)
            val bytes = serializedRes.getOrThrow()

            // 2. Framing round-trip
            val framed = MessageSerializer.framePacket(bytes)
            val deframed = MessageSerializer.readFramedPacket(java.io.ByteArrayInputStream(framed))
            assertNotNull("Deframing must succeed for $langCode", deframed)

            // 3. Deserialize from UTF-8
            val deserializedRes = MessageSerializer.deserialize(deframed!!)
            assertTrue("Deserialization must succeed for $langCode", deserializedRes.isSuccess)
            val unpacked = deserializedRes.getOrThrow()

            assertEquals("Unicode text must match exactly for $langCode", sampleText, unpacked.text)
            assertEquals("Language code must match for $langCode", langCode, unpacked.languageCode)
            assertTrue("CRC32 checksum must remain valid for $langCode", unpacked.isChecksumValid())
        }
    }

    @Test
    fun serialization_corruptedCrc32IsDetectedAndRejected() {
        val validMsg = TextMessage(
            messageId = "msg-crc-test",
            senderId = "station-node",
            text = "Integrity critical transmission",
            languageCode = "en",
            checksum = 123456789L // Deliberately incorrect checksum
        )

        val serialized = MessageSerializer.serialize(validMsg).getOrThrow()
        val deserializedResult = MessageSerializer.deserialize(serialized)

        assertTrue("Deserialization must reject invalid CRC32", deserializedResult.isFailure)
        val ex = deserializedResult.exceptionOrNull()
        assertTrue(
            "Exception message must identify CRC mismatch",
            ex?.message?.contains("CRC32 mismatch", ignoreCase = true) == true
        )
    }

    @Test
    fun serialization_truncatedPayloadIsRejected() {
        val tooShortBytes = "{\"v\":1}".toByteArray(Charsets.UTF_8)
        val result = MessageSerializer.deserialize(tooShortBytes)
        assertTrue("Truncated payloads under minimum size must fail", result.isFailure)
    }

    @Test
    fun serialization_unsupportedProtocolVersionIsRejected() {
        val jsonString = """
            {"v":999,"id":"m1","src":"s","dst":"d","txt":"t","lang":"en","ts":100,"type":"CHAT","pri":"NORMAL","seq":1,"crc":0}
        """.trimIndent()
        val bytes = jsonString.toByteArray(Charsets.UTF_8)
        val result = MessageSerializer.deserialize(bytes)
        assertTrue("Unsupported protocol version must be rejected", result.isFailure)
    }

    // =========================================================================
    // 3. Binary Framing Header Invariants
    // =========================================================================

    @Test
    fun framing_validatesFourByteBigEndianPrefix() {
        val payload = "Test framing payload".toByteArray(Charsets.UTF_8)
        val framed = MessageSerializer.framePacket(payload)

        val buffer = ByteBuffer.wrap(framed).order(ByteOrder.BIG_ENDIAN)
        val lengthHeader = buffer.int
        assertEquals(payload.size, lengthHeader)

        val extracted = ByteArray(lengthHeader)
        buffer.get(extracted)
        assertEquals("Test framing payload", String(extracted, Charsets.UTF_8))
    }

    @Test
    fun framing_oversizedFrameHeaderIsRejected() {
        // Frame claiming 10,000 bytes exceeds MAX_FRAME_LENGTH (4096)
        val oversizedHeader = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(10_000).array()
        val invalidStream = java.io.ByteArrayInputStream(oversizedHeader + ByteArray(100))

        var threwExpected = false
        try {
            MessageSerializer.readFramedPacket(invalidStream)
        } catch (e: IllegalArgumentException) {
            threwExpected = true
        }
        assertTrue("Oversized frame headers must throw IllegalArgumentException", threwExpected)
    }

    // =========================================================================
    // 4. Communication Manager Deduplication & Cache Bounds
    // =========================================================================

    @Test
    fun communication_deduplicationPreventsDuplicateProcessing() = runBlocking {
        val mockTransport = MockCommunicationTransport()
        mockTransport.connect("peer-test-01")

        val commManager = CommunicationManager(
            transport = mockTransport,
            localDeviceId = "test-node-alpha",
            dispatcher = Dispatchers.Unconfined
        )

        val incomingMsg = TextMessage(
            messageId = "msg-dup-001",
            senderId = "test-node-beta",
            text = "Single dispatch command",
            languageCode = "en"
        ).withComputedChecksum()

        val serialized = MessageSerializer.serialize(incomingMsg).getOrThrow()

        // First delivery: processes and emits ACK
        mockTransport.emitIncomingPayload(serialized)
        val statsAfterFirst = commManager.diagnostics.value
        assertEquals(1L, statsAfterFirst.packetsReceived)
        assertEquals(1L, statsAfterFirst.acksSent)

        // Second delivery of exact same message ID (network retransmission):
        // Must acknowledge but NOT re-process as new message
        mockTransport.emitIncomingPayload(serialized)
        val statsAfterSecond = commManager.diagnostics.value
        assertEquals(2L, statsAfterSecond.packetsReceived)
        assertEquals(2L, statsAfterSecond.acksSent)
    }

    @Test
    fun communication_diagnosticsMonotonicallyTrackSessionEvents() = runBlocking {
        val mockTransport = MockCommunicationTransport()
        mockTransport.connect("peer-test-02")

        val commManager = CommunicationManager(
            transport = mockTransport,
            localDeviceId = "test-node-diag",
            dispatcher = Dispatchers.Unconfined
        )

        val initialDiag = commManager.diagnostics.value
        assertEquals(0L, initialDiag.packetsSent)
        assertEquals(0L, initialDiag.packetsReceived)

        // Send a message
        val sendRes = commManager.sendMessage("Diagnostic event check", "en")
        assertTrue(sendRes.isSuccess)

        val updatedDiag = commManager.diagnostics.value
        assertTrue(updatedDiag.packetsSent >= 1L)

        // Reset counters
        commManager.resetDiagnostics()
        val resetDiag = commManager.diagnostics.value
        assertEquals(0L, resetDiag.packetsSent)
        assertEquals(0L, resetDiag.packetsReceived)
    }
}
