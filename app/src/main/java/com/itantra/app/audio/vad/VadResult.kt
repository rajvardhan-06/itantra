package com.itantra.app.audio.vad

/**
 * Result of evaluating voice activity on an audio stream at a specific point in time.
 *
 * @param state Current [VadState] of the detector.
 * @param isSpeech Whether active human speech is currently detected.
 * @param energyDb Raw decibel energy level of the most recent frame (in dBFS).
 * @param smoothedEnergyDb Exponentially smoothed energy level (in dBFS).
 * @param rms Root-Mean-Square amplitude of the most recent frame.
 * @param speechDurationMs Cumulative duration of active speech in milliseconds.
 * @param silenceDurationMs Duration of continuous silence in milliseconds.
 * @param speechSegment Bounded speech segment if speech was confirmed and/or ended.
 * @param timestampMs Elapsed audio timestamp in milliseconds since session start.
 */
data class VadResult(
    val state: VadState,
    val isSpeech: Boolean,
    val energyDb: Float = -100.0f,
    val smoothedEnergyDb: Float = -100.0f,
    val rms: Float = 0.0f,
    val speechDurationMs: Long = 0L,
    val silenceDurationMs: Long = 0L,
    val speechSegment: SpeechSegment? = null,
    val timestampMs: Long = 0L
) {
    companion object {
        val IDLE = VadResult(
            state = VadState.IDLE,
            isSpeech = false
        )
    }
}
