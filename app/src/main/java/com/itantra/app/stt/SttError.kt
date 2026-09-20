package com.itantra.app.stt

/**
 * Type-safe error hierarchy for offline Speech-to-Text operations.
 */
sealed class SttError(val userMessage: String) {

    data class ModelMissing(val language: SttLanguage) :
        SttError("Language model for ${language.displayName} is not installed.")

    data class ModelLoadingFailed(val details: String) :
        SttError("Failed to load language model: $details")

    data class InitializationFailed(val details: String) :
        SttError("STT engine initialization failed: $details")

    data class AudioInvalid(val details: String) :
        SttError("Invalid audio format: $details")

    data class RecognitionFailed(val details: String) :
        SttError("Speech recognition failed: $details")

    data object RecognitionTimeout :
        SttError("Speech recognition timed out.")

    data class Unsupported(val language: SttLanguage) :
        SttError("${language.displayName} is not currently supported for offline recognition.")

    data object Cancelled :
        SttError("Speech recognition was cancelled.")

    data class InsufficientMemory(val details: String) :
        SttError("Insufficient memory to run STT model: $details")

    data class InsufficientStorage(val details: String) :
        SttError("Insufficient storage space to install model: $details")
}
