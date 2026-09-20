package com.itantra.app.tts

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Deterministic mock implementation of [TextToSpeechEngine] for rapid JVM unit tests
 * and interactive Compose previews.
 */
class MockTextToSpeechEngine(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
    var autoPlayComplete: Boolean = true,
    var speakDelayMs: Long = 10L
) : TextToSpeechEngine {

    private val engineScope = CoroutineScope(SupervisorJob() + dispatcher)
    private var activeSpeechJob: Job? = null

    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    override fun observeState(): StateFlow<TtsState> = _state.asStateFlow()

    private var initialized: Boolean = false
    private var currentLanguage: TtsLanguage? = null
    private var speechRate: Float = TtsConfiguration.DEFAULT_SPEECH_RATE
    private var voicePitch: Float = TtsConfiguration.DEFAULT_PITCH

    // Test inspection hooks
    val spokenUtterances = mutableListOf<SpokenItem>()
    var simulateInitFailure: Boolean = false
    var simulateAudioFocusFailure: Boolean = false
    var hasAudioFocus: Boolean = false
        private set

    private val languageAvailabilityMap = ConcurrentHashMap<TtsLanguage, VoiceAvailability>().apply {
        // By default, English and Hindi are fully installed
        put(TtsLanguage.ENGLISH, VoiceAvailability.AVAILABLE)
        put(TtsLanguage.HINDI, VoiceAvailability.AVAILABLE)
        put(TtsLanguage.TELUGU, VoiceAvailability.AVAILABLE)
        put(TtsLanguage.BENGALI, VoiceAvailability.AVAILABLE)
        put(TtsLanguage.GUJARATI, VoiceAvailability.MISSING_DATA)
        put(TtsLanguage.MARATHI, VoiceAvailability.AVAILABLE)
        put(TtsLanguage.KANNADA, VoiceAvailability.AVAILABLE)
        put(TtsLanguage.MALAYALAM, VoiceAvailability.AVAILABLE)
        put(TtsLanguage.TAMIL, VoiceAvailability.AVAILABLE)
        put(TtsLanguage.ODIA, VoiceAvailability.MISSING_DATA)
    }

    data class SpokenItem(
        val utteranceId: String,
        val text: String,
        val language: TtsLanguage?,
        val timestampMs: Long = System.currentTimeMillis()
    )

    fun setLanguageAvailability(language: TtsLanguage, availability: VoiceAvailability) {
        languageAvailabilityMap[language] = availability
    }

    override suspend fun initialize(language: TtsLanguage): Result<Unit> {
        _state.value = TtsState.Initializing(language)

        if (simulateInitFailure) {
            val error = TtsError.InitializationFailed("Simulated mock initialization error.")
            _state.value = TtsState.Error(error)
            initialized = false
            return Result.failure(IllegalStateException(error.message))
        }

        val availability = languageAvailabilityMap[language] ?: VoiceAvailability.NOT_SUPPORTED
        if (availability == VoiceAvailability.MISSING_DATA) {
            val error = TtsError.MissingVoiceData(language)
            _state.value = TtsState.LanguageUnavailable(language, availability)
            initialized = true
            currentLanguage = language
            return Result.failure(IllegalStateException(error.message))
        } else if (availability == VoiceAvailability.NOT_SUPPORTED) {
            val error = TtsError.UnsupportedLanguage(language.code)
            _state.value = TtsState.LanguageUnavailable(language, availability)
            initialized = false
            return Result.failure(IllegalStateException(error.message))
        }

        initialized = true
        currentLanguage = language
        _state.value = TtsState.Idle
        return Result.success(Unit)
    }

    override suspend fun speak(text: String, utteranceId: String): Result<Unit> {
        if (!initialized) {
            val err = TtsError.EngineNotInitialized("speak")
            _state.value = TtsState.Error(err)
            return Result.failure(IllegalStateException(err.message))
        }

        if (text.isBlank()) {
            val err = TtsError.EmptyText
            _state.value = TtsState.Error(err)
            return Result.failure(IllegalArgumentException(err.message))
        }

        if (text.length > TtsConfiguration.MAX_TEXT_CHARACTERS) {
            val err = TtsError.TextTooLong(text.length)
            _state.value = TtsState.Error(err)
            return Result.failure(IllegalArgumentException(err.message))
        }

        val lang = currentLanguage
        if (lang != null) {
            val availability = languageAvailabilityMap[lang] ?: VoiceAvailability.NOT_SUPPORTED
            if (availability != VoiceAvailability.AVAILABLE) {
                val err = TtsError.MissingVoiceData(lang)
                _state.value = TtsState.LanguageUnavailable(lang, availability)
                return Result.failure(IllegalStateException(err.message))
            }
        }

        if (simulateAudioFocusFailure) {
            val err = TtsError.AudioFocusDenied
            _state.value = TtsState.Error(err)
            return Result.failure(IllegalStateException(err.message))
        }

        hasAudioFocus = true

        activeSpeechJob?.cancel()
        _state.value = TtsState.Speaking(text = text, language = lang, utteranceId = utteranceId)
        spokenUtterances.add(SpokenItem(utteranceId, text, lang))

        if (autoPlayComplete) {
            activeSpeechJob = engineScope.launch {
                if (speakDelayMs > 0) delay(speakDelayMs)
                _state.value = TtsState.Completed(utteranceId = utteranceId)
                hasAudioFocus = false
                delay(10L)
                if (_state.value is TtsState.Completed) {
                    _state.value = TtsState.Idle
                }
            }
        }

        return Result.success(Unit)
    }

    override suspend fun stop(): Result<Unit> {
        activeSpeechJob?.cancel()
        activeSpeechJob = null
        hasAudioFocus = false
        if (initialized) {
            _state.value = TtsState.Idle
        }
        return Result.success(Unit)
    }

    override suspend fun pause(): Result<Unit> {
        val current = _state.value
        if (current is TtsState.Speaking) {
            activeSpeechJob?.cancel()
            _state.value = TtsState.Paused(text = current.text, messageId = current.messageId)
        }
        return Result.success(Unit)
    }

    override suspend fun release() {
        stop()
        initialized = false
        currentLanguage = null
        spokenUtterances.clear()
        _state.value = TtsState.Idle
    }

    override fun isInitialized(): Boolean = initialized

    override fun setSpeechRate(rate: Float): Result<Unit> {
        val validation = TtsConfiguration.validateSpeechRate(rate)
        if (validation.isFailure) return Result.failure(validation.exceptionOrNull()!!)
        speechRate = rate
        return Result.success(Unit)
    }

    override fun setPitch(pitch: Float): Result<Unit> {
        val validation = TtsConfiguration.validatePitch(pitch)
        if (validation.isFailure) return Result.failure(validation.exceptionOrNull()!!)
        voicePitch = pitch
        return Result.success(Unit)
    }

    override fun checkLanguageAvailability(language: TtsLanguage): VoiceAvailability {
        return languageAvailabilityMap[language] ?: VoiceAvailability.NOT_SUPPORTED
    }

    override fun getCurrentLanguage(): TtsLanguage? = currentLanguage

    fun simulateAudioFocusLoss() {
        hasAudioFocus = false
        activeSpeechJob?.cancel()
        _state.value = TtsState.Error(TtsError.PlaybackInterrupted("Audio focus lost."))
    }
}
