package com.itantra.app.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Dedicated ViewModel for managing the lifecycle, permissions, and reactive state
 * of real-time microphone recording.
 *
 * Keeps audio capture logic isolated from UI Composables and acts as a single
 * source of truth for audio recording sessions.
 */
class AudioRecordingViewModel(
    private val audioRecorder: AudioRecorder = MockAudioRecorder(),
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _isPermanentlyDenied = MutableStateFlow(false)
    val isPermanentlyDenied: StateFlow<Boolean> = _isPermanentlyDenied.asStateFlow()

    private var durationTickerJob: Job? = null
    private var recordingStartTimeMs: Long = 0L

    init {
        observeAmplitude()
    }

    private fun observeAmplitude() {
        scope.launch {
            audioRecorder.observeAmplitude().collect { level ->
                _audioLevel.value = level
                val current = _recordingState.value
                if (current is RecordingState.Recording) {
                    _recordingState.value = current.copy(audioLevel = level)
                }
            }
        }
    }

    /**
     * Updates permission state following an Android runtime permission request.
     */
    fun onPermissionResult(isGranted: Boolean, permanentlyDenied: Boolean = false) {
        _hasPermission.value = isGranted
        _isPermanentlyDenied.value = permanentlyDenied
        if (isGranted) {
            if (_recordingState.value is RecordingState.RequestingPermission) {
                _recordingState.value = RecordingState.Ready
            }
        } else {
            val message = if (permanentlyDenied) {
                "Microphone permission permanently denied. Enable it in App Settings."
            } else {
                "Microphone permission is required to record audio."
            }
            _recordingState.value = RecordingState.Error(message)
        }
    }

    /**
     * Signals that permission request has been launched.
     */
    fun onRequestingPermission() {
        _recordingState.value = RecordingState.RequestingPermission
    }

    /**
     * Initiates microphone audio capture.
     * Prevents multiple concurrent starts and ensures permission is available.
     */
    fun startRecording() {
        val currentState = _recordingState.value
        if (currentState is RecordingState.Recording || currentState is RecordingState.Stopping) {
            return
        }

        scope.launch {
            _recordingState.value = RecordingState.Recording(
                durationMs = 0L,
                byteCount = 0,
                audioLevel = 0f
            )
            recordingStartTimeMs = System.currentTimeMillis()

            val result = audioRecorder.startRecording()
            if (result.isSuccess) {
                startDurationTicker()
            } else {
                stopDurationTicker()
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to start recording."
                _recordingState.value = RecordingState.Error(errorMsg, result.exceptionOrNull())
            }
        }
    }

    /**
     * Stops recording, collects captured PCM data, and emits [RecordingState.Completed].
     */
    fun stopRecording() {
        val currentState = _recordingState.value
        if (currentState !is RecordingState.Recording) return

        stopDurationTicker()
        _recordingState.value = RecordingState.Stopping

        scope.launch {
            val result = audioRecorder.stopRecording()
            if (result.isSuccess) {
                val pcmData = result.getOrThrow()
                val recordingResult = AudioRecordingResult(pcmData = pcmData)
                _recordingState.value = RecordingState.Completed(recordingResult)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Recording failed."
                _recordingState.value = RecordingState.Error(errorMsg, result.exceptionOrNull())
            }
        }
    }

    /**
     * Cancels active recording and discards buffered audio.
     */
    fun cancelRecording() {
        stopDurationTicker()
        scope.launch {
            audioRecorder.cancelRecording()
            _recordingState.value = RecordingState.Cancelled
            delay(800L)
            _recordingState.value = RecordingState.Idle
        }
    }

    /**
     * Resets recording state back to [RecordingState.Idle].
     */
    fun reset() {
        stopDurationTicker()
        _recordingState.value = RecordingState.Idle
    }

    /**
     * Dismisses an active error state.
     */
    fun dismissError() {
        if (_recordingState.value is RecordingState.Error) {
            _recordingState.value = RecordingState.Idle
        }
    }

    private fun startDurationTicker() {
        durationTickerJob?.cancel()
        durationTickerJob = scope.launch {
            while (true) {
                delay(100L) // Update ~10 times per second
                val elapsed = System.currentTimeMillis() - recordingStartTimeMs

                if (elapsed >= AudioConfiguration.MAX_RECORDING_DURATION_MS) {
                    // Maximum duration ceiling reached (30s). Trigger automatic stop.
                    stopRecording()
                    break
                }

                val currentBytes = AudioConfiguration.durationMsToBytes(elapsed)
                val current = _recordingState.value
                if (current is RecordingState.Recording) {
                    _recordingState.value = current.copy(
                        durationMs = elapsed,
                        byteCount = currentBytes
                    )
                }
            }
        }
    }

    private fun stopDurationTicker() {
        durationTickerJob?.cancel()
        durationTickerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopDurationTicker()
        audioRecorder.release()
    }
}
