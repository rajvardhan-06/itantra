package com.itantra.app.stt

/**
 * Replaceable abstraction for offline Speech-to-Text inference engines.
 *
 * Implementations:
 * - [OfflineSpeechToTextEngine]: Production offline speech recognition engine.
 * - [MockSpeechToTextEngine]: Predictable mock engine for unit tests and UI previews.
 */
interface SpeechToTextEngine {

    /**
     * Initializes or switches the acoustic model for the specified language.
     *
     * @param language The target language for recognition.
     * @return [Result.success] when model weights are loaded and engine is ready,
     *         or [Result.failure] with [SttError] if model is missing or unsupported.
     */
    suspend fun initialize(language: SttLanguage): Result<Unit>

    /**
     * Transcribes 16 kHz Mono 16-bit PCM audio samples into recognized text.
     *
     * @param audio Raw 16-bit signed PCM audio bytes.
     * @return [Result.success] containing [SttResult] on success, or [Result.failure] on error.
     */
    suspend fun transcribe(audio: ByteArray): Result<SttResult>

    /**
     * Releases active model weights and frees memory.
     */
    suspend fun release()

    /**
     * Whether an acoustic model is currently initialized and ready to transcribe.
     */
    fun isInitialized(): Boolean
}
