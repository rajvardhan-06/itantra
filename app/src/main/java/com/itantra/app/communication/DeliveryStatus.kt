package com.itantra.app.communication

/**
 * Lifecycle transmission and acknowledgement states for an individual message.
 */
enum class DeliveryStatus {
    /** Message created and queued locally for transmission. */
    PENDING,

    /** Active socket transmission in progress. */
    SENDING,

    /** Packet pushed across socket to peer transport. */
    SENT,

    /** Delivery acknowledgement (ACK) received from the destination peer. */
    DELIVERED,

    /** Transmission failed or acknowledgement timed out after max retries. */
    FAILED
}
