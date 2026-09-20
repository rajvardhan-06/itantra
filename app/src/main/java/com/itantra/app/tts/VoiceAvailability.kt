package com.itantra.app.tts

/**
 * Status of offline voice synthesis data on the host Android device for a specific [TtsLanguage].
 */
enum class VoiceAvailability {
    /** Voice data is installed locally and ready for immediate speech synthesis. */
    AVAILABLE,

    /** Language is supported by the engine, but local voice data needs to be downloaded/installed. */
    MISSING_DATA,

    /** Language is completely unsupported by the host device's active TTS engine. */
    NOT_SUPPORTED,

    /** Engine is querying language availability. */
    CHECKING
}
