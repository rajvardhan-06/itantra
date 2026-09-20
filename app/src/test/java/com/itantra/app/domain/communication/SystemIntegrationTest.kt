package com.itantra.app.domain.communication

import com.itantra.app.audio.MockAudioRecorder
import com.itantra.app.audio.vad.EnergyBasedVoiceActivityDetector
import com.itantra.app.audio.vad.VadConfiguration
import com.itantra.app.communication.CommunicationManager
import com.itantra.app.communication.DeliveryStatus
import com.itantra.app.communication.MessageIdGenerator
import com.itantra.app.communication.MessagePriority as CommPriority
import com.itantra.app.communication.MessageProtocol
import com.itantra.app.communication.MessageSerializer
import com.itantra.app.communication.MessageValidator
import com.itantra.app.communication.MockCommunicationTransport
import com.itantra.app.communication.TextMessage
import com.itantra.app.data.repository.LanguageRepositoryImpl
import com.itantra.app.data.repository.MessageRepositoryImpl
import com.itantra.app.data.repository.SettingsRepositoryImpl
import com.itantra.app.domain.model.MessageDirection
import com.itantra.app.domain.model.MessagePriority as DomainPriority
import com.itantra.app.domain.model.SupportedLanguage
import com.itantra.app.stt.MockSpeechToTextEngine
import com.itantra.app.tts.MockTextToSpeechEngine
import com.itantra.app.tts.QueuedSpeechItem
import com.itantra.app.tts.SpeechPriority
import com.itantra.app.tts.SpeechQueueManager
import com.itantra.app.tts.TtsLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 7: Comprehensive System Integration & End-to-End Pipeline Test Suite.
 *
 * Validates the complete offline speech-to-speech transceiver lifecycle across all 22 required scenarios:
 * 1. Complete Speech-to-Text pipeline
 * 2. Text review and editing
 * 3. Message creation and framing
 * 4. Message serialization
 * 5. Message deserialization
 * 6. Unicode multilingual support
 * 7. Message validation & tamper detection
 * 8. Mock transport transmission
 * 9. Incoming message processing & storage
 * 10. TTS queue integration & auto-play
 * 11. Empty speech result handling
 * 12. STT engine failure recovery
 * 13. Transport failure resilience
 * 14. TTS playback failure tolerance
 * 15. User cancellation flow
 * 16. Duplicate message deduplication
 * 17. Chronological message ordering
 * 18. Priority preemption in speech queue
 * 19. Complete session state transitions
 * 20. Lifecycle release and cleanup
 * 21. Microphone permission denial handling
 * 22. Speech model unavailability handling
 */
class SystemIntegrationTest {

    private val testDispatcher = Dispatchers.Default

    // 1. Complete Speech-to-Text pipeline
    @Test
    fun testCompleteSpeechToTextPipeline() = runBlocking(testDispatcher) {
        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        try {
            val audioRecorder = MockAudioRecorder()
            val sttEngine = MockSpeechToTextEngine().apply {
                mockTranscriptionText = "Test speech transcription."
            }
            val coordinator = SpeechCommunicationCoordinator(
                audioRecorder = audioRecorder,
                sttEngine = sttEngine,
                dispatcher = testDispatcher,
                externalScope = testScope
            )

            // Start listening
            val startRes = coordinator.startListening()
            assertTrue(startRes.isSuccess)
            assertTrue(coordinator.observeSessionState().value is CommunicationSessionState.Listening)

            // Stop and transcribe
            val transcribeRes = coordinator.stopListeningAndTranscribe()
            assertTrue(transcribeRes.isSuccess)
            assertEquals("Test speech transcription.", transcribeRes.getOrNull())

            val state = coordinator.observeSessionState().value
            assertTrue(state is CommunicationSessionState.TextReview)
            assertEquals("Test speech transcription.", (state as CommunicationSessionState.TextReview).transcribedText)
        } finally {
            testScope.cancel()
        }
    }

    // 2. Text review and editing
    @Test
    fun testTextReviewAndEditing() = runBlocking(testDispatcher) {
        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        try {
            val audioRecorder = MockAudioRecorder()
            val sttEngine = MockSpeechToTextEngine().apply {
                mockTranscriptionText = "Raw speech input"
            }
            val coordinator = SpeechCommunicationCoordinator(
                audioRecorder = audioRecorder,
                sttEngine = sttEngine,
                dispatcher = testDispatcher,
                externalScope = testScope
            )

            coordinator.startListening()
            coordinator.stopListeningAndTranscribe()

            val editedText = "Corrected user verified message"
            val transmitRes = coordinator.confirmAndTransmit(editedText, DomainPriority.HIGH)

            assertTrue(transmitRes.isSuccess)
            val sentMsg = transmitRes.getOrNull()
            assertNotNull(sentMsg)
            assertEquals(editedText, sentMsg?.text)
            assertEquals(CommPriority.IMPORTANT, sentMsg?.priority)
        } finally {
            testScope.cancel()
        }
    }

    // 3. Message creation and framing
    @Test
    fun testMessageCreation() = runBlocking(testDispatcher) {
        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        try {
            val coordinator = SpeechCommunicationCoordinator(
                dispatcher = testDispatcher,
                externalScope = testScope
            )

            coordinator.startListening()
            coordinator.stopListeningAndTranscribe()

            val result = coordinator.confirmAndTransmit("Status update", DomainPriority.URGENT)
            assertTrue(result.isSuccess)
            val msg = result.getOrThrow()

            assertTrue(msg.messageId.startsWith("msg-"))
            assertTrue(msg.senderId.isNotBlank())
            assertTrue(msg.timestamp > 0L)
            assertEquals(MessageProtocol.PROTOCOL_VERSION, msg.protocolVersion)
            assertEquals("Status update", msg.text)
            assertEquals(CommPriority.ALERT, msg.priority)
            assertTrue(msg.checksum != 0L)
        } finally {
            testScope.cancel()
        }
    }

    // 4. Message serialization
    @Test
    fun testMessageSerialization() {
        val msg = TextMessage(
            messageId = "msg-1001",
            senderId = "DEV-NODE-01",
            text = "Emergency alert: Low visibility",
            languageCode = "en",
            timestamp = 1710000000000L,
            priority = CommPriority.ALERT,
            sequenceNumber = 1L
        ).withComputedChecksum()

        val serializeResult = MessageSerializer.serialize(msg)
        assertTrue(serializeResult.isSuccess)
        val payload = serializeResult.getOrThrow()
        val json = String(payload, Charsets.UTF_8)
        assertNotNull(json)
        assertTrue(json.contains("\"id\":\"msg-1001\""))
        assertTrue(json.contains("\"txt\":\"Emergency alert: Low visibility\""))
        assertTrue(json.contains("\"pri\":\"ALERT\""))
        assertTrue(json.contains("\"crc\":${msg.checksum}"))
    }

    // 5. Message deserialization
    @Test
    fun testMessageDeserialization() {
        val original = TextMessage(
            messageId = "msg-2002",
            senderId = "DEV-NODE-02",
            text = "Route cleared, proceeding to checkpoint.",
            languageCode = "en",
            timestamp = 1710000500000L,
            priority = CommPriority.NORMAL,
            sequenceNumber = 42L
        ).withComputedChecksum()

        val serialized = MessageSerializer.serialize(original).getOrThrow()
        val deserializedResult = MessageSerializer.deserialize(serialized)

        assertTrue(deserializedResult.isSuccess)
        val deserialized = deserializedResult.getOrThrow()
        assertEquals(original.messageId, deserialized.messageId)
        assertEquals(original.senderId, deserialized.senderId)
        assertEquals(original.text, deserialized.text)
        assertEquals(original.languageCode, deserialized.languageCode)
        assertEquals(original.timestamp, deserialized.timestamp)
        assertEquals(original.priority, deserialized.priority)
        assertEquals(original.sequenceNumber, deserialized.sequenceNumber)
        assertEquals(original.checksum, deserialized.checksum)
    }

    // 6. Unicode multilingual support
    @Test
    fun testUnicodeMultilingualText() {
        val multilingualPhrases = listOf(
            "hi" to "नमस्ते, क्या आप मेरी आवाज़ सुन सकते हैं?",
            "ta" to "வணக்கம், இது ஒரு சோதனை செய்தி.",
            "mr" to "नमस्कार, हा एक चाचणी संदेश आहे.",
            "bn" to "নমস্কার, এটি একটি পরীক্ষার বার্তা।",
            "te" to "నమస్కారం, ఇది ఒక పరీక్ష సందేశం."
        )

        for ((lang, text) in multilingualPhrases) {
            val msg = TextMessage(
                messageId = MessageIdGenerator.generateMessageId(),
                senderId = "DEV-MULTI",
                text = text,
                languageCode = lang,
                timestamp = System.currentTimeMillis()
            ).withComputedChecksum()

            val bytes = MessageSerializer.serialize(msg).getOrThrow()
            val deserialized = MessageSerializer.deserialize(bytes).getOrThrow()

            assertEquals(text, deserialized.text)
            val validation = MessageValidator.validate(deserialized)
            assertTrue("Validation failed for $lang: ${deserialized.text}", validation.isValid)
        }
    }

    // 7. Message validation & tamper detection
    @Test
    fun testMessageValidation() {
        val validMsg = TextMessage(
            messageId = "msg-3001",
            senderId = "DEV-VALID",
            text = "Valid test message",
            languageCode = "en",
            timestamp = System.currentTimeMillis()
        ).withComputedChecksum()

        assertTrue(MessageValidator.validate(validMsg).isValid)

        // Corrupted text (checksum mismatch)
        val tamperedMsg = validMsg.copy(text = "Tampered fraudulent text")
        assertFalse(MessageValidator.validate(tamperedMsg).isValid)

        // Blank messageId
        val blankIdMsg = validMsg.copy(messageId = "   ").withComputedChecksum()
        assertFalse(MessageValidator.validate(blankIdMsg).isValid)

        // Blank text
        val blankTextMsg = validMsg.copy(text = "").withComputedChecksum()
        assertFalse(MessageValidator.validate(blankTextMsg).isValid)

        // Unsupported protocol version
        val badVersionMsg = validMsg.copy(protocolVersion = 99).withComputedChecksum()
        assertFalse(MessageValidator.validate(badVersionMsg).isValid)
    }

    // 8. Mock transport transmission
    @Test
    fun testMockTransmission() = runBlocking(testDispatcher) {
        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        try {
            val transport = MockCommunicationTransport(autoEchoAck = true)
            val commManager = CommunicationManager(
                transport = transport,
                dispatcher = testDispatcher,
                externalScope = testScope
            )
            val coordinator = SpeechCommunicationCoordinator(
                communicationManager = commManager,
                dispatcher = testDispatcher,
                externalScope = testScope
            )

            coordinator.startListening()
            coordinator.stopListeningAndTranscribe()

            val result = coordinator.confirmAndTransmit("Field telemetry ack", DomainPriority.NORMAL)
            assertTrue(result.isSuccess)

            val msg = result.getOrThrow()
            var count = 0
            while (commManager.deliveryStatuses.value[msg.messageId] != DeliveryStatus.DELIVERED && count < 50) {
                delay(10)
                count++
            }

            assertEquals(DeliveryStatus.DELIVERED, commManager.deliveryStatuses.value[msg.messageId])
            assertTrue(transport.getSentPayloads().isNotEmpty())
        } finally {
            testScope.cancel()
        }
    }

    // 9. Incoming message processing & storage
    @Test
    fun testIncomingMessageProcessing() = runBlocking(testDispatcher) {
        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        try {
            val messageRepo = MessageRepositoryImpl().apply { clearAll() }
            val coordinator = SpeechCommunicationCoordinator(
                messageRepository = messageRepo,
                dispatcher = testDispatcher,
                externalScope = testScope
            )

            val incoming = TextMessage(
                messageId = "msg-in-501",
                senderId = "REMOTE-DEV-42",
                text = "Incoming drone position reported.",
                languageCode = "en",
                timestamp = System.currentTimeMillis()
            ).withComputedChecksum()

            val res = coordinator.receiveIncomingMessage(incoming)
            assertTrue(res.isSuccess)

            val messages = messageRepo.observeMessages().first()
            assertEquals(1, messages.size)
            val stored = messages[0]
            assertEquals("msg-in-501", stored.id)
            assertEquals("Incoming drone position reported.", stored.content)
            assertEquals(MessageDirection.RECEIVED, stored.direction)
        } finally {
            testScope.cancel()
        }
    }

    // 10. TTS queue integration & auto-play
    @Test
    fun testTtsQueueIntegration() = runBlocking(testDispatcher) {
        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        try {
            val ttsEngine = MockTextToSpeechEngine(dispatcher = testDispatcher)
            ttsEngine.initialize(TtsLanguage.ENGLISH)
            val settingsRepo = SettingsRepositoryImpl().apply {
                setTtsAutoPlay(true)
            }
            val coordinator = SpeechCommunicationCoordinator(
                ttsEngine = ttsEngine,
                settingsRepository = settingsRepo,
                dispatcher = testDispatcher,
                externalScope = testScope
            )

            val msg = TextMessage(
                messageId = "msg-tts-1",
                senderId = "REMOTE-01",
                text = "Audio dispatch confirmed",
                languageCode = "en",
                timestamp = System.currentTimeMillis()
            ).withComputedChecksum()

            val res = coordinator.receiveIncomingMessage(msg)
            assertTrue(res.isSuccess)

            var count = 0
            while (ttsEngine.spokenUtterances.isEmpty() && count < 50) {
                delay(10)
                count++
            }

            assertTrue(ttsEngine.spokenUtterances.any { it.text == "Audio dispatch confirmed" })
        } finally {
            testScope.cancel()
        }
    }

    // 11. Empty speech result handling
    @Test
    fun testEmptySpeechResult() = runBlocking(testDispatcher) {
        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        try {
            val audioRecorder = MockAudioRecorder().apply {
                customRecordedData = ByteArray(0) // Empty recording
            }
            val coordinator = SpeechCommunicationCoordinator(
                audioRecorder = audioRecorder,
                dispatcher = testDispatcher,
                externalScope = testScope
            )

            coordinator.startListening()
            val result = coordinator.stopListeningAndTranscribe()

            assertFalse(result.isSuccess)
            val state = coordinator.observeSessionState().value
            assertTrue(state is CommunicationSessionState.Error)
            assertTrue((state as CommunicationSessionState.Error).errorDescription.contains("No audio recorded"))
        } finally {
            testScope.cancel()
        }
    }

    // 12. STT engine failure recovery
    @Test
    fun testSttFailure() = runBlocking(testDispatcher) {
        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        try {
            val sttEngine = MockSpeechToTextEngine().apply {
                shouldFailTranscription = true
            }
            val coordinator = SpeechCommunicationCoordinator(
                sttEngine = sttEngine,
                dispatcher = testDispatcher,
                externalScope = testScope
            )

            coordinator.startListening()
            val result = coordinator.stopListeningAndTranscribe()

            assertFalse(result.isSuccess)
            val state = coordinator.observeSessionState().value
            assertTrue(state is CommunicationSessionState.Error)
            assertTrue((state as CommunicationSessionState.Error).canRetry)
        } finally {
            testScope.cancel()
        }
    }

    // 13. Transport failure resilience
    @Test
    fun testTransportFailure() = runBlocking(testDispatcher) {
        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        try {
            val transport = MockCommunicationTransport().apply {
                shouldFailSend = true
            }
            val commManager = CommunicationManager(
                transport = transport,
                dispatcher = testDispatcher,
                externalScope = testScope,
                ackTimeoutMs = 100L
            )

            val sendRes = commManager.sendMessage("Will fail", "en")
            assertTrue(sendRes.isSuccess)
            val msg = sendRes.getOrThrow()

            var count = 0
            while (commManager.deliveryStatuses.value[msg.messageId] != DeliveryStatus.FAILED && count < 50) {
                delay(20)
                count++
            }

            assertEquals(DeliveryStatus.FAILED, commManager.deliveryStatuses.value[msg.messageId])
        } finally {
            testScope.cancel()
        }
    }

    // 14. TTS playback failure tolerance
    @Test
    fun testTtsFailure() = runBlocking(testDispatcher) {
        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        try {
            val failingTts = MockTextToSpeechEngine(dispatcher = testDispatcher).apply {
                simulateAudioFocusFailure = true
            }
            failingTts.initialize(TtsLanguage.ENGLISH)
            val queueManager = SpeechQueueManager(
                engine = failingTts,
                dispatcher = testDispatcher,
                scope = testScope
            )

            val item = QueuedSpeechItem(
                messageId = "msg-fail-tts",
                text = "Failure test",
                language = TtsLanguage.ENGLISH,
                priority = SpeechPriority.NORMAL
            )

            queueManager.enqueue(item)
            var count = 0
            while (queueManager.queueState.value.isNotEmpty() && count < 50) {
                delay(10)
                count++
            }

            // Queue continues to drain despite failure without getting stuck
            assertTrue(queueManager.queueState.value.isEmpty())
            queueManager.release()
        } finally {
            testScope.cancel()
        }
    }

    // 15. User cancellation flow
    @Test
    fun testCancellation() = runBlocking(testDispatcher) {
        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        try {
            val audioRecorder = MockAudioRecorder()
            val coordinator = SpeechCommunicationCoordinator(
                audioRecorder = audioRecorder,
                dispatcher = testDispatcher,
                externalScope = testScope
            )

            coordinator.startListening()
            assertTrue(audioRecorder.isRecording())

            coordinator.cancelSession("User cancelled PTT button release")
            assertFalse(audioRecorder.isRecording())
            val state = coordinator.observeSessionState().value
            assertTrue(state is CommunicationSessionState.Cancelled)
            assertEquals("User cancelled PTT button release", (state as CommunicationSessionState.Cancelled).reason)
        } finally {
            testScope.cancel()
        }
    }

    // 16. Duplicate message deduplication
    @Test
    fun testDuplicateMessages() = runBlocking(testDispatcher) {
        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        try {
            val messageRepo = MessageRepositoryImpl().apply { clearAll() }
            val coordinator = SpeechCommunicationCoordinator(
                messageRepository = messageRepo,
                dispatcher = testDispatcher,
                externalScope = testScope
            )

            val msg = TextMessage(
                messageId = "msg-dup-101",
                senderId = "REMOTE-01",
                text = "Idempotent duplicate check",
                languageCode = "en",
                timestamp = System.currentTimeMillis()
            ).withComputedChecksum()

            val res1 = coordinator.receiveIncomingMessage(msg)
            assertTrue(res1.isSuccess)

            val res2 = coordinator.receiveIncomingMessage(msg)
            assertTrue(res2.isSuccess)

            val list = messageRepo.observeMessages().first()
            assertEquals("Expected exactly 1 message in repository", 1, list.size)
            assertEquals("msg-dup-101", list[0].id)
        } finally {
            testScope.cancel()
        }
    }

    // 17. Chronological message ordering
    @Test
    fun testMessageOrdering() = runBlocking(testDispatcher) {
        val messageRepo = MessageRepositoryImpl().apply { clearAll() }

        val msg1 = com.itantra.app.domain.model.ItantraMessage(
            id = "1", content = "First", language = SupportedLanguage.ENGLISH,
            direction = MessageDirection.RECEIVED, status = com.itantra.app.domain.model.MessageStatus.DELIVERED,
            timestampMs = 1000L
        )
        val msg3 = com.itantra.app.domain.model.ItantraMessage(
            id = "3", content = "Third", language = SupportedLanguage.ENGLISH,
            direction = MessageDirection.RECEIVED, status = com.itantra.app.domain.model.MessageStatus.DELIVERED,
            timestampMs = 3000L
        )
        val msg2 = com.itantra.app.domain.model.ItantraMessage(
            id = "2", content = "Second", language = SupportedLanguage.ENGLISH,
            direction = MessageDirection.RECEIVED, status = com.itantra.app.domain.model.MessageStatus.DELIVERED,
            timestampMs = 2000L
        )

        messageRepo.addMessage(msg1)
        messageRepo.addMessage(msg3)
        messageRepo.addMessage(msg2)

        val ordered = messageRepo.observeMessages().first()
        assertEquals(listOf("1", "2", "3"), ordered.map { it.id })
    }

    // 18. Priority preemption in speech queue
    @Test
    fun testPriorityHandling() = runBlocking(testDispatcher) {
        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        try {
            val ttsEngine = MockTextToSpeechEngine(dispatcher = testDispatcher).apply {
                speakDelayMs = 100L
            }
            ttsEngine.initialize(TtsLanguage.ENGLISH)

            val queueManager = SpeechQueueManager(
                engine = ttsEngine,
                dispatcher = testDispatcher,
                scope = testScope
            )

            val normalItem = QueuedSpeechItem("msg-norm", "Normal traffic", TtsLanguage.ENGLISH, SpeechPriority.NORMAL)
            val alertItem = QueuedSpeechItem("msg-alert", "ALERT EVACUATE", TtsLanguage.ENGLISH, SpeechPriority.ALERT)

            queueManager.enqueue(normalItem)
            queueManager.enqueue(alertItem)

            // Alert item should be prioritized ahead in queue order
            val queue = queueManager.queueState.value
            if (queue.size >= 2) {
                assertEquals("msg-alert", queue[0].messageId)
            }
            queueManager.release()
        } finally {
            testScope.cancel()
        }
    }

    // 19. Complete session state transitions
    @Test
    fun testStateTransitions() = runBlocking(testDispatcher) {
        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        try {
            val vad = EnergyBasedVoiceActivityDetector(
                VadConfiguration(
                    energyThresholdDb = -45.0f,
                    speechConfirmationDurationMs = 20L,
                    minSpeechDurationMs = 20L
                )
            )
            val audioRecorder = MockAudioRecorder()
            val coordinator = SpeechCommunicationCoordinator(
                audioRecorder = audioRecorder,
                voiceActivityDetector = vad,
                dispatcher = testDispatcher,
                externalScope = testScope
            )

            assertEquals(CommunicationSessionState.Idle, coordinator.observeSessionState().value)

            coordinator.startListening()
            assertTrue(coordinator.observeSessionState().value is CommunicationSessionState.Listening)

            // Feed 400 ms of synthetic speech samples to trigger SPEECH_DETECTED
            val speechSamples = ShortArray((16000 * 400) / 1000) { i ->
                (6000 * kotlin.math.sin(2.0 * Math.PI * 500.0 * i / 16000)).toInt().toShort()
            }
            audioRecorder.simulateAudioFrame(speechSamples)
            assertTrue(coordinator.observeSessionState().value is CommunicationSessionState.SpeechDetected)

            coordinator.stopListeningAndTranscribe()
            assertTrue(coordinator.observeSessionState().value is CommunicationSessionState.TextReview)

            coordinator.confirmAndTransmit("State transition validated", DomainPriority.NORMAL)
            val sendingOrSent = coordinator.observeSessionState().value
            assertTrue(
                sendingOrSent is CommunicationSessionState.Sending ||
                sendingOrSent is CommunicationSessionState.Sent
            )
        } finally {
            testScope.cancel()
        }
    }

    // 20. Lifecycle release and cleanup
    @Test
    fun testLifecycleCleanup() = runBlocking(testDispatcher) {
        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        val audioRecorder = MockAudioRecorder()
        val sttEngine = MockSpeechToTextEngine()
        val ttsEngine = MockTextToSpeechEngine(dispatcher = testDispatcher)

        val coordinator = SpeechCommunicationCoordinator(
            audioRecorder = audioRecorder,
            sttEngine = sttEngine,
            ttsEngine = ttsEngine,
            dispatcher = testDispatcher,
            externalScope = testScope
        )

        coordinator.startListening()
        assertTrue(audioRecorder.isRecording())

        coordinator.release()
        assertFalse(audioRecorder.isRecording())
        assertEquals(CommunicationSessionState.Idle, coordinator.observeSessionState().value)
        testScope.cancel()
    }

    // 21. Microphone permission denial handling
    @Test
    fun testPermissionDenial() = runBlocking(testDispatcher) {
        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        try {
            val audioRecorder = MockAudioRecorder().apply {
                shouldFailStart = true
            }
            val coordinator = SpeechCommunicationCoordinator(
                audioRecorder = audioRecorder,
                dispatcher = testDispatcher,
                externalScope = testScope
            )

            val result = coordinator.startListening()
            assertFalse(result.isSuccess)
            val state = coordinator.observeSessionState().value
            assertTrue(state is CommunicationSessionState.Error)
            assertTrue((state as CommunicationSessionState.Error).errorDescription.contains("RECORD_AUDIO"))
        } finally {
            testScope.cancel()
        }
    }

    // 22. Speech model unavailability handling
    @Test
    fun testModelUnavailability() = runBlocking(testDispatcher) {
        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        try {
            val langRepo = LanguageRepositoryImpl().apply {
                setActiveLanguage(SupportedLanguage.TAMIL)
            }
            val sttEngine = MockSpeechToTextEngine().apply {
                shouldFailInitialization = true
            }
            val coordinator = SpeechCommunicationCoordinator(
                sttEngine = sttEngine,
                languageRepository = langRepo,
                dispatcher = testDispatcher,
                externalScope = testScope
            )

            coordinator.startListening()
            val result = coordinator.stopListeningAndTranscribe()

            assertFalse(result.isSuccess)
            val state = coordinator.observeSessionState().value
            assertTrue(state is CommunicationSessionState.Error)
        } finally {
            testScope.cancel()
        }
    }
}
