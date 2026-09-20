package com.itantra.app.stt

import android.content.Context
import com.itantra.app.stt.model.LocalModelManager
import com.itantra.app.stt.model.ModelManager

/**
 * Factory for creating [SpeechToTextEngine] instances.
 */
object SttEngineFactory {

    /**
     * Creates a production offline STT engine backed by the local model directory.
     */
    fun createOfflineEngine(
        context: Context,
        modelManager: ModelManager = LocalModelManager(context)
    ): SpeechToTextEngine {
        return OfflineSpeechToTextEngine(modelManager = modelManager)
    }

    /**
     * Creates a mock STT engine for unit testing and UI previews.
     */
    fun createMockEngine(): SpeechToTextEngine {
        return MockSpeechToTextEngine()
    }
}
