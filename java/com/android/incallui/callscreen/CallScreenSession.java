package com.android.incallui.callscreen;

import androidx.annotation.NonNull;

/**
 * Represents an active call screening session.
 *
 * <p>Holds state for a single screening operation including the transcript,
 * current state, and timing information.
 *
 * <p>This class is not thread-safe. All access should be from the main thread.
 */
public class CallScreenSession {

    private final String callId;
    private final long startTime;
    private final StringBuilder transcript;
    private CallScreenState state;

    /**
     * Creates a new screening session for the given call.
     *
     * @param callId The unique identifier for the call being screened
     */
    public CallScreenSession(@NonNull String callId) {
        this.callId = callId;
        this.startTime = System.currentTimeMillis();
        this.transcript = new StringBuilder();
        this.state = CallScreenState.IDLE;
    }

    /** Returns the call ID this session is screening. */
    @NonNull
    public String getCallId() {
        return callId;
    }

    /** Returns the current screening state. */
    @NonNull
    public CallScreenState getState() {
        return state;
    }

    /** Updates the screening state. */
    public void setState(@NonNull CallScreenState state) {
        this.state = state;
    }

    /** Returns the accumulated transcript text. */
    @NonNull
    public String getTranscript() {
        return transcript.toString();
    }

    /** Appends text to the transcript, adding a space separator if needed. */
    public void appendTranscript(@NonNull String text) {
        if (transcript.length() > 0) {
            transcript.append(" ");
        }
        transcript.append(text);
    }

    /** Returns the timestamp when this session was created. */
    public long getStartTime() {
        return startTime;
    }
}
