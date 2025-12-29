package com.android.incallui.callscreen;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AndroidSpeechRecognitionEngineTest {

    @Test
    public void newEngine_isNotTranscribing() {
        AndroidSpeechRecognitionEngine engine =
            new AndroidSpeechRecognitionEngine(RuntimeEnvironment.getApplication());
        assertFalse(engine.isTranscribing());
    }

    @Test
    public void release_canBeCalledMultipleTimes() {
        AndroidSpeechRecognitionEngine engine =
            new AndroidSpeechRecognitionEngine(RuntimeEnvironment.getApplication());
        engine.release();
        engine.release(); // Should not throw
    }

    @Test
    public void startTranscription_requiresInitialize() {
        AndroidSpeechRecognitionEngine engine =
            new AndroidSpeechRecognitionEngine(RuntimeEnvironment.getApplication());

        boolean started = engine.startTranscription(new NoOpCallback());
        assertFalse(started);
    }

    private static class NoOpCallback implements TranscriptionEngine.TranscriptionCallback {
        @Override public void onPartialResult(String partialText) {}
        @Override public void onFinalResult(String finalText, float confidence) {}
        @Override public void onError(int errorCode, String errorMessage) {}
    }
}
