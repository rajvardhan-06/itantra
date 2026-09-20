package com.itantra.app.audio.vad

/**
 * Finite state machine states for Voice Activity Detection.
 */
enum class VadState {
    /**
     * Initial state. Detector is idle and waiting for incoming audio frames.
     */
    IDLE,

    /**
     * Energy has exceeded threshold; verifying consecutive frames to reject short noise bursts.
     */
    POSSIBLE_SPEECH,

    /**
     * Speech has been confirmed and the user is actively speaking.
     */
    SPEECH_DETECTED,

    /**
     * Energy dropped below threshold during speech; waiting to verify if it is an inter-word pause or end of speech.
     */
    POSSIBLE_SILENCE,

    /**
     * Silence timeout has been reached after confirmed speech; end of speech detected.
     */
    SILENCE_DETECTED,

    /**
     * VAD session completed and final speech segment has been extracted.
     */
    COMPLETED,

    /**
     * An error occurred during audio processing.
     */
    ERROR
}
