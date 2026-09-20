package com.itantra.app.stt

/**
 * Encapsulates the output of offline speech recognition.
 *
 * @param text Transcribed speech text.
 * @param language Language used for transcription.
 * @param durationMs Processing duration in milliseconds.
 * @param confidence Recognition confidence score between 0.0f and 1.0f, or null if the engine
 *        does not provide calibrated confidence metrics. (Never fabricated).
 * @param isFinal Whether this is a finalized transcription result.
 */
data class SttResult(
    val text: String,
    val language: SttLanguage,
    val durationMs: Long = 0L,
    val confidence: Float? = null,
    val isFinal: Boolean = true
) {
    val isEmpty: Boolean
        get() = text.isBlank()
}
