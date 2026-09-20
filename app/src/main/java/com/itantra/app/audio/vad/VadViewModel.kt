package com.itantra.app.audio.vad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itantra.app.audio.AudioRecorder
import com.itantra.app.audio.MockAudioRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Dedicated ViewModel for managing real-time Voice Activity Detection (VAD) sessions.
 *
 * Coordinates sample processing between [AudioRecorder] and [VoiceActivityDetector],
 * handles automatic stop on speech completion / silence timeout, and exposes observable
 * reactive states for the UI.
 */
class VadViewModel(
    private val audioRecorder: AudioRecorder = MockAudioRecorder(),
    val detector: VoiceActivityDetector = EnergyBasedVoiceActivityDetector(),
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _vadState = MutableStateFlow(VadState.IDLE)
    val vadState: StateFlow<VadState> = _vadState.asStateFlow()

    private val _vadResult = MutableStateFlow(VadResult.IDLE)
    val vadResult: StateFlow<VadResult> = _vadResult.asStateFlow()

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    /**
     * Optional callback triggered when silence timeout confirms the user finished speaking.
     */
    var onSpeechEndedListener: ((SpeechSegment?) -> Unit)? = null

    init {
        audioRecorder.setAudioFrameListener { samples, length ->
            if (_isSessionActive.value) {
                processAudioFrame(samples, length)
            }
        }
    }

    /**
     * Starts a new VAD session, resetting the detector and arming listeners.
     */
    fun startSession() {
        detector.reset()
        _vadState.value = VadState.IDLE
        _vadResult.value = VadResult.IDLE
        _isSessionActive.value = true
    }

    /**
     * Feeds incoming PCM samples into the detector and evaluates state changes.
     */
    fun processAudioFrame(samples: ShortArray, length: Int) {
        if (!_isSessionActive.value) return

        val result = detector.processAudio(samples, 0, length)
        _vadState.value = result.state
        _vadResult.value = result

        // Check if silence timeout triggered speech completion
        if (result.state == VadState.SILENCE_DETECTED) {
            _isSessionActive.value = false
            scope.launch {
                onSpeechEndedListener?.invoke(result.speechSegment)
            }
        } else if (result.state == VadState.COMPLETED) {
            _isSessionActive.value = false
            scope.launch {
                onSpeechEndedListener?.invoke(result.speechSegment)
            }
        }
    }

    /**
     * Concludes the VAD session and flushes any pending speech segments.
     */
    fun finishSession(): VadResult {
        _isSessionActive.value = false
        val finalResult = detector.finish()
        _vadState.value = finalResult.state
        _vadResult.value = finalResult
        return finalResult
    }

    /**
     * Cancels the active VAD session and resets states to IDLE.
     */
    fun cancelSession() {
        _isSessionActive.value = false
        detector.reset()
        _vadState.value = VadState.IDLE
        _vadResult.value = VadResult.IDLE
    }

    /**
     * Resets the ViewModel to initial state.
     */
    fun reset() {
        cancelSession()
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.setAudioFrameListener(null)
        cancelSession()
    }
}
