package com.itantra.app.domain.communication

import com.itantra.app.communication.TextMessage
import com.itantra.app.domain.model.MessagePriority
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface contract for end-to-end speech communication sessions.
 *
 * Coordinates microphone recording, speech detection, transcription, user review,
 * transport serialization/dispatch, and incoming speech synthesis.
 */
interface SpeechMessagePipeline {

    /**
     * Commences microphone recording and voice activity detection.
     */
    suspend fun startListening(): Result<Unit>

    /**
     * Halts recording, validates captured PCM, and transcribes audio via offline STT.
     * Transitions session to [CommunicationSessionState.TextReview] upon success.
     */
    suspend fun stopListeningAndTranscribe(): Result<String>

    /**
     * Confirms the reviewed/edited text and transmits it across the active communication transport.
     */
    suspend fun confirmAndTransmit(
        editedText: String,
        priority: MessagePriority = MessagePriority.NORMAL
    ): Result<TextMessage>

    /**
     * Cancels the active recording, recognition, or pending transmission.
     */
    suspend fun cancelSession(reason: String = "User cancelled session")

    /**
     * Ingests, validates, stores, and optionally synthesizes an incoming wire message.
     */
    suspend fun receiveIncomingMessage(message: TextMessage): Result<Unit>

    /**
     * Observes the active reactive state flow of the communication session.
     */
    fun observeSessionState(): StateFlow<CommunicationSessionState>

    /**
     * Releases audio buffers, stops pending tasks, and shuts down coordinator resources.
     */
    suspend fun release()
}
