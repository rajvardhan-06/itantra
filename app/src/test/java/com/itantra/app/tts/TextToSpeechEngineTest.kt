package com.itantra.app.tts

import com.itantra.app.data.repository.SettingsRepositoryImpl
import com.itantra.app.domain.model.ItantraMessage
import com.itantra.app.domain.model.MessageDirection
import com.itantra.app.domain.model.MessagePriority
import com.itantra.app.domain.model.MessageStatus
import com.itantra.app.domain.model.SupportedLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive automated unit test suite for Phase 6: Offline Multilingual Text-to-Speech (TTS).
 *
 * Verifies all 22 required scenarios including engine initialization, language checks,
 * voice data availability, bounds, multilingual scripts, speech queue ordering with priority,
 * replay, audio focus, and ViewModel integration.
 */
class TextToSpeechEngineTest {

    private val testDispatcher = Dispatchers.Unconfined

    // 1. TTS engine initialization
    @Test
    fun testTtsEngineInitialization() = runBlocking(testDispatcher) {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher)
        val result = engine.initialize(TtsLanguage.ENGLISH)

        assertTrue(result.isSuccess)
        assertTrue(engine.isInitialized())
        assertEquals(TtsLanguage.ENGLISH, engine.getCurrentLanguage())
        assertTrue(engine.observeState().value is TtsState.Idle)
    }

    // 2. Initialization failure
    @Test
    fun testInitializationFailure() = runBlocking(testDispatcher) {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher).apply {
            simulateInitFailure = true
        }
        val result = engine.initialize(TtsLanguage.HINDI)

        assertFalse(result.isSuccess)
        assertFalse(engine.isInitialized())
        assertTrue(engine.observeState().value is TtsState.Error)
    }

    // 3. Supported language check
    @Test
    fun testSupportedLanguageCheck() {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher)
        assertEquals(VoiceAvailability.AVAILABLE, engine.checkLanguageAvailability(TtsLanguage.ENGLISH))
        assertEquals(VoiceAvailability.AVAILABLE, engine.checkLanguageAvailability(TtsLanguage.HINDI))
        assertEquals(VoiceAvailability.AVAILABLE, engine.checkLanguageAvailability(TtsLanguage.TELUGU))
    }

    // 4. Unsupported language rejection
    @Test
    fun testUnsupportedLanguageRejection() = runBlocking(testDispatcher) {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher).apply {
            setLanguageAvailability(TtsLanguage.ODIA, VoiceAvailability.NOT_SUPPORTED)
        }
        val result = engine.initialize(TtsLanguage.ODIA)

        assertFalse(result.isSuccess)
        assertTrue(engine.observeState().value is TtsState.LanguageUnavailable)
    }

    // 5. Missing voice data handling
    @Test
    fun testMissingVoiceDataHandling() = runBlocking(testDispatcher) {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher).apply {
            setLanguageAvailability(TtsLanguage.GUJARATI, VoiceAvailability.MISSING_DATA)
        }
        val result = engine.initialize(TtsLanguage.GUJARATI)

        assertFalse(result.isSuccess)
        val state = engine.observeState().value
        assertTrue(state is TtsState.LanguageUnavailable)
        assertEquals(TtsLanguage.GUJARATI, (state as TtsState.LanguageUnavailable).language)
    }

    // 6. Empty text rejection
    @Test
    fun testEmptyTextRejection() = runBlocking(testDispatcher) {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher)
        engine.initialize(TtsLanguage.ENGLISH)

        val result = engine.speak("   ")
        assertFalse(result.isSuccess)
        assertTrue(engine.observeState().value is TtsState.Error)
        assertEquals(TtsError.EmptyText, (engine.observeState().value as TtsState.Error).error)
    }

    // 7. Valid text synthesis
    @Test
    fun testValidTextSynthesis() = runBlocking(testDispatcher) {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher, autoPlayComplete = false)
        engine.initialize(TtsLanguage.ENGLISH)

        val result = engine.speak("Hello iTantra, offline transceiver is ready.")
        assertTrue(result.isSuccess)
        assertTrue(engine.observeState().value is TtsState.Speaking)
        assertEquals(1, engine.spokenUtterances.size)
        assertEquals("Hello iTantra, offline transceiver is ready.", engine.spokenUtterances.first().text)
    }

    // 8. Multilingual Unicode text (Hindi)
    @Test
    fun testMultilingualUnicodeText() = runBlocking(testDispatcher) {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher, autoPlayComplete = false)
        engine.initialize(TtsLanguage.HINDI)

        val hindiText = "नमस्ते दुनिया! यह एक ऑफ़लाइन ट्रांससीव्हर है।"
        val result = engine.speak(hindiText)

        assertTrue(result.isSuccess)
        assertEquals(hindiText, engine.spokenUtterances.first().text)
        assertEquals(TtsLanguage.HINDI, engine.spokenUtterances.first().language)
    }

    // 9. Telugu script support
    @Test
    fun testTeluguScriptSupport() = runBlocking(testDispatcher) {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher, autoPlayComplete = false)
        engine.initialize(TtsLanguage.TELUGU)

        val teluguText = "నమస్కారం ఐతంత్ర! ఇది ఆఫ్‌లైన్ వాయిస్ పరీక్ష."
        val result = engine.speak(teluguText)

        assertTrue(result.isSuccess)
        assertEquals(teluguText, engine.spokenUtterances.first().text)
        assertEquals(TtsLanguage.TELUGU, engine.spokenUtterances.first().language)
    }

    // 10. Long text exceeding 1000 characters rejection
    @Test
    fun testTextExceeding1000CharsRejection() = runBlocking(testDispatcher) {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher)
        engine.initialize(TtsLanguage.ENGLISH)

        val excessivelyLongText = "A".repeat(1001)
        val result = engine.speak(excessivelyLongText)

        assertFalse(result.isSuccess)
        val state = engine.observeState().value
        assertTrue(state is TtsState.Error)
        assertTrue((state as TtsState.Error).error is TtsError.TextTooLong)
    }

    // 11. Speech rate validation
    @Test
    fun testSpeechRateValidation() {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher)

        assertTrue(engine.setSpeechRate(0.5f).isSuccess)
        assertTrue(engine.setSpeechRate(1.0f).isSuccess)
        assertTrue(engine.setSpeechRate(2.0f).isSuccess)

        assertFalse(engine.setSpeechRate(0.2f).isSuccess)
        assertFalse(engine.setSpeechRate(3.5f).isSuccess)
        assertFalse(engine.setSpeechRate(Float.NaN).isSuccess)
    }

    // 12. Pitch validation
    @Test
    fun testPitchValidation() {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher)

        assertTrue(engine.setPitch(0.5f).isSuccess)
        assertTrue(engine.setPitch(1.0f).isSuccess)
        assertTrue(engine.setPitch(2.0f).isSuccess)

        assertFalse(engine.setPitch(0.1f).isSuccess)
        assertFalse(engine.setPitch(2.5f).isSuccess)
        assertFalse(engine.setPitch(Float.POSITIVE_INFINITY).isSuccess)
    }

    // 13. Queue insertion
    @Test
    fun testQueueInsertion() = runBlocking(testDispatcher) {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher, autoPlayComplete = false)
        val queueScope = CoroutineScope(SupervisorJob() + testDispatcher)
        val queueManager = SpeechQueueManager(engine = engine, scope = queueScope)

        try {
            val item1 = QueuedSpeechItem("msg-1", "First message", TtsLanguage.ENGLISH)
            val item2 = QueuedSpeechItem("msg-2", "Second message", TtsLanguage.ENGLISH)

            queueManager.enqueue(item1)
            assertEquals("msg-1", queueManager.currentItem.value?.messageId)

            queueManager.enqueue(item2)
            assertEquals(1, queueManager.getPendingCount())
            assertEquals("msg-2", queueManager.queueState.value.first().messageId)
        } finally {
            queueScope.cancel()
        }
    }

    // 14. Queue removal
    @Test
    fun testQueueRemoval() = runBlocking(testDispatcher) {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher, autoPlayComplete = true, speakDelayMs = 10L)
        val queueScope = CoroutineScope(SupervisorJob() + testDispatcher)
        val queueManager = SpeechQueueManager(engine = engine, scope = queueScope)

        try {
            val item1 = QueuedSpeechItem("msg-1", "First message", TtsLanguage.ENGLISH)
            queueManager.enqueue(item1)

            var attempts = 0
            while (queueManager.currentItem.value != null && attempts < 20) {
                delay(20)
                attempts++
            }
            assertEquals(0, queueManager.getPendingCount())
        } finally {
            queueScope.cancel()
        }
    }

    // 15. Queue ordering with priority
    @Test
    fun testQueueOrderingWithPriority() = runBlocking(testDispatcher) {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher, autoPlayComplete = false)
        val queueScope = CoroutineScope(SupervisorJob() + testDispatcher)
        val queueManager = SpeechQueueManager(engine = engine, scope = queueScope)

        try {
            // First item starts speaking immediately
            val initialItem = QueuedSpeechItem("msg-init", "Currently speaking", TtsLanguage.ENGLISH, SpeechPriority.NORMAL)
            queueManager.enqueue(initialItem)
            assertEquals("msg-init", queueManager.currentItem.value?.messageId)

            // Enqueue NORMAL, then IMPORTANT, then ALERT
            val normalItem = QueuedSpeechItem("msg-norm", "Normal priority", TtsLanguage.ENGLISH, SpeechPriority.NORMAL)
            val importantItem = QueuedSpeechItem("msg-imp", "Important priority", TtsLanguage.ENGLISH, SpeechPriority.IMPORTANT)

            queueManager.enqueue(normalItem, preemptIfAlert = false)
            queueManager.enqueue(importantItem, preemptIfAlert = false)

            val pending = queueManager.queueState.value
            assertEquals(2, pending.size)
            // IMPORTANT should be first before NORMAL
            assertEquals("msg-imp", pending[0].messageId)
            assertEquals("msg-norm", pending[1].messageId)

            // Now enqueue ALERT: it should preempt or jump to top of pending queue
            val alertItem = QueuedSpeechItem("msg-alert", "ALERT PRIORITY", TtsLanguage.ENGLISH, SpeechPriority.ALERT)
            queueManager.enqueue(alertItem, preemptIfAlert = true)

            // Since alert preempts lower-priority speaking item, alert is now playing!
            assertEquals("msg-alert", queueManager.currentItem.value?.messageId)
        } finally {
            queueScope.cancel()
        }
    }

    // 16. Stop speech
    @Test
    fun testStopSpeech() = runBlocking(testDispatcher) {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher, autoPlayComplete = false)
        val queueScope = CoroutineScope(SupervisorJob() + testDispatcher)
        val queueManager = SpeechQueueManager(engine = engine, scope = queueScope)

        try {
            val item = QueuedSpeechItem("msg-16", "Will be stopped", TtsLanguage.ENGLISH)
            queueManager.enqueue(item)

            var attempts = 0
            while (engine.observeState().value !is TtsState.Speaking && attempts < 20) {
                delay(10)
                attempts++
            }
            assertTrue(engine.observeState().value is TtsState.Speaking)

            queueManager.stopCurrent()
            attempts = 0
            while (engine.observeState().value !is TtsState.Idle && attempts < 20) {
                delay(10)
                attempts++
            }
            assertTrue(engine.observeState().value is TtsState.Idle)
            assertEquals(null, queueManager.currentItem.value)
        } finally {
            queueScope.cancel()
        }
    }

    // 17. Replay message
    @Test
    fun testReplayMessage() = runBlocking(testDispatcher) {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher, autoPlayComplete = false)
        val queueScope = CoroutineScope(SupervisorJob() + testDispatcher)
        val queueManager = SpeechQueueManager(engine = engine, scope = queueScope)

        try {
            val item = QueuedSpeechItem("msg-17", "Replay me", TtsLanguage.ENGLISH)
            queueManager.replay(item)

            var attempts = 0
            while (engine.observeState().value !is TtsState.Speaking && attempts < 20) {
                delay(10)
                attempts++
            }
            assertEquals("msg-17", queueManager.currentItem.value?.messageId)
            assertTrue(engine.observeState().value is TtsState.Speaking)
        } finally {
            queueScope.cancel()
        }
    }

    // 18. Cancellation
    @Test
    fun testCancellation() = runBlocking(testDispatcher) {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher, autoPlayComplete = false)
        val queueScope = CoroutineScope(SupervisorJob() + testDispatcher)
        val queueManager = SpeechQueueManager(engine = engine, scope = queueScope)

        try {
            queueManager.enqueue(QueuedSpeechItem("msg-a", "A", TtsLanguage.ENGLISH))
            queueManager.enqueue(QueuedSpeechItem("msg-b", "B", TtsLanguage.ENGLISH))
            queueManager.enqueue(QueuedSpeechItem("msg-c", "C", TtsLanguage.ENGLISH))

            assertEquals(2, queueManager.getPendingCount())

            queueManager.clearQueue()
            assertEquals(0, queueManager.getPendingCount())
            assertEquals(null, queueManager.currentItem.value)
            assertTrue(engine.observeState().value is TtsState.Idle)
        } finally {
            queueScope.cancel()
        }
    }

    // 19. Language switching
    @Test
    fun testLanguageSwitching() = runBlocking(testDispatcher) {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher)

        engine.initialize(TtsLanguage.ENGLISH)
        assertEquals(TtsLanguage.ENGLISH, engine.getCurrentLanguage())

        engine.initialize(TtsLanguage.HINDI)
        assertEquals(TtsLanguage.HINDI, engine.getCurrentLanguage())

        engine.initialize(TtsLanguage.TELUGU)
        assertEquals(TtsLanguage.TELUGU, engine.getCurrentLanguage())
    }

    // 20. ViewModel state transitions
    @Test
    fun testViewModelStateTransitions() = runBlocking(testDispatcher) {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher, autoPlayComplete = false)
        val settingsRepo = SettingsRepositoryImpl()
        val vmScope = CoroutineScope(SupervisorJob() + testDispatcher)

        try {
            val viewModel = TtsViewModel(
                engine = engine,
                settingsRepository = settingsRepo,
                externalScope = vmScope
            )

            // 1. Initial state
            assertEquals(TtsState.Idle, viewModel.uiState.value.ttsState)
            assertEquals(1.0f, viewModel.uiState.value.speechRate)
            assertEquals(1.0f, viewModel.uiState.value.pitch)
            assertFalse(viewModel.uiState.value.autoPlayEnabled)

            // 2. Adjust settings
            viewModel.setSpeechRate(1.5f)
            delay(10)
            assertEquals(1.5f, viewModel.uiState.value.speechRate)

            viewModel.setPitch(1.2f)
            delay(10)
            assertEquals(1.2f, viewModel.uiState.value.pitch)

            viewModel.setAutoPlay(true)
            delay(10)
            assertTrue(viewModel.uiState.value.autoPlayEnabled)

            // 3. Speak a received message
            val testMsg = ItantraMessage(
                id = "msg-vm-20",
                content = "Testing ViewModel TTS speech flow",
                language = SupportedLanguage.ENGLISH,
                direction = MessageDirection.RECEIVED,
                status = MessageStatus.DELIVERED,
                priority = MessagePriority.URGENT,
                timestampMs = System.currentTimeMillis()
            )
            viewModel.speakMessage(testMsg)
            delay(10)

            assertTrue(viewModel.uiState.value.ttsState is TtsState.Speaking)
            assertEquals("msg-vm-20", viewModel.uiState.value.currentItem?.messageId)

            // 4. Stop speech
            viewModel.stop()
            delay(10)
            assertTrue(viewModel.uiState.value.ttsState is TtsState.Idle)
            assertEquals(null, viewModel.uiState.value.currentItem)
        } finally {
            vmScope.cancel()
        }
    }

    // 21. Resource release
    @Test
    fun testResourceRelease() = runBlocking(testDispatcher) {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher)
        engine.initialize(TtsLanguage.ENGLISH)
        assertTrue(engine.isInitialized())

        engine.release()
        assertFalse(engine.isInitialized())
        assertEquals(null, engine.getCurrentLanguage())
        assertTrue(engine.observeState().value is TtsState.Idle)
    }

    // 22. Audio focus handling
    @Test
    fun testAudioFocusHandling() = runBlocking(testDispatcher) {
        val engine = MockTextToSpeechEngine(dispatcher = testDispatcher, autoPlayComplete = false)
        engine.initialize(TtsLanguage.ENGLISH)

        engine.speak("Testing audio focus lifecycle.")
        assertTrue(engine.hasAudioFocus)

        // Simulate external audio focus loss (e.g. incoming call or mic activation)
        engine.simulateAudioFocusLoss()
        assertFalse(engine.hasAudioFocus)
        assertTrue(engine.observeState().value is TtsState.Error)
    }
}
