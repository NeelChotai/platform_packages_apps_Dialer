package com.android.incallui.callscreen;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.android.dialer.common.LogUtil;

/**
 * Transcription engine that buffers audio for future processing.
 *
 * <p>Uses SecureAudioBuffer for secure memory handling with guaranteed zeroing.
 * Implements a circular buffer by resetting when capacity is reached.
 * Audio buffer will be used by Whisper.cpp integration in a future change.
 */
public class BufferingTranscriptionEngine implements TranscriptionEngine {

    private static final String TAG = "BufferingTranscription";

    /** Maximum audio buffer size: 30 seconds at 16kHz mono 16-bit = 960KB */
    private static final int MAX_BUFFER_SIZE = 30 * 16000 * 2;

    /** Interval between placeholder emissions in milliseconds */
    private static final long PLACEHOLDER_INTERVAL_MS = 2000;

    private SecureAudioBuffer audioBuffer;
    private long totalBytesReceived = 0;
    private boolean initialized = false;
    private boolean transcribing = false;
    private TranscriptionCallback callback;
    private Handler handler;
    private final Runnable placeholderEmitter = this::emitPlaceholder;

    @Override
    public boolean initialize() {
        if (initialized) {
            LogUtil.w(TAG, "Already initialized");
            return true;
        }

        audioBuffer = new SecureAudioBuffer(MAX_BUFFER_SIZE);
        handler = new Handler(Looper.getMainLooper());
        initialized = true;
        LogUtil.i(TAG, "Initialized with secure buffer size: %d bytes", MAX_BUFFER_SIZE);
        return true;
    }

    @Override
    public boolean startTranscription(@NonNull TranscriptionCallback callback) {
        if (!initialized) {
            LogUtil.w(TAG, "Cannot start transcription: not initialized");
            return false;
        }

        if (transcribing) {
            LogUtil.w(TAG, "Already transcribing");
            return false;
        }

        this.callback = callback;
        this.transcribing = true;
        this.totalBytesReceived = 0;

        // Clear buffer for new session (also zeros memory)
        if (audioBuffer != null && !audioBuffer.isClosed()) {
            audioBuffer.clear();
        }

        // Start periodic placeholder emission
        handler.postDelayed(placeholderEmitter, PLACEHOLDER_INTERVAL_MS);

        LogUtil.i(TAG, "Started transcription");
        return true;
    }

    @Override
    public void feedAudio(@NonNull byte[] audioData, int length) {
        if (!transcribing || audioBuffer == null || audioBuffer.isClosed()) {
            return;
        }

        int bytesToWrite = Math.min(length, audioData.length);

        // Check if buffer has space
        int currentSize = audioBuffer.size();
        int remaining = audioBuffer.capacity() - currentSize;

        if (remaining < bytesToWrite) {
            // Buffer is full - clear and start fresh (zeros memory securely)
            LogUtil.d(TAG, "Buffer full, clearing for new audio window");
            audioBuffer.clear();
        }

        // Write new data to buffer
        int written = audioBuffer.write(audioData, 0, bytesToWrite);
        totalBytesReceived += written;

        LogUtil.d(TAG, "feedAudio: received %d bytes, total %d", length, totalBytesReceived);
    }

    @Override
    public void stopTranscription() {
        if (!transcribing) {
            return;
        }

        handler.removeCallbacks(placeholderEmitter);
        transcribing = false;

        // Emit final result
        if (callback != null) {
            String finalText = String.format("[Transcription placeholder - received %d bytes total]",
                    totalBytesReceived);
            callback.onFinalResult(finalText, -1);
        }

        LogUtil.i(TAG, "Stopped transcription, total bytes received: %d", totalBytesReceived);
    }

    @Override
    public void release() {
        stopTranscription();

        // Close SecureAudioBuffer (zeros memory securely)
        if (audioBuffer != null) {
            audioBuffer.close();
            audioBuffer = null;
        }

        callback = null;
        initialized = false;
        totalBytesReceived = 0;

        LogUtil.i(TAG, "Released");
    }

    @Override
    public boolean isTranscribing() {
        return transcribing;
    }

    @Override
    public boolean isAvailable() {
        return true; // Always available as a placeholder engine
    }

    /**
     * Returns the total number of bytes received via feedAudio.
     * Exposed for testing.
     */
    public long getTotalBytesReceived() {
        return totalBytesReceived;
    }

    private void emitPlaceholder() {
        if (!transcribing || callback == null) {
            return;
        }

        String placeholder = String.format("[Transcription placeholder - received %d bytes]",
                totalBytesReceived);
        callback.onPartialResult(placeholder);

        // Schedule next emission
        handler.postDelayed(placeholderEmitter, PLACEHOLDER_INTERVAL_MS);
    }
}
