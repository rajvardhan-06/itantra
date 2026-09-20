package com.itantra.app.domain.model

/**
 * Result of an offline speech-to-text transcription operation.
 */
sealed class TranscriptionResult {
    data class Success(
        val text: String,
        val language: SupportedLanguage,
        val confidenceScore: Float? = null,
        val processingTimeMs: Long = 0L
    ) : TranscriptionResult()

    data class Failure(val error: String) : TranscriptionResult()

    /** STT engine is not yet loaded (model file missing or not initialised). */
    data object EngineNotReady : TranscriptionResult()
}

/**
 * Result of an offline text-to-speech synthesis operation.
 */
sealed class TtsResult {
    data class Success(
        val audioFilePath: String,
        val durationMs: Long
    ) : TtsResult()

    data class Failure(val error: String) : TtsResult()

    /** TTS engine is not yet loaded (model file missing or not initialised). */
    data object EngineNotReady : TtsResult()
}
