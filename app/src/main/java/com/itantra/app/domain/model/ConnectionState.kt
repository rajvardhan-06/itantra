package com.itantra.app.domain.model

/**
 * Represents the current state of a remote device connection.
 * Uses a sealed class to carry state-specific payload without nullability hacks.
 */
sealed class ConnectionState {
    /** No connection attempted; initial state. */
    data object Disconnected : ConnectionState()

    /** Actively scanning for nearby devices. */
    data object Scanning : ConnectionState()

    /** In the process of establishing a connection to the given device. */
    data class Connecting(val deviceName: String) : ConnectionState()

    /** Successfully connected to a remote device. */
    data class Connected(
        val deviceName: String,
        val deviceId: String,
        val transport: TransportType,
        /** Signal strength 0–100 */
        val signalStrength: Int = 100
    ) : ConnectionState()

    /** Connection attempt or maintenance failed. */
    data class Error(val reason: String) : ConnectionState()
}

/**
 * Represents a discoverable remote device found during a scan.
 */
data class DiscoveredDevice(
    val id: String,
    val name: String,
    val transport: TransportType,
    /** Signal strength 0–100 */
    val signalStrength: Int
)
