package com.itantra.app.stt

/**
 * Technical configuration parameters and audio constraints for offline speech recognition.
 */
object SttConfiguration {

    const val REQUIRED_SAMPLE_RATE: Int = 16_000
    const val REQUIRED_CHANNELS: Int = 1
    const val REQUIRED_BITS_PER_SAMPLE: Int = 16
    const val BYTES_PER_SAMPLE: Int = 2

    // 16,000 samples/sec * 1 channel * 2 bytes = 32,000 bytes/sec
    const val BYTES_PER_SECOND: Int = REQUIRED_SAMPLE_RATE * REQUIRED_CHANNELS * BYTES_PER_SAMPLE

    const val MIN_AUDIO_DURATION_MS: Long = 200L
    const val MIN_AUDIO_BYTES: Int = ((MIN_AUDIO_DURATION_MS * BYTES_PER_SECOND) / 1000L).toInt() // 6,400 bytes

    const val MAX_AUDIO_DURATION_MS: Long = 30_000L
    const val MAX_AUDIO_BYTES: Int = ((MAX_AUDIO_DURATION_MS * BYTES_PER_SECOND) / 1000L).toInt() // 960,000 bytes

    const val RECOGNITION_TIMEOUT_MS: Long = 15_000L

    /**
     * Validates input PCM byte array against speech recognition prerequisites.
     * Returns null if valid, or an [SttError.AudioInvalid] describing the defect.
     */
    fun validatePcmAudio(audio: ByteArray): SttError? {
        if (audio.isEmpty()) {
            return SttError.AudioInvalid("Audio buffer is empty (0 bytes).")
        }
        if (audio.size % BYTES_PER_SAMPLE != 0) {
            return SttError.AudioInvalid("Audio byte length (${audio.size}) is not aligned to 16-bit PCM samples.")
        }
        if (audio.size < MIN_AUDIO_BYTES) {
            return SttError.AudioInvalid("Audio is too short (${audio.size} bytes). Minimum required is $MIN_AUDIO_BYTES bytes (~200ms).")
        }
        if (audio.size > MAX_AUDIO_BYTES) {
            return SttError.AudioInvalid("Audio exceeds maximum supported duration of 30 seconds ($MAX_AUDIO_BYTES bytes).")
        }
        return null
    }
}
