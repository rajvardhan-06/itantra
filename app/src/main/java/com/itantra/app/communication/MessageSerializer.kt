package com.itantra.app.communication

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.json.JSONException
import org.json.JSONObject

/**
 * Serializes and deserializes [TextMessage] instances to and from UTF-8 byte streams.
 *
 * Employs native Android [JSONObject] for lightweight zero-dependency serialization
 * and validates CRC32 integrity checksums and wire protocol versions.
 */
object MessageSerializer {

    private const val KEY_VERSION = "v"
    private const val KEY_ID = "id"
    private const val KEY_SENDER = "src"
    private const val KEY_RECEIVER = "dst"
    private const val KEY_TEXT = "txt"
    private const val KEY_LANG = "lang"
    private const val KEY_TIME = "ts"
    private const val KEY_TYPE = "type"
    private const val KEY_PRIORITY = "pri"
    private const val KEY_SEQ = "seq"
    private const val KEY_CHECKSUM = "crc"

    /**
     * Serializes a [TextMessage] into UTF-8 JSON bytes.
     * Computes the CRC32 checksum if not already populated.
     *
     * @throws IllegalArgumentException if the serialized payload exceeds [MessageProtocol.MAX_SERIALIZED_PAYLOAD_BYTES].
     */
    fun serialize(message: TextMessage): Result<ByteArray> {
        return try {
            val messageWithCrc = if (message.checksum == 0L) {
                message.withComputedChecksum()
            } else {
                message
            }

            val json = JSONObject().apply {
                put(KEY_VERSION, messageWithCrc.protocolVersion)
                put(KEY_ID, messageWithCrc.messageId)
                put(KEY_SENDER, messageWithCrc.senderId)
                if (messageWithCrc.receiverId != null) {
                    put(KEY_RECEIVER, messageWithCrc.receiverId)
                }
                put(KEY_TEXT, messageWithCrc.text)
                put(KEY_LANG, messageWithCrc.languageCode)
                put(KEY_TIME, messageWithCrc.timestamp)
                put(KEY_TYPE, messageWithCrc.messageType.name)
                put(KEY_PRIORITY, messageWithCrc.priority.name)
                put(KEY_SEQ, messageWithCrc.sequenceNumber)
                put(KEY_CHECKSUM, messageWithCrc.checksum)
            }

            val bytes = json.toString().toByteArray(Charsets.UTF_8)
            if (bytes.size > MessageProtocol.MAX_SERIALIZED_PAYLOAD_BYTES) {
                return Result.failure(
                    IllegalArgumentException(
                        "Serialized payload of ${bytes.size} bytes exceeds maximum allowed ${MessageProtocol.MAX_SERIALIZED_PAYLOAD_BYTES} bytes."
                    )
                )
            }

            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deserializes a raw UTF-8 JSON byte array into a verified [TextMessage].
     *
     * Validates protocol version, required keys, and verifies the CRC32 checksum.
     */
    fun deserialize(bytes: ByteArray): Result<TextMessage> {
        if (bytes.size < MessageProtocol.MIN_SERIALIZED_PAYLOAD_BYTES) {
            return Result.failure(IllegalArgumentException("Payload too short (${bytes.size} bytes)."))
        }
        if (bytes.size > MessageProtocol.MAX_SERIALIZED_PAYLOAD_BYTES) {
            return Result.failure(
                IllegalArgumentException("Payload of ${bytes.size} bytes exceeds limit of ${MessageProtocol.MAX_SERIALIZED_PAYLOAD_BYTES} bytes.")
            )
        }

        return try {
            val jsonString = String(bytes, Charsets.UTF_8)
            val json = JSONObject(jsonString)

            val version = json.optInt(KEY_VERSION, -1)
            if (version != MessageProtocol.PROTOCOL_VERSION) {
                return Result.failure(
                    IllegalArgumentException("Unsupported protocol version: $version (expected ${MessageProtocol.PROTOCOL_VERSION}).")
                )
            }

            val messageId = json.getString(KEY_ID)
            val senderId = json.getString(KEY_SENDER)
            val receiverId = if (json.has(KEY_RECEIVER)) json.getString(KEY_RECEIVER) else null
            val text = json.getString(KEY_TEXT)
            val languageCode = json.getString(KEY_LANG)
            val timestamp = json.getLong(KEY_TIME)
            val typeStr = json.optString(KEY_TYPE, MessageType.CHAT.name)
            val messageType = try {
                MessageType.valueOf(typeStr)
            } catch (e: Exception) {
                MessageType.CHAT
            }
            val priorityStr = json.optString(KEY_PRIORITY, MessagePriority.NORMAL.name)
            val priority = try {
                MessagePriority.valueOf(priorityStr)
            } catch (e: Exception) {
                MessagePriority.NORMAL
            }
            val seq = json.optLong(KEY_SEQ, 0L)
            val checksum = json.getLong(KEY_CHECKSUM)

            val message = TextMessage(
                messageId = messageId,
                senderId = senderId,
                receiverId = receiverId,
                text = text,
                languageCode = languageCode,
                timestamp = timestamp,
                messageType = messageType,
                priority = priority,
                sequenceNumber = seq,
                protocolVersion = version,
                checksum = checksum
            )

            if (!message.isChecksumValid()) {
                return Result.failure(
                    IllegalStateException("Message integrity check failed (CRC32 mismatch).")
                )
            }

            Result.success(message)
        } catch (e: JSONException) {
            Result.failure(IllegalArgumentException("Malformed JSON message payload: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Frames a raw payload with a 4-byte Big-Endian length prefix header for stream transmission.
     */
    fun framePacket(payload: ByteArray): ByteArray {
        val buffer = ByteBuffer.allocate(MessageProtocol.HEADER_FRAME_LENGTH_BYTES + payload.size)
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(payload.size)
        buffer.put(payload)
        return buffer.array()
    }

    /**
     * Reads a single framed packet from an input stream using the 4-byte length prefix header.
     * Returns null if the stream is closed before a full frame is received.
     */
    fun readFramedPacket(input: InputStream): ByteArray? {
        val lengthHeader = ByteArray(MessageProtocol.HEADER_FRAME_LENGTH_BYTES)
        var bytesRead = 0
        while (bytesRead < MessageProtocol.HEADER_FRAME_LENGTH_BYTES) {
            val count = input.read(lengthHeader, bytesRead, MessageProtocol.HEADER_FRAME_LENGTH_BYTES - bytesRead)
            if (count == -1) return null
            bytesRead += count
        }

        val lengthBuffer = ByteBuffer.wrap(lengthHeader).order(ByteOrder.BIG_ENDIAN)
        val payloadLength = lengthBuffer.int
        if (payloadLength < MessageProtocol.MIN_SERIALIZED_PAYLOAD_BYTES || payloadLength > MessageProtocol.MAX_SERIALIZED_PAYLOAD_BYTES) {
            throw IllegalArgumentException("Invalid framed packet length: $payloadLength bytes.")
        }

        val payload = ByteArray(payloadLength)
        var payloadRead = 0
        while (payloadRead < payloadLength) {
            val count = input.read(payload, payloadRead, payloadLength - payloadRead)
            if (count == -1) return null
            payloadRead += count
        }

        return payload
    }
}
