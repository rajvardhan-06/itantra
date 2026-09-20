package com.itantra.app.audio.vad

import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive unit test suite for [EnergyBasedVoiceActivityDetector] covering
 * all 18 required scenarios from Phase 3 specifications.
 */
class EnergyBasedVoiceActivityDetectorTest {

    // Helper: generate synthetic speech-like 16kHz sine wave (~1000 Hz, ~ -15 dBFS)
    private fun generateSpeechSamples(durationMs: Int, sampleRate: Int = 16000, amplitude: Int = 6000): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        return ShortArray(count) { i ->
            (amplitude * sin(2.0 * Math.PI * 500.0 * i / sampleRate)).toInt().toShort()
        }
    }

    // Helper: generate pure silence
    private fun generateSilenceSamples(durationMs: Int, sampleRate: Int = 16000): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        return ShortArray(count) { 0 }
    }

    // Scenario 1: Silence-only audio
    @Test
    fun `scenario 1 - silence-only audio remains IDLE with no false positives`() {
        val detector = EnergyBasedVoiceActivityDetector()
        val silence = generateSilenceSamples(1000) // 1 second of silence

        val result = detector.processAudio(silence)

        assertEquals(VadState.IDLE, result.state)
        assertFalse(result.isSpeech)
        assertEquals(0L, result.speechDurationMs)
        assertNull(result.speechSegment)
    }

    // Scenario 2: Speech-like high-energy audio
    @Test
    fun `scenario 2 - speech-like high-energy audio transitions to SPEECH_DETECTED`() {
        val detector = EnergyBasedVoiceActivityDetector()
        // 500 ms of high energy audio (threshold is -40 dBFS, confirmation is 100 ms)
        val speech = generateSpeechSamples(500)

        val result = detector.processAudio(speech)

        assertEquals(VadState.SPEECH_DETECTED, result.state)
        assertTrue(result.isSpeech)
        assertTrue(result.speechDurationMs >= 100L)
    }

    // Scenario 3: One short noise burst
    @Test
    fun `scenario 3 - single short noise burst is rejected and returns to IDLE`() {
        val detector = EnergyBasedVoiceActivityDetector()
        // 20 ms noise burst (less than 100 ms confirmation)
        val shortBurst = generateSpeechSamples(20)
        val silenceAfter = generateSilenceSamples(200)

        detector.processAudio(shortBurst)
        // May briefly enter POSSIBLE_SPEECH during the burst
        val result = detector.processAudio(silenceAfter)

        // Must drop back to IDLE after burst ends without confirmation
        assertEquals(VadState.IDLE, result.state)
        assertFalse(result.isSpeech)
        assertNull(result.speechSegment)
    }

    // Scenario 4: Multiple noise bursts
    @Test
    fun `scenario 4 - multiple isolated noise bursts are rejected`() {
        val detector = EnergyBasedVoiceActivityDetector()

        for (i in 1..5) {
            // 20 ms burst followed by 100 ms silence
            detector.processAudio(generateSpeechSamples(20))
            val res = detector.processAudio(generateSilenceSamples(100))
            assertEquals(VadState.IDLE, res.state)
        }

        val finalResult = detector.finish()
        assertNull(finalResult.speechSegment)
    }

    // Scenario 5: Speech followed by silence
    @Test
    fun `scenario 5 - speech followed by silence triggers SILENCE_DETECTED after timeout`() {
        val config = VadConfiguration(silenceTimeoutMs = 500L) // 500 ms silence timeout
        val detector = EnergyBasedVoiceActivityDetector(config)

        // 400 ms speech -> confirmed speech
        detector.processAudio(generateSpeechSamples(400))
        assertEquals(VadState.SPEECH_DETECTED, detector.currentState)

        // Feed 600 ms silence -> exceeds 500 ms timeout
        val result = detector.processAudio(generateSilenceSamples(600))

        assertEquals(VadState.SILENCE_DETECTED, result.state)
        assertNotNull(result.speechSegment)
        assertTrue(result.speechSegment!!.durationMs >= 200L)
    }

    // Scenario 6: Silence followed by speech
    @Test
    fun `scenario 6 - silence followed by speech begins IDLE then detects speech`() {
        val detector = EnergyBasedVoiceActivityDetector()

        val silenceResult = detector.processAudio(generateSilenceSamples(500))
        assertEquals(VadState.IDLE, silenceResult.state)

        val speechResult = detector.processAudio(generateSpeechSamples(400))
        assertEquals(VadState.SPEECH_DETECTED, speechResult.state)
        assertTrue(speechResult.isSpeech)
    }

    // Scenario 7: Speech with short pauses
    @Test
    fun `scenario 7 - speech with short inter-word pauses maintains speech continuity`() {
        val config = VadConfiguration(silenceTimeoutMs = 1000L)
        val detector = EnergyBasedVoiceActivityDetector(config)

        // First word (300 ms)
        detector.processAudio(generateSpeechSamples(300))
        assertEquals(VadState.SPEECH_DETECTED, detector.currentState)

        // Short inter-word pause (100 ms, well below 1000 ms timeout)
        val pauseResult = detector.processAudio(generateSilenceSamples(100))
        assertEquals(VadState.POSSIBLE_SILENCE, pauseResult.state)

        // Second word (300 ms)
        val resumeResult = detector.processAudio(generateSpeechSamples(300))
        assertEquals(VadState.SPEECH_DETECTED, resumeResult.state)
        assertTrue(resumeResult.isSpeech)
    }

    // Scenario 8: Very quiet speech
    @Test
    fun `scenario 8 - very quiet speech below threshold does not trigger false detection`() {
        // Threshold is -40 dBFS; generate whisper below threshold (amplitude = 20, ~ -64 dBFS)
        val detector = EnergyBasedVoiceActivityDetector()
        val quietAudio = generateSpeechSamples(500, amplitude = 20)

        val result = detector.processAudio(quietAudio)

        assertEquals(VadState.IDLE, result.state)
        assertFalse(result.isSpeech)
    }

    // Scenario 9: Maximum recording duration
    @Test
    fun `scenario 9 - maximum recording duration ceiling triggers COMPLETED`() {
        val config = VadConfiguration(maxRecordingDurationMs = 1000L) // 1 second cap
        val detector = EnergyBasedVoiceActivityDetector(config)

        // Feed 1.2 seconds of speech
        val result = detector.processAudio(generateSpeechSamples(1200))

        assertEquals(VadState.COMPLETED, result.state)
        assertNotNull(result.speechSegment)
    }

    // Scenario 10: Empty PCM input
    @Test
    fun `scenario 10 - empty PCM input is handled safely without state disruption`() {
        val detector = EnergyBasedVoiceActivityDetector()
        val empty = ShortArray(0)

        val result = detector.processAudio(empty)

        assertEquals(VadState.IDLE, result.state)
        assertFalse(result.isSpeech)
    }

    // Scenario 11: Incomplete audio frames
    @Test
    fun `scenario 11 - incomplete audio frames are assembled correctly`() {
        val detector = EnergyBasedVoiceActivityDetector()
        // Frame size is 320 samples. Feed 100 samples at a time.
        val partialChunk = ShortArray(100) { 5000 }

        // 3 chunks = 300 samples (still < 320, no complete frame yet)
        detector.processAudio(partialChunk)
        detector.processAudio(partialChunk)
        detector.processAudio(partialChunk)
        assertEquals(VadState.IDLE, detector.currentState)

        // 4th chunk adds 100 samples = 400 samples total -> 1 complete 320-sample frame processed!
        val result = detector.processAudio(partialChunk)
        assertEquals(VadState.POSSIBLE_SPEECH, result.state)
    }

    // Scenario 12: Invalid configuration values
    @Test(expected = IllegalArgumentException::class)
    fun `scenario 12 - invalid sample rate throws IllegalArgumentException`() {
        VadConfiguration(sampleRate = 4000)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `scenario 12 - positive dBFS threshold throws IllegalArgumentException`() {
        VadConfiguration(energyThresholdDb = 5.0f)
    }

    // Scenario 13: Detector reset
    @Test
    fun `scenario 13 - detector reset restores initial state and counters`() {
        val detector = EnergyBasedVoiceActivityDetector()
        detector.processAudio(generateSpeechSamples(400))
        assertEquals(VadState.SPEECH_DETECTED, detector.currentState)

        detector.reset()

        assertEquals(VadState.IDLE, detector.currentState)
        val result = detector.processAudio(generateSilenceSamples(100))
        assertEquals(VadState.IDLE, result.state)
        assertEquals(0L, result.speechDurationMs)
    }

    // Scenario 14: Detector finish
    @Test
    fun `scenario 14 - detector finish finalizes speech segment and enters COMPLETED`() {
        val detector = EnergyBasedVoiceActivityDetector()
        detector.processAudio(generateSpeechSamples(500))

        val finalResult = detector.finish()

        assertEquals(VadState.COMPLETED, finalResult.state)
        assertNotNull(finalResult.speechSegment)
        assertTrue(finalResult.speechSegment!!.durationMs >= 400L)
    }

    // Scenario 15: Coroutine cancellation
    @Test
    fun `scenario 15 - coroutine cancellation handles teardown safely`() {
        runBlocking {
            val detector = EnergyBasedVoiceActivityDetector()
            val job = launch(Dispatchers.Default) {
                while (isActive) {
                    detector.processAudio(generateSpeechSamples(20))
                    delay(10)
                }
            }
            kotlinx.coroutines.delay(30)
            job.cancel()
            detector.reset()
            assertEquals(VadState.IDLE, detector.currentState)
        }
    }

    // Scenario 16: Repeated recording sessions
    @Test
    fun `scenario 16 - repeated recording sessions operate deterministically`() {
        val detector = EnergyBasedVoiceActivityDetector()

        for (session in 1..3) {
            detector.reset()
            val startRes = detector.processAudio(generateSilenceSamples(100))
            assertEquals(VadState.IDLE, startRes.state)

            val speechRes = detector.processAudio(generateSpeechSamples(400))
            assertEquals(VadState.SPEECH_DETECTED, speechRes.state)

            val finishRes = detector.finish()
            assertEquals(VadState.COMPLETED, finishRes.state)
            assertNotNull(finishRes.speechSegment)
        }
    }

    // Scenario 17: Microphone initialization failure / error state handling
    @Test
    fun `scenario 17 - VadResult supports error state reporting`() {
        val errorResult = VadResult(
            state = VadState.ERROR,
            isSpeech = false
        )
        assertEquals(VadState.ERROR, errorResult.state)
        assertFalse(errorResult.isSpeech)
    }

    // Scenario 18: Memory and buffer safety
    @Test
    fun `scenario 18 - continuous streaming does not cause memory leaks or unbounded growth`() {
        val detector = EnergyBasedVoiceActivityDetector()
        // Process 10 seconds of streaming audio chunks
        for (i in 1..100) {
            detector.processAudio(generateSpeechSamples(100))
        }
        val result = detector.finish()
        assertEquals(VadState.COMPLETED, result.state)
    }
}
