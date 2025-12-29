package com.android.incallui.callscreen;

import android.content.Context;
import android.telecom.Call;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.dialer.common.LogUtil;

/**
 * Main coordinator for the Call Screen feature.
 *
 * <p>Orchestrates the screening flow:
 * <ol>
 *   <li>Answers the call silently</li>
 *   <li>Plays TTS greeting to caller</li>
 *   <li>Captures and transcribes caller's response</li>
 *   <li>Displays transcript to user</li>
 *   <li>Handles user's decision (answer/decline/respond)</li>
 * </ol>
 */
public class CallScreenCoordinator {

    private static final String TAG = "CallScreenCoordinator";
    private static final long SCREENING_TIMEOUT_MS = 60_000; // 60 seconds

    /** Callback for screening events. */
    public interface ScreeningCallback {
        void onScreeningStarted(@NonNull CallScreenSession session);
        void onTranscriptUpdated(@NonNull String transcript);
        void onScreeningEnded(@NonNull CallScreenSession session, @NonNull EndReason reason);
        void onScreeningFailed(@NonNull String error);
    }

    /** Reasons why screening ended. */
    public enum EndReason {
        USER_ANSWERED,
        USER_DECLINED,
        CALLER_HUNG_UP,
        TIMEOUT,
        ERROR
    }

    private final Context context;
    private final CallScreenSettings settings;

    @Nullable
    private CallScreenSession activeSession;
    @Nullable
    private ScreeningCallback callback;
    @Nullable
    private AudioInjector audioInjector;
    @Nullable
    private AudioCapturer audioCapturer;
    @Nullable
    private TtsEngine ttsEngine;
    @Nullable
    private TranscriptionEngine transcriptionEngine;

    public CallScreenCoordinator(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.settings = new CallScreenSettings(context);
    }

    /**
     * Checks if call screening is supported on this device.
     *
     * @return true if TYPE_TELEPHONY audio device is available
     */
    public boolean isScreeningSupported() {
        return TelephonyAudioDevice.isSupported(context);
    }

    /**
     * Returns true if there's an active screening session.
     */
    public boolean hasActiveSession() {
        return activeSession != null;
    }

    /**
     * Returns the active screening session, or null if none.
     */
    @Nullable
    public CallScreenSession getActiveSession() {
        return activeSession;
    }

    /**
     * Starts screening the given call.
     *
     * @param call The call to screen
     * @param callback The callback for screening events
     * @return true if screening started successfully
     */
    public boolean startScreening(@NonNull Call call, @NonNull ScreeningCallback callback) {
        if (activeSession != null) {
            LogUtil.w(TAG, "Already screening a call");
            return false;
        }

        if (!isScreeningSupported()) {
            LogUtil.w(TAG, "Screening not supported on this device");
            callback.onScreeningFailed("Call screening not supported on this device");
            return false;
        }

        this.callback = callback;
        // Generate a unique session ID based on the call's hash code
        String callId = "call_" + System.currentTimeMillis() + "_" + call.hashCode();
        activeSession = new CallScreenSession(callId);
        activeSession.setState(CallScreenState.INITIALIZING);

        // Initialize audio components
        if (!initializeAudioPipeline()) {
            cleanup();
            callback.onScreeningFailed("Failed to initialize audio");
            return false;
        }

        // Answer the call silently
        call.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY);

        // Start the screening flow
        activeSession.setState(CallScreenState.PLAYING_GREETING);
        callback.onScreeningStarted(activeSession);

        playGreeting();

        return true;
    }

    private boolean initializeAudioPipeline() {
        // Initialize audio injector
        audioInjector = new AudioInjector();
        var telephonyDevice = TelephonyAudioDevice.findTelephonyOutput(context);
        if (telephonyDevice == null || !audioInjector.initialize(telephonyDevice)) {
            LogUtil.e(TAG, "Failed to initialize audio injector");
            return false;
        }

        // Initialize audio capturer
        audioCapturer = new AudioCapturer();
        if (!audioCapturer.initialize()) {
            LogUtil.e(TAG, "Failed to initialize audio capturer");
            return false;
        }

        // Initialize TTS
        ttsEngine = new TtsEngine();
        ttsEngine.initialize(context, new TtsCallback());

        // Initialize transcription (use stub for now)
        transcriptionEngine = new StubTranscriptionEngine();
        transcriptionEngine.initialize();

        return true;
    }

    private void playGreeting() {
        if (ttsEngine == null || !ttsEngine.isReady()) {
            LogUtil.w(TAG, "TTS not ready, skipping greeting");
            startTranscription();
            return;
        }

        ttsEngine.speak(CallScreenPrompts.GREETING, "greeting");
    }

    private void startTranscription() {
        if (activeSession == null) return;

        activeSession.setState(CallScreenState.TRANSCRIBING);

        if (transcriptionEngine != null) {
            transcriptionEngine.startTranscription(new TranscriptionCallback());
        }

        if (audioCapturer != null) {
            audioCapturer.startCapture((audioData, length) -> {
                if (transcriptionEngine != null) {
                    transcriptionEngine.feedAudio(audioData, length);
                }
            });
        }
    }

    /**
     * User chose to answer the call.
     */
    public void answerCall() {
        if (activeSession == null) return;

        activeSession.setState(CallScreenState.ANSWERING);
        endScreening(EndReason.USER_ANSWERED);
    }

    /**
     * User chose to decline the call.
     */
    public void declineCall() {
        if (activeSession == null) return;

        activeSession.setState(CallScreenState.DECLINING);
        // Play "not interested" message before hanging up
        if (ttsEngine != null && ttsEngine.isReady()) {
            ttsEngine.speak(CallScreenPrompts.NOT_INTERESTED, "decline");
        }
        endScreening(EndReason.USER_DECLINED);
    }

    /**
     * User chose to send a follow-up prompt.
     *
     * @param promptId The ID of the prompt to send
     */
    public void sendPrompt(int promptId) {
        if (activeSession == null || ttsEngine == null) return;

        String prompt = CallScreenPrompts.getPromptById(promptId);
        if (prompt != null) {
            activeSession.setState(CallScreenState.PLAYING_FOLLOWUP);
            ttsEngine.speak(prompt, "prompt_" + promptId);
        }
    }

    /**
     * Called when the caller hangs up during screening.
     */
    public void onCallerHungUp() {
        endScreening(EndReason.CALLER_HUNG_UP);
    }

    private void endScreening(EndReason reason) {
        if (activeSession == null) return;

        CallScreenSession session = activeSession;
        session.setState(CallScreenState.CLEANING_UP);

        if (callback != null) {
            callback.onScreeningEnded(session, reason);
        }

        cleanup();
    }

    /**
     * Cleans up all resources. Safe to call multiple times.
     */
    public void cleanup() {
        if (audioInjector != null) {
            audioInjector.release();
            audioInjector = null;
        }

        if (audioCapturer != null) {
            audioCapturer.release();
            audioCapturer = null;
        }

        if (ttsEngine != null) {
            ttsEngine.shutdown();
            ttsEngine = null;
        }

        if (transcriptionEngine != null) {
            transcriptionEngine.release();
            transcriptionEngine = null;
        }

        activeSession = null;
        callback = null;
    }

    private class TtsCallback implements TtsEngine.TtsCallback {
        @Override
        public void onTtsReady() {
            // TTS ready, can start greeting if we were waiting
        }

        @Override
        public void onTtsStart(String utteranceId) {}

        @Override
        public void onTtsDone(String utteranceId) {
            if ("greeting".equals(utteranceId)) {
                startTranscription();
            } else if (utteranceId.startsWith("prompt_")) {
                // Resume transcription after follow-up
                if (activeSession != null) {
                    activeSession.setState(CallScreenState.TRANSCRIBING);
                }
            }
        }

        @Override
        public void onTtsError(String utteranceId, int errorCode) {
            LogUtil.e(TAG, "TTS error: " + errorCode);
            if ("greeting".equals(utteranceId)) {
                // Continue without greeting
                startTranscription();
            }
        }

        @Override
        public void onSynthesisComplete(String utteranceId, java.io.File audioFile) {}
    }

    private class TranscriptionCallback implements TranscriptionEngine.TranscriptionCallback {
        @Override
        public void onPartialResult(@NonNull String partialText) {
            if (activeSession != null) {
                activeSession.appendTranscript(partialText);
                if (callback != null) {
                    callback.onTranscriptUpdated(activeSession.getTranscript());
                }
            }
        }

        @Override
        public void onFinalResult(@NonNull String finalText, float confidence) {
            if (activeSession != null) {
                activeSession.appendTranscript(finalText);
                if (callback != null) {
                    callback.onTranscriptUpdated(activeSession.getTranscript());
                }
            }
        }

        @Override
        public void onError(int errorCode, @NonNull String errorMessage) {
            LogUtil.e(TAG, "Transcription error: " + errorMessage);
        }
    }
}
