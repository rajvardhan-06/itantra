package com.itantra.app.communication

/**
 * Protocol specifications, limits, and transport constants for iTantra communication.
 */
object MessageProtocol {

    /** Current protocol wire version. */
    const val PROTOCOL_VERSION: Int = 1

    /** Maximum allowed character count for message text (1000 characters). */
    const val MAX_TEXT_CHARACTERS: Int = 1000

    /** Maximum allowed serialized UTF-8 payload size in bytes (4 KB). */
    const val MAX_SERIALIZED_PAYLOAD_BYTES: Int = 4096

    /** Minimum valid serialized message size in bytes. */
    const val MIN_SERIALIZED_PAYLOAD_BYTES: Int = 16

    /** 4-byte length prefix used to frame packets over TCP and RFCOMM byte streams. */
    const val HEADER_FRAME_LENGTH_BYTES: Int = 4

    /** Default local TCP port for Wi-Fi Direct and local ad-hoc communication. */
    const val DEFAULT_PORT: Int = 8988

    /** Connection timeout in milliseconds. */
    const val CONNECT_TIMEOUT_MS: Int = 5000

    /** Timeout in milliseconds before retransmitting unacknowledged messages. */
    const val ACK_TIMEOUT_MS: Long = 3000L

    /** Maximum number of retransmission attempts before marking message delivery FAILED. */
    const val MAX_RETRY_ATTEMPTS: Int = 3

    /** Time-to-live for duplicate message detection entries (5 minutes in milliseconds). */
    const val DEDUPLICATION_CACHE_TTL_MS: Long = 5 * 60 * 1000L

    /**
     * Standard ISO 639-1 language codes recognized across the iTantra multilingual pipeline.
     */
    val SUPPORTED_LANGUAGE_CODES: Set<String> = setOf(
        "en", "hi", "bn", "gu", "mr", "kn", "ml", "ta", "te", "or"
    )

    /**
     * Checks whether an ISO 639-1 language code is supported by the protocol.
     */
    fun isLanguageSupported(code: String): Boolean {
        return SUPPORTED_LANGUAGE_CODES.contains(code.lowercase().trim())
    }
}
