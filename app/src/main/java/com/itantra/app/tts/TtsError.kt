package com.itantra.app.tts

/**
 * Structured error hierarchy representing text-to-speech failure scenarios.
 */
sealed class TtsError(val message: String) {

    data class InitializationFailed(val reason: String) :
        TtsError("TTS engine initialization failed: $reason")

    data class UnsupportedLanguage(val languageCode: String) :
        TtsError("Language code '$languageCode' is not supported by the offline TTS engine.")

    data class MissingVoiceData(val language: TtsLanguage) :
        TtsError("Voice data for ${language.displayName} (${language.code}) is not installed on this device.")

    data class SynthesisError(val errorCode: Int, val detail: String) :
        TtsError("Speech synthesis failed with error code $errorCode: $detail")

    data object EmptyText :
        TtsError("Cannot synthesize empty or blank text.")

    data class TextTooLong(val characterCount: Int, val limit: Int = TtsConfiguration.MAX_TEXT_CHARACTERS) :
        TtsError("Text length ($characterCount chars) exceeds maximum limit of $limit characters.")

    data class PlaybackInterrupted(val reason: String) :
        TtsError("Speech playback was interrupted: $reason")

    data object AudioFocusDenied :
        TtsError("Audio focus request was denied by the Android audio system.")

    data object QueueOverflow :
        TtsError("Speech queue capacity exceeded limit of ${TtsConfiguration.MAX_QUEUE_CAPACITY} items.")

    data class EngineNotInitialized(val action: String) :
        TtsError("Cannot perform '$action': TTS engine is not initialized.")
}
