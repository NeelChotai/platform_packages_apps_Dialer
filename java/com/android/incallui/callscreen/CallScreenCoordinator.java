package com.android.incallui.callscreen;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
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

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable timeoutRunnable = this::handleTimeout;

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

        // Start screening timeout
        timeoutHandler.postDelayed(timeoutRunnable, SCREENING_TIMEOUT_MS);

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

        // Initialize transcription engine
        transcriptionEngine = new BufferingTranscriptionEngine();
        transcriptionEngine.initialize();

        return true;
    }

    private void playGreeting() {
        if (ttsEngine == null || !ttsEngine.isReady() || audioInjector == null) {
            LogUtil.w(TAG, "TTS or AudioInjector not ready, skipping greeting");
            startTranscription();
            return;
        }

        ttsEngine.synthesizeToBuffer(CallScreenPrompts.GREETING, "greeting",
                new TtsEngine.AudioSynthesisCallback() {
                    @Override
                    public void onAudioReady(byte[] audioData, String utteranceId) {
                        if (audioInjector != null) {
                            audioInjector.playAudio(audioData, TtsEngine.SAMPLE_RATE);
                        }
                        startTranscription();
                    }

                    @Override
                    public void onSynthesisError(String utteranceId, int errorCode) {
                        LogUtil.e(TAG, "TTS synthesis failed: " + errorCode);
                        startTranscription(); // Continue without greeting
                    }
                });
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
        if (ttsEngine != null && ttsEngine.isReady() && audioInjector != null) {
            ttsEngine.synthesizeToBuffer(CallScreenPrompts.NOT_INTERESTED, "decline",
                    new TtsEngine.AudioSynthesisCallback() {
                        @Override
                        public void onAudioReady(byte[] audioData, String utteranceId) {
                            if (audioInjector != null) {
                                audioInjector.playAudio(audioData, TtsEngine.SAMPLE_RATE);
                            }
                            endScreening(EndReason.USER_DECLINED);
                        }

                        @Override
                        public void onSynthesisError(String utteranceId, int errorCode) {
                            endScreening(EndReason.USER_DECLINED);
                        }
                    });
        } else {
            endScreening(EndReason.USER_DECLINED);
        }
    }

    /**
     * User chose to send a follow-up prompt.
     *
     * @param promptId The ID of the prompt to send
     */
    public void sendPrompt(int promptId) {
        if (activeSession == null || ttsEngine == null || audioInjector == null) return;

        String prompt = CallScreenPrompts.getPromptById(promptId);
        if (prompt != null) {
            activeSession.setState(CallScreenState.PLAYING_FOLLOWUP);
            ttsEngine.synthesizeToBuffer(prompt, "prompt_" + promptId,
                    new TtsEngine.AudioSynthesisCallback() {
                        @Override
                        public void onAudioReady(byte[] audioData, String utteranceId) {
                            if (audioInjector != null) {
                                audioInjector.playAudio(audioData, TtsEngine.SAMPLE_RATE);
                            }
                            // Resume transcription after follow-up
                            if (activeSession != null) {
                                activeSession.setState(CallScreenState.TRANSCRIBING);
                            }
                        }

                        @Override
                        public void onSynthesisError(String utteranceId, int errorCode) {
                            LogUtil.e(TAG, "Prompt synthesis failed: " + errorCode);
                            // Resume transcription even on error
                            if (activeSession != null) {
                                activeSession.setState(CallScreenState.TRANSCRIBING);
                            }
                        }
                    });
        }
    }

    /**
     * Called when the caller hangs up during screening.
     */
    public void onCallerHungUp() {
        endScreening(EndReason.CALLER_HUNG_UP);
    }

    /**
     * Handles screening timeout.
     */
    private void handleTimeout() {
        LogUtil.w(TAG, "Screening timeout reached");

        if (ttsEngine != null && ttsEngine.isReady() && audioInjector != null) {
            ttsEngine.synthesizeToBuffer(CallScreenPrompts.TIMEOUT, "timeout",
                    new TtsEngine.AudioSynthesisCallback() {
                        @Override
                        public void onAudioReady(byte[] audioData, String utteranceId) {
                            if (audioInjector != null) {
                                audioInjector.playAudio(audioData, TtsEngine.SAMPLE_RATE);
                            }
                            endScreening(EndReason.TIMEOUT);
                        }

                        @Override
                        public void onSynthesisError(String utteranceId, int errorCode) {
                            endScreening(EndReason.TIMEOUT);
                        }
                    });
        } else {
            endScreening(EndReason.TIMEOUT);
        }
    }

    private void endScreening(EndReason reason) {
        if (activeSession == null) return;

        // Cancel the timeout
        timeoutHandler.removeCallbacks(timeoutRunnable);

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
        // Cancel any pending timeout
        timeoutHandler.removeCallbacks(timeoutRunnable);

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
            // Non-fatal: continue session, user can still answer/decline
            if (callback != null) {
                callback.onTranscriptUpdated("[Transcription unavailable]");
            }
        }
    }

    /**
     * Handles a fatal error that requires ending the screening session.
     *
     * @param error Description of the error
     */
    private void handleFatalError(String error) {
        LogUtil.e(TAG, "Fatal screening error: " + error);
        if (callback != null) {
            callback.onScreeningFailed(error);
        }
        cleanup();
    }
}
