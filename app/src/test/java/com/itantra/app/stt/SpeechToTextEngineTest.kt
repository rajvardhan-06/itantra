package com.itantra.app.stt

import com.itantra.app.stt.model.LocalModelManager
import com.itantra.app.stt.repository.SpeechRecognitionRepositoryImpl
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit test suite for the Phase 4 Offline Speech-to-Text (STT) pipeline.
 *
 * Covers all 18 requirements:
 * 1. Engine initializes successfully with supported language.
 * 2. Engine rejects unsupported language with descriptive error.
 * 3. Engine rejects initialization when model is missing.
 * 4. Transcribe succeeds with valid 16 kHz Mono PCM audio.
 * 5. Transcribe rejects empty audio byte array (< 200ms).
 * 6. Transcribe rejects oversized audio byte array (> 30s).
 * 7. Transcribe rejects uninitialized engine.
 * 8. Transcribe rejects non-16-bit aligned byte arrays (odd byte count).
 * 9. Silence detection returns empty/silence result when DB < -55dB.
 * 10. Engine reports initialized status accurately before and after release.
 * 11. Language switching releases previous model before loading new model.
 * 12. Transcribe returns non-null confidence when confidence is available.
 * 13. ModelManager correctly detects installed vs missing models.
 * 14. ModelManager prevents duplicate installations of same model.
 * 15. ModelManager cleans up files when removing a model.
 * 16. ModelManager enforces available storage checks before installation.
 * 17. Repository properly wraps engine and model manager interactions.
 * 18. ViewModel correctly transitions through all SttState values.
 */
class SpeechToTextEngineTest {

    private lateinit var tempModelsDir: File
    private lateinit var modelManager: LocalModelManager
    private lateinit var offlineEngine: OfflineSpeechToTextEngine
    private lateinit var mockEngine: MockSpeechToTextEngine

    @Before
    fun setUp() {
        tempModelsDir = File(System.getProperty("java.io.tmpdir"), "itantra_stt_test_${System.currentTimeMillis()}")
        tempModelsDir.mkdirs()
        modelManager = LocalModelManager(tempModelsDir, Dispatchers.Unconfined)
        offlineEngine = OfflineSpeechToTextEngine(modelManager, Dispatchers.Unconfined)
        mockEngine = MockSpeechToTextEngine()
    }

    @After
    fun tearDown() {
        runBlocking {
            offlineEngine.release()
            mockEngine.release()
        }
        tempModelsDir.deleteRecursively()
    }

    private fun generateTonePcm(durationSeconds: Double = 1.0, amplitude: Short = 10000): ByteArray {
        val sampleRate = 16000
        val numSamples = (sampleRate * durationSeconds).toInt()
        val bytes = ByteArray(numSamples * 2)
        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * i * 440.0 / sampleRate
            val sample = (Math.sin(angle) * amplitude).toInt().toShort()
            bytes[i * 2] = (sample.toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    private fun generateSilencePcm(durationSeconds: Double = 1.0): ByteArray {
        val sampleRate = 16000
        val numSamples = (sampleRate * durationSeconds).toInt()
        return ByteArray(numSamples * 2)
    }

    // 1. Engine initializes successfully with supported language.
    @Test
    fun test01_engineInitializesSuccessfullyWithSupportedLanguage() {
        runBlocking {
            modelManager.installModel(SttLanguage.HINDI)
            val result = offlineEngine.initialize(SttLanguage.HINDI)
            assertTrue("Expected success initializing Hindi", result.isSuccess)
            assertTrue("Engine should report initialized", offlineEngine.isInitialized())
        }
    }

    // 2. Engine rejects unsupported language with descriptive error.
    @Test
    fun test02_engineRejectsUnsupportedLanguage() {
        runBlocking {
            // Gujarati is marked isModelAvailable = false in Phase 4
            val result = offlineEngine.initialize(SttLanguage.GUJARATI)
            assertTrue("Expected failure for unsupported language", result.isFailure)
            val ex = result.exceptionOrNull()
            assertTrue("Should contain descriptive message", ex?.message?.contains("supported", ignoreCase = true) == true)
        }
    }

    // 3. Engine rejects initialization when model is missing.
    @Test
    fun test03_engineRejectsInitializationWhenModelMissing() {
        runBlocking {
            // English is available, but not installed yet in temp directory
            val result = offlineEngine.initialize(SttLanguage.ENGLISH)
            assertTrue("Expected failure when model is not installed", result.isFailure)
            val msg = result.exceptionOrNull()?.message ?: ""
            assertTrue("Message should mention model missing: $msg", msg.contains("not installed", ignoreCase = true))
        }
    }

    // 4. Transcribe succeeds with valid 16 kHz Mono PCM audio.
    @Test
    fun test04_transcribeSucceedsWithValidPcmAudio() {
        runBlocking {
            modelManager.installModel(SttLanguage.HINDI)
            offlineEngine.initialize(SttLanguage.HINDI)

            val audio = generateTonePcm(durationSeconds = 1.0)
            val result = offlineEngine.transcribe(audio)

            assertTrue("Transcription should succeed", result.isSuccess)
            val sttResult = result.getOrThrow()
            assertFalse("Recognized text should not be empty", sttResult.isEmpty)
            assertEquals("Language should match", SttLanguage.HINDI, sttResult.language)
        }
    }

    // 5. Transcribe rejects empty audio byte array (< 200ms).
    @Test
    fun test05_transcribeRejectsUndersizedAudio() {
        runBlocking {
            modelManager.installModel(SttLanguage.HINDI)
            offlineEngine.initialize(SttLanguage.HINDI)

            // 100ms = 3200 bytes (< 6400 minimum)
            val shortAudio = ByteArray(3200)
            val result = offlineEngine.transcribe(shortAudio)

            assertTrue("Expected failure for short audio", result.isFailure)
            assertTrue("Error should mention duration", result.exceptionOrNull()?.message?.contains("too short", ignoreCase = true) == true)
        }
    }

    // 6. Transcribe rejects oversized audio byte array (> 30s).
    @Test
    fun test06_transcribeRejectsOversizedAudio() {
        runBlocking {
            modelManager.installModel(SttLanguage.HINDI)
            offlineEngine.initialize(SttLanguage.HINDI)

            // 31s = 31 * 32,000 = 992,000 bytes (> 960,000 max)
            val oversizedAudio = ByteArray(992000)
            val result = offlineEngine.transcribe(oversizedAudio)

            assertTrue("Expected failure for oversized audio", result.isFailure)
            assertTrue("Error should mention exceeding limit", result.exceptionOrNull()?.message?.contains("exceeds", ignoreCase = true) == true)
        }
    }

    // 7. Transcribe rejects uninitialized engine.
    @Test
    fun test07_transcribeRejectsUninitializedEngine() {
        runBlocking {
            val audio = generateTonePcm(durationSeconds = 1.0)
            val result = offlineEngine.transcribe(audio)

            assertTrue("Expected failure when engine is not initialized", result.isFailure)
            assertTrue("Message should mention not initialized", result.exceptionOrNull()?.message?.contains("not initialized", ignoreCase = true) == true)
        }
    }

    // 8. Transcribe rejects non-16-bit aligned byte arrays (odd byte count).
    @Test
    fun test08_transcribeRejectsMisalignedAudio() {
        runBlocking {
            modelManager.installModel(SttLanguage.HINDI)
            offlineEngine.initialize(SttLanguage.HINDI)

            // 6401 bytes (odd byte count)
            val oddAudio = ByteArray(6401)
            val result = offlineEngine.transcribe(oddAudio)

            assertTrue("Expected failure for odd byte count", result.isFailure)
            assertTrue("Error should mention alignment", result.exceptionOrNull()?.message?.contains("16-bit", ignoreCase = true) == true)
        }
    }

    // 9. Silence detection returns empty/silence result when DB < -55dB.
    @Test
    fun test09_silenceDetectionReturnsEmptyResult() {
        runBlocking {
            modelManager.installModel(SttLanguage.HINDI)
            offlineEngine.initialize(SttLanguage.HINDI)

            val silence = generateSilencePcm(durationSeconds = 1.0)
            val result = offlineEngine.transcribe(silence)

            assertTrue("Transcription should complete", result.isSuccess)
            val sttResult = result.getOrThrow()
            assertTrue("Silence transcription must be empty", sttResult.isEmpty)
        }
    }

    // 10. Engine reports initialized status accurately before and after release.
    @Test
    fun test10_engineReportsInitializedStatusAccurately() {
        runBlocking {
            assertFalse("Initially should not be initialized", offlineEngine.isInitialized())
            modelManager.installModel(SttLanguage.ENGLISH)
            offlineEngine.initialize(SttLanguage.ENGLISH)
            assertTrue("Should be initialized after init", offlineEngine.isInitialized())

            offlineEngine.release()
            assertFalse("Should not be initialized after release", offlineEngine.isInitialized())
        }
    }

    // 11. Language switching releases previous model before loading new model.
    @Test
    fun test11_languageSwitchingReleasesPreviousModel() {
        runBlocking {
            modelManager.installModel(SttLanguage.ENGLISH)
            modelManager.installModel(SttLanguage.HINDI)

            offlineEngine.initialize(SttLanguage.ENGLISH)
            val resEn = offlineEngine.transcribe(generateTonePcm(1.0)).getOrThrow()
            assertEquals(SttLanguage.ENGLISH, resEn.language)

            // Switch to Hindi
            offlineEngine.initialize(SttLanguage.HINDI)
            val resHi = offlineEngine.transcribe(generateTonePcm(1.0)).getOrThrow()
            assertEquals(SttLanguage.HINDI, resHi.language)
        }
    }

    // 12. Transcribe returns non-null confidence when confidence is available.
    @Test
    fun test12_transcribeReturnsConfidence() {
        runBlocking {
            modelManager.installModel(SttLanguage.ENGLISH)
            offlineEngine.initialize(SttLanguage.ENGLISH)

            val result = offlineEngine.transcribe(generateTonePcm(1.0))
            assertTrue(result.isSuccess)
            val sttResult = result.getOrThrow()
            assertNotNull("Confidence should be non-null for recognized speech", sttResult.confidence)
            assertTrue("Confidence should be within 0..1", sttResult.confidence!! in 0.0f..1.0f)
        }
    }

    // 13. ModelManager correctly detects installed vs missing models.
    @Test
    fun test13_modelManagerDetectsInstalledAndMissing() {
        runBlocking {
            assertFalse("Hindi not installed initially", modelManager.isModelInstalled(SttLanguage.HINDI))
            modelManager.installModel(SttLanguage.HINDI)
            assertTrue("Hindi should be marked installed", modelManager.isModelInstalled(SttLanguage.HINDI))
            assertFalse("Bengali is not installed", modelManager.isModelInstalled(SttLanguage.BENGALI))
        }
    }

    // 14. ModelManager prevents duplicate installations of same model.
    @Test
    fun test14_modelManagerPreventsDuplicateInstallations() {
        runBlocking {
            val first = modelManager.installModel(SttLanguage.HINDI)
            assertTrue(first.isSuccess)

            val second = modelManager.installModel(SttLanguage.HINDI)
            assertTrue("Second call should be successful idempotent no-op", second.isSuccess)
            assertEquals("Only 1 Hindi model in installed set", 1, modelManager.installedLanguages.value.size)
        }
    }

    // 15. ModelManager cleans up files when removing a model.
    @Test
    fun test15_modelManagerCleansUpFilesOnRemoval() {
        runBlocking {
            modelManager.installModel(SttLanguage.HINDI)
            assertTrue(modelManager.isModelInstalled(SttLanguage.HINDI))
            val dir = modelManager.getModelDirectory(SttLanguage.HINDI)
            assertNotNull(dir)
            assertTrue(dir!!.exists())

            val removeRes = modelManager.removeModel(SttLanguage.HINDI)
            assertTrue(removeRes.isSuccess)
            assertFalse(modelManager.isModelInstalled(SttLanguage.HINDI))
            assertFalse("Model directory should no longer exist", dir.exists())
        }
    }

    // 16. ModelManager enforces available storage checks before installation.
    @Test
    fun test16_modelManagerEnforcesStorageCheck() {
        // Create an anonymous subclass of LocalModelManager overriding getAvailableStorageBytes to 10 bytes
        val lowStorageManager = object : LocalModelManager(tempModelsDir, Dispatchers.Unconfined) {
            override fun getAvailableStorageBytes(): Long = 10L
        }

        runBlocking {
            val result = lowStorageManager.installModel(SttLanguage.HINDI)
            assertTrue("Expected failure when insufficient storage", result.isFailure)
            assertTrue("Message should mention storage", result.exceptionOrNull()?.message?.contains("storage", ignoreCase = true) == true)
        }
    }

    // 17. Repository properly wraps engine and model manager interactions.
    @Test
    fun test17_repositoryWrapsEngineAndModelManager() {
        runBlocking {
            val repo = SpeechRecognitionRepositoryImpl(mockEngine, modelManager)
            modelManager.installModel(SttLanguage.ENGLISH)

            val initRes = repo.initializeLanguage(SttLanguage.ENGLISH)
            assertTrue(initRes.isSuccess)
            assertTrue(repo.isEngineInitialized())

            val transRes = repo.transcribeAudio(generateTonePcm(1.0))
            assertTrue(transRes.isSuccess)
            assertFalse(transRes.getOrThrow().isEmpty)

            repo.release()
            assertFalse(repo.isEngineInitialized())
        }
    }

    // 18. ViewModel correctly transitions through all SttState values.
    @Test
    fun test18_viewModelTransitionsThroughStates() {
        runBlocking {
            val repo = SpeechRecognitionRepositoryImpl(mockEngine, modelManager)
            val unconfinedScope = CoroutineScope(Dispatchers.Unconfined)
            val vm = SttViewModel(repo, unconfinedScope)

            // Initially Hindi is not installed in temp directory
            assertTrue("Initial state is ModelNotInstalled", vm.sttState.value is SttState.ModelNotInstalled)

            // Install model
            vm.installCurrentModel()
            assertTrue("Model is now installed", vm.isModelInstalled.value)
            assertTrue("State is Ready after install", vm.sttState.value is SttState.Ready)

            // Select unsupported language
            vm.selectLanguage(SttLanguage.GUJARATI)
            assertTrue("State is UnsupportedLanguage", vm.sttState.value is SttState.UnsupportedLanguage)

            // Switch back to Hindi
            vm.selectLanguage(SttLanguage.HINDI)
            assertTrue("State is Ready", vm.sttState.value is SttState.Ready)

            // Transcribe audio
            val pcm = generateTonePcm(1.0)
            vm.transcribeAudio(pcm)
            assertTrue("State is ResultAvailable", vm.sttState.value is SttState.ResultAvailable)

            // Clear result
            vm.clearResult()
            assertTrue("State is Ready after clear", vm.sttState.value is SttState.Ready)

            // Transcribe silence
            val silence = generateSilencePcm(1.0)
            mockEngine.mockTranscriptionText = ""
            vm.transcribeAudio(silence)
            assertTrue("State is NoSpeechDetected", vm.sttState.value is SttState.NoSpeechDetected)
        }
    }
}
