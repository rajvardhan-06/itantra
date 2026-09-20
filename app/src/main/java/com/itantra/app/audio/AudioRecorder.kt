package com.itantra.app.audio

import kotlinx.coroutines.flow.Flow

/**
 * Replaceable abstraction for microphone audio capture.
 *
 * Implementations:
 * - [AudioRecordRecorder]: Native Android AudioRecord implementation for low-latency PCM audio.
 * - [MockAudioRecorder]: Simulated recorder for testing and UI previews.
 *
 * Design constraints:
 * - Non-blocking execution using Kotlin Coroutines on background dispatchers.
 * - Thread-safe state guarding to prevent multiple concurrent recording sessions.
 * - Safe release of hardware microphone resources.
 */
interface AudioRecorder {

    /**
     * Initiates microphone audio capture.
     *
     * @return [Result.success] when recording begins, or [Result.failure] with a descriptive
     *         error if permission is missing, hardware is busy, or AudioRecord fails to initialise.
     */
    suspend fun startRecording(): Result<Unit>

    /**
     * Stops the active recording session and returns the captured 16-bit PCM byte array.
     *
     * @return [Result.success] containing raw PCM samples, or [Result.failure] if the recorder
     *         was not recording, the buffer was empty, or capture failed.
     */
    suspend fun stopRecording(): Result<ByteArray>

    /**
     * Cancels the current recording session, discarding any captured audio buffers and resetting state.
     */
    suspend fun cancelRecording()

    /**
     * Whether the recorder is actively capturing audio right now.
     */
    fun isRecording(): Boolean

    /**
     * Hot flow of the current normalised audio amplitude level (0.0f–1.0f).
     * Emits dynamically during active recording for waveform visual feedback.
     */
    fun observeAmplitude(): Flow<Float>

    /**
     * Registers a listener callback invoked whenever a new PCM short buffer is captured.
     * Used by Voice Activity Detection (VAD) to process frames in real-time off the main thread.
     */
    fun setAudioFrameListener(listener: ((ShortArray, Int) -> Unit)?)

    /**
     * Safely tears down and releases underlying hardware audio resources.
     */
    fun release()
}
