package com.android.incallui.callscreen;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.dialer.common.LogUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wrapper around Android TextToSpeech for call screening.
 *
 * <p>Provides both direct speaking and synthesis to file/buffer
 * for routing through the telephony audio device.
 */
public class TtsEngine {

    private static final String TAG = "TtsEngine";

    /** Sample rate for synthesized audio (16kHz for speech). */
    public static final int SAMPLE_RATE = 16000;

    /** States for the TTS engine. */
    public enum State {
        NOT_INITIALIZED,
        INITIALIZING,
        READY,
        SPEAKING,
        SHUTDOWN
    }

    /** Callback for TTS events. */
    public interface TtsCallback {
        void onTtsReady();
        void onTtsStart(String utteranceId);
        void onTtsDone(String utteranceId);
        void onTtsError(String utteranceId, int errorCode);
        void onSynthesisComplete(String utteranceId, File audioFile);
    }

    /** Callback for audio synthesis to buffer. */
    public interface AudioSynthesisCallback {
        /**
         * Called when audio synthesis is complete and audio data is ready.
         *
         * @param audioData Raw PCM audio data (16kHz, mono, 16-bit)
         * @param utteranceId The utterance ID that was synthesized
         */
        void onAudioReady(byte[] audioData, String utteranceId);

        /**
         * Called when synthesis fails.
         *
         * @param utteranceId The utterance ID that failed
         * @param errorCode The error code
         */
        void onSynthesisError(String utteranceId, int errorCode);
    }

    /** Error codes for synthesis. */
    public static final int ERROR_NOT_READY = -1;
    public static final int ERROR_SYNTHESIS_FAILED = -2;
    public static final int ERROR_FILE_READ_FAILED = -3;
    public static final int ERROR_INVALID_WAV = -4;

    @Nullable
    private TextToSpeech tts;
    @Nullable
    private TtsCallback callback;
    @Nullable
    private Context context;
    private State state = State.NOT_INITIALIZED;
    private final Handler handler = new Handler(Looper.getMainLooper());

    /** Pending buffer synthesis operations, keyed by utterance ID. */
    private final Map<String, PendingSynthesis> pendingSyntheses = new ConcurrentHashMap<>();

    private static class PendingSynthesis {
        final File tempFile;
        final AudioSynthesisCallback callback;

        PendingSynthesis(File tempFile, AudioSynthesisCallback callback) {
            this.tempFile = tempFile;
            this.callback = callback;
        }
    }

    /**
     * Initializes the TTS engine.
     *
     * @param context The context to use
     * @param callback The callback for TTS events
     */
    public void initialize(@NonNull Context context, @Nullable TtsCallback callback) {
        if (state != State.NOT_INITIALIZED) {
            LogUtil.w(TAG, "Already initialized or initializing");
            return;
        }

        this.context = context.getApplicationContext();
        this.callback = callback;
        state = State.INITIALIZING;

        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                tts.setOnUtteranceProgressListener(new UtteranceListener());
                state = State.READY;
                if (callback != null) {
                    callback.onTtsReady();
                }
            } else {
                LogUtil.e(TAG, "TTS initialization failed: " + status);
                state = State.NOT_INITIALIZED;
            }
        });
    }

    /**
     * Speaks the given text.
     *
     * @param text The text to speak
     * @param utteranceId An ID to track this utterance
     * @return true if speech started
     */
    public boolean speak(@NonNull String text, @NonNull String utteranceId) {
        if (tts == null || state != State.READY) {
            return false;
        }

        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);

        int result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params);
        if (result == TextToSpeech.SUCCESS) {
            state = State.SPEAKING;
            return true;
        }
        return false;
    }

    /**
     * Synthesizes text to an audio file.
     *
     * @param text The text to synthesize
     * @param outputFile The file to write audio to
     * @param utteranceId An ID to track this synthesis
     * @return true if synthesis started
     */
    public boolean synthesizeToFile(@NonNull String text, @NonNull File outputFile,
            @NonNull String utteranceId) {
        if (tts == null || state != State.READY) {
            return false;
        }

        int result = tts.synthesizeToFile(text, null, outputFile, utteranceId);
        return result == TextToSpeech.SUCCESS;
    }

    /**
     * Synthesizes text to a byte buffer for playback via AudioInjector.
     *
     * <p>Uses a temporary file internally, reads the WAV output, extracts PCM data,
     * and delivers it via callback. The temp file is deleted after reading.
     *
     * @param text The text to synthesize
     * @param utteranceId An ID to track this synthesis
     * @param callback The callback to receive synthesized audio data
     * @return true if synthesis started
     */
    public boolean synthesizeToBuffer(@NonNull String text, @NonNull String utteranceId,
            @NonNull AudioSynthesisCallback callback) {
        if (tts == null || state != State.READY || context == null) {
            return false;
        }

        try {
            // Create temp file for TTS output
            File tempFile = File.createTempFile("tts_" + utteranceId + "_", ".wav",
                    context.getCacheDir());

            // Store pending synthesis info
            pendingSyntheses.put(utteranceId, new PendingSynthesis(tempFile, callback));

            // Start synthesis
            int result = tts.synthesizeToFile(text, null, tempFile, utteranceId);
            if (result != TextToSpeech.SUCCESS) {
                pendingSyntheses.remove(utteranceId);
                tempFile.delete();
                return false;
            }

            return true;

        } catch (IOException e) {
            LogUtil.e(TAG, "Failed to create temp file for synthesis", e);
            return false;
        }
    }

    /**
     * Reads a WAV file and extracts raw PCM data.
     *
     * @param wavFile The WAV file to read
     * @return Raw PCM audio data, or null if reading failed
     */
    @Nullable
    private byte[] readWavFile(File wavFile) {
        try (FileInputStream fis = new FileInputStream(wavFile)) {
            byte[] header = new byte[44];
            if (fis.read(header) != 44) {
                LogUtil.e(TAG, "Failed to read WAV header");
                return null;
            }

            // Verify WAV header
            if (header[0] != 'R' || header[1] != 'I' || header[2] != 'F' || header[3] != 'F') {
                LogUtil.e(TAG, "Invalid WAV file: bad RIFF header");
                return null;
            }

            if (header[8] != 'W' || header[9] != 'A' || header[10] != 'V' || header[11] != 'E') {
                LogUtil.e(TAG, "Invalid WAV file: bad WAVE header");
                return null;
            }

            // Read data size from header (bytes 40-43, little-endian)
            ByteBuffer bb = ByteBuffer.wrap(header, 40, 4).order(ByteOrder.LITTLE_ENDIAN);
            int dataSize = bb.getInt();

            // Read PCM data
            byte[] pcmData = new byte[dataSize];
            int bytesRead = 0;
            while (bytesRead < dataSize) {
                int read = fis.read(pcmData, bytesRead, dataSize - bytesRead);
                if (read == -1) {
                    break;
                }
                bytesRead += read;
            }

            if (bytesRead < dataSize) {
                LogUtil.w(TAG, "WAV file truncated: expected %d, got %d", dataSize, bytesRead);
                // Return what we have
                return Arrays.copyOf(pcmData, bytesRead);
            }

            return pcmData;

        } catch (IOException e) {
            LogUtil.e(TAG, "Failed to read WAV file", e);
            return null;
        }
    }

    /** Stops any current speech. */
    public void stop() {
        if (tts != null) {
            tts.stop();
            if (state == State.SPEAKING) {
                state = State.READY;
            }
        }
    }

    /** Shuts down the TTS engine. Safe to call multiple times. */
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }

        // Clean up any pending syntheses
        for (PendingSynthesis pending : pendingSyntheses.values()) {
            if (pending.tempFile != null) {
                pending.tempFile.delete();
            }
        }
        pendingSyntheses.clear();

        callback = null;
        context = null;
        state = State.SHUTDOWN;
    }

    /** Returns true if the engine is ready to speak. */
    public boolean isReady() {
        return state == State.READY;
    }

    /** Returns true if currently speaking. */
    public boolean isSpeaking() {
        return state == State.SPEAKING;
    }

    /** Returns the current state. */
    @NonNull
    public State getState() {
        return state;
    }

    private class UtteranceListener extends UtteranceProgressListener {
        @Override
        public void onStart(String utteranceId) {
            if (callback != null) {
                callback.onTtsStart(utteranceId);
            }
        }

        @Override
        public void onDone(String utteranceId) {
            state = State.READY;

            // Check if this was a buffer synthesis
            PendingSynthesis pending = pendingSyntheses.remove(utteranceId);
            if (pending != null) {
                // Read WAV file and extract PCM
                byte[] pcmData = readWavFile(pending.tempFile);

                // Delete temp file immediately
                pending.tempFile.delete();

                // Deliver result on main thread
                if (pcmData != null) {
                    handler.post(() -> pending.callback.onAudioReady(pcmData, utteranceId));
                } else {
                    handler.post(() -> pending.callback.onSynthesisError(utteranceId,
                            ERROR_FILE_READ_FAILED));
                }
            }

            if (callback != null) {
                callback.onTtsDone(utteranceId);
            }
        }

        @Override
        public void onError(String utteranceId) {
            state = State.READY;

            // Check if this was a buffer synthesis
            PendingSynthesis pending = pendingSyntheses.remove(utteranceId);
            if (pending != null) {
                pending.tempFile.delete();
                handler.post(() -> pending.callback.onSynthesisError(utteranceId,
                        ERROR_SYNTHESIS_FAILED));
            }

            if (callback != null) {
                callback.onTtsError(utteranceId, -1);
            }
        }
    }
}
