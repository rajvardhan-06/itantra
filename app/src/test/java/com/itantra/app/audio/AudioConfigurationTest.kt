package com.itantra.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AudioConfiguration] constants, memory math, and duration conversions.
 */
class AudioConfigurationTest {

    @Test
    fun `audio configuration matches Phase 2 specifications`() {
        assertEquals(16_000, AudioConfiguration.SAMPLE_RATE)
        assertEquals(1, AudioConfiguration.CHANNEL_COUNT)
        assertEquals(16, AudioConfiguration.BITS_PER_SAMPLE)
        assertEquals(2, AudioConfiguration.BYTES_PER_SAMPLE)
        assertEquals(30, AudioConfiguration.MAX_RECORDING_DURATION_SECONDS)
        assertEquals(30_000L, AudioConfiguration.MAX_RECORDING_DURATION_MS)
    }

    @Test
    fun `bytes per second calculation is exactly 32,000 bytes`() {
        // 16,000 samples/sec * 1 channel * 2 bytes/sample = 32,000 bytes/sec
        assertEquals(32_000, AudioConfiguration.BYTES_PER_SECOND)
    }

    @Test
    fun `maximum 30-second audio bytes is exactly 960,000 bytes`() {
        // 32,000 bytes/sec * 30 seconds = 960,000 bytes (~937.5 KiB)
        assertEquals(960_000, AudioConfiguration.MAX_AUDIO_BYTES)
        // Hard buffer capacity is 1 MiB (1,048,576 bytes)
        assertTrue(AudioConfiguration.MAX_BUFFER_CAPACITY_BYTES >= AudioConfiguration.MAX_AUDIO_BYTES)
    }

    @Test
    fun `bytesToDurationMs converts correctly`() {
        assertEquals(0L, AudioConfiguration.bytesToDurationMs(0))
        assertEquals(1000L, AudioConfiguration.bytesToDurationMs(32_000))
        assertEquals(5000L, AudioConfiguration.bytesToDurationMs(160_000))
        assertEquals(30_000L, AudioConfiguration.bytesToDurationMs(960_000))
    }

    @Test
    fun `durationMsToBytes converts correctly and caps at MAX_AUDIO_BYTES`() {
        assertEquals(0, AudioConfiguration.durationMsToBytes(0L))
        assertEquals(32_000, AudioConfiguration.durationMsToBytes(1000L))
        assertEquals(160_000, AudioConfiguration.durationMsToBytes(5000L))
        assertEquals(960_000, AudioConfiguration.durationMsToBytes(30_000L))
        // Excess duration is capped
        assertEquals(AudioConfiguration.MAX_AUDIO_BYTES, AudioConfiguration.durationMsToBytes(60_000L))
    }
}
