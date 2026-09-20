package com.itantra.app.audio

import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Mock implementation of [AudioRecorder] used during unit testing and UI development.
 * Simulates realistic recording behaviour and waveform amplitude without requiring
 * physical microphone hardware or permissions.
 */
class MockAudioRecorder : AudioRecorder {

    @Volatile
    private var _isRecording = false

    var shouldFailStart: Boolean = false
    var customRecordedData: ByteArray? = null

    override fun isRecording(): Boolean = _isRecording

    override suspend fun startRecording(): Result<Unit> {
        if (shouldFailStart) {
            return Result.failure(SecurityException("RECORD_AUDIO permission denied"))
        }
        if (_isRecording) {
            return Result.failure(IllegalStateException("MockAudioRecorder is already recording"))
        }
        _isRecording = true
        return Result.success(Unit)
    }

    override suspend fun stopRecording(): Result<ByteArray> {
        if (!_isRecording) {
            return Result.failure(IllegalStateException("MockAudioRecorder is not currently recording"))
        }
        _isRecording = false
        val mockData = customRecordedData ?: ByteArray(AudioConfiguration.BYTES_PER_SECOND)
        return Result.success(mockData)
    }

    override suspend fun cancelRecording() {
        _isRecording = false
    }

    override fun observeAmplitude(): Flow<Float> = flow {
        var t = 0.0
        while (true) {
            val amplitude = if (_isRecording) {
                val base = 0.4f + (0.4f * sin(t * 3.0).toFloat())
                val noise = (Math.random() * 0.2f).toFloat()
                (base + noise).coerceIn(0f, 1f)
            } else {
                0f
            }
            emit(amplitude)
            t += 0.1
            delay(50L) // ~20 fps
        }
    }

    private var frameListener: ((ShortArray, Int) -> Unit)? = null

    override fun setAudioFrameListener(listener: ((ShortArray, Int) -> Unit)?) {
        frameListener = listener
    }

    /**
     * Helper for test suites to manually feed synthetic PCM frames to the registered listener.
     */
    fun simulateAudioFrame(samples: ShortArray, length: Int = samples.size) {
        frameListener?.invoke(samples, length)
    }

    override fun release() {
        _isRecording = false
        frameListener = null
    }
}
