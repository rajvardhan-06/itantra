package com.itantra.app.communication

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * Generates unique, collision-resistant message and transmission identifiers.
 */
object MessageIdGenerator {

    private val sequenceCounter = AtomicLong(0L)

    /**
     * Generates a unique message ID prefixed with `msg-`.
     * Format: `msg-<epochMs>-<seq>-<shortUuid>`
     */
    fun generateMessageId(): String {
        val now = System.currentTimeMillis()
        val seq = sequenceCounter.incrementAndGet() and 0xFFFF
        val suffix = UUID.randomUUID().toString().substring(0, 8)
        return "msg-$now-$seq-$suffix"
    }

    /**
     * Generates a unique device identifier for local transmission sessions.
     */
    fun generateDeviceId(prefix: String = "itantra"): String {
        val randomSuffix = UUID.randomUUID().toString().substring(0, 6)
        return "$prefix-$randomSuffix"
    }

    /**
     * Generates an acknowledgement packet ID for a given message ID.
     */
    fun generateAckId(messageId: String): String {
        return "ack-$messageId"
    }
}
