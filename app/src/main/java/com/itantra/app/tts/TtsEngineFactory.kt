package com.itantra.app.tts

import android.content.Context

/**
 * Factory for instantiating text-to-speech engine implementations.
 */
object TtsEngineFactory {

    /**
     * Creates a production offline [AndroidTextToSpeechEngine] utilizing native on-device TTS.
     */
    fun create(context: Context): TextToSpeechEngine {
        return AndroidTextToSpeechEngine(context.applicationContext)
    }

    /**
     * Creates a deterministic [MockTextToSpeechEngine] for rapid testing and Compose previews.
     */
    fun createMock(autoPlayComplete: Boolean = true): TextToSpeechEngine {
        return MockTextToSpeechEngine(autoPlayComplete = autoPlayComplete)
    }
}
