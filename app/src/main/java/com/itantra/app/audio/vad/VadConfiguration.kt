package com.itantra.app.audio.vad

/**
 * Configuration parameters for the Voice Activity Detection (VAD) pipeline.
 *
 * All parameters are configurable with initial engineering defaults tuned for
 * 16 kHz mono speech in low-to-moderate ambient noise environments.
 *
 * @param sampleRate Audio sampling rate in Hertz (default: 16,000 Hz).
 * @param frameDurationMs Duration of each audio analysis frame in milliseconds (default: 20 ms).
 * @param energyThresholdDb Energy threshold in decibels relative to full scale (dBFS)
 *        above which an audio frame is considered speech (default: -40.0 dBFS).
 * @param speechConfirmationDurationMs Consecutive high-energy duration in ms required
 *        before transitioning to [VadState.SPEECH_DETECTED] (default: 100 ms).
 * @param minSpeechDurationMs Minimum valid speech segment duration in ms (default: 200 ms).
 * @param silenceTimeoutMs Duration of continuous silence in ms required to automatically
 *        stop recording once speech has been confirmed (default: 1,200 ms).
 * @param minSilenceDurationMs Consecutive low-energy duration in ms required before
 *        transitioning to [VadState.POSSIBLE_SILENCE] (default: 300 ms).
 * @param energySmoothingFactor Exponential Moving Average factor (alpha) between 0.0f and 1.0f
 *        for temporal energy smoothing (default: 0.3f).
 * @param maxRecordingDurationMs Hard timeout in ms after which recording automatically terminates (default: 30,000 ms).
 */
data class VadConfiguration(
    val sampleRate: Int = 16_000,
    val frameDurationMs: Int = 20,
    val energyThresholdDb: Float = -40.0f,
    val speechConfirmationDurationMs: Long = 100L,
    val minSpeechDurationMs: Long = 200L,
    val silenceTimeoutMs: Long = 1200L,
    val minSilenceDurationMs: Long = 300L,
    val energySmoothingFactor: Float = 0.3f,
    val maxRecordingDurationMs: Long = 30_000L
) {
    /**
     * Calculated number of PCM samples per frame (e.g. (16000 * 20) / 1000 = 320 samples).
     */
    val frameSizeSamples: Int
        get() = (sampleRate * frameDurationMs) / 1000

    /**
     * Frame size in bytes for 16-bit PCM (samples * 2).
     */
    val frameSizeBytes: Int
        get() = frameSizeSamples * 2

    init {
        validate()
    }

    /**
     * Validates configuration parameters, throwing [IllegalArgumentException] if invalid.
     */
    fun validate() {
        require(sampleRate in 8000..48000) { "sampleRate must be between 8,000 and 48,000 Hz, was $sampleRate" }
        require(frameDurationMs in 10..100) { "frameDurationMs must be between 10 and 100 ms, was $frameDurationMs" }
        require(energyThresholdDb in -100.0f..0.0f) { "energyThresholdDb must be between -100.0 and 0.0 dBFS, was $energyThresholdDb" }
        require(speechConfirmationDurationMs >= 0) { "speechConfirmationDurationMs must be non-negative" }
        require(minSpeechDurationMs >= 0) { "minSpeechDurationMs must be non-negative" }
        require(silenceTimeoutMs > 0) { "silenceTimeoutMs must be positive" }
        require(minSilenceDurationMs >= 0) { "minSilenceDurationMs must be non-negative" }
        require(energySmoothingFactor in 0.01f..1.0f) { "energySmoothingFactor must be between 0.01 and 1.0, was $energySmoothingFactor" }
        require(maxRecordingDurationMs > 0) { "maxRecordingDurationMs must be positive" }
    }
}
