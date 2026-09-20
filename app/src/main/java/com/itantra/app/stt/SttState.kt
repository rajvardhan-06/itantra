package com.itantra.app.stt

/**
 * Observable reactive state hierarchy for Speech-to-Text inference and model lifecycle.
 */
sealed class SttState {

    data object Idle : SttState()

    data class Initializing(val language: SttLanguage) : SttState()

    data class ModelNotInstalled(val language: SttLanguage) : SttState()

    data class Ready(val language: SttLanguage) : SttState()

    data class Recognizing(val durationMs: Long = 0L) : SttState()

    data class ResultAvailable(val result: SttResult) : SttState()

    data object NoSpeechDetected : SttState()

    data class UnsupportedLanguage(val language: SttLanguage) : SttState()

    data class Error(val error: SttError) : SttState()

    data object Cancelled : SttState()
}
