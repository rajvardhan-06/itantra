package com.itantra.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itantra.app.data.repository.MessageRepositoryImpl
import com.itantra.app.domain.model.ItantraMessage
import com.itantra.app.domain.model.MessagePriority
import com.itantra.app.domain.repository.MessageRepository
import com.itantra.app.tts.MockTextToSpeechEngine
import com.itantra.app.tts.QueuedSpeechItem
import com.itantra.app.tts.SpeechPriority
import com.itantra.app.tts.SpeechQueueManager
import com.itantra.app.tts.TextToSpeechEngine
import com.itantra.app.tts.TtsLanguage
import com.itantra.app.tts.TtsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val messages: List<ItantraMessage> = emptyList(),
    val isLoading: Boolean = true,
    val currentlyPlayingMessageId: String? = null,
    val ttsState: TtsState = TtsState.Idle
)

class HistoryViewModel(
    private val messageRepository: MessageRepository = MessageRepositoryImpl(),
    val ttsEngine: TextToSpeechEngine = MockTextToSpeechEngine(),
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val queueManager = SpeechQueueManager(
        engine = ttsEngine,
        scope = scope
    )

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            messageRepository.observeMessages().collect { messages ->
                _uiState.update { it.copy(messages = messages, isLoading = false) }
            }
        }
        scope.launch {
            queueManager.currentItem.collect { current ->
                _uiState.update { it.copy(currentlyPlayingMessageId = current?.messageId) }
            }
        }
        scope.launch {
            ttsEngine.observeState().collect { state ->
                _uiState.update { it.copy(ttsState = state) }
            }
        }
    }

    fun speakMessage(message: ItantraMessage) {
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
            queueManager.enqueue(item)
        }
    }

    fun stopSpeech() {
        scope.launch {
            queueManager.stopCurrent()
        }
    }

    fun replayMessage(message: ItantraMessage) {
        scope.launch {
            val ttsLanguage = TtsLanguage.fromCode(message.language.code) ?: TtsLanguage.ENGLISH
            val item = QueuedSpeechItem(
                messageId = message.id,
                text = message.content,
                language = ttsLanguage,
                priority = SpeechPriority.IMPORTANT,
                timestampMs = System.currentTimeMillis()
            )
            queueManager.replay(item)
        }
    }

    fun deleteMessage(messageId: String) {
        scope.launch {
            messageRepository.deleteMessage(messageId)
        }
    }

    fun clearAll() {
        scope.launch {
            queueManager.clearQueue()
            messageRepository.clearAll()
        }
    }

    override fun onCleared() {
        super.onCleared()
        scope.launch {
            queueManager.release()
        }
    }
}
