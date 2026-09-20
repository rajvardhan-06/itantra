package com.itantra.app.domain.communication

import com.itantra.app.domain.model.ItantraMessage
import com.itantra.app.domain.model.SupportedLanguage

/**
 * Observable end-to-end communication session states representing the full lifecycle
 * from microphone capture to peer transmission, reception, and speech synthesis.
 */
sealed class CommunicationSessionState {

    /** Ready to start a new transmission or reception session. */
    data object Idle : CommunicationSessionState()

    /** Initializing audio hardware, acoustic models, or transport. */
    data class Preparing(val reason: String = "Initializing…") : CommunicationSessionState()

    /** Actively capturing microphone audio and streaming to VAD. */
    data class Listening(
        val durationMs: Long = 0L,
        val audioLevel: Float = 0f
    ) : CommunicationSessionState()

    /** Voice activity detection confirmed active human speech. */
    data class SpeechDetected(
        val durationMs: Long = 0L
    ) : CommunicationSessionState()

    /** Speech segment closed; offline STT is transcribing audio into text. */
    data class Transcribing(
        val language: SupportedLanguage
    ) : CommunicationSessionState()

    /**
     * Speech transcribed; presented to user for review, editing, and transmission confirmation.
     */
    data class TextReview(
        val transcribedText: String,
        val language: SupportedLanguage,
        val confidence: Int = 100,
        val latencyMs: Long = 0L
    ) : CommunicationSessionState()

    /** Validated message is currently being transmitted over the wire transport. */
    data class Sending(
        val messageId: String
    ) : CommunicationSessionState()

    /** Message was successfully delivered over transport and acknowledged. */
    data class Sent(
        val messageId: String
    ) : CommunicationSessionState()

    /** Wire transport is actively streaming an incoming frame. */
    data object Receiving : CommunicationSessionState()

    /** A complete, validated incoming message has been parsed and stored. */
    data class MessageReceived(
        val message: ItantraMessage
    ) : CommunicationSessionState()

    /** Text-to-speech engine is actively speaking received text. */
    data class Speaking(
        val text: String,
        val messageId: String? = null
    ) : CommunicationSessionState()

    /** The communication session finished successfully. */
    data class Completed(
        val summary: String = "Session completed."
    ) : CommunicationSessionState()

    /** The current session was cancelled by user action or preempted. */
    data class Cancelled(
        val reason: String = "Session cancelled."
    ) : CommunicationSessionState()

    /** An error occurred during the session pipeline. */
    data class Error(
        val errorDescription: String,
        val canRetry: Boolean = true,
        val cause: Throwable? = null
    ) : CommunicationSessionState()

    /**
     * Human-readable status description for UI indicators.
     */
    val displayStatus: String
        get() = when (this) {
            is Idle -> "Ready"
            is Preparing -> reason
            is Listening -> "Listening…"
            is SpeechDetected -> "Speech detected"
            is Transcribing -> "Transcribing (${language.displayName})…"
            is TextReview -> "Review transcription"
            is Sending -> "Transmitting…"
            is Sent -> "Delivered"
            is Receiving -> "Receiving packet…"
            is MessageReceived -> "Message received from ${message.remoteDeviceId ?: "peer"}"
            is Speaking -> "Speaking…"
            is Completed -> summary
            is Cancelled -> reason
            is Error -> errorDescription
        }
}
