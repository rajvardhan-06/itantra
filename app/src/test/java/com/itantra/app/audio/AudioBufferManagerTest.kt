package com.itantra.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AudioBufferManager] bounding, memory safety, and operations.
 */
class AudioBufferManagerTest {

    @Test
    fun `newly created buffer is empty`() {
        val manager = AudioBufferManager()
        assertTrue(manager.isEmpty())
        assertFalse(manager.isFull())
        assertEquals(0, manager.getByteCount())
        assertEquals(0L, manager.getDurationMs())
        assertEquals(0, manager.toByteArray().size)
    }

    @Test
    fun `append writes bytes and updates count and duration`() {
        val manager = AudioBufferManager()
        val chunk = ByteArray(3200) // 100ms of 16kHz mono 16-bit PCM

        val accepted = manager.append(chunk)

        assertTrue(accepted)
        assertFalse(manager.isEmpty())
        assertEquals(3200, manager.getByteCount())
        assertEquals(100L, manager.getDurationMs())
        assertEquals(3200, manager.toByteArray().size)
    }

    @Test
    fun `append multiple chunks accumulates properly`() {
        val manager = AudioBufferManager()
        val chunk1 = byteArrayOf(1, 2, 3, 4)
        val chunk2 = byteArrayOf(5, 6, 7, 8)

        manager.append(chunk1)
        manager.append(chunk2)

        assertEquals(8, manager.getByteCount())
        val combined = manager.toByteArray()
        assertEquals(8, combined.size)
        assertEquals(1.toByte(), combined[0])
        assertEquals(8.toByte(), combined[7])
    }

    @Test
    fun `clear resets the buffer to empty`() {
        val manager = AudioBufferManager()
        manager.append(ByteArray(1000))
        assertFalse(manager.isEmpty())

        manager.clear()

        assertTrue(manager.isEmpty())
        assertEquals(0, manager.getByteCount())
        assertEquals(0L, manager.getDurationMs())
        assertEquals(0, manager.toByteArray().size)
    }

    @Test
    fun `buffer caps strictly at max allowed capacity preventing unbounded growth`() {
        // Create small manager with 1 second capacity (32,000 bytes)
        val manager = AudioBufferManager(maxDurationSeconds = 1, maxCapacityBytes = 32_000)
        assertEquals(32_000, manager.maxAudioBytes)

        // Write 30,000 bytes -> should succeed
        val firstChunk = ByteArray(30_000)
        val acceptedFirst = manager.append(firstChunk)
        assertTrue(acceptedFirst)
        assertEquals(30_000, manager.getByteCount())
        assertFalse(manager.isFull())

        // Try writing 5,000 more bytes -> only 2,000 should be accepted
        val secondChunk = ByteArray(5_000)
        val acceptedSecond = manager.append(secondChunk)
        assertFalse(acceptedSecond) // Signals that buffer is now full
        assertEquals(32_000, manager.getByteCount())
        assertTrue(manager.isFull())

        // Any further write should be rejected
        val thirdChunk = ByteArray(1_000)
        val acceptedThird = manager.append(thirdChunk)
        assertFalse(acceptedThird)
        assertEquals(32_000, manager.getByteCount())
    }
}
