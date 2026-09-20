package com.itantra.app.stt

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Mock implementation of [SpeechToTextEngine] for unit tests, offline previews, and debugging.
 */
class MockSpeechToTextEngine : SpeechToTextEngine {

    private val initialized = AtomicBoolean(false)
    private var activeLanguage: SttLanguage? = null

    /** Allows tests to inject canned transcription text or errors. */
    var mockTranscriptionText: String? = null
    var shouldFailInitialization: Boolean = false
    var shouldFailTranscription: Boolean = false

    override fun isInitialized(): Boolean = initialized.get()

    override suspend fun initialize(language: SttLanguage): Result<Unit> {
        if (shouldFailInitialization) {
            return Result.failure(IllegalStateException("Simulated initialization failure."))
        }
        if (!language.isModelAvailable) {
            return Result.failure(IllegalArgumentException("Language ${language.displayName} is not supported."))
        }
        activeLanguage = language
        initialized.set(true)
        return Result.success(Unit)
    }

    override suspend fun transcribe(audio: ByteArray): Result<SttResult> {
        if (!initialized.get() || activeLanguage == null) {
            return Result.failure(IllegalStateException("Mock engine is not initialized."))
        }
        if (shouldFailTranscription) {
            return Result.failure(IllegalStateException("Simulated transcription failure."))
        }

        val validationError = SttConfiguration.validatePcmAudio(audio)
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError.userMessage))
        }

        val lang = activeLanguage!!
        val text = mockTranscriptionText ?: when (lang) {
            SttLanguage.HINDI -> "नमस्ते, क्या आप मेरी आवाज़ सुन सकते हैं?"
            SttLanguage.ENGLISH -> "Hello, this is a test transmission over iTantra."
            SttLanguage.BENGALI -> "নমস্কার, আপনি কি শুনতে পাচ্ছেন?"
            else -> "Speech recognized in ${lang.displayName}"
        }

        return Result.success(
            SttResult(
                text = text,
                language = lang,
                durationMs = 40L,
                confidence = 0.95f,
                isFinal = true
            )
        )
    }

    override suspend fun release() {
        initialized.set(false)
        activeLanguage = null
    }
}
