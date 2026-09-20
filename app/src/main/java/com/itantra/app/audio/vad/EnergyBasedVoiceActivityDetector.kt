package com.itantra.app.audio.vad

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Robust, lightweight energy-based implementation of [VoiceActivityDetector].
 *
 * Employs Root-Mean-Square (RMS) and decibel relative to full scale (dBFS) energy
 * thresholding with exponential moving average smoothing, noise burst rejection,
 * inter-word pause tolerance, and automatic silence timeout detection.
 *
 * Designed for low latency and zero memory allocations during continuous stream processing.
 */
class EnergyBasedVoiceActivityDetector(
    override val configuration: VadConfiguration = VadConfiguration()
) : VoiceActivityDetector {

    private val lock = ReentrantLock()

    @Volatile
    private var _currentState: VadState = VadState.IDLE
    override val currentState: VadState
        get() = _currentState

    // Internal sample buffer for assembling complete analysis frames
    private val frameSize = configuration.frameSizeSamples
    private val frameDurationMs = configuration.frameDurationMs.toLong()
    private val frameBuffer = ShortArray(frameSize)
    private var frameBufferPos = 0

    // Tracking variables
    private var totalProcessedMs: Long = 0L
    private var smoothedEnergyDb: Float = AudioEnergyCalculator.SILENCE_DB_FLOOR
    private var lastRawEnergyDb: Float = AudioEnergyCalculator.SILENCE_DB_FLOOR
    private var lastRms: Float = 0.0f

    // Speech & Silence Durations
    private var candidateSpeechStartMs: Long = 0L
    private var speechStartMs: Long = 0L
    private var speechEndMs: Long = 0L
    private var consecutiveSpeechMs: Long = 0L
    private var consecutiveSilenceMs: Long = 0L
    private var totalSpeechDurationMs: Long = 0L

    init {
        configuration.validate()
    }

    override fun reset(): Unit = lock.withLock {
        _currentState = VadState.IDLE
        frameBufferPos = 0
        totalProcessedMs = 0L
        smoothedEnergyDb = AudioEnergyCalculator.SILENCE_DB_FLOOR
        lastRawEnergyDb = AudioEnergyCalculator.SILENCE_DB_FLOOR
        lastRms = 0.0f
        candidateSpeechStartMs = 0L
        speechStartMs = 0L
        speechEndMs = 0L
        consecutiveSpeechMs = 0L
        consecutiveSilenceMs = 0L
        totalSpeechDurationMs = 0L
    }

    override fun processAudio(samples: ShortArray, offset: Int, length: Int): VadResult = lock.withLock {
        if (length <= 0 || samples.isEmpty()) {
            return buildCurrentResult()
        }

        val safeOffset = offset.coerceIn(0, samples.size)
        val safeLength = length.coerceIn(0, samples.size - safeOffset)
        if (safeLength <= 0) return buildCurrentResult()

        var currentOffset = safeOffset
        var remainingSamples = safeLength

        while (remainingSamples > 0) {
            val needed = frameSize - frameBufferPos
            val toCopy = minOf(needed, remainingSamples)

            System.arraycopy(samples, currentOffset, frameBuffer, frameBufferPos, toCopy)
            frameBufferPos += toCopy
            currentOffset += toCopy
            remainingSamples -= toCopy

            // When a full frame has been accumulated, analyze it
            if (frameBufferPos == frameSize) {
                processSingleFrame(frameBuffer)
                frameBufferPos = 0
            }
        }

        return buildCurrentResult()
    }

    /**
     * Evaluates a complete 20 ms audio frame through the finite state machine.
     */
    private fun processSingleFrame(frame: ShortArray) {
        totalProcessedMs += frameDurationMs

        // 1. Calculate raw energy metrics
        lastRms = AudioEnergyCalculator.calculateRms(frame, 0, frame.size)
        lastRawEnergyDb = AudioEnergyCalculator.calculateDb(lastRms)

        // 2. Exponential Moving Average smoothing
        smoothedEnergyDb = if (smoothedEnergyDb <= AudioEnergyCalculator.SILENCE_DB_FLOOR) {
            lastRawEnergyDb
        } else {
            AudioEnergyCalculator.applySmoothing(
                currentDb = lastRawEnergyDb,
                previousDb = smoothedEnergyDb,
                alpha = configuration.energySmoothingFactor
            )
        }

        val isFrameSpeech = smoothedEnergyDb >= configuration.energyThresholdDb

        // 3. Evaluate Finite State Machine
        when (_currentState) {
            VadState.IDLE -> {
                if (isFrameSpeech) {
                    _currentState = VadState.POSSIBLE_SPEECH
                    consecutiveSpeechMs = frameDurationMs
                    candidateSpeechStartMs = (totalProcessedMs - frameDurationMs).coerceAtLeast(0L)
                }
            }

            VadState.POSSIBLE_SPEECH -> {
                if (isFrameSpeech) {
                    consecutiveSpeechMs += frameDurationMs
                    // Speech confirmed if consecutive frames meet confirmation threshold
                    if (consecutiveSpeechMs >= configuration.speechConfirmationDurationMs) {
                        _currentState = VadState.SPEECH_DETECTED
                        speechStartMs = candidateSpeechStartMs
                        totalSpeechDurationMs = consecutiveSpeechMs
                        consecutiveSilenceMs = 0L
                    }
                } else {
                    // Energy dropped before confirmation: transient click or noise burst rejected!
                    _currentState = VadState.IDLE
                    consecutiveSpeechMs = 0L
                    candidateSpeechStartMs = 0L
                }
            }

            VadState.SPEECH_DETECTED -> {
                if (isFrameSpeech) {
                    totalSpeechDurationMs += frameDurationMs
                    consecutiveSilenceMs = 0L
                } else {
                    _currentState = VadState.POSSIBLE_SILENCE
                    consecutiveSilenceMs = frameDurationMs
                }
            }

            VadState.POSSIBLE_SILENCE -> {
                if (isFrameSpeech) {
                    // User resumed speaking: treat previous silence as an inter-word pause
                    totalSpeechDurationMs += (consecutiveSilenceMs + frameDurationMs)
                    consecutiveSilenceMs = 0L
                    _currentState = VadState.SPEECH_DETECTED
                } else {
                    consecutiveSilenceMs += frameDurationMs
                    // Automatic stop trigger: continuous silence exceeded configured timeout
                    if (consecutiveSilenceMs >= configuration.silenceTimeoutMs) {
                        _currentState = VadState.SILENCE_DETECTED
                        speechEndMs = (totalProcessedMs - consecutiveSilenceMs).coerceAtLeast(speechStartMs)
                    }
                }
            }

            VadState.SILENCE_DETECTED,
            VadState.COMPLETED,
            VadState.ERROR -> {
                // Terminal states until reset() or finish()
            }
        }

        // 4. Hard ceiling check: maximum duration reached
        if (totalProcessedMs >= configuration.maxRecordingDurationMs) {
            if (_currentState == VadState.SPEECH_DETECTED || _currentState == VadState.POSSIBLE_SILENCE) {
                speechEndMs = totalProcessedMs
            }
            _currentState = VadState.COMPLETED
        }
    }

    override fun finish(): VadResult = lock.withLock {
        // If there are lingering samples in frameBuffer, process them
        if (frameBufferPos > 0) {
            val partial = frameBuffer.copyOf(frameBufferPos)
            processSingleFrame(partial)
            frameBufferPos = 0
        }

        if (_currentState == VadState.SPEECH_DETECTED || _currentState == VadState.POSSIBLE_SILENCE) {
            speechEndMs = totalProcessedMs
            _currentState = VadState.COMPLETED
        } else if (_currentState == VadState.SILENCE_DETECTED) {
            _currentState = VadState.COMPLETED
        } else if (_currentState != VadState.COMPLETED && _currentState != VadState.ERROR) {
            _currentState = VadState.COMPLETED
        }

        return buildCurrentResult()
    }

    private fun buildCurrentResult(): VadResult {
        val hasValidSegment = speechEndMs > speechStartMs && totalSpeechDurationMs >= configuration.minSpeechDurationMs
        val segment = if (hasValidSegment) {
            SpeechSegment(
                startMs = speechStartMs,
                endMs = speechEndMs,
                confidence = 1.0f
            )
        } else null

        val isSpeakingNow = _currentState == VadState.SPEECH_DETECTED ||
            _currentState == VadState.POSSIBLE_SPEECH

        return VadResult(
            state = _currentState,
            isSpeech = isSpeakingNow,
            energyDb = lastRawEnergyDb,
            smoothedEnergyDb = smoothedEnergyDb,
            rms = lastRms,
            speechDurationMs = totalSpeechDurationMs,
            silenceDurationMs = consecutiveSilenceMs,
            speechSegment = segment,
            timestampMs = totalProcessedMs
        )
    }
}
