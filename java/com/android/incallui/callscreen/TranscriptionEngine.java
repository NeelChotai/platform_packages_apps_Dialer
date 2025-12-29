package com.android.incallui.callscreen;

import androidx.annotation.NonNull;

/**
 * Interface for speech-to-text transcription engines.
 *
 * <p>Implementations may use Android's SpeechRecognizer, Whisper.cpp,
 * or other on-device transcription solutions.
 */
public interface TranscriptionEngine {

    /** Callback for transcription results. */
    interface TranscriptionCallback {
        /**
         * Called when partial transcription is available.
         *
         * @param partialText The partial transcription so far
         */
        void onPartialResult(@NonNull String partialText);

        /**
         * Called when final transcription is available.
         *
         * @param finalText The complete transcription
         * @param confidence Confidence score 0.0-1.0, or -1 if unavailable
         */
        void onFinalResult(@NonNull String finalText, float confidence);

        /**
         * Called when transcription encounters an error.
         *
         * @param errorCode The error code
         * @param errorMessage Human-readable error description
         */
        void onError(int errorCode, @NonNull String errorMessage);
    }

    /**
     * Initializes the transcription engine.
     *
     * @return true if initialization succeeded
     */
    boolean initialize();

    /**
     * Starts transcription with the given callback.
     *
     * @param callback The callback to receive transcription results
     * @return true if transcription started
     */
    boolean startTranscription(@NonNull TranscriptionCallback callback);

    /**
     * Feeds audio data to the transcription engine.
     *
     * <p>For streaming engines, call this repeatedly with audio chunks.
     * For batch engines, accumulate audio and call once.
     *
     * @param audioData Raw PCM audio data (16kHz, mono, 16-bit)
     * @param length Number of valid bytes in audioData
     */
    void feedAudio(@NonNull byte[] audioData, int length);

    /**
     * Stops transcription and returns final result.
     */
    void stopTranscription();

    /**
     * Releases all resources.
     */
    void release();

    /**
     * Returns true if the engine is currently transcribing.
     */
    boolean isTranscribing();

    /**
     * Returns true if this engine is available on the current device.
     */
    boolean isAvailable();
}
