package com.itantra.app.domain.model

/**
 * Represents every phase of the Push-to-Talk recording lifecycle.
 * UI layer maps each state to a distinct visual presentation.
 */
sealed class RecordingState {
    /** Microphone is idle; awaiting user action. */
    data object Idle : RecordingState()

    /** Actively capturing audio from the microphone. */
    data class Recording(
        /** Duration in milliseconds since recording started. */
        val durationMs: Long = 0L,
        /** Normalised amplitude level 0.0f–1.0f for waveform display. */
        val audioLevel: Float = 0f
    ) : RecordingState()

    /** Audio captured; offline STT model is transcribing. */
    data class Processing(
        /** Progress fraction 0.0f–1.0f, if known; null if indeterminate. */
        val progress: Float? = null
    ) : RecordingState()

    /** Transcription complete; text ready for review and editing before send. */
    data class ReadyForReview(
        val transcribedText: String,
        val confidenceScore: Float? = null
    ) : RecordingState()

    /** Message is being transmitted to the connected device. */
    data object Sending : RecordingState()

    /** Message successfully transmitted. */
    data object Sent : RecordingState()

    /** An error occurred at any stage of the recording pipeline. */
    data class Error(val message: String) : RecordingState()
}
