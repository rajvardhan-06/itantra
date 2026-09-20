package com.itantra.app.tts

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Priority weighting for queue ordering.
 */
enum class SpeechPriority(val weight: Int) {
    NORMAL(1),
    IMPORTANT(2),
    ALERT(3)
}

/**
 * Represents an individual text message buffered for offline speech synthesis.
 */
data class QueuedSpeechItem(
    val messageId: String,
    val text: String,
    val language: TtsLanguage,
    val priority: SpeechPriority = SpeechPriority.NORMAL,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Manages sequential, priority-ordered speech playback.
 *
 * Ensures received messages are spoken sequentially without overlapping audio,
 * prioritizes ALERT and IMPORTANT announcements, enforces capacity limits,
 * and handles cancellation, preemption, and replays.
 */
class SpeechQueueManager(
    private val engine: TextToSpeechEngine,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
) {

    private val mutex = Mutex()
    private val pendingQueue = mutableListOf<QueuedSpeechItem>()
    private val recentMessageIds = ConcurrentHashMap<String, Long>()

    private val _queueState = MutableStateFlow<List<QueuedSpeechItem>>(emptyList())
    val queueState: StateFlow<List<QueuedSpeechItem>> = _queueState.asStateFlow()

    private val _currentItem = MutableStateFlow<QueuedSpeechItem?>(null)
    val currentItem: StateFlow<QueuedSpeechItem?> = _currentItem.asStateFlow()

    private var playbackJob: Job? = null
    private var isPlaying: Boolean = false

    init {
        // Monitor engine state to advance queue on completion
        scope.launch {
            engine.observeState().collect { state ->
                if (state is TtsState.Completed || state is TtsState.Error || state is TtsState.LanguageUnavailable) {
                    onSpeechFinished()
                }
            }
        }
    }

    /**
     * Enqueues a message for speech synthesis.
     *
     * @param item The message item to buffer.
     * @param preemptIfAlert When true, incoming [SpeechPriority.ALERT] items will immediately
     *                       stop current lower-priority playback and speak.
     */
    suspend fun enqueue(
        item: QueuedSpeechItem,
        preemptIfAlert: Boolean = true
    ): Result<Unit> = mutex.withLock {
        // 1. Check queue bounds
        if (pendingQueue.size >= TtsConfiguration.MAX_QUEUE_CAPACITY) {
            return Result.failure(IllegalStateException(TtsError.QueueOverflow.message))
        }

        // 2. Prevent duplicate entries already buffered
        if (pendingQueue.any { it.messageId == item.messageId } || _currentItem.value?.messageId == item.messageId) {
            return Result.success(Unit) // Deduplicated, already in flight or queued
        }

        // 3. Insert and sort by priority (highest weight first, then FIFO timestamp)
        pendingQueue.add(item)
        sortQueue()
        updateQueueFlow()

        // 4. Preemption check for ALERT messages
        if (preemptIfAlert && item.priority == SpeechPriority.ALERT) {
            val current = _currentItem.value
            if (current != null && current.priority < SpeechPriority.ALERT && isPlaying) {
                // Preempt lower-priority speech
                engine.stop()
                // Return current item to head of pending queue
                pendingQueue.add(0, current)
                sortQueue()
                updateQueueFlow()
                _currentItem.value = null
                isPlaying = false
            }
        }

        // 5. Trigger playback if idle
        if (!isPlaying && _currentItem.value == null) {
            processNextLocked()
        }

        Result.success(Unit)
    }

    /**
     * Replays a specific message immediately or enqueues it with high priority.
     */
    suspend fun replay(item: QueuedSpeechItem): Result<Unit> = mutex.withLock {
        // Enqueue as IMPORTANT so it jumps before ordinary backlog
        val replayItem = item.copy(
            priority = if (item.priority == SpeechPriority.ALERT) SpeechPriority.ALERT else SpeechPriority.IMPORTANT,
            timestampMs = System.currentTimeMillis()
        )
        pendingQueue.add(0, replayItem)
        sortQueue()
        updateQueueFlow()

        if (!isPlaying && _currentItem.value == null) {
            processNextLocked()
        }
        Result.success(Unit)
    }

    /**
     * Immediately halts the currently playing speech utterance.
     */
    suspend fun stopCurrent(): Result<Unit> = mutex.withLock {
        playbackJob?.cancel()
        playbackJob = null
        isPlaying = false
        _currentItem.value = null
        engine.stop()
    }

    /**
     * Clears all pending speech items and terminates current playback.
     */
    suspend fun clearQueue(): Result<Unit> = mutex.withLock {
        playbackJob?.cancel()
        playbackJob = null
        pendingQueue.clear()
        updateQueueFlow()
        isPlaying = false
        _currentItem.value = null
        engine.stop()
    }

    /**
     * Skips the current playing message and speaks the next item in the queue.
     */
    suspend fun skipNext(): Result<Unit> = mutex.withLock {
        engine.stop()
        isPlaying = false
        _currentItem.value = null
        processNextLocked()
        Result.success(Unit)
    }

    private suspend fun processNextLocked() {
        if (pendingQueue.isEmpty()) {
            _currentItem.value = null
            isPlaying = false
            return
        }

        val nextItem = pendingQueue.removeAt(0)
        updateQueueFlow()
        _currentItem.value = nextItem
        isPlaying = true

        playbackJob = scope.launch {
            // Ensure engine is initialized for target language
            if (engine.getCurrentLanguage() != nextItem.language) {
                val initResult = engine.initialize(nextItem.language)
                if (initResult.isFailure) {
                    onSpeechFinished()
                    return@launch
                }
            }

            val speakResult = engine.speak(
                text = nextItem.text,
                utteranceId = nextItem.messageId
            )
            if (speakResult.isFailure) {
                onSpeechFinished()
            }
        }
    }

    private fun onSpeechFinished() {
        scope.launch {
            mutex.withLock {
                isPlaying = false
                _currentItem.value = null
                processNextLocked()
            }
        }
    }

    private fun sortQueue() {
        pendingQueue.sortWith(
            compareByDescending<QueuedSpeechItem> { it.priority.weight }
                .thenBy { it.timestampMs }
        )
    }

    private fun updateQueueFlow() {
        _queueState.value = pendingQueue.toList()
    }

    fun getPendingCount(): Int = pendingQueue.size

    suspend fun release() {
        clearQueue()
        engine.release()
    }
}
