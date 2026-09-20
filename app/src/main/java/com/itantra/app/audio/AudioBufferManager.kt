package com.itantra.app.audio

import java.io.ByteArrayOutputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Memory-conscious buffer manager for capturing raw 16-bit PCM audio streams.
 *
 * Design & Memory Architecture:
 * ------------------------------
 * 1. Audio Specification:
 *    - 16,000 samples/sec * 1 channel (mono) * 2 bytes/sample (16-bit) = 32,000 bytes/sec.
 * 2. Hard Capacity Ceiling:
 *    - For a maximum 30-second push-to-talk transmission: 32,000 * 30 = 960,000 bytes (~937.5 KiB).
 *    - An absolute maximum cap of [maxCapacityBytes] (default: 1,048,576 bytes / 1 MiB) is enforced.
 *    - This bounds total heap footprint strictly below 1 MB, entirely preventing memory growth
 *      leaks, excessive Garbage Collection (GC) pauses, and Out-Of-Memory (OOM) errors.
 * 3. Thread-Safety:
 *    - Uses a reentrant lock to allow concurrent audio reading coroutines and UI state observation
 *      without race conditions.
 *
 * @param maxDurationSeconds Maximum recording duration in seconds (default: 30).
 * @param maxCapacityBytes Absolute memory upper bound in bytes (default: 1 MiB).
 */
class AudioBufferManager(
    val maxDurationSeconds: Int = AudioConfiguration.MAX_RECORDING_DURATION_SECONDS,
    val maxCapacityBytes: Int = AudioConfiguration.MAX_BUFFER_CAPACITY_BYTES
) {

    private val lock = ReentrantLock()

    /** Maximum allowed bytes for the configured duration. */
    val maxAudioBytes: Int = (AudioConfiguration.BYTES_PER_SECOND * maxDurationSeconds).coerceAtMost(maxCapacityBytes)

    // Initialise with expected capacity to prevent repeated buffer re-allocations
    private var outputStream = ByteArrayOutputStream(maxAudioBytes)

    /**
     * Appends a chunk of PCM audio bytes to the buffer.
     *
     * If the incoming chunk would cause the buffer to exceed [maxAudioBytes],
     * only the remaining capacity is appended and false is returned to signal that
     * the maximum capacity has been reached.
     *
     * @param data The byte array containing raw PCM samples.
     * @param offset The starting offset within [data].
     * @param length The number of bytes to append.
     * @return `true` if all bytes were appended; `false` if the buffer is now full.
     */
    fun append(data: ByteArray, offset: Int = 0, length: Int = data.size): Boolean = lock.withLock {
        if (length <= 0) return true

        val currentSize = outputStream.size()
        val remainingCapacity = maxAudioBytes - currentSize

        if (remainingCapacity <= 0) {
            return false
        }

        val bytesToWrite = length.coerceAtMost(remainingCapacity)
        outputStream.write(data, offset, bytesToWrite)

        return bytesToWrite == length
    }

    /**
     * Returns a snapshot copy of the captured PCM audio bytes.
     */
    fun toByteArray(): ByteArray = lock.withLock {
        outputStream.toByteArray()
    }

    /**
     * Current number of bytes stored in the buffer.
     */
    fun getByteCount(): Int = lock.withLock {
        outputStream.size()
    }

    /**
     * Calculated duration in milliseconds based on current byte count and sample rate.
     */
    fun getDurationMs(): Long = lock.withLock {
        AudioConfiguration.bytesToDurationMs(outputStream.size())
    }

    /**
     * Whether the buffer is empty (0 bytes captured).
     */
    fun isEmpty(): Boolean = lock.withLock {
        outputStream.size() == 0
    }

    /**
     * Whether the buffer has reached its maximum recording capacity.
     */
    fun isFull(): Boolean = lock.withLock {
        outputStream.size() >= maxAudioBytes
    }

    /**
     * Resets the buffer, clearing all captured audio and releasing excess capacity.
     */
    fun clear(): Unit = lock.withLock {
        outputStream.reset()
    }
}
