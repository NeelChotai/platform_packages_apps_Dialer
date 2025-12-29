package com.android.incallui.callscreen;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.dialer.common.LogUtil;

import java.util.Arrays;

/**
 * Captures audio from the caller during a call.
 *
 * <p>Uses AudioRecord with VOICE_CALL source to capture the remote party's audio
 * during an active call. This requires the CAPTURE_AUDIO_OUTPUT permission.
 *
 * <p>Audio is captured at 16kHz mono PCM, suitable for speech recognition.
 */
public class AudioCapturer {

    private static final String TAG = "AudioCapturer";

    /** Sample rate for audio capture (16kHz optimal for speech recognition). */
    public static final int SAMPLE_RATE = 16000;

    /** Audio channel configuration (mono). */
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

    /** Audio encoding format. */
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    /** Audio source for capturing call audio. */
    public static final int AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_CALL;

    /** States for the audio capturer. */
    public enum State {
        IDLE,
        INITIALIZED,
        RECORDING,
        RELEASED
    }

    /** Callback for receiving captured audio data. */
    public interface AudioCallback {
        /**
         * Called when audio data is captured.
         *
         * @param audioData The raw PCM audio data
         * @param length The number of valid bytes in audioData
         */
        void onAudioCaptured(@NonNull byte[] audioData, int length);
    }

    @Nullable
    private AudioRecord audioRecord;
    @Nullable
    private Thread captureThread;
    @Nullable
    private AudioCallback callback;
    private volatile State state = State.IDLE;
    private volatile boolean shouldCapture = false;

    /**
     * Initializes the audio capturer.
     *
     * @return true if initialization succeeded
     */
    public boolean initialize() {
        if (state != State.IDLE) {
            LogUtil.w(TAG, "Cannot initialize: already in state " + state);
            return false;
        }

        try {
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

            audioRecord = new AudioRecord(
                AUDIO_SOURCE,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
            );

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                LogUtil.e(TAG, "AudioRecord failed to initialize");
                audioRecord.release();
                audioRecord = null;
                return false;
            }

            state = State.INITIALIZED;
            return true;

        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to initialize AudioRecord", e);
            return false;
        }
    }

    /**
     * Starts capturing audio.
     *
     * @param callback The callback to receive audio data
     * @return true if capture started successfully
     */
    public boolean startCapture(@NonNull AudioCallback callback) {
        if (audioRecord == null || state != State.INITIALIZED) {
            return false;
        }

        this.callback = callback;
        shouldCapture = true;

        audioRecord.startRecording();
        state = State.RECORDING;

        captureThread = new Thread(this::captureLoop, "AudioCapturer");
        captureThread.start();

        return true;
    }

    private void captureLoop() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        byte[] buffer = new byte[bufferSize];

        try {
            while (shouldCapture && audioRecord != null) {
                int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                if (bytesRead > 0 && callback != null) {
                    callback.onAudioCaptured(buffer, bytesRead);
                }
            }
        } finally {
            // Zero the buffer when capture ends for security
            Arrays.fill(buffer, (byte) 0);
        }
    }

    /** Stops capturing audio. */
    public void stopCapture() {
        shouldCapture = false;

        if (captureThread != null) {
            try {
                captureThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            captureThread = null;
        }

        if (audioRecord != null && state == State.RECORDING) {
            audioRecord.stop();
            state = State.INITIALIZED;
        }
    }

    /** Releases all resources. Safe to call multiple times. */
    public void release() {
        stopCapture();

        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }

        callback = null;
        state = State.RELEASED;
    }

    /** Returns true if currently recording. */
    public boolean isRecording() {
        return state == State.RECORDING;
    }

    /** Returns true if initialized. */
    public boolean isInitialized() {
        return state == State.INITIALIZED || state == State.RECORDING;
    }

    /** Returns the current state. */
    @NonNull
    public State getState() {
        return state;
    }
}
