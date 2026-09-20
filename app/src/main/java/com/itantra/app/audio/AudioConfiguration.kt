package com.itantra.app.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

/**
 * Configuration parameters and audio specifications for the iTantra audio pipeline.
 *
 * Designed for low-bandwidth, offline speech processing (VAD, STT, and P2P transmission):
 * - Sample rate: 16,000 Hz (16 kHz, standard for modern neural STT like Whisper/Sherpa/Vosk)
 * - Channel: Mono (1 channel)
 * - Encoding: PCM 16-bit Signed Little-Endian
 * - Audio Source: VOICE_RECOGNITION (hardware noise suppression / AGC tuned for speech) with MIC fallback
 * - Maximum recording duration: 30 seconds
 * - Memory ceiling: 960,000 bytes (~937.5 KiB)
 */
object AudioConfiguration {

    /** Sample rate in Hertz (16,000 Hz). */
    const val SAMPLE_RATE: Int = 16_000

    /** Mono channel configuration for speech recognition. */
    const val CHANNEL_CONFIG: Int = AudioFormat.CHANNEL_IN_MONO

    /** Number of audio channels (1 = Mono). */
    const val CHANNEL_COUNT: Int = 1

    /** PCM 16-bit per sample (linear PCM). */
    const val AUDIO_FORMAT: Int = AudioFormat.ENCODING_PCM_16BIT

    /** Bits per individual sample (16 bits). */
    const val BITS_PER_SAMPLE: Int = 16

    /** Bytes per individual sample (2 bytes). */
    const val BYTES_PER_SAMPLE: Int = BITS_PER_SAMPLE / 8

    /**
     * Primary audio input source: tuned for voice recognition (DSP beamforming / acoustic echo cancellation).
     */
    const val AUDIO_SOURCE_PRIMARY: Int = MediaRecorder.AudioSource.VOICE_RECOGNITION

    /**
     * Fallback audio input source if VOICE_RECOGNITION is unavailable on the hardware.
     */
    const val AUDIO_SOURCE_FALLBACK: Int = MediaRecorder.AudioSource.MIC

    /** Maximum allowed recording duration in seconds. */
    const val MAX_RECORDING_DURATION_SECONDS: Int = 30

    /** Maximum allowed recording duration in milliseconds. */
    const val MAX_RECORDING_DURATION_MS: Long = MAX_RECORDING_DURATION_SECONDS * 1000L

    /**
     * Throughput: 16,000 samples/sec * 1 channel * 2 bytes/sample = 32,000 bytes/sec.
     */
    const val BYTES_PER_SECOND: Int = SAMPLE_RATE * CHANNEL_COUNT * BYTES_PER_SAMPLE

    /**
     * Maximum expected audio payload for a full 30-second recording:
     * 32,000 bytes/sec * 30 sec = 960,000 bytes (~937.5 KiB).
     */
    const val MAX_AUDIO_BYTES: Int = BYTES_PER_SECOND * MAX_RECORDING_DURATION_SECONDS

    /**
     * Absolute hard memory safety ceiling for buffer allocation: 1 MiB (1,048,576 bytes).
     * Prevents unbounded memory growth or OOM under any circumstances.
     */
    const val MAX_BUFFER_CAPACITY_BYTES: Int = 1024 * 1024

    /**
     * Calculate the minimum buffer size supported by the Android audio subsystem.
     * Returns a valid buffer size, or -1 if the device does not support this configuration.
     */
    fun calculateMinBufferSize(): Int {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        return if (minBufferSize <= 0 ||
            minBufferSize == AudioRecord.ERROR ||
            minBufferSize == AudioRecord.ERROR_BAD_VALUE
        ) {
            -1
        } else {
            minBufferSize
        }
    }

    /**
     * Convert a raw byte count to duration in milliseconds.
     */
    fun bytesToDurationMs(byteCount: Int): Long {
        if (byteCount <= 0) return 0L
        return (byteCount.toLong() * 1000L) / BYTES_PER_SECOND
    }

    /**
     * Convert duration in milliseconds to expected raw byte count.
     */
    fun durationMsToBytes(durationMs: Long): Int {
        if (durationMs <= 0L) return 0
        return ((durationMs * BYTES_PER_SECOND) / 1000L).toInt().coerceAtMost(MAX_AUDIO_BYTES)
    }
}
