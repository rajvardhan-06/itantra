package com.itantra.app.audio

import java.util.Locale

/**
 * Encapsulates the output of a completed microphone recording session.
 *
 * Contains raw 16-bit PCM samples alongside metadata required for downstream
 * Voice Activity Detection (VAD), Speech-to-Text (STT) inference, or P2P transmission.
 *
 * Audio Format Specifications:
 * - Sample rate: 16,000 Hz
 * - Channels: 1 (Mono)
 * - Encoding: Linear PCM 16-bit Signed
 * - Byte order: Little-Endian (native Android AudioRecord format)
 */
data class AudioRecordingResult(
    /** Captured raw PCM audio bytes. */
    val pcmData: ByteArray,
    /** Audio sample rate in Hz (typically 16000). */
    val sampleRate: Int = AudioConfiguration.SAMPLE_RATE,
    /** Number of audio channels (typically 1 for Mono). */
    val channels: Int = AudioConfiguration.CHANNEL_COUNT,
    /** Bits per sample (typically 16). */
    val bitsPerSample: Int = AudioConfiguration.BITS_PER_SAMPLE,
    /** Total recorded duration in milliseconds. */
    val durationMs: Long = AudioConfiguration.bytesToDurationMs(pcmData.size),
    /** Total byte count of captured PCM data. */
    val byteCount: Int = pcmData.size
) {
    /** Duration in decimal seconds (e.g., 5.2s). */
    val durationSeconds: Float
        get() = durationMs / 1000f

    /** Formatted duration string for UI display (e.g., "00:05"). */
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }

    /** Human-readable size format (e.g., "160.0 KB"). */
    val formattedSize: String
        get() {
            val kb = byteCount / 1024.0
            return if (kb >= 1024.0) {
                String.format(Locale.US, "%.1f MB", kb / 1024.0)
            } else {
                String.format(Locale.US, "%.1f KB", kb)
            }
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioRecordingResult

        if (!pcmData.contentEquals(other.pcmData)) return false
        if (sampleRate != other.sampleRate) return false
        if (channels != other.channels) return false
        if (bitsPerSample != other.bitsPerSample) return false
        if (durationMs != other.durationMs) return false
        if (byteCount != other.byteCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pcmData.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + channels
        result = 31 * result + bitsPerSample
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + byteCount
        return result
    }

    override fun toString(): String {
        return "AudioRecordingResult(byteCount=$byteCount bytes, durationMs=${durationMs}ms, sampleRate=$sampleRate Hz, channels=$channels)"
    }
}
