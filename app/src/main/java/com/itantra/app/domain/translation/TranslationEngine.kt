package com.itantra.app.domain.translation

/**
 * Extensible interface defining offline neural/statistical machine translation.
 *
 * Current Architecture Status:
 * In Phase 9, iTantra natively transmits raw text across nodes in the sender's configured
 * language and relies on the receiving node's TTS engine configured for multilingual
 * synthesis. Translation across distinct Indian languages (e.g., Hindi -> Tamil) is
 * an extensible capability planned for integration with on-device NMT models (such as
 * IndicTrans2 / ONNX runtime or ML Kit offline translation packs).
 *
 * This contract establishes the strict asynchronous interface and error domain for
 * upcoming translation modules without mocking or faking outputs.
 */
interface TranslationEngine {

    /**
     * Translates input [text] from [sourceLanguage] to [targetLanguage].
     *
     * @param text The input source text to translate.
     * @param sourceLanguage BCP-47 or ISO-639-1 language code (e.g., "hi", "en", "ta").
     * @param targetLanguage BCP-47 or ISO-639-1 language code (e.g., "kn", "gu", "te").
     * @return [Result.success] containing translated text, or [Result.failure] with [TranslationException].
     */
    suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): Result<String>

    /**
     * Checks if offline translation assets for the given language pair are installed locally.
     */
    fun isLanguagePairAvailable(sourceLanguage: String, targetLanguage: String): Boolean

    /**
     * Returns the set of language pairs currently supported by the engine.
     */
    fun getSupportedLanguagePairs(): Set<Pair<String, String>>
}

/**
 * Base exception for translation operations.
 */
sealed class TranslationException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class ModelNotInstalledException(val source: String, val target: String) :
        TranslationException("Offline translation model for $source -> $target is not installed.")

    class UnsupportedLanguagePairException(val source: String, val target: String) :
        TranslationException("Language pair $source -> $target is not supported by this engine.")

    class TranslationUnavailableException(reason: String) :
        TranslationException("Translation service unavailable: $reason")
}

/**
 * Default implementation for Phase 9 indicating translation engine state.
 * Passes through identical language codes and cleanly reports unavailability
 * for distinct language pairs rather than generating mock translations.
 */
class NoOpTranslationEngine : TranslationEngine {

    override suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): Result<String> {
        if (sourceLanguage.equals(targetLanguage, ignoreCase = true)) {
            return Result.success(text)
        }
        return Result.failure(
            TranslationException.TranslationUnavailableException(
                "Offline translation between $sourceLanguage and $targetLanguage is in design. " +
                    "Direct text transmission is active."
            )
        )
    }

    override fun isLanguagePairAvailable(sourceLanguage: String, targetLanguage: String): Boolean {
        return sourceLanguage.equals(targetLanguage, ignoreCase = true)
    }

    override fun getSupportedLanguagePairs(): Set<Pair<String, String>> = emptySet()
}
