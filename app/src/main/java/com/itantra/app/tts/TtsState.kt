package com.itantra.app.tts

/**
 * Observable UI state of the text-to-speech synthesis pipeline.
 */
sealed class TtsState {

    /** Engine is idle and ready to accept speech requests. */
    data object Idle : TtsState()

    /** Engine is initializing or switching language models. */
    data class Initializing(val language: TtsLanguage? = null) : TtsState()

    /** Speech synthesis and playback is actively underway. */
    data class Speaking(
        val text: String,
        val messageId: String? = null,
        val language: TtsLanguage? = null,
        val utteranceId: String? = null
    ) : TtsState()

    /** Speech playback is temporarily paused. */
    data class Paused(
        val text: String,
        val messageId: String? = null
    ) : TtsState()

    /** Speech playback finished successfully. */
    data class Completed(
        val messageId: String? = null,
        val utteranceId: String? = null
    ) : TtsState()

    /** Voice data for the requested language is missing or unsupported on the device. */
    data class LanguageUnavailable(
        val language: TtsLanguage,
        val availability: VoiceAvailability = VoiceAvailability.MISSING_DATA,
        val message: String = "Voice unavailable for ${language.displayName} (${language.code})"
    ) : TtsState()

    /** An error occurred during synthesis or playback. */
    data class Error(val error: TtsError) : TtsState()

    /**
     * User-facing localized status description conforming to Phase 6 UX requirements.
     */
    val displayStatus: String
        get() = when (this) {
            is Idle -> "Ready to speak"
            is Initializing -> "Preparing voice..."
            is Speaking -> "Speaking..."
            is Paused -> "Speech paused"
            is Completed -> "Speech completed"
            is LanguageUnavailable -> "Voice unavailable for this language"
            is Error -> "Text-to-speech failed"
        }
}
