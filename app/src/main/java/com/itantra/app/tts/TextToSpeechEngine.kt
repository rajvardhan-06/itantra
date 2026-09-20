package com.itantra.app.tts

import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Replaceable abstraction interface for offline text-to-speech synthesis engines.
 *
 * Designed to decouple the UI and ViewModel layers from direct Android [android.speech.tts.TextToSpeech]
 * framework classes, enabling rapid JVM unit testing and alternative offline backend implementations.
 */
interface TextToSpeechEngine {

    /**
     * Initializes the TTS engine with the designated [language].
     *
     * @param language The target [TtsLanguage] for voice synthesis.
     * @return [Result.success] if initialized and voice data is ready, or [Result.failure] with [TtsError].
     */
    suspend fun initialize(language: TtsLanguage): Result<Unit>

    /**
     * Synthesizes and plays the provided [text] audibly through the device audio subsystem.
     *
     * @param text The input text string (up to [TtsConfiguration.MAX_TEXT_CHARACTERS] characters).
     * @param utteranceId Optional unique tracking identifier for this speech request.
     * @return [Result.success] if playback commenced, or [Result.failure] with [TtsError].
     */
    suspend fun speak(
        text: String,
        utteranceId: String = UUID.randomUUID().toString()
    ): Result<Unit>

    /**
     * Immediately stops any currently active speech synthesis and playback.
     */
    suspend fun stop(): Result<Unit>

    /**
     * Pauses the currently active speech synthesis.
     */
    suspend fun pause(): Result<Unit>

    /**
     * Releases all native resources, audio focus, and background engine connections.
     */
    suspend fun release()

    /**
     * Returns true if the engine has completed initialization and is ready to synthesize speech.
     */
    fun isInitialized(): Boolean

    /**
     * Configures the playback speech rate multiplier (typically between 0.5x and 2.0x).
     */
    fun setSpeechRate(rate: Float): Result<Unit>

    /**
     * Configures the voice pitch multiplier (typically between 0.5x and 2.0x).
     */
    fun setPitch(pitch: Float): Result<Unit>

    /**
     * Checks whether offline voice data is installed and available for [language].
     */
    fun checkLanguageAvailability(language: TtsLanguage): VoiceAvailability

    /**
     * Observes the reactive state transitions of the speech synthesis pipeline.
     */
    fun observeState(): StateFlow<TtsState>

    /**
     * The currently active language configured on the engine.
     */
    fun getCurrentLanguage(): TtsLanguage?
}
