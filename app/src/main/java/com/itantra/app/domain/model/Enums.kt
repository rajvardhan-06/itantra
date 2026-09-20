package com.itantra.app.domain.model

/** Message priority levels for transmission ordering. */
enum class MessagePriority { LOW, NORMAL, HIGH, URGENT }

/** Application-level communication mode. */
enum class CommunicationMode(
    val displayName: String,
    val description: String,
    val isSupported: Boolean
) {
    PUSH_TO_TALK(
        displayName = "Push-to-Talk (PTT)",
        description = "Hold button to record speech, review offline transcription, and transmit micro-payload.",
        isSupported = true
    ),
    TEXT_ONLY(
        displayName = "Text Communication",
        description = "Direct text drafting and transmission over wireless links without microphone capture.",
        isSupported = true
    ),
    VOICE_ASSISTED(
        displayName = "Voice-Assisted Mode",
        description = "PTT speech capture paired with automated text-to-speech announcement of all incoming messages.",
        isSupported = true
    ),
    PHONE_MODE(
        displayName = "Phone Mode (Planned)",
        description = "Continuous full-duplex voice call. Requires bidirectional PCM streaming and jitter buffers (In Design).",
        isSupported = false
    )
}

/** User-preferred theme setting. */
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

/** Transport type used for a connection. */
enum class TransportType {
    WIFI_DIRECT,
    BLUETOOTH
}

/** Delivery status for a transmitted message. */
enum class MessageStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED
}

/** Direction of a message in the conversation. */
enum class MessageDirection {
    SENT,
    RECEIVED
}
