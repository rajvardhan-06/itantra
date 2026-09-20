package com.itantra.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Memory safety, boundary constraint, and overflow tests for [AudioBufferManager].
 *
 * Verifies that the audio subsystem strictly enforces memory ceilings under
 * stress, rapid bursts, and invalid slice requests to protect low-resource devices.
 */
class AudioBufferManagerMemoryTest {

    @Test
    fun `audio configuration strictly defines 1 MiB maximum memory ceiling`() {
        assertTrue(
            "Hard ceiling must not exceed 1 MiB to prevent OOM on low-resource devices",
            AudioConfiguration.MAX_BUFFER_CAPACITY_BYTES <= 1024 * 1024
        )
        // 30 seconds at 16kHz 16-bit mono is 960,000 bytes (< 1,048,576 bytes)
        assertEquals(960_000, AudioConfiguration.MAX_AUDIO_BYTES)
        assertEquals(30_000L, AudioConfiguration.MAX_RECORDING_DURATION_MS)
    }

    @Test
    fun `default buffer enforces 30-second duration and exact byte cap`() {
        val manager = AudioBufferManager()
        assertEquals(AudioConfiguration.MAX_AUDIO_BYTES, manager.maxAudioBytes)
        assertEquals(30, manager.maxDurationSeconds)
    }

    @Test
    fun `overflow attack with huge chunks is truncated safely at exact limit`() {
        val manager = AudioBufferManager(maxDurationSeconds = 1, maxCapacityBytes = 32_000)
        val massiveChunk = ByteArray(100_000) { (it % 128).toByte() }

        val accepted = manager.append(massiveChunk)

        assertFalse("Massive chunk overflow must signal full buffer", accepted)
        assertTrue("Buffer must be marked full", manager.isFull())
        assertEquals(32_000, manager.getByteCount())
        assertEquals(1_000L, manager.getDurationMs())

        val extracted = manager.toByteArray()
        assertEquals(32_000, extracted.size)
        assertEquals(0.toByte(), extracted[0])
        assertEquals((31_999 % 128).toByte(), extracted[31_999])
    }

    @Test
    fun `repeated small chunks accumulate safely without leak or off-by-one errors`() {
        val manager = AudioBufferManager(maxDurationSeconds = 2, maxCapacityBytes = 64_000)
        val smallChunk = ByteArray(640) // 20ms of audio

        // Write 100 chunks = 64,000 bytes exactly
        for (i in 0 until 100) {
            val res = manager.append(smallChunk)
            assertTrue("Chunk $i should be accepted", res)
        }

        assertEquals(64_000, manager.getByteCount())
        assertEquals(2_000L, manager.getDurationMs())
        assertTrue(manager.isFull())

        // 101st chunk must be completely rejected
        val overflowChunk = ByteArray(640)
        val overflowRes = manager.append(overflowChunk)
        assertFalse("101st chunk must be rejected", overflowRes)
        assertEquals(64_000, manager.getByteCount())
    }

    @Test
    fun `clear and reuse cycle releases memory and resets all counters`() {
        val manager = AudioBufferManager(maxDurationSeconds = 1, maxCapacityBytes = 32_000)
        manager.append(ByteArray(32_000))
        assertTrue(manager.isFull())

        manager.clear()
        assertTrue(manager.isEmpty())
        assertFalse(manager.isFull())
        assertEquals(0, manager.getByteCount())
        assertEquals(0L, manager.getDurationMs())

        // Can record again after clear
        val reaccepted = manager.append(ByteArray(16_000))
        assertTrue(reaccepted)
        assertEquals(16_000, manager.getByteCount())
        assertEquals(500L, manager.getDurationMs())
    }
}
