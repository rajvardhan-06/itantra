package com.itantra.app.tts

/**
 * Result outcome of a speech synthesis request.
 */
sealed class TtsResult {

    /** Speech synthesis and playback completed successfully. */
    data class Success(val utteranceId: String, val text: String) : TtsResult()

    /** Speech was cancelled or preempted by user action or high-priority interruption. */
    data class Cancelled(val utteranceId: String, val text: String, val reason: String) : TtsResult()

    /** Speech synthesis encountered an error. */
    data class Error(val utteranceId: String, val error: TtsError) : TtsResult()
}
