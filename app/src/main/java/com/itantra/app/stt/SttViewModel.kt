package com.itantra.app.stt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itantra.app.stt.repository.SpeechRecognitionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Dedicated ViewModel for managing the Speech-to-Text inference lifecycle,
 * model installation checks, and reactive recognition states.
 */
class SttViewModel(
    private val repository: SpeechRecognitionRepository,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _sttState = MutableStateFlow<SttState>(SttState.Idle)
    val sttState: StateFlow<SttState> = _sttState.asStateFlow()

    private val _currentLanguage = MutableStateFlow(SttLanguage.HINDI)
    val currentLanguage: StateFlow<SttLanguage> = _currentLanguage.asStateFlow()

    private val _isModelInstalled = MutableStateFlow(false)
    val isModelInstalled: StateFlow<Boolean> = _isModelInstalled.asStateFlow()

    private var recognitionJob: Job? = null

    init {
        scope.launch {
            repository.installedLanguages.collect { installedSet ->
                _isModelInstalled.value = installedSet.contains(_currentLanguage.value)
            }
        }
        selectLanguage(_currentLanguage.value)
    }

    fun selectLanguage(language: SttLanguage) {
        _currentLanguage.value = language
        _isModelInstalled.value = repository.isModelInstalled(language)

        if (!language.isModelAvailable) {
            _sttState.value = SttState.UnsupportedLanguage(language)
            return
        }

        if (!_isModelInstalled.value) {
            _sttState.value = SttState.ModelNotInstalled(language)
            return
        }

        scope.launch {
            _sttState.value = SttState.Initializing(language)
            val result = repository.initializeLanguage(language)
            if (result.isSuccess) {
                _sttState.value = SttState.Ready(language)
            } else {
                _sttState.value = SttState.Error(
                    SttError.InitializationFailed(result.exceptionOrNull()?.message ?: "Initialization failed")
                )
            }
        }
    }

    fun installCurrentModel() {
        val lang = _currentLanguage.value
        scope.launch {
            _sttState.value = SttState.Initializing(lang)
            val result = repository.installModel(lang)
            if (result.isSuccess) {
                selectLanguage(lang)
            } else {
                _sttState.value = SttState.Error(
                    SttError.ModelLoadingFailed(result.exceptionOrNull()?.message ?: "Installation failed")
                )
            }
        }
    }

    fun transcribeAudio(audioBytes: ByteArray) {
        val lang = _currentLanguage.value

        if (!repository.isEngineInitialized()) {
            if (!_isModelInstalled.value) {
                _sttState.value = SttState.ModelNotInstalled(lang)
                return
            }
        }

        recognitionJob?.cancel()
        recognitionJob = scope.launch {
            _sttState.value = SttState.Recognizing(0L)
            val startTime = System.currentTimeMillis()

            val result = repository.transcribeAudio(audioBytes)
            val duration = System.currentTimeMillis() - startTime

            if (result.isSuccess) {
                val sttResult = result.getOrThrow()
                if (sttResult.isEmpty) {
                    _sttState.value = SttState.NoSpeechDetected
                } else {
                    _sttState.value = SttState.ResultAvailable(sttResult.copy(durationMs = duration))
                }
            } else {
                val ex = result.exceptionOrNull()
                _sttState.value = SttState.Error(
                    SttError.RecognitionFailed(ex?.message ?: "Recognition failed.")
                )
            }
        }
    }

    fun cancelRecognition() {
        recognitionJob?.cancel()
        recognitionJob = null
        _sttState.value = SttState.Cancelled
    }

    fun clearResult() {
        if (_sttState.value !is SttState.Recognizing) {
            _sttState.value = if (_isModelInstalled.value) SttState.Ready(_currentLanguage.value)
            else SttState.ModelNotInstalled(_currentLanguage.value)
        }
    }

    override fun onCleared() {
        super.onCleared()
        recognitionJob?.cancel()
        scope.launch {
            repository.release()
        }
    }
}
