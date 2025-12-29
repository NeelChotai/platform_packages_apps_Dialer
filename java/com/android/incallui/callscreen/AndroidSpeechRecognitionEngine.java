package com.android.incallui.callscreen;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.dialer.common.LogUtil;

import java.util.ArrayList;

/**
 * TranscriptionEngine implementation using Android's SpeechRecognizer.
 *
 * <p>Uses the system speech recognition service with offline mode enabled
 * for privacy. Falls back to online if offline unavailable.
 */
public class AndroidSpeechRecognitionEngine implements TranscriptionEngine {

    private static final String TAG = "AndroidSpeechRecognition";

    private final Context context;

    @Nullable
    private SpeechRecognizer speechRecognizer;
    @Nullable
    private TranscriptionCallback callback;
    private boolean initialized = false;
    private boolean transcribing = false;

    public AndroidSpeechRecognitionEngine(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public boolean initialize() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            LogUtil.w(TAG, "Speech recognition not available");
            return false;
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(new SpeechListener());
            initialized = true;
            return true;
        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to create SpeechRecognizer", e);
            return false;
        }
    }

    @Override
    public boolean startTranscription(@NonNull TranscriptionCallback callback) {
        if (!initialized || speechRecognizer == null) {
            return false;
        }

        this.callback = callback;

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);

        try {
            speechRecognizer.startListening(intent);
            transcribing = true;
            return true;
        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to start listening", e);
            return false;
        }
    }

    @Override
    public void feedAudio(@NonNull byte[] audioData, int length) {
        // SpeechRecognizer captures its own audio, so this is a no-op.
        // For direct audio feeding, we'd need to use a different approach.
    }

    @Override
    public void stopTranscription() {
        if (speechRecognizer != null && transcribing) {
            speechRecognizer.stopListening();
        }
        transcribing = false;
    }

    @Override
    public void release() {
        stopTranscription();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        callback = null;
        initialized = false;
    }

    @Override
    public boolean isTranscribing() {
        return transcribing;
    }

    @Override
    public boolean isAvailable() {
        return SpeechRecognizer.isRecognitionAvailable(context);
    }

    private class SpeechListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {}

        @Override
        public void onBeginningOfSpeech() {}

        @Override
        public void onRmsChanged(float rmsdB) {}

        @Override
        public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {}

        @Override
        public void onError(int error) {
            transcribing = false;
            if (callback != null) {
                callback.onError(error, getErrorMessage(error));
            }
        }

        @Override
        public void onResults(Bundle results) {
            transcribing = false;
            if (callback != null) {
                ArrayList<String> matches = results.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
                float[] scores = results.getFloatArray(
                    SpeechRecognizer.CONFIDENCE_SCORES);

                if (matches != null && !matches.isEmpty()) {
                    float confidence = (scores != null && scores.length > 0) ? scores[0] : -1f;
                    callback.onFinalResult(matches.get(0), confidence);
                }
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            if (callback != null) {
                ArrayList<String> matches = partialResults.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    callback.onPartialResult(matches.get(0));
                }
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {}

        private String getErrorMessage(int error) {
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO: return "Audio recording error";
                case SpeechRecognizer.ERROR_CLIENT: return "Client side error";
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Insufficient permissions";
                case SpeechRecognizer.ERROR_NETWORK: return "Network error";
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Network timeout";
                case SpeechRecognizer.ERROR_NO_MATCH: return "No recognition result";
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Recognizer busy";
                case SpeechRecognizer.ERROR_SERVER: return "Server error";
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No speech input";
                default: return "Unknown error: " + error;
            }
        }
    }
}
