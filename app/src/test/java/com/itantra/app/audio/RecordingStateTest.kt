package com.itantra.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [RecordingState] and [AudioRecordingResult].
 */
class RecordingStateTest {

    @Test
    fun `RecordingState hierarchy supports all expected lifecycle states`() {
        val states: List<RecordingState> = listOf(
            RecordingState.Idle,
            RecordingState.RequestingPermission,
            RecordingState.Ready,
            RecordingState.Recording(durationMs = 1200L, byteCount = 38400, audioLevel = 0.5f),
            RecordingState.Stopping,
            RecordingState.Completed(AudioRecordingResult(pcmData = ByteArray(16000))),
            RecordingState.Cancelled,
            RecordingState.Error(message = "Microphone unavailable")
        )

        assertEquals(8, states.size)
        assertTrue(states[0] is RecordingState.Idle)
        assertTrue(states[1] is RecordingState.RequestingPermission)
        assertTrue(states[2] is RecordingState.Ready)
        assertTrue(states[3] is RecordingState.Recording)
        assertTrue(states[4] is RecordingState.Stopping)
        assertTrue(states[5] is RecordingState.Completed)
        assertTrue(states[6] is RecordingState.Cancelled)
        assertTrue(states[7] is RecordingState.Error)
    }

    @Test
    fun `AudioRecordingResult calculates duration and formatted string correctly`() {
        // 5 seconds of 16kHz mono 16-bit PCM = 5 * 32,000 = 160,000 bytes
        val pcm = ByteArray(160_000)
        val result = AudioRecordingResult(pcmData = pcm)

        assertEquals(160_000, result.byteCount)
        assertEquals(5000L, result.durationMs)
        assertEquals(5.0f, result.durationSeconds, 0.001f)
        assertEquals("00:05", result.formattedDuration)
        // 160,000 / 1024 = 156.25 KB
        assertTrue(result.formattedSize.contains("KB"))
    }

    @Test
    fun `AudioRecordingResult equals and hashCode work on byte content`() {
        val bytes1 = byteArrayOf(10, 20, 30)
        val bytes2 = byteArrayOf(10, 20, 30)
        val bytes3 = byteArrayOf(10, 20, 40)

        val result1 = AudioRecordingResult(bytes1)
        val result2 = AudioRecordingResult(bytes2)
        val result3 = AudioRecordingResult(bytes3)

        assertEquals(result1, result2)
        assertEquals(result1.hashCode(), result2.hashCode())
        assertNotEquals(result1, result3)
    }
}
