package com.itantra.app.audio.vad

import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AudioEnergyCalculator].
 */
class AudioEnergyCalculatorTest {

    @Test
    fun `calculateRms on empty or zero-length input returns zero`() {
        assertEquals(0.0f, AudioEnergyCalculator.calculateRms(ShortArray(0)), 0.001f)
        assertEquals(0.0f, AudioEnergyCalculator.calculateRms(ShortArray(100), 0, 0), 0.001f)
        assertEquals(0.0f, AudioEnergyCalculator.calculateRms(ShortArray(100), 10, -5), 0.001f)
    }

    @Test
    fun `calculateRms on pure silence returns zero`() {
        val silence = ShortArray(320) { 0 }
        assertEquals(0.0f, AudioEnergyCalculator.calculateRms(silence), 0.001f)
    }

    @Test
    fun `calculateRms on constant amplitude calculates correctly`() {
        val constant = ShortArray(320) { 1000 }
        assertEquals(1000.0f, AudioEnergyCalculator.calculateRms(constant), 0.01f)
    }

    @Test
    fun `calculateRms on full-scale sine wave approximates expected RMS`() {
        // RMS of full-scale sine wave (amplitude A = 32767) is A / sqrt(2) ≈ 23169.7
        val sine = ShortArray(1600) { i ->
            (32767 * sin(2.0 * Math.PI * 440.0 * i / 16000.0)).toInt().toShort()
        }
        val rms = AudioEnergyCalculator.calculateRms(sine)
        assertEquals(23170.0f, rms, 50.0f)
    }

    @Test
    fun `calculateDb returns floor for silence and near 0 for full scale`() {
        assertEquals(AudioEnergyCalculator.SILENCE_DB_FLOOR, AudioEnergyCalculator.calculateDb(0.0f), 0.001f)

        // RMS of 32767 -> 0 dBFS
        assertEquals(0.0f, AudioEnergyCalculator.calculateDb(32767.0f), 0.01f)

        // RMS of ~3276.7 -> ~ -20 dBFS
        assertEquals(-20.0f, AudioEnergyCalculator.calculateDb(3276.7f), 0.1f)
    }

    @Test
    fun `calculateNormalizedAmplitude bounds between 0 and 1`() {
        assertEquals(0.0f, AudioEnergyCalculator.calculateNormalizedAmplitude(0.0f), 0.001f)
        assertEquals(1.0f, AudioEnergyCalculator.calculateNormalizedAmplitude(32767.0f), 0.001f)
        assertEquals(0.5f, AudioEnergyCalculator.calculateNormalizedAmplitude(16383.5f), 0.01f)
    }

    @Test
    fun `applySmoothing weights properly with alpha`() {
        val prev = -60.0f
        val current = -20.0f
        // alpha = 0.5 -> average = -40.0
        assertEquals(-40.0f, AudioEnergyCalculator.applySmoothing(current, prev, 0.5f), 0.001f)
        // alpha = 1.0 -> current = -20.0
        assertEquals(-20.0f, AudioEnergyCalculator.applySmoothing(current, prev, 1.0f), 0.001f)
        // alpha = 0.0 -> prev = -60.0
        assertEquals(-60.0f, AudioEnergyCalculator.applySmoothing(current, prev, 0.0f), 0.001f)
    }
}
