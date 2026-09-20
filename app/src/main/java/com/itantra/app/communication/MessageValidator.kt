package com.itantra.app.communication

/**
 * Result of validating a [TextMessage] before serialization or after receipt.
 */
sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()

    val isValid: Boolean get() = this is Valid
}

/**
 * Validates protocol constraints, field bounds, and payload integrity on [TextMessage] instances.
 */
object MessageValidator {

    private const val MAX_FUTURE_TIME_TOLERANCE_MS = 5 * 60 * 1000L // 5 minutes
    private const val MAX_PAST_TIME_TOLERANCE_MS = 7 * 24 * 60 * 60 * 1000L // 7 days

    /**
     * Validates all fields and constraints of a [TextMessage].
     *
     * @param message The message to validate.
     * @param verifyChecksum Whether to enforce that the message checksum matches its content.
     * @return [ValidationResult.Valid] if compliant, or [ValidationResult.Invalid] with reason.
     */
    fun validate(message: TextMessage, verifyChecksum: Boolean = true): ValidationResult {
        // 1. Protocol version
        if (message.protocolVersion != MessageProtocol.PROTOCOL_VERSION) {
            return ValidationResult.Invalid(
                "Unsupported protocol version: ${message.protocolVersion} (supported: ${MessageProtocol.PROTOCOL_VERSION})"
            )
        }

        // 2. Message identifier
        if (message.messageId.isBlank()) {
            return ValidationResult.Invalid("Message ID cannot be empty or blank.")
        }
        if (message.messageId.length > 64 || message.messageId.contains('\u0000')) {
            return ValidationResult.Invalid("Message ID contains illegal characters or exceeds 64 characters.")
        }

        // 3. Sender identifier
        if (message.senderId.isBlank()) {
            return ValidationResult.Invalid("Sender ID cannot be empty or blank.")
        }
        if (message.senderId.length > 64 || message.senderId.contains('\u0000')) {
            return ValidationResult.Invalid("Sender ID contains illegal characters or exceeds 64 characters.")
        }

        // Optional Receiver identifier
        if (message.receiverId != null && (message.receiverId.length > 64 || message.receiverId.contains('\u0000'))) {
            return ValidationResult.Invalid("Receiver ID contains illegal characters or exceeds 64 characters.")
        }

        // 4. Text content (for CHAT messages)
        if (message.messageType == MessageType.CHAT) {
            if (message.text.isBlank()) {
                return ValidationResult.Invalid("Message text cannot be empty or blank.")
            }
            if (message.text.contains('\u0000')) {
                return ValidationResult.Invalid("Message text contains prohibited null bytes.")
            }
            if (message.text.any { it < ' ' && it != '\n' && it != '\r' && it != '\t' }) {
                return ValidationResult.Invalid("Message text contains prohibited control characters.")
            }
            if (message.text.length > MessageProtocol.MAX_TEXT_CHARACTERS) {
                return ValidationResult.Invalid(
                    "Message text of ${message.text.length} characters exceeds maximum limit of ${MessageProtocol.MAX_TEXT_CHARACTERS} characters."
                )
            }
            val utf8Bytes = message.text.toByteArray(Charsets.UTF_8).size
            if (utf8Bytes > MessageProtocol.MAX_SERIALIZED_PAYLOAD_BYTES) {
                return ValidationResult.Invalid(
                    "Message text UTF-8 size of $utf8Bytes bytes exceeds limit of ${MessageProtocol.MAX_SERIALIZED_PAYLOAD_BYTES} bytes."
                )
            }
        }

        // 5. Language code
        if (!MessageProtocol.isLanguageSupported(message.languageCode)) {
            return ValidationResult.Invalid(
                "Unsupported or invalid language code '${message.languageCode}'."
            )
        }

        // 6. Sequence number
        if (message.sequenceNumber < 0L) {
            return ValidationResult.Invalid("Sequence number must be non-negative (${message.sequenceNumber}).")
        }

        // 7. Timestamp reasonableness
        val now = System.currentTimeMillis()
        if (message.timestamp > now + MAX_FUTURE_TIME_TOLERANCE_MS) {
            return ValidationResult.Invalid("Message timestamp is too far in the future.")
        }
        if (message.timestamp < now - MAX_PAST_TIME_TOLERANCE_MS) {
            return ValidationResult.Invalid("Message timestamp is older than permitted 7-day window.")
        }

        // 8. Checksum verification
        if (verifyChecksum && message.checksum != 0L) {
            if (!message.isChecksumValid()) {
                return ValidationResult.Invalid("Message CRC32 checksum mismatch. Data may be corrupted.")
            }
        }

        return ValidationResult.Valid
    }
}
