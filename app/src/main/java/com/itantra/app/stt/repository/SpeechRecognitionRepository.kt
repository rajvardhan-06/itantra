package com.itantra.app.stt.repository

import com.itantra.app.stt.SpeechToTextEngine
import com.itantra.app.stt.SttLanguage
import com.itantra.app.stt.SttResult
import com.itantra.app.stt.model.ModelManager
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean Architecture repository abstracting offline speech recognition and model lifecycle.
 */
interface SpeechRecognitionRepository {

    val installedLanguages: StateFlow<Set<SttLanguage>>

    suspend fun initializeLanguage(language: SttLanguage): Result<Unit>

    suspend fun transcribeAudio(pcmBytes: ByteArray): Result<SttResult>

    suspend fun installModel(language: SttLanguage): Result<Unit>

    suspend fun removeModel(language: SttLanguage): Result<Unit>

    suspend fun release()

    fun isModelInstalled(language: SttLanguage): Boolean

    fun isModelAvailable(language: SttLanguage): Boolean

    fun isEngineInitialized(): Boolean
}

class SpeechRecognitionRepositoryImpl(
    private val sttEngine: SpeechToTextEngine,
    private val modelManager: ModelManager
) : SpeechRecognitionRepository {

    override val installedLanguages: StateFlow<Set<SttLanguage>> = modelManager.installedLanguages

    override suspend fun initializeLanguage(language: SttLanguage): Result<Unit> {
        return sttEngine.initialize(language)
    }

    override suspend fun transcribeAudio(pcmBytes: ByteArray): Result<SttResult> {
        return sttEngine.transcribe(pcmBytes)
    }

    override suspend fun installModel(language: SttLanguage): Result<Unit> {
        return modelManager.installModel(language)
    }

    override suspend fun removeModel(language: SttLanguage): Result<Unit> {
        if (sttEngine.isInitialized()) {
            sttEngine.release()
        }
        return modelManager.removeModel(language)
    }

    override suspend fun release() {
        sttEngine.release()
    }

    override fun isModelInstalled(language: SttLanguage): Boolean {
        return modelManager.isModelInstalled(language)
    }

    override fun isModelAvailable(language: SttLanguage): Boolean {
        return modelManager.isModelAvailable(language)
    }

    override fun isEngineInitialized(): Boolean {
        return sttEngine.isInitialized()
    }
}
