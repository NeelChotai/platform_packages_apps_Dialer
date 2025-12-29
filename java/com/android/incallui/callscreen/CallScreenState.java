package com.android.incallui.callscreen;

/**
 * States for the call screening state machine.
 */
public enum CallScreenState {
    /** No active screening session. */
    IDLE,

    /** Setting up audio pipeline for screening. */
    INITIALIZING,

    /** Playing TTS greeting to caller. */
    PLAYING_GREETING,

    /** Capturing and transcribing caller's response. */
    TRANSCRIBING,

    /** Playing a follow-up prompt to caller. */
    PLAYING_FOLLOWUP,

    /** User chose to answer - transitioning to normal call. */
    ANSWERING,

    /** User chose to decline - hanging up. */
    DECLINING,

    /** Cleaning up resources after screening ends. */
    CLEANING_UP,

    /** Screening failed - fell back to normal call flow. */
    DEGRADED
}
