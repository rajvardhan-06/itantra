package com.itantra.app.domain.model

import java.util.UUID

/**
 * Represents a single communication message in the iTantra system.
 * Both sent and received messages are modelled uniformly.
 */
data class ItantraMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val language: SupportedLanguage,
    val direction: MessageDirection,
    val status: MessageStatus = MessageStatus.PENDING,
    val priority: MessagePriority = MessagePriority.NORMAL,
    val timestampMs: Long = System.currentTimeMillis(),
    /** ID of the remote device this message was sent to / received from. */
    val remoteDeviceId: String? = null,
    /** Optional audio file path if the original audio has been saved locally. */
    val audioFilePath: String? = null
)
