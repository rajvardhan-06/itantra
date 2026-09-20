package com.itantra.app.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Production-grade native [AudioRecorder] backed by Android's [AudioRecord] API.
 *
 * Captures 16 kHz, Mono, 16-bit Linear PCM audio on a dedicated background coroutine.
 *
 * Key Capabilities:
 * - Dynamic minimum buffer calculation with safety factor.
 * - Hardware acoustic echo/noise reduction via [AudioConfiguration.AUDIO_SOURCE_PRIMARY]
 *   with fallback to [AudioConfiguration.AUDIO_SOURCE_FALLBACK].
 * - Real-time Root-Mean-Square (RMS) amplitude calculation emitted to [observeAmplitude].
 * - Enforces memory and duration bounds via [AudioBufferManager] (max 30 seconds / ~960 KB).
 * - Safe concurrency protection preventing multiple simultaneous sessions.
 * - Graceful error handling and hardware resource cleanup.
 */
class AudioRecordRecorder(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val bufferManager: AudioBufferManager = AudioBufferManager()
) : AudioRecorder {

    companion object {
        private const val TAG = "iTantra-Audio"
    }

    private val mutex = Mutex()
    private val _isRecording = AtomicBoolean(false)
    private var recordingJob: Job? = null
    private var audioRecord: AudioRecord? = null

    private val _amplitudeFlow = MutableStateFlow(0f)
    private var audioFrameListener: ((ShortArray, Int) -> Unit)? = null

    override fun isRecording(): Boolean = _isRecording.get()

    override fun observeAmplitude(): Flow<Float> = _amplitudeFlow.asStateFlow()

    override fun setAudioFrameListener(listener: ((ShortArray, Int) -> Unit)?) {
        audioFrameListener = listener
    }

    override suspend fun startRecording(): Result<Unit> = mutex.withLock {
        withContext(ioDispatcher) {
            if (_isRecording.get()) {
                Log.w(TAG, "startRecording() called while already recording. Ignoring request.")
                return@withContext Result.failure(
                    IllegalStateException("A recording session is already active.")
                )
            }

            // 1. Permission Check
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "startRecording() failed: RECORD_AUDIO permission not granted.")
                return@withContext Result.failure(
                    SecurityException("Microphone permission (RECORD_AUDIO) has not been granted.")
                )
            }

            // 2. Buffer Size Calculation
            val minBufferSize = AudioConfiguration.calculateMinBufferSize()
            if (minBufferSize <= 0) {
                Log.e(TAG, "startRecording() failed: AudioRecord configuration not supported by hardware.")
                return@withContext Result.failure(
                    IllegalStateException("Audio hardware does not support 16kHz 16-bit Mono PCM recording.")
                )
            }

            // Safety factor (2x minBufferSize, at least 2048 bytes) for stable read loop
            val readBufferSize = maxOf(minBufferSize * 2, 2048)
            Log.d(TAG, "Calculated minBufferSize=$minBufferSize bytes, readBufferSize=$readBufferSize bytes")

            // 3. AudioRecord Initialization with Fallback
            val recordInstance = createAudioRecord(readBufferSize)
            if (recordInstance == null || recordInstance.state != AudioRecord.STATE_INITIALIZED) {
                recordInstance?.release()
                Log.e(TAG, "AudioRecord initialization failed for both VOICE_RECOGNITION and MIC sources.")
                return@withContext Result.failure(
                    IllegalStateException("Failed to initialize microphone hardware. The audio device may be in use.")
                )
            }
            audioRecord = recordInstance

            // 4. Start Hardware Capture
            try {
                recordInstance.startRecording()
                if (recordInstance.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    recordInstance.stop()
                    recordInstance.release()
                    audioRecord = null
                    Log.e(TAG, "AudioRecord failed to enter RECORDSTATE_RECORDING state.")
                    return@withContext Result.failure(
                        IllegalStateException("Microphone could not begin capturing audio.")
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception starting AudioRecord: ${e.message}", e)
                recordInstance.release()
                audioRecord = null
                return@withContext Result.failure(e)
            }

            bufferManager.clear()
            _isRecording.set(true)
            _amplitudeFlow.value = 0f
            Log.i(TAG, "Microphone recording started successfully at ${AudioConfiguration.SAMPLE_RATE} Hz (16-bit Mono)")

            // 5. Launch Background Read Coroutine
            recordingJob = CoroutineScope(ioDispatcher).launch {
                runAudioCaptureLoop(recordInstance, readBufferSize)
            }

            Result.success(Unit)
        }
    }

    override suspend fun stopRecording(): Result<ByteArray> = mutex.withLock {
        withContext(ioDispatcher) {
            if (!_isRecording.get()) {
                Log.w(TAG, "stopRecording() called but recorder is not active.")
                return@withContext Result.failure(
                    IllegalStateException("No active recording session to stop.")
                )
            }

            _isRecording.set(false)
            _amplitudeFlow.value = 0f

            // Cancel reading job and wait for clean shutdown
            recordingJob?.cancel()
            recordingJob = null

            // Stop and release hardware AudioRecord
            safeStopAndReleaseAudioRecord()

            val capturedData = bufferManager.toByteArray()
            val totalBytes = capturedData.size
            val durationMs = bufferManager.getDurationMs()

            Log.i(TAG, "Microphone recording stopped. Captured $totalBytes bytes (${durationMs}ms)")

            if (totalBytes == 0) {
                Log.w(TAG, "Recording finished with 0 bytes captured.")
                return@withContext Result.failure(
                    IllegalStateException("No audio captured from the microphone.")
                )
            }

            Result.success(capturedData)
        }
    }

    override suspend fun cancelRecording() = mutex.withLock {
        withContext(ioDispatcher) {
            if (_isRecording.get()) {
                Log.i(TAG, "Microphone recording cancelled by user. Discarding audio buffer.")
                _isRecording.set(false)
                _amplitudeFlow.value = 0f
                recordingJob?.cancel()
                recordingJob = null
                safeStopAndReleaseAudioRecord()
                bufferManager.clear()
            }
        }
    }

    override fun release() {
        _isRecording.set(false)
        _amplitudeFlow.value = 0f
        recordingJob?.cancel()
        recordingJob = null
        safeStopAndReleaseAudioRecord()
        bufferManager.clear()
        Log.i(TAG, "AudioRecordRecorder hardware resources released.")
    }

    /**
     * Continuous audio read loop executing on [Dispatchers.IO].
     */
    private suspend fun runAudioCaptureLoop(record: AudioRecord, readBufferSize: Int) {
        val shortBuffer = ShortArray(readBufferSize / 2)
        val byteBuffer = ByteArray(readBufferSize)

        try {
            while (_isRecording.get() && kotlin.coroutines.coroutineContext.isActive) {
                val shortsRead = record.read(shortBuffer, 0, shortBuffer.size)

                if (shortsRead > 0) {
                    // Dispatch raw PCM frames to VAD listener off main thread
                    try {
                        audioFrameListener?.invoke(shortBuffer, shortsRead)
                    } catch (e: Exception) {
                        Log.w(TAG, "Exception in audioFrameListener: ${e.message}")
                    }

                    // 1. Calculate normalized Root Mean Square (RMS) amplitude
                    var sumOfSquares = 0.0
                    for (i in 0 until shortsRead) {
                        val sample = shortBuffer[i].toInt()
                        sumOfSquares += sample * sample
                    }
                    val rms = sqrt(sumOfSquares / shortsRead)
                    // Maximum short amplitude is 32767. Normalize to 0.0f - 1.0f
                    val normalizedAmplitude = (rms / 32767.0).toFloat().coerceIn(0f, 1f)
                    _amplitudeFlow.value = normalizedAmplitude

                    // 2. Convert ShortArray to Little-Endian ByteArray
                    val bytesToWrite = shortsRead * 2
                    for (i in 0 until shortsRead) {
                        val s = shortBuffer[i].toInt()
                        byteBuffer[i * 2] = (s and 0xFF).toByte()
                        byteBuffer[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
                    }

                    // 3. Append to memory-bounded buffer manager
                    val accepted = bufferManager.append(byteBuffer, 0, bytesToWrite)
                    if (!accepted || bufferManager.isFull()) {
                        Log.i(TAG, "Maximum recording capacity reached (${bufferManager.getByteCount()} bytes). Stopping capture loop.")
                        _isRecording.set(false)
                        break
                    }
                } else if (shortsRead < 0) {
                    val errorMsg = when (shortsRead) {
                        AudioRecord.ERROR_INVALID_OPERATION -> "ERROR_INVALID_OPERATION"
                        AudioRecord.ERROR_BAD_VALUE -> "ERROR_BAD_VALUE"
                        AudioRecord.ERROR_DEAD_OBJECT -> "ERROR_DEAD_OBJECT"
                        else -> "Unknown AudioRecord error: $shortsRead"
                    }
                    Log.e(TAG, "AudioRecord read error: $errorMsg")
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during audio capture loop: ${e.message}", e)
        } finally {
            _amplitudeFlow.value = 0f
        }
    }

    /**
     * Initializes [AudioRecord], attempting [AudioConfiguration.AUDIO_SOURCE_PRIMARY] first,
     * then falling back to [AudioConfiguration.AUDIO_SOURCE_FALLBACK].
     */
    private fun createAudioRecord(bufferSize: Int): AudioRecord? {
        // Try Primary Source: VOICE_RECOGNITION
        try {
            val primaryRecord = AudioRecord(
                AudioConfiguration.AUDIO_SOURCE_PRIMARY,
                AudioConfiguration.SAMPLE_RATE,
                AudioConfiguration.CHANNEL_CONFIG,
                AudioConfiguration.AUDIO_FORMAT,
                bufferSize
            )
            if (primaryRecord.state == AudioRecord.STATE_INITIALIZED) {
                Log.d(TAG, "Initialized AudioRecord with primary source VOICE_RECOGNITION")
                return primaryRecord
            }
            primaryRecord.release()
        } catch (e: SecurityException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed initializing with VOICE_RECOGNITION source: ${e.message}")
        }

        // Try Fallback Source: MIC
        return try {
            val fallbackRecord = AudioRecord(
                AudioConfiguration.AUDIO_SOURCE_FALLBACK,
                AudioConfiguration.SAMPLE_RATE,
                AudioConfiguration.CHANNEL_CONFIG,
                AudioConfiguration.AUDIO_FORMAT,
                bufferSize
            )
            if (fallbackRecord.state == AudioRecord.STATE_INITIALIZED) {
                Log.d(TAG, "Initialized AudioRecord with fallback source MIC")
                fallbackRecord
            } else {
                fallbackRecord.release()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed initializing fallback MIC source: ${e.message}")
            null
        }
    }

    /**
     * Stops and releases [AudioRecord] safely, ignoring lifecycle exceptions.
     */
    private fun safeStopAndReleaseAudioRecord() {
        audioRecord?.let { record ->
            try {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exception while stopping AudioRecord: ${e.message}")
            }
            try {
                record.release()
            } catch (e: Exception) {
                Log.w(TAG, "Exception while releasing AudioRecord: ${e.message}")
            }
        }
        audioRecord = null
    }
}
