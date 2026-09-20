package com.itantra.app.communication

import java.util.zip.CRC32

/**
 * Message priority classification for transmission scheduling and alert hierarchy.
 */
enum class MessagePriority {
    NORMAL,
    IMPORTANT,
    ALERT
}

/**
 * Functional message type for packet routing and protocol handshaking.
 */
enum class MessageType {
    CHAT,
    ACK,
    PING,
    PONG,
    SYSTEM
}

/**
 * Core structured text communication unit for iTantra peer-to-peer transmission.
 *
 * Designed for low-bandwidth offline links with length ceilings and CRC32 integrity verification.
 *
 * @property messageId Globally unique message identifier (prefixed `msg-<timestamp>-<randomHex>`).
 * @property senderId Unique identifier of the originating device.
 * @property receiverId Optional identifier of the destination peer, or null for local broadcast.
 * @property text Transcribed speech message content (UTF-8 encoded).
 * @property languageCode ISO 639-1 language code of the message (e.g., "hi", "en", "te").
 * @property timestamp Epoch timestamp in milliseconds.
 * @property messageType Semantic category of message ([MessageType.CHAT], [MessageType.ACK], etc.).
 * @property priority Transmission importance ([MessagePriority.NORMAL], [MessagePriority.IMPORTANT], [MessagePriority.ALERT]).
 * @property sequenceNumber Monotonically increasing sequence number from the sender session.
 * @property protocolVersion Wire protocol version integer (defaults to [MessageProtocol.PROTOCOL_VERSION]).
 * @property checksum CRC32 integrity checksum over the canonical message content.
 */
data class TextMessage(
    val messageId: String,
    val senderId: String,
    val receiverId: String? = null,
    val text: String,
    val languageCode: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: MessageType = MessageType.CHAT,
    val priority: MessagePriority = MessagePriority.NORMAL,
    val sequenceNumber: Long = 0L,
    val protocolVersion: Int = MessageProtocol.PROTOCOL_VERSION,
    val checksum: Long = 0L
) {
    /**
     * Calculates the canonical CRC32 checksum for this message content.
     */
    fun calculateChecksum(): Long {
        val crc = CRC32()
        val canonical = "$protocolVersion|$senderId|${receiverId ?: ""}|$messageId|$sequenceNumber|$timestamp|$languageCode|${messageType.name}|${priority.name}|$text"
        crc.update(canonical.toByteArray(Charsets.UTF_8))
        return crc.value
    }

    /**
     * Returns a copy of this message with the calculated CRC32 checksum populated.
     */
    fun withComputedChecksum(): TextMessage {
        return copy(checksum = calculateChecksum())
    }

    /**
     * Validates whether the stored checksum matches the computed content checksum.
     */
    fun isChecksumValid(): Boolean {
        return checksum == calculateChecksum()
    }
}
