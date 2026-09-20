package com.itantra.app.tts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.itantra.app.data.repository.SettingsRepositoryImpl
import com.itantra.app.domain.model.ItantraMessage
import com.itantra.app.domain.model.MessagePriority
import com.itantra.app.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI State for the Text-to-Speech management and playback subsystem.
 */
data class TtsUiState(
    val ttsState: TtsState = TtsState.Idle,
    val queue: List<QueuedSpeechItem> = emptyList(),
    val currentItem: QueuedSpeechItem? = null,
    val speechRate: Float = TtsConfiguration.DEFAULT_SPEECH_RATE,
    val pitch: Float = TtsConfiguration.DEFAULT_PITCH,
    val autoPlayEnabled: Boolean = false,
    val lastError: TtsError? = null
)

/**
 * ViewModel orchestrating offline speech synthesis, priority queue dispatch,
 * and user preferences.
 */
class TtsViewModel(
    application: Application,
    val engine: TextToSpeechEngine = TtsEngineFactory.create(application),
    private val settingsRepository: SettingsRepository = SettingsRepositoryImpl(),
    private val externalScope: CoroutineScope? = null
) : AndroidViewModel(application) {

    /**
     * Testing constructor allowing injection of mock engine and repository without an Android Context.
     */
    constructor(
        engine: TextToSpeechEngine = MockTextToSpeechEngine(),
        settingsRepository: SettingsRepository = SettingsRepositoryImpl(),
        externalScope: CoroutineScope? = null
    ) : this(
        application = Application(),
        engine = engine,
        settingsRepository = settingsRepository,
        externalScope = externalScope
    )

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    val queueManager = SpeechQueueManager(
        engine = engine,
        scope = scope
    )

    private val _uiState = MutableStateFlow(TtsUiState())
    val uiState: StateFlow<TtsUiState> = _uiState.asStateFlow()

    init {
        observeEngineState()
        observeQueueState()
        observeSettings()
    }

    private fun observeEngineState() {
        scope.launch {
            engine.observeState().collect { state ->
                val error = (state as? TtsState.Error)?.error
                _uiState.value = _uiState.value.copy(
                    ttsState = state,
                    lastError = error ?: _uiState.value.lastError
                )
            }
        }
    }

    private fun observeQueueState() {
        scope.launch {
            queueManager.queueState.collect { queue ->
                _uiState.value = _uiState.value.copy(queue = queue)
            }
        }
        scope.launch {
            queueManager.currentItem.collect { current ->
                _uiState.value = _uiState.value.copy(currentItem = current)
            }
        }
    }

    private fun observeSettings() {
        scope.launch {
            settingsRepository.observeTtsSpeechRate().collect { rate ->
                engine.setSpeechRate(rate)
                _uiState.value = _uiState.value.copy(speechRate = rate)
            }
        }
        scope.launch {
            settingsRepository.observeTtsPitch().collect { pitch ->
                engine.setPitch(pitch)
                _uiState.value = _uiState.value.copy(pitch = pitch)
            }
        }
        scope.launch {
            settingsRepository.observeTtsAutoPlay().collect { autoPlay ->
                _uiState.value = _uiState.value.copy(autoPlayEnabled = autoPlay)
            }
        }
    }

    /**
     * Enqueues an [ItantraMessage] for offline speech playback.
     */
    fun speakMessage(message: ItantraMessage, replay: Boolean = false) {
        scope.launch {
            val ttsLanguage = TtsLanguage.fromCode(message.language.code) ?: TtsLanguage.ENGLISH
            val speechPriority = when (message.priority) {
                MessagePriority.URGENT -> SpeechPriority.ALERT
                MessagePriority.HIGH -> SpeechPriority.IMPORTANT
                MessagePriority.NORMAL, MessagePriority.LOW -> SpeechPriority.NORMAL
            }

            val item = QueuedSpeechItem(
                messageId = message.id,
                text = message.content,
                language = ttsLanguage,
                priority = speechPriority,
                timestampMs = message.timestampMs
            )

            if (replay) {
                queueManager.replay(item)
            } else {
                queueManager.enqueue(item)
            }
        }
    }

    /**
     * Synthesizes raw text with designated language and priority.
     */
    fun speakText(
        text: String,
        language: TtsLanguage = TtsLanguage.ENGLISH,
        priority: SpeechPriority = SpeechPriority.NORMAL,
        messageId: String = "manual-${System.currentTimeMillis()}"
    ) {
        scope.launch {
            val item = QueuedSpeechItem(
                messageId = messageId,
                text = text,
                language = language,
                priority = priority
            )
            queueManager.enqueue(item)
        }
    }

    /**
     * Stops the currently playing speech utterance.
     */
    fun stop() {
        scope.launch {
            queueManager.stopCurrent()
        }
    }

    /**
     * Clears all pending speech items from the queue and halts audio.
     */
    fun clearQueue() {
        scope.launch {
            queueManager.clearQueue()
        }
    }

    /**
     * Replays a message with prioritized urgency.
     */
    fun replay(message: ItantraMessage) {
        speakMessage(message, replay = true)
    }

    /**
     * Updates speech synthesis rate preference.
     */
    fun setSpeechRate(rate: Float) {
        scope.launch {
            val valid = TtsConfiguration.validateSpeechRate(rate)
            if (valid.isSuccess) {
                settingsRepository.setTtsSpeechRate(rate)
            }
        }
    }

    /**
     * Updates voice pitch multiplier preference.
     */
    fun setPitch(pitch: Float) {
        scope.launch {
            val valid = TtsConfiguration.validatePitch(pitch)
            if (valid.isSuccess) {
                settingsRepository.setTtsPitch(pitch)
            }
        }
    }

    /**
     * Toggles automatic playback for received incoming messages.
     */
    fun setAutoPlay(enabled: Boolean) {
        scope.launch {
            settingsRepository.setTtsAutoPlay(enabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
        scope.launch {
            queueManager.release()
        }
    }
}
