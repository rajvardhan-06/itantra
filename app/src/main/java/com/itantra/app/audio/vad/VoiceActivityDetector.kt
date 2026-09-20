package com.itantra.app.audio.vad

/**
 * Replaceable interface for Voice Activity Detection (VAD).
 *
 * Designed to process continuous 16-bit PCM audio samples, identify speech onset/offset,
 * reject noise transients, and produce [VadResult] updates.
 */
interface VoiceActivityDetector {

    /** Current configuration parameters. */
    val configuration: VadConfiguration

    /** Current state of the detector. */
    val currentState: VadState

    /**
     * Resets internal detector state, energy smoothing, and segment bounds.
     */
    fun reset()

    /**
     * Processes a buffer of 16-bit PCM samples.
     *
     * Incomplete frames are buffered internally until a full analysis frame is assembled.
     *
     * @param samples Array of raw 16-bit signed PCM samples.
     * @param offset Starting index in [samples].
     * @param length Number of samples to process.
     * @return The updated [VadResult] following this buffer's evaluation.
     */
    fun processAudio(samples: ShortArray, offset: Int = 0, length: Int = samples.size): VadResult

    /**
     * Concludes the active VAD session, flushes any buffered audio,
     * and finalizes the detected [SpeechSegment].
     */
    fun finish(): VadResult
}
