package com.itantra.app.audio.vad

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Utility for computing energy metrics from raw 16-bit PCM audio samples.
 *
 * Implements Root-Mean-Square (RMS) amplitude and conversion to decibels
 * relative to full scale (dBFS), where 0 dBFS represents maximum clipping
 * amplitude (32,767 for signed 16-bit PCM).
 */
object AudioEnergyCalculator {

    /** Maximum possible sample value for signed 16-bit PCM. */
    const val MAX_PCM_16_VALUE: Float = 32767.0f

    /** Floor value in dBFS representing absolute silence. */
    const val SILENCE_DB_FLOOR: Float = -100.0f

    /**
     * Calculates the Root-Mean-Square (RMS) amplitude for a slice of 16-bit PCM samples.
     *
     * @param samples Array of 16-bit signed PCM audio samples.
     * @param offset Starting index in [samples].
     * @param length Number of samples to process.
     * @return Calculated RMS value (0.0f to 32767.0f).
     */
    fun calculateRms(samples: ShortArray, offset: Int = 0, length: Int = samples.size): Float {
        if (length <= 0 || samples.isEmpty()) return 0.0f

        val safeOffset = offset.coerceIn(0, samples.size)
        val safeLength = length.coerceIn(0, samples.size - safeOffset)
        if (safeLength <= 0) return 0.0f

        var sumOfSquares = 0.0
        for (i in safeOffset until (safeOffset + safeLength)) {
            val sample = samples[i].toDouble()
            sumOfSquares += sample * sample
        }

        return sqrt(sumOfSquares / safeLength).toFloat()
    }

    /**
     * Converts an RMS amplitude into decibels relative to full scale (dBFS).
     *
     * Result is bounded between [SILENCE_DB_FLOOR] (-100 dBFS) and 0.0 dBFS.
     */
    fun calculateDb(rms: Float): Float {
        if (rms <= 0.0f) return SILENCE_DB_FLOOR
        val ratio = max(rms / MAX_PCM_16_VALUE, 1e-5f)
        val db = 20.0f * log10(ratio)
        return db.coerceIn(SILENCE_DB_FLOOR, 0.0f)
    }

    /**
     * Normalizes an RMS amplitude to a 0.0f .. 1.0f scale.
     */
    fun calculateNormalizedAmplitude(rms: Float): Float {
        return (rms / MAX_PCM_16_VALUE).coerceIn(0.0f, 1.0f)
    }

    /**
     * Applies Exponential Moving Average (EMA) smoothing between current and previous energy levels.
     *
     * @param currentDb Current frame energy in dBFS.
     * @param previousDb Previous smoothed energy in dBFS.
     * @param alpha Smoothing factor between 0.0f and 1.0f (higher = more reactive).
     */
    fun applySmoothing(currentDb: Float, previousDb: Float, alpha: Float): Float {
        val safeAlpha = alpha.coerceIn(0.0f, 1.0f)
        return (safeAlpha * currentDb) + ((1.0f - safeAlpha) * previousDb)
    }
}
