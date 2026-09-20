package com.itantra.app.audio

/**
 * Type-safe states representing the lifecycle of microphone audio capture.
 *
 * Exposed as a reactive [kotlinx.coroutines.flow.StateFlow] to drive the Push-to-Talk UI.
 */
sealed class RecordingState {

    /**
     * Initial state. Recorder is idle and waiting for user action.
     */
    data object Idle : RecordingState()

    /**
     * Runtime microphone permission (RECORD_AUDIO) is being requested.
     */
    data object RequestingPermission : RecordingState()

    /**
     * Permission is granted, audio hardware is verified, and system is primed to record.
     */
    data object Ready : RecordingState()

    /**
     * Microphone is actively recording PCM audio.
     *
     * @param durationMs Elapsed duration in milliseconds since recording started.
     * @param byteCount Number of raw PCM bytes captured so far.
     * @param audioLevel Normalised audio amplitude level (0.0f - 1.0f) for waveform animation.
     */
    data class Recording(
        val durationMs: Long = 0L,
        val byteCount: Int = 0,
        val audioLevel: Float = 0f
    ) : RecordingState()

    /**
     * User released the PTT button or reached the 30-second duration limit;
     * audio pipeline is draining buffers and stopping the AudioRecord thread.
     */
    data object Stopping : RecordingState()

    /**
     * Audio capture completed successfully.
     *
     * @param result Encapsulated PCM bytes and recording metadata.
     */
    data class Completed(
        val result: AudioRecordingResult
    ) : RecordingState()

    /**
     * Recording was discarded or cancelled by the user before completion.
     */
    data object Cancelled : RecordingState()

    /**
     * An error occurred during initialization, permission check, or recording.
     *
     * @param message Human-readable error description.
     * @param cause Underlying exception, if any.
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : RecordingState()
}
