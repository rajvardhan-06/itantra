package com.itantra.app.domain.communication

import com.itantra.app.audio.AudioRecorder
import com.itantra.app.audio.MockAudioRecorder
import com.itantra.app.audio.RecordingState
import com.itantra.app.audio.vad.EnergyBasedVoiceActivityDetector
import com.itantra.app.audio.vad.VadState
import com.itantra.app.audio.vad.VoiceActivityDetector
import com.itantra.app.communication.CommunicationManager
import com.itantra.app.communication.DeliveryStatus
import com.itantra.app.communication.MessagePriority as CommPriority
import com.itantra.app.communication.MessageValidator
import com.itantra.app.communication.TextMessage
import com.itantra.app.data.repository.LanguageRepositoryImpl
import com.itantra.app.data.repository.MessageRepositoryImpl
import com.itantra.app.data.repository.SettingsRepositoryImpl
import com.itantra.app.domain.model.ItantraMessage
import com.itantra.app.domain.model.MessageDirection
import com.itantra.app.domain.model.MessagePriority as DomainPriority
import com.itantra.app.domain.model.MessageStatus
import com.itantra.app.domain.model.SupportedLanguage
import com.itantra.app.domain.repository.LanguageRepository
import com.itantra.app.domain.repository.MessageRepository
import com.itantra.app.domain.repository.SettingsRepository
import com.itantra.app.stt.MockSpeechToTextEngine
import com.itantra.app.stt.SpeechToTextEngine
import com.itantra.app.stt.SttLanguage
import com.itantra.app.stt.SttResult
import com.itantra.app.tts.MockTextToSpeechEngine
import com.itantra.app.tts.QueuedSpeechItem
import com.itantra.app.tts.SpeechPriority
import com.itantra.app.tts.SpeechQueueManager
import com.itantra.app.tts.TextToSpeechEngine
import com.itantra.app.tts.TtsConfiguration
import com.itantra.app.tts.TtsLanguage
import com.itantra.app.tts.TtsState
import com.itantra.app.tts.VoiceAvailability
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Central orchestrator managing the complete end-to-end communication lifecycle.
 *
 * Coordinates microphone recording, VAD speech gating, offline STT, text review/editing,
 * transmission over P2P transport, incoming message ingestion, deduplication, and TTS playback.
 */
class SpeechCommunicationCoordinator(
    private val audioRecorder: AudioRecorder = MockAudioRecorder(),
    private val voiceActivityDetector: VoiceActivityDetector = EnergyBasedVoiceActivityDetector(),
    private val sttEngine: SpeechToTextEngine = MockSpeechToTextEngine(),
    private val communicationManager: CommunicationManager = CommunicationManager(),
    private val ttsEngine: TextToSpeechEngine = MockTextToSpeechEngine(),
    private val messageRepository: MessageRepository = MessageRepositoryImpl(),
    private val settingsRepository: SettingsRepository = SettingsRepositoryImpl(),
    private val languageRepository: LanguageRepository = LanguageRepositoryImpl(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val externalScope: CoroutineScope? = null
) : SpeechMessagePipeline {

    private val coordinatorJob = SupervisorJob()
    private val scope = externalScope ?: CoroutineScope(coordinatorJob + dispatcher)
    private val sessionMutex = Mutex()

    private val _sessionState = MutableStateFlow<CommunicationSessionState>(CommunicationSessionState.Idle)
    override fun observeSessionState(): StateFlow<CommunicationSessionState> = _sessionState.asStateFlow()

    private val speechQueueManager = SpeechQueueManager(
        engine = ttsEngine,
        dispatcher = dispatcher,
        scope = scope
    )

    private var durationTickerJob: Job? = null
    private var amplitudeCollectorJob: Job? = null
    private var currentAudioLevel: Float = 0f
    private var recordingStartTimeMs: Long = 0L
    private var pendingTranscribedText: String = ""
    private var activeSessionLanguage: SupportedLanguage = SupportedLanguage.DEFAULT
    private val receivedMessageIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    init {
        setupAudioFrameListener()
        observeTtsEngine()
        observeDeliveryStatuses()
    }

    private fun setupAudioFrameListener() {
        audioRecorder.setAudioFrameListener { samples, length ->
            val currentState = _sessionState.value
            if (currentState is CommunicationSessionState.Listening || currentState is CommunicationSessionState.SpeechDetected) {
                val vadResult = voiceActivityDetector.processAudio(samples, 0, length)
                if (vadResult.state == VadState.SPEECH_DETECTED && currentState !is CommunicationSessionState.SpeechDetected) {
                    val elapsed = System.currentTimeMillis() - recordingStartTimeMs
                    _sessionState.value = CommunicationSessionState.SpeechDetected(durationMs = elapsed)
                } else if (vadResult.state == VadState.SILENCE_DETECTED) {
                    // Speech segment ended automatically by silence detection
                    scope.launch {
                        stopListeningAndTranscribe()
                    }
                }
            }
        }
    }

    private fun observeTtsEngine() {
        scope.launch {
            ttsEngine.observeState().collect { state ->
                when (state) {
                    is TtsState.Speaking -> {
                        _sessionState.value = CommunicationSessionState.Speaking(state.text, state.messageId)
                    }
                    is TtsState.Completed -> {
                        if (_sessionState.value is CommunicationSessionState.Speaking) {
                            _sessionState.value = CommunicationSessionState.Completed("Speech finished.")
                        }
                    }
                    is TtsState.LanguageUnavailable -> {
                        _sessionState.value = CommunicationSessionState.Error(
                            "Voice unavailable for ${state.language.displayName}",
                            canRetry = false
                        )
                    }
                    is TtsState.Error -> {
                        _sessionState.value = CommunicationSessionState.Error(
                            "Speech synthesis failed: ${state.error.message}",
                            canRetry = true
                        )
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun observeDeliveryStatuses() {
        scope.launch {
            communicationManager.deliveryStatuses.collect { statuses ->
                val current = _sessionState.value
                if (current is CommunicationSessionState.Sending) {
                    val status = statuses[current.messageId]
                    if (status == DeliveryStatus.SENT || status == DeliveryStatus.DELIVERED) {
                        _sessionState.value = CommunicationSessionState.Sent(current.messageId)
                    } else if (status == DeliveryStatus.FAILED) {
                        _sessionState.value = CommunicationSessionState.Error(
                            "Message delivery failed.",
                            canRetry = true
                        )
                    }
                }
            }
        }
    }

    override suspend fun startListening(): Result<Unit> = sessionMutex.withLock {
        // Prevent multiple simultaneous recording sessions
        val current = _sessionState.value
        if (current is CommunicationSessionState.Listening || current is CommunicationSessionState.SpeechDetected || current is CommunicationSessionState.Transcribing) {
            return Result.failure(IllegalStateException("A speech session is already in progress."))
        }

        activeSessionLanguage = languageRepository.observeActiveLanguage().first()
        voiceActivityDetector.reset()

        val startResult = audioRecorder.startRecording()
        if (startResult.isFailure) {
            val ex = startResult.exceptionOrNull()
            _sessionState.value = CommunicationSessionState.Error(
                "Microphone recording failed: ${ex?.message}",
                cause = ex
            )
            return Result.failure(ex ?: IllegalStateException("Microphone failed."))
        }

        recordingStartTimeMs = System.currentTimeMillis()
        _sessionState.value = CommunicationSessionState.Listening(durationMs = 0L, audioLevel = 0f)
        amplitudeCollectorJob?.cancel()
        amplitudeCollectorJob = scope.launch {
            audioRecorder.observeAmplitude().collect { currentAudioLevel = it }
        }
        startDurationTicker()

        Result.success(Unit)
    }

    override suspend fun stopListeningAndTranscribe(): Result<String> = sessionMutex.withLock {
        val current = _sessionState.value
        if (current !is CommunicationSessionState.Listening && current !is CommunicationSessionState.SpeechDetected) {
            return Result.failure(IllegalStateException("Cannot transcribe: Not currently listening."))
        }

        stopDurationTicker()
        val stopResult = audioRecorder.stopRecording()
        if (stopResult.isFailure) {
            val ex = stopResult.exceptionOrNull()
            _sessionState.value = CommunicationSessionState.Error("Failed to stop recording: ${ex?.message}")
            return Result.failure(ex ?: IllegalStateException("Recording stop error."))
        }

        val pcmData = stopResult.getOrThrow()

        if (pcmData.isEmpty()) {
            _sessionState.value = CommunicationSessionState.Error(
                "No audio recorded. Please hold Push-to-Talk while speaking.",
                canRetry = true
            )
            return Result.failure(IllegalArgumentException("Empty audio buffer."))
        }

        _sessionState.value = CommunicationSessionState.Transcribing(activeSessionLanguage)

        // Resolve STT language mapping and ensure initialized
        val sttLanguage = SttLanguage.fromCode(activeSessionLanguage.code) ?: SttLanguage.HINDI
        if (!sttEngine.isInitialized()) {
            val initRes = sttEngine.initialize(sttLanguage)
            if (initRes.isFailure) {
                val ex = initRes.exceptionOrNull()
                val errorMsg = ex?.message ?: "Model initialization failed."
                _sessionState.value = CommunicationSessionState.Error(errorMsg, canRetry = false, cause = ex)
                return Result.failure(ex ?: IllegalStateException(errorMsg))
            }
        }
        val transcribeResult = sttEngine.transcribe(pcmData)

        if (transcribeResult.isFailure) {
            val ex = transcribeResult.exceptionOrNull()
            val errorMsg = ex?.message ?: "Speech recognition failed."
            _sessionState.value = CommunicationSessionState.Error(errorMsg, canRetry = true, cause = ex)
            return Result.failure(ex ?: IllegalStateException(errorMsg))
        }

        val sttResult = transcribeResult.getOrThrow()
        val text = sttResult.text.trim()

        if (text.isBlank()) {
            _sessionState.value = CommunicationSessionState.Error(
                "No clear speech recognized.",
                canRetry = true
            )
            return Result.failure(IllegalArgumentException("Empty transcription."))
        }

        pendingTranscribedText = text
        val confPercent = ((sttResult.confidence ?: 0.9f) * 100).toInt()
        _sessionState.value = CommunicationSessionState.TextReview(
            transcribedText = text,
            language = activeSessionLanguage,
            confidence = confPercent,
            latencyMs = sttResult.durationMs
        )

        Result.success(text)
    }

    override suspend fun confirmAndTransmit(
        editedText: String,
        priority: DomainPriority
    ): Result<TextMessage> = sessionMutex.withLock {
        val current = _sessionState.value
        if (current !is CommunicationSessionState.TextReview) {
            return Result.failure(IllegalStateException("Cannot transmit: No active text review session."))
        }

        val text = editedText.trim()
        if (text.isBlank()) {
            _sessionState.value = CommunicationSessionState.Error("Cannot transmit empty text.", canRetry = true)
            return Result.failure(IllegalArgumentException("Empty message text."))
        }

        if (text.length > TtsConfiguration.MAX_TEXT_CHARACTERS) {
            _sessionState.value = CommunicationSessionState.Error(
                "Text length exceeds maximum allowed ${TtsConfiguration.MAX_TEXT_CHARACTERS} characters.",
                canRetry = true
            )
            return Result.failure(IllegalArgumentException("Text exceeds character limit."))
        }

        val commPriority = when (priority) {
            DomainPriority.URGENT -> CommPriority.ALERT
            DomainPriority.HIGH -> CommPriority.IMPORTANT
            DomainPriority.NORMAL, DomainPriority.LOW -> CommPriority.NORMAL
        }

        val sendResult = communicationManager.sendMessage(
            text = text,
            languageCode = activeSessionLanguage.code,
            priority = commPriority
        )

        if (sendResult.isFailure) {
            val ex = sendResult.exceptionOrNull()
            _sessionState.value = CommunicationSessionState.Error(
                "Message transmission failed: ${ex?.message}",
                canRetry = true,
                cause = ex
            )
            return Result.failure(ex ?: IllegalStateException("Transmission error."))
        }

        val sentMessage = sendResult.getOrThrow()
        _sessionState.value = CommunicationSessionState.Sending(sentMessage.messageId)

        Result.success(sentMessage)
    }

    override suspend fun cancelSession(reason: String) = sessionMutex.withLock {
        stopDurationTicker()
        amplitudeCollectorJob?.cancel()
        audioRecorder.stopRecording()
        _sessionState.value = CommunicationSessionState.Cancelled(reason)
    }

    fun resetSession() {
        _sessionState.value = CommunicationSessionState.Idle
    }

    override suspend fun receiveIncomingMessage(message: TextMessage): Result<Unit> = withContext(dispatcher) {
        // Validate protocol invariants
        val validation = MessageValidator.validate(message)
        if (!validation.isValid) {
            val reason = (validation as? com.itantra.app.communication.ValidationResult.Invalid)?.reason
                ?: "Invalid packet invariants."
            _sessionState.value = CommunicationSessionState.Error("Invalid incoming packet: $reason", canRetry = false)
            return@withContext Result.failure(IllegalArgumentException(reason))
        }

        if (!receivedMessageIds.add(message.messageId)) {
            // Duplicate message received; ignore gracefully
            return@withContext Result.success(Unit)
        }

        _sessionState.value = CommunicationSessionState.Receiving

        val domainLang = SupportedLanguage.fromCode(message.languageCode)
        val itantraMessage = ItantraMessage(
            id = message.messageId,
            content = message.text,
            language = domainLang,
            direction = MessageDirection.RECEIVED,
            status = MessageStatus.DELIVERED,
            priority = when (message.priority) {
                CommPriority.NORMAL -> DomainPriority.NORMAL
                CommPriority.IMPORTANT -> DomainPriority.HIGH
                CommPriority.ALERT -> DomainPriority.URGENT
            },
            timestampMs = message.timestamp,
            remoteDeviceId = message.senderId
        )

        messageRepository.addMessage(itantraMessage)
        _sessionState.value = CommunicationSessionState.MessageReceived(itantraMessage)

        // Check user auto-play preference
        val autoPlay = settingsRepository.observeTtsAutoPlay().firstOrNull() ?: false
        if (autoPlay) {
            val ttsLang = TtsLanguage.fromCode(message.languageCode) ?: TtsLanguage.ENGLISH
            val availability = ttsEngine.checkLanguageAvailability(ttsLang)
            if (availability == VoiceAvailability.AVAILABLE) {
                val speechPriority = when (message.priority) {
                    CommPriority.ALERT -> SpeechPriority.ALERT
                    CommPriority.IMPORTANT -> SpeechPriority.IMPORTANT
                    CommPriority.NORMAL -> SpeechPriority.NORMAL
                }
                val queuedItem = QueuedSpeechItem(
                    messageId = message.messageId,
                    text = message.text,
                    language = ttsLang,
                    priority = speechPriority,
                    timestampMs = message.timestamp
                )
                speechQueueManager.enqueue(queuedItem)
            } else {
                _sessionState.value = CommunicationSessionState.Error(
                    "Voice unavailable for ${ttsLang.displayName} (${ttsLang.code})",
                    canRetry = false
                )
            }
        }

        Result.success(Unit)
    }

    private fun startDurationTicker() {
        durationTickerJob?.cancel()
        durationTickerJob = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(100L)
                val current = _sessionState.value
                val elapsed = System.currentTimeMillis() - recordingStartTimeMs
                if (current is CommunicationSessionState.Listening) {
                    _sessionState.value = current.copy(
                        durationMs = elapsed,
                        audioLevel = currentAudioLevel
                    )
                } else if (current is CommunicationSessionState.SpeechDetected) {
                    _sessionState.value = current.copy(durationMs = elapsed)
                } else {
                    break
                }
            }
        }
    }

    private fun stopDurationTicker() {
        durationTickerJob?.cancel()
        durationTickerJob = null
    }

    override suspend fun release() {
        stopDurationTicker()
        amplitudeCollectorJob?.cancel()
        audioRecorder.release()
        sttEngine.release()
        communicationManager.release()
        speechQueueManager.release()
        coordinatorJob.cancel()
        _sessionState.value = CommunicationSessionState.Idle
    }

    fun getQueueManager(): SpeechQueueManager = speechQueueManager
}
