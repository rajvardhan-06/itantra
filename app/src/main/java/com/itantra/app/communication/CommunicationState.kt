package com.itantra.app.communication

import com.itantra.app.domain.model.DiscoveredDevice
import com.itantra.app.domain.model.TransportType

/**
 * Type-safe state hierarchy representing the real-time operational status of the communication subsystem.
 */
sealed class CommunicationState {

    /** No active peer connection; subsystem is idle. */
    data object Disconnected : CommunicationState()

    /** Actively scanning for nearby Wi-Fi or Bluetooth peers. */
    data class Discovering(val transport: TransportType) : CommunicationState()

    /** Handshake or socket connection attempt in progress. */
    data class Connecting(val deviceName: String, val transport: TransportType) : CommunicationState()

    /** Actively connected to a remote peer device and ready for message exchange. */
    data class Connected(
        val device: DiscoveredDevice,
        val endpoint: String? = null
    ) : CommunicationState()

    /** Actively transmitting a payload across the transport link. */
    data class Sending(val messageId: String) : CommunicationState()

    /** Message packet successfully transmitted over the socket. */
    data class Sent(val messageId: String) : CommunicationState()

    /** Incoming payload is being received and framed from peer socket. */
    data object Receiving : CommunicationState()

    /** Confirmation acknowledgement received for a delivered message. */
    data class Delivered(val messageId: String) : CommunicationState()

    /** Error occurred during connection, transmission, or framing. */
    data class Failed(
        val reason: String,
        val throwable: Throwable? = null,
        val failedMessageId: String? = null
    ) : CommunicationState()

    /** Attempting to restore a lost connection to a peer. */
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : CommunicationState()
}
