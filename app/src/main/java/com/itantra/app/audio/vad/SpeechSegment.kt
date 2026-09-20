package com.itantra.app.audio.vad

/**
 * Encapsulates the boundaries and metrics of a detected segment of speech.
 *
 * @param startMs Timestamp in milliseconds (from start of recording) where speech began.
 * @param endMs Timestamp in milliseconds where speech ended.
 * @param startByteOffset Byte offset in the raw PCM stream where speech began.
 * @param endByteOffset Byte offset in the raw PCM stream where speech ended.
 * @param confidence Confidence score of speech detection (0.0f to 1.0f).
 */
data class SpeechSegment(
    val startMs: Long,
    val endMs: Long,
    val startByteOffset: Int = ((startMs * 32_000L) / 1000L).toInt(),
    val endByteOffset: Int = ((endMs * 32_000L) / 1000L).toInt(),
    val confidence: Float = 1.0f
) {
    /** Duration of this speech segment in milliseconds. */
    val durationMs: Long
        get() = (endMs - startMs).coerceAtLeast(0L)

    /**
     * Whether this segment meets the minimum required speech duration.
     */
    fun isValid(minSpeechDurationMs: Long = 200L): Boolean {
        return durationMs >= minSpeechDurationMs
    }
}
