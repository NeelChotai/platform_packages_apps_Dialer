package com.android.incallui.callscreen;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Stub implementation of TranscriptionEngine for testing.
 *
 * <p>Returns configurable fake transcription results. Used during
 * development to test the audio pipeline before real transcription
 * is integrated.
 */
public class StubTranscriptionEngine implements TranscriptionEngine {

    private static final String DEFAULT_RESPONSE = "[Transcription placeholder]";

    @Nullable
    private TranscriptionCallback callback;
    private boolean transcribing = false;
    private String stubResponse = DEFAULT_RESPONSE;
    private int feedCount = 0;

    @Override
    public boolean initialize() {
        return true;
    }

    @Override
    public boolean startTranscription(@NonNull TranscriptionCallback callback) {
        this.callback = callback;
        this.transcribing = true;
        this.feedCount = 0;
        return true;
    }

    @Override
    public void feedAudio(@NonNull byte[] audioData, int length) {
        if (!transcribing || callback == null) {
            return;
        }

        feedCount++;

        // Always return full stub response for testing simplicity
        callback.onPartialResult(stubResponse);
    }

    @Override
    public void stopTranscription() {
        if (callback != null && transcribing) {
            callback.onFinalResult(stubResponse, 1.0f);
        }
        transcribing = false;
        callback = null;
    }

    @Override
    public void release() {
        stopTranscription();
    }

    @Override
    public boolean isTranscribing() {
        return transcribing;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * Sets the stub response to return.
     *
     * @param response The text to return as transcription
     */
    public void setStubResponse(@NonNull String response) {
        this.stubResponse = response;
    }
}
