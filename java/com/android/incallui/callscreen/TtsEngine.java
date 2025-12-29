package com.android.incallui.callscreen;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.dialer.common.LogUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;

/**
 * Wrapper around Android TextToSpeech for call screening.
 *
 * <p>Provides both direct speaking and synthesis to file/buffer
 * for routing through the telephony audio device.
 */
public class TtsEngine {

    private static final String TAG = "TtsEngine";

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

    @Nullable
    private TextToSpeech tts;
    @Nullable
    private TtsCallback callback;
    private State state = State.NOT_INITIALIZED;

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
        callback = null;
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
            if (callback != null) {
                callback.onTtsDone(utteranceId);
            }
        }

        @Override
        public void onError(String utteranceId) {
            state = State.READY;
            if (callback != null) {
                callback.onTtsError(utteranceId, -1);
            }
        }
    }
}
