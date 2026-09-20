package com.itantra.app.communication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer

/**
 * Security hardening and payload defense tests for [MessageValidator] and [MessageSerializer].
 *
 * Verifies that malformed, corrupted, oversized, or malicious packet structures
 * are strictly detected and rejected before reaching application state or repositories.
 */
class SecurityAndPayloadValidationTest {

    private fun createValidMessage(text: String = "Normal communications check."): TextMessage {
        return TextMessage(
            messageId = "msg-1700000000000-1-abcd1234",
            senderId = "itantra-device-alpha",
            receiverId = "itantra-device-beta",
            text = text,
            languageCode = "hi",
            messageType = MessageType.CHAT,
            priority = MessagePriority.NORMAL
        ).withComputedChecksum()
    }

    @Test
    fun `valid message passes validation completely`() {
        val msg = createValidMessage()
        val result = MessageValidator.validate(msg)
        assertTrue("Valid message must pass validation", result is ValidationResult.Valid)
    }

    @Test
    fun `reject message containing null byte injection`() {
        val poisoned = createValidMessage("Malicious payload with null byte \u0000 injection.").withComputedChecksum()
        val result = MessageValidator.validate(poisoned)
        assertTrue("Must be Invalid", result is ValidationResult.Invalid)
        val reason = (result as ValidationResult.Invalid).reason
        assertTrue(reason.contains("null bytes", ignoreCase = true) || reason.contains("illegal", ignoreCase = true))
    }

    @Test
    fun `reject message containing illegal control characters`() {
        val controlCharMsg = createValidMessage("Text with escape byte: \u001B[31mRed").withComputedChecksum()
        val result = MessageValidator.validate(controlCharMsg)
        assertTrue("Must be Invalid", result is ValidationResult.Invalid)
        val reason = (result as ValidationResult.Invalid).reason
        assertTrue(reason.contains("control character", ignoreCase = true) || reason.contains("illegal", ignoreCase = true))
    }

    @Test
    fun `reject message text exceeding maximum 1000 characters`() {
        val oversizedText = "a".repeat(1001)
        val msg = createValidMessage(oversizedText).withComputedChecksum()
        val result = MessageValidator.validate(msg)
        assertTrue("Must be Invalid", result is ValidationResult.Invalid)
        val reason = (result as ValidationResult.Invalid).reason
        assertTrue(reason.contains("maximum") || reason.contains("1000"))
    }

    @Test
    fun `reject message with empty or blank text`() {
        val emptyMsg = createValidMessage("   ").withComputedChecksum()
        val result = MessageValidator.validate(emptyMsg)
        assertTrue("Must be Invalid", result is ValidationResult.Invalid)
        val reason = (result as ValidationResult.Invalid).reason
        assertTrue(reason.contains("blank") || reason.contains("empty"))
    }

    @Test
    fun `reject message with invalid protocol version`() {
        val badVersion = createValidMessage().copy(protocolVersion = 99).withComputedChecksum()
        val result = MessageValidator.validate(badVersion)
        assertTrue("Must be Invalid", result is ValidationResult.Invalid)
        val reason = (result as ValidationResult.Invalid).reason
        assertTrue(reason.contains("protocol version") || reason.contains("Unsupported"))
    }

    @Test
    fun `reject message with corrupted CRC32 checksum`() {
        val valid = createValidMessage()
        val corruptedChecksum = valid.copy(checksum = valid.checksum xor 0xDEADBEEF)
        val result = MessageValidator.validate(corruptedChecksum)
        assertTrue("Must be Invalid", result is ValidationResult.Invalid)
        val reason = (result as ValidationResult.Invalid).reason
        assertTrue(reason.contains("checksum") || reason.contains("mismatch"))
    }

    @Test
    fun `reject message with invalid language code`() {
        val badLang = createValidMessage().copy(languageCode = "xyz").withComputedChecksum()
        val result = MessageValidator.validate(badLang)
        assertTrue("Must be Invalid", result is ValidationResult.Invalid)
        val reason = (result as ValidationResult.Invalid).reason
        assertTrue(reason.contains("language code") || reason.contains("Unsupported"))
    }

    @Test
    fun `reject senderId or receiverId exceeding 64 characters`() {
        val hugeSender = createValidMessage().copy(senderId = "id-".repeat(30)).withComputedChecksum()
        val result = MessageValidator.validate(hugeSender)
        assertTrue("Must be Invalid", result is ValidationResult.Invalid)
        val reason = (result as ValidationResult.Invalid).reason
        assertTrue(reason.contains("Sender ID") || reason.contains("64 characters"))
    }

    @Test
    fun `framed packet deserialization rejects frame claiming length exceeding 4096 bytes`() {
        val header = ByteBuffer.allocate(4).putInt(100_000).array()
        val stream = ByteArrayInputStream(header)

        try {
            MessageSerializer.readFramedPacket(stream)
            fail("Expected IllegalArgumentException for oversized framed packet")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Invalid framed packet length") == true)
        }
    }

    @Test
    fun `framed packet deserialization rejects frame claiming length below 16 bytes`() {
        val header = ByteBuffer.allocate(4).putInt(5).array()
        val stream = ByteArrayInputStream(header)

        try {
            MessageSerializer.readFramedPacket(stream)
            fail("Expected IllegalArgumentException for undersized framed packet")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Invalid framed packet length") == true)
        }
    }

    @Test
    fun `framed packet round trip preserves message integrity`() {
        val msg = createValidMessage("सुरक्षित संदेश परीक्षण")
        val rawBytes = MessageSerializer.serialize(msg).getOrThrow()
        val framedBytes = MessageSerializer.framePacket(rawBytes)

        val stream = ByteArrayInputStream(framedBytes)
        val readPayload = MessageSerializer.readFramedPacket(stream)
        assertNotNull(readPayload)

        val deserialized = MessageSerializer.deserialize(readPayload!!).getOrThrow()
        assertEquals(msg.messageId, deserialized.messageId)
        assertEquals(msg.text, deserialized.text)
        assertEquals(msg.checksum, deserialized.checksum)
        assertTrue(deserialized.isChecksumValid())
    }
}
