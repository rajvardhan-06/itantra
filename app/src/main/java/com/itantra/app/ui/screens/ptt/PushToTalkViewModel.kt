package com.itantra.app.ui.screens.ptt

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.itantra.app.audio.AudioConfiguration
import com.itantra.app.audio.AudioRecordRecorder
import com.itantra.app.audio.AudioRecorder
import com.itantra.app.audio.AudioRecordingResult
import com.itantra.app.audio.MockAudioRecorder
import com.itantra.app.audio.RecordingState
import com.itantra.app.audio.vad.EnergyBasedVoiceActivityDetector
import com.itantra.app.audio.vad.VadResult
import com.itantra.app.audio.vad.VadState
import com.itantra.app.audio.vad.VoiceActivityDetector
import com.itantra.app.communication.CommunicationManager
import com.itantra.app.communication.CommunicationState
import com.itantra.app.communication.DeliveryStatus
import com.itantra.app.communication.MessagePriority
import com.itantra.app.data.repository.ConnectionRepositoryImpl
import com.itantra.app.data.repository.LanguageRepositoryImpl
import com.itantra.app.data.repository.MessageRepositoryImpl
import com.itantra.app.domain.model.ItantraMessage
import com.itantra.app.domain.model.MessageDirection
import com.itantra.app.domain.model.MessageStatus
import com.itantra.app.domain.repository.ConnectionRepository
import com.itantra.app.domain.repository.LanguageRepository
import com.itantra.app.domain.repository.MessageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.itantra.app.stt.MockSpeechToTextEngine
import com.itantra.app.stt.SpeechToTextEngine
import com.itantra.app.stt.SttError
import com.itantra.app.stt.SttLanguage
import com.itantra.app.stt.SttResult
import com.itantra.app.stt.SttState
import com.itantra.app.stt.model.LocalModelManager
import com.itantra.app.stt.model.ModelManager
import kotlinx.coroutines.CoroutineScope

/**
 * ViewModel for the Push-to-Talk transceiver interface.
 *
 * Coordinates real-time microphone recording via [AudioRecorder], Voice Activity Detection (VAD)
 * via [VoiceActivityDetector], offline Speech-to-Text (STT) via [SpeechToTextEngine],
 * enforces 30-second buffer constraints, manages permission state transitions, feeds audio
 * and transcription metrics to the UI, and dispatches structured text messages via [CommunicationManager].
 */
class PushToTalkViewModel(
    application: Application,
    private val audioRecorder: AudioRecorder = AudioRecordRecorder(application.applicationContext),
    private val voiceActivityDetector: VoiceActivityDetector = EnergyBasedVoiceActivityDetector(),
    private val sttEngine: SpeechToTextEngine = MockSpeechToTextEngine(),
    private val modelManager: ModelManager? = null,
    private val messageRepository: MessageRepository = MessageRepositoryImpl(),
    private val languageRepository: LanguageRepository = LanguageRepositoryImpl(),
    private val connectionRepository: ConnectionRepository = ConnectionRepositoryImpl(),
    private val communicationManager: CommunicationManager = CommunicationManager(messageRepository = messageRepository),
    private val externalScope: CoroutineScope? = null,
    private val demoSimulationManager: com.itantra.app.demo.DemoSimulationManager? = null
) : AndroidViewModel(application) {

    /**
     * Testing constructor allowing injection of mock audio recorder, VAD, STT, and repositories.
     */
    constructor(
        audioRecorder: AudioRecorder = MockAudioRecorder(),
        voiceActivityDetector: VoiceActivityDetector = EnergyBasedVoiceActivityDetector(),
        sttEngine: SpeechToTextEngine = MockSpeechToTextEngine(),
        modelManager: ModelManager? = null,
        messageRepository: MessageRepository = MessageRepositoryImpl(),
        languageRepository: LanguageRepository = LanguageRepositoryImpl(),
        connectionRepository: ConnectionRepository = ConnectionRepositoryImpl(),
        communicationManager: CommunicationManager = CommunicationManager(messageRepository = messageRepository),
        externalScope: CoroutineScope? = null,
        demoSimulationManager: com.itantra.app.demo.DemoSimulationManager? = null
    ) : this(
        application = Application(),
        audioRecorder = audioRecorder,
        voiceActivityDetector = voiceActivityDetector,
        sttEngine = sttEngine,
        modelManager = modelManager,
        messageRepository = messageRepository,
        languageRepository = languageRepository,
        connectionRepository = connectionRepository,
        communicationManager = communicationManager,
        externalScope = externalScope,
        demoSimulationManager = demoSimulationManager
    )

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val effectiveModelManager: ModelManager? = modelManager ?: runCatching {
        LocalModelManager(application.applicationContext)
    }.getOrNull()

    private val _uiState = MutableStateFlow(PttUiState(isDemoMode = demoSimulationManager != null))
    val uiState: StateFlow<PttUiState> = _uiState.asStateFlow()

    private var durationTickerJob: Job? = null
    private var recordingStartTimeMs: Long = 0L

    init {
        observeLanguageAndConnection()
        observeAudioLevel()
        setupAudioFrameListener()
        observeCommunicationManager()
        observeDemoSimulation()
    }

    private fun observeDemoSimulation() {
        demoSimulationManager?.let { demo ->
            scope.launch {
                demo.lastSimulatedAction.collect { action ->
                    _uiState.update { it.copy(lastDemoAction = action) }
                }
            }
        }
    }

    fun simulateIncomingPeerMessage(presetIndex: Int = 0) {
        scope.launch {
            demoSimulationManager?.simulateIncomingPeerMessage(presetIndex)
        }
    }

    private fun observeCommunicationManager() {
        scope.launch {
            communicationManager.communicationState.collect { commState ->
                _uiState.update { it.copy(communicationState = commState) }
            }
        }
        scope.launch {
            communicationManager.deliveryStatuses.collect { statuses ->
                val lastId = _uiState.value.lastSentMessageId
                if (lastId != null) {
                    val status = statuses[lastId]
                    _uiState.update { it.copy(deliveryStatus = status) }
                }
            }
        }
    }

    private fun setupAudioFrameListener() {
        audioRecorder.setAudioFrameListener { samples, length ->
            val currentState = _uiState.value.recordingState
            if (currentState is RecordingState.Recording) {
                val result = voiceActivityDetector.processAudio(samples, 0, length)
                _uiState.update { it.copy(vadState = result.state, vadResult = result) }

                // Silence timeout reached after confirmed speech: auto-stop recording!
                if (result.state == VadState.SILENCE_DETECTED) {
                    stopRecording()
                }
            }
        }
    }

    private fun observeLanguageAndConnection() {
        scope.launch {
            combine(
                languageRepository.observeActiveLanguage(),
                connectionRepository.observeConnectionState()
            ) { language, connection ->
                language to connection
            }.collect { (language, connection) ->
                val sttLang = SttLanguage.fromSupportedLanguage(language)
                val isInstalled = effectiveModelManager?.isModelInstalled(sttLang) ?: true
                _uiState.update {
                    it.copy(
                        activeLanguage = language,
                        connectionState = connection,
                        isModelInstalled = isInstalled,
                        sttState = if (!sttLang.isModelAvailable) {
                            SttState.UnsupportedLanguage(sttLang)
                        } else if (!isInstalled) {
                            SttState.ModelNotInstalled(sttLang)
                        } else {
                            if (it.sttState is SttState.ModelNotInstalled || it.sttState is SttState.UnsupportedLanguage) {
                                SttState.Idle
                            } else {
                                it.sttState
                            }
                        }
                    )
                }
            }
        }
    }

    private fun observeAudioLevel() {
        scope.launch {
            audioRecorder.observeAmplitude().collect { level ->
                _uiState.update { state ->
                    val updatedRecordingState = when (val rec = state.recordingState) {
                        is RecordingState.Recording -> rec.copy(audioLevel = level)
                        else -> rec
                    }
                    state.copy(
                        audioLevel = level,
                        recordingState = updatedRecordingState
                    )
                }
            }
        }
    }

    /**
     * Called when the user initiates a permission check or is about to be prompted.
     */
    fun onRequestingPermission() {
        _uiState.update { it.copy(recordingState = RecordingState.RequestingPermission) }
    }

    /**
     * Called with the result of the runtime RECORD_AUDIO permission request.
     */
    fun onPermissionResult(isGranted: Boolean, permanentlyDenied: Boolean = false) {
        _uiState.update {
            it.copy(
                hasMicPermission = isGranted,
                isMicPermanentlyDenied = permanentlyDenied,
                recordingState = if (isGranted) {
                    if (it.recordingState is RecordingState.RequestingPermission) RecordingState.Ready else it.recordingState
                } else {
                    val msg = if (permanentlyDenied) {
                        "Microphone permission permanently denied. Enable it in App Settings."
                    } else {
                        "Microphone permission is required to record audio."
                    }
                    RecordingState.Error(msg)
                },
                errorMessage = if (!isGranted) "Microphone permission required." else null
            )
        }
    }

    /**
     * Starts microphone audio capture on PTT button press.
     */
    fun startRecording() {
        val currentState = _uiState.value.recordingState
        if (currentState is RecordingState.Recording || currentState is RecordingState.Stopping) {
            return
        }

        scope.launch {
            voiceActivityDetector.reset()
            _uiState.update {
                it.copy(
                    recordingState = RecordingState.Recording(0L, 0, 0f),
                    vadState = VadState.IDLE,
                    vadResult = null,
                    capturedAudioResult = null,
                    errorMessage = null
                )
            }
            recordingStartTimeMs = System.currentTimeMillis()

            val result = audioRecorder.startRecording()
            if (result.isSuccess) {
                startDurationTicker()
            } else {
                stopDurationTicker()
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to start microphone recording."
                _uiState.update {
                    it.copy(
                        recordingState = RecordingState.Error(errorMsg, result.exceptionOrNull()),
                        errorMessage = errorMsg
                    )
                }
            }
        }
    }

    /**
     * Stops audio capture on PTT button release or VAD silence timeout, retrieves PCM bytes,
     * feeds PCM to the offline STT engine, and updates UI state with transcription.
     */
    fun stopRecording() {
        val currentState = _uiState.value.recordingState
        if (currentState !is RecordingState.Recording) return

        stopDurationTicker()
        _uiState.update { it.copy(recordingState = RecordingState.Stopping) }

        scope.launch {
            val finalVad = voiceActivityDetector.finish()
            val result = audioRecorder.stopRecording()
            if (result.isSuccess) {
                val pcmData = result.getOrThrow()
                val audioResult = AudioRecordingResult(pcmData = pcmData)
                val sttLang = SttLanguage.fromSupportedLanguage(_uiState.value.activeLanguage)
                val isInstalled = effectiveModelManager?.isModelInstalled(sttLang) ?: true

                _uiState.update {
                    it.copy(
                        recordingState = RecordingState.Completed(audioResult),
                        vadState = finalVad.state,
                        vadResult = finalVad,
                        capturedAudioResult = audioResult,
                        isModelInstalled = isInstalled
                    )
                }

                if (!sttLang.isModelAvailable) {
                    _uiState.update {
                        it.copy(
                            sttState = SttState.UnsupportedLanguage(sttLang),
                            editableTranscription = ""
                        )
                    }
                    return@launch
                }

                if (!isInstalled) {
                    _uiState.update {
                        it.copy(
                            sttState = SttState.ModelNotInstalled(sttLang),
                            editableTranscription = ""
                        )
                    }
                    return@launch
                }

                // Transition to Recognizing
                _uiState.update {
                    it.copy(sttState = SttState.Recognizing(0L))
                }

                // Initialize engine if needed
                if (!sttEngine.isInitialized()) {
                    val initResult = sttEngine.initialize(sttLang)
                    if (initResult.isFailure) {
                        val errorMsg = initResult.exceptionOrNull()?.message ?: "Failed to initialize STT model."
                        _uiState.update {
                            it.copy(
                                sttState = SttState.Error(SttError.InitializationFailed(errorMsg)),
                                errorMessage = errorMsg
                            )
                        }
                        return@launch
                    }
                }

                // Transcribe audio
                val transcriptionResult = sttEngine.transcribe(pcmData)
                if (transcriptionResult.isSuccess) {
                    val sttResult = transcriptionResult.getOrThrow()
                    if (sttResult.isEmpty) {
                        _uiState.update {
                            it.copy(
                                sttState = SttState.NoSpeechDetected,
                                sttResult = null,
                                editableTranscription = ""
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                sttState = SttState.ResultAvailable(sttResult),
                                sttResult = sttResult,
                                editableTranscription = sttResult.text
                            )
                        }
                    }
                } else {
                    val errorMsg = transcriptionResult.exceptionOrNull()?.message ?: "Speech recognition failed."
                    _uiState.update {
                        it.copy(
                            sttState = SttState.Error(SttError.RecognitionFailed(errorMsg)),
                            errorMessage = errorMsg
                        )
                    }
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Recording stopped with an error."
                _uiState.update {
                    it.copy(
                        recordingState = RecordingState.Error(errorMsg, result.exceptionOrNull()),
                        errorMessage = errorMsg
                    )
                }
            }
        }
    }

    /**
     * Installs the offline model package for the currently active language.
     */
    fun installActiveModel() {
        val sttLang = SttLanguage.fromSupportedLanguage(_uiState.value.activeLanguage)
        scope.launch {
            _uiState.update { it.copy(sttState = SttState.Initializing(sttLang)) }
            val result = effectiveModelManager?.installModel(sttLang) ?: Result.success(Unit)
            if (result.isSuccess) {
                sttEngine.initialize(sttLang)
                _uiState.update {
                    it.copy(
                        isModelInstalled = true,
                        sttState = SttState.Ready(sttLang)
                    )
                }
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Model installation failed."
                _uiState.update {
                    it.copy(
                        sttState = SttState.Error(SttError.ModelLoadingFailed(msg)),
                        errorMessage = msg
                    )
                }
            }
        }
    }

    /**
     * Cancels the active recording session, discarding buffered PCM data.
     */
    fun cancelRecording() {
        stopDurationTicker()
        voiceActivityDetector.reset()
        scope.launch {
            audioRecorder.cancelRecording()
            _uiState.update {
                it.copy(
                    recordingState = RecordingState.Cancelled,
                    vadState = VadState.IDLE,
                    vadResult = null,
                    capturedAudioResult = null,
                    sttState = SttState.Cancelled
                )
            }
            delay(600L)
            _uiState.update {
                val sttLang = SttLanguage.fromSupportedLanguage(it.activeLanguage)
                val installed = effectiveModelManager?.isModelInstalled(sttLang) ?: true
                it.copy(
                    recordingState = RecordingState.Idle,
                    sttState = if (installed) SttState.Idle else SttState.ModelNotInstalled(sttLang)
                )
            }
        }
    }

    /**
     * Clears any captured recording and returns to Idle.
     */
    fun clearRecording() {
        stopDurationTicker()
        voiceActivityDetector.reset()
        val sttLang = SttLanguage.fromSupportedLanguage(_uiState.value.activeLanguage)
        val isInstalled = effectiveModelManager?.isModelInstalled(sttLang) ?: true
        _uiState.update {
            it.copy(
                recordingState = RecordingState.Idle,
                vadState = VadState.IDLE,
                vadResult = null,
                capturedAudioResult = null,
                sttState = if (!sttLang.isModelAvailable) {
                    SttState.UnsupportedLanguage(sttLang)
                } else if (!isInstalled) {
                    SttState.ModelNotInstalled(sttLang)
                } else {
                    SttState.Idle
                },
                sttResult = null,
                editableTranscription = "",
                errorMessage = null
            )
        }
    }

    /**
     * Sets the transmission priority for the message.
     */
    fun selectPriority(priority: MessagePriority) {
        _uiState.update { it.copy(selectedPriority = priority) }
    }

    /**
     * Called when the user edits the transcription text field.
     */
    fun onTranscriptionEdited(text: String) {
        _uiState.update { it.copy(editableTranscription = text) }
    }

    /**
     * Pre-send check that prompts for confirmation if emergency priority is selected.
     */
    fun onSendClicked() {
        if (!_uiState.value.canSubmitMessage) return
        if (_uiState.value.selectedPriority == MessagePriority.ALERT) {
            _uiState.update { it.copy(showEmergencyConfirmation = true) }
        } else {
            sendMessage()
        }
    }

    fun dismissEmergencyConfirmation() {
        _uiState.update { it.copy(showEmergencyConfirmation = false) }
    }

    fun confirmEmergencySend() {
        _uiState.update { it.copy(showEmergencyConfirmation = false) }
        sendMessage()
    }

    /**
     * Sends the current transcription over the offline communication protocol.
     * Prevents duplicate submissions while transmission is active.
     */
    fun sendMessage() {
        val text = _uiState.value.editableTranscription.trim()
        if (text.isBlank() || _uiState.value.isTransmitting) return

        val langCode = _uiState.value.activeLanguage.code
        val priority = _uiState.value.selectedPriority

        _uiState.update { it.copy(isTransmitting = true, errorMessage = null) }

        scope.launch {
            val result = communicationManager.sendMessage(
                text = text,
                languageCode = langCode,
                priority = priority
            )

            if (result.isSuccess) {
                val sentMsg = result.getOrThrow()
                val currentStatus = communicationManager.deliveryStatuses.value[sentMsg.messageId] ?: DeliveryStatus.PENDING
                _uiState.update {
                    it.copy(
                        lastSentMessageId = sentMsg.messageId,
                        deliveryStatus = currentStatus,
                        isTransmitting = false
                    )
                }
                clearRecording()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to transmit message."
                _uiState.update {
                    it.copy(
                        deliveryStatus = DeliveryStatus.FAILED,
                        errorMessage = errorMsg,
                        isTransmitting = false
                    )
                }
            }
        }
    }

    /**
     * Retries transmitting a failed message.
     */
    fun retrySendMessage(messageId: String? = null) {
        val targetId = messageId ?: _uiState.value.lastSentMessageId ?: return
        scope.launch {
            _uiState.update { it.copy(deliveryStatus = DeliveryStatus.SENDING) }
            val result = communicationManager.retryMessage(targetId)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        deliveryStatus = DeliveryStatus.FAILED,
                        errorMessage = result.exceptionOrNull()?.message ?: "Retry failed."
                    )
                }
            } else {
                val currentStatus = communicationManager.deliveryStatuses.value[targetId] ?: DeliveryStatus.DELIVERED
                _uiState.update { it.copy(deliveryStatus = currentStatus) }
            }
        }
    }

    /**
     * Dismisses an active error and returns to Idle.
     */
    fun dismissError() {
        _uiState.update {
            it.copy(recordingState = RecordingState.Idle, errorMessage = null)
        }
    }

    private fun startDurationTicker() {
        durationTickerJob?.cancel()
        durationTickerJob = scope.launch {
            while (true) {
                delay(100L)
                val elapsed = System.currentTimeMillis() - recordingStartTimeMs

                if (elapsed >= AudioConfiguration.MAX_RECORDING_DURATION_MS) {
                    // Maximum duration ceiling reached (30s) -> auto stop
                    stopRecording()
                    break
                }

                val currentBytes = AudioConfiguration.durationMsToBytes(elapsed)
                _uiState.update { state ->
                    val currentRec = state.recordingState
                    if (currentRec is RecordingState.Recording) {
                        state.copy(
                            recordingState = currentRec.copy(
                                durationMs = elapsed,
                                byteCount = currentBytes
                            )
                        )
                    } else {
                        state
                    }
                }
            }
        }
    }

    private fun stopDurationTicker() {
        durationTickerJob?.cancel()
        durationTickerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopDurationTicker()
        audioRecorder.release()
    }
}
