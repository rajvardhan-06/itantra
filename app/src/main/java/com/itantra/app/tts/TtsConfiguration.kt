package com.itantra.app.tts

/**
 * Configuration limits, constraints, and defaults for offline text-to-speech synthesis.
 */
object TtsConfiguration {

    /** Minimum allowable speech synthesis rate (0.5x). */
    const val MIN_SPEECH_RATE: Float = 0.5f

    /** Maximum allowable speech synthesis rate (2.0x). */
    const val MAX_SPEECH_RATE: Float = 2.0f

    /** Default standard speech rate (1.0x). */
    const val DEFAULT_SPEECH_RATE: Float = 1.0f

    /** Minimum voice pitch multiplier (0.5x). */
    const val MIN_PITCH: Float = 0.5f

    /** Maximum voice pitch multiplier (2.0x). */
    const val MAX_PITCH: Float = 2.0f

    /** Default voice pitch multiplier (1.0x). */
    const val DEFAULT_PITCH: Float = 1.0f

    /** Maximum allowed character count for text synthesis (1000 chars, matching Phase 5 limits). */
    const val MAX_TEXT_CHARACTERS: Int = 1000

    /** Maximum number of messages that can be buffered in the speech queue. */
    const val MAX_QUEUE_CAPACITY: Int = 50

    /** Maximum duration in milliseconds to await TTS engine initialization before timing out. */
    const val INITIALIZATION_TIMEOUT_MS: Long = 10_000L

    /**
     * Validates and coerces a speech rate within [MIN_SPEECH_RATE] and [MAX_SPEECH_RATE].
     */
    fun validateSpeechRate(rate: Float): Result<Float> {
        return if (rate.isNaN() || rate.isInfinite()) {
            Result.failure(IllegalArgumentException("Speech rate cannot be NaN or Infinite."))
        } else if (rate < MIN_SPEECH_RATE || rate > MAX_SPEECH_RATE) {
            Result.failure(
                IllegalArgumentException("Speech rate $rate is out of bounds [$MIN_SPEECH_RATE, $MAX_SPEECH_RATE].")
            )
        } else {
            Result.success(rate)
        }
    }

    /**
     * Validates and coerces a voice pitch multiplier within [MIN_PITCH] and [MAX_PITCH].
     */
    fun validatePitch(pitch: Float): Result<Float> {
        return if (pitch.isNaN() || pitch.isInfinite()) {
            Result.failure(IllegalArgumentException("Voice pitch cannot be NaN or Infinite."))
        } else if (pitch < MIN_PITCH || pitch > MAX_PITCH) {
            Result.failure(
                IllegalArgumentException("Voice pitch $pitch is out of bounds [$MIN_PITCH, $MAX_PITCH].")
            )
        } else {
            Result.success(pitch)
        }
    }
}
