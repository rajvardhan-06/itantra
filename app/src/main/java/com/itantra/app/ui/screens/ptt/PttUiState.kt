package com.itantra.app.ui.screens.ptt

import com.itantra.app.audio.AudioRecordingResult
import com.itantra.app.audio.RecordingState
import com.itantra.app.audio.vad.VadResult
import com.itantra.app.audio.vad.VadState
import com.itantra.app.communication.CommunicationState
import com.itantra.app.communication.DeliveryStatus
import com.itantra.app.communication.MessagePriority
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.SupportedLanguage

import com.itantra.app.stt.SttResult
import com.itantra.app.stt.SttState

/**
 * Reactive UI state for the Push-to-Talk (PTT) screen in Phase 5 (Protocol Transceiver).
 */
data class PttUiState(
    val recordingState: RecordingState = RecordingState.Idle,
    val vadState: VadState = VadState.IDLE,
    val vadResult: VadResult? = null,
    val sttState: SttState = SttState.Idle,
    val sttResult: SttResult? = null,
    val isModelInstalled: Boolean = true,
    val activeLanguage: SupportedLanguage = SupportedLanguage.DEFAULT,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val editableTranscription: String = "",
    val audioLevel: Float = 0f,
    val hasMicPermission: Boolean = false,
    val isMicPermanentlyDenied: Boolean = false,
    val capturedAudioResult: AudioRecordingResult? = null,
    val selectedPriority: MessagePriority = MessagePriority.NORMAL,
    val lastSentMessageId: String? = null,
    val deliveryStatus: DeliveryStatus? = null,
    val communicationState: CommunicationState = CommunicationState.Disconnected,
    val isDemoMode: Boolean = true,
    val lastDemoAction: String? = null,
    val isTransmitting: Boolean = false,
    val showEmergencyConfirmation: Boolean = false,
    val errorMessage: String? = null
) {
    val canSubmitMessage: Boolean
        get() = editableTranscription.isNotBlank() && !isTransmitting && deliveryStatus != DeliveryStatus.SENDING
}

enum class PttSequenceStage {
    IDLE,
    RECORDING,
    SPEECH_DETECTED,
    PROCESSING_STT,
    REVIEW_AND_CONFIRM,
    TRANSMITTING,
    COMPLETED,
    ERROR
}

