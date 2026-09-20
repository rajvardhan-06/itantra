package com.itantra.app.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * Production offline implementation of [TextToSpeechEngine] wrapping Android's native
 * [android.speech.tts.TextToSpeech] API.
 *
 * Utilizes pre-installed on-device voice data, manages audio focus via [AudioManager],
 * processes synthesis callbacks via [UtteranceProgressListener], and enforces timeout guards.
 */
class AndroidTextToSpeechEngine(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : TextToSpeechEngine {

    private var tts: TextToSpeech? = null
    private var isInitialized: Boolean = false
    private var currentLanguage: TtsLanguage? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    override fun observeState(): StateFlow<TtsState> = _state.asStateFlow()

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                tts?.stop()
                val current = _state.value
                if (current is TtsState.Speaking) {
                    _state.value = TtsState.Paused(current.text, current.messageId)
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Focus restored
            }
        }
    }

    private val utteranceProgressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            val current = _state.value
            if (current is TtsState.Speaking) {
                // State already speaking
            }
        }

        override fun onDone(utteranceId: String?) {
            abandonAudioFocus()
            _state.value = TtsState.Completed(utteranceId = utteranceId)
            _state.value = TtsState.Idle
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            abandonAudioFocus()
            _state.value = TtsState.Error(TtsError.SynthesisError(-1, "General TTS synthesis error."))
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            abandonAudioFocus()
            _state.value = TtsState.Error(
                TtsError.SynthesisError(errorCode, "Synthesis error with code: $errorCode")
            )
        }
    }

    override suspend fun initialize(language: TtsLanguage): Result<Unit> = withContext(dispatcher) {
        _state.value = TtsState.Initializing(language)

        if (tts == null) {
            val initDeferred = CompletableDeferred<Int>()
            tts = TextToSpeech(context.applicationContext) { status ->
                initDeferred.complete(status)
            }

            val status = withTimeoutOrNull(TtsConfiguration.INITIALIZATION_TIMEOUT_MS) {
                initDeferred.await()
            }

            if (status == null || status != TextToSpeech.SUCCESS) {
                isInitialized = false
                val error = TtsError.InitializationFailed(
                    if (status == null) "Initialization timed out." else "Engine reported failure code: $status"
                )
                _state.value = TtsState.Error(error)
                return@withContext Result.failure(IllegalStateException(error.message))
            }

            tts?.setOnUtteranceProgressListener(utteranceProgressListener)
            isInitialized = true
        }

        // Configure language
        val availability = checkLanguageAvailability(language)
        when (availability) {
            VoiceAvailability.AVAILABLE -> {
                val setLangResult = tts?.setLanguage(language.locale)
                if (setLangResult == TextToSpeech.LANG_MISSING_DATA || setLangResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val err = TtsError.MissingVoiceData(language)
                    _state.value = TtsState.LanguageUnavailable(language, VoiceAvailability.MISSING_DATA)
                    return@withContext Result.failure(IllegalStateException(err.message))
                }
                currentLanguage = language
                _state.value = TtsState.Idle
                Result.success(Unit)
            }
            VoiceAvailability.MISSING_DATA -> {
                _state.value = TtsState.LanguageUnavailable(language, VoiceAvailability.MISSING_DATA)
                Result.failure(IllegalStateException(TtsError.MissingVoiceData(language).message))
            }
            VoiceAvailability.NOT_SUPPORTED -> {
                _state.value = TtsState.LanguageUnavailable(language, VoiceAvailability.NOT_SUPPORTED)
                Result.failure(IllegalStateException(TtsError.UnsupportedLanguage(language.code).message))
            }
            VoiceAvailability.CHECKING -> {
                _state.value = TtsState.Initializing(language)
                Result.success(Unit)
            }
        }
    }

    override suspend fun speak(text: String, utteranceId: String): Result<Unit> = withContext(dispatcher) {
        if (!isInitialized || tts == null) {
            val err = TtsError.EngineNotInitialized("speak")
            _state.value = TtsState.Error(err)
            return@withContext Result.failure(IllegalStateException(err.message))
        }

        if (text.isBlank()) {
            val err = TtsError.EmptyText
            _state.value = TtsState.Error(err)
            return@withContext Result.failure(IllegalArgumentException(err.message))
        }

        if (text.length > TtsConfiguration.MAX_TEXT_CHARACTERS) {
            val err = TtsError.TextTooLong(text.length)
            _state.value = TtsState.Error(err)
            return@withContext Result.failure(IllegalArgumentException(err.message))
        }

        val lang = currentLanguage
        if (lang != null && checkLanguageAvailability(lang) != VoiceAvailability.AVAILABLE) {
            val err = TtsError.MissingVoiceData(lang)
            _state.value = TtsState.LanguageUnavailable(lang)
            return@withContext Result.failure(IllegalStateException(err.message))
        }

        if (!requestAudioFocus()) {
            val err = TtsError.AudioFocusDenied
            _state.value = TtsState.Error(err)
            return@withContext Result.failure(IllegalStateException(err.message))
        }

        _state.value = TtsState.Speaking(text = text, language = lang, utteranceId = utteranceId)

        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }

        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result == TextToSpeech.SUCCESS) {
            Result.success(Unit)
        } else {
            abandonAudioFocus()
            val err = TtsError.SynthesisError(result ?: -1, "speak() rejected request.")
            _state.value = TtsState.Error(err)
            Result.failure(IllegalStateException(err.message))
        }
    }

    override suspend fun stop(): Result<Unit> = withContext(dispatcher) {
        tts?.stop()
        abandonAudioFocus()
        if (isInitialized) {
            _state.value = TtsState.Idle
        }
        Result.success(Unit)
    }

    override suspend fun pause(): Result<Unit> = withContext(dispatcher) {
        tts?.stop()
        abandonAudioFocus()
        val current = _state.value
        if (current is TtsState.Speaking) {
            _state.value = TtsState.Paused(current.text, current.messageId)
        }
        Result.success(Unit)
    }

    override suspend fun release() = withContext(dispatcher) {
        tts?.stop()
        tts?.shutdown()
        tts = null
        abandonAudioFocus()
        isInitialized = false
        currentLanguage = null
        _state.value = TtsState.Idle
    }

    override fun isInitialized(): Boolean = isInitialized

    override fun setSpeechRate(rate: Float): Result<Unit> {
        val validation = TtsConfiguration.validateSpeechRate(rate)
        if (validation.isFailure) return Result.failure(validation.exceptionOrNull()!!)
        tts?.setSpeechRate(rate)
        return Result.success(Unit)
    }

    override fun setPitch(pitch: Float): Result<Unit> {
        val validation = TtsConfiguration.validatePitch(pitch)
        if (validation.isFailure) return Result.failure(validation.exceptionOrNull()!!)
        tts?.setPitch(pitch)
        return Result.success(Unit)
    }

    override fun checkLanguageAvailability(language: TtsLanguage): VoiceAvailability {
        val engine = tts ?: return VoiceAvailability.NOT_SUPPORTED
        return when (engine.isLanguageAvailable(language.locale)) {
            TextToSpeech.LANG_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> VoiceAvailability.AVAILABLE

            TextToSpeech.LANG_MISSING_DATA -> VoiceAvailability.MISSING_DATA
            TextToSpeech.LANG_NOT_SUPPORTED -> VoiceAvailability.NOT_SUPPORTED
            else -> VoiceAvailability.NOT_SUPPORTED
        }
    }

    override fun getCurrentLanguage(): TtsLanguage? = currentLanguage

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attr)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = request
            am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(audioFocusChangeListener)
        }
    }
}
