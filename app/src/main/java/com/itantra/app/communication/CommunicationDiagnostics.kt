package com.itantra.app.communication

/**
 * Snapshot of runtime transport and protocol statistics for the iTantra diagnostic engine.
 *
 * All values are maintained monotonically across the current active session and can be reset
 * via the diagnostics dashboard.
 */
data class CommunicationDiagnostics(
    val packetsSent: Long = 0L,
    val packetsReceived: Long = 0L,
    val acksSent: Long = 0L,
    val acksReceived: Long = 0L,
    val deliverySuccesses: Long = 0L,
    val deliveryFailures: Long = 0L,
    val crc32Successes: Long = 0L,
    val crc32Failures: Long = 0L,
    val lastRoundTripTimeMs: Long? = null,
    val activeTransportType: String = "None",
    val remoteEndpoint: String = "Not Connected",
    val sessionStartedTimestampMs: Long = System.currentTimeMillis()
)
