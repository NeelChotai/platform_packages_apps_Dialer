package com.android.incallui.callscreen;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(JUnit4.class)
public class StubTranscriptionEngineTest {

    private StubTranscriptionEngine engine;

    @Before
    public void setUp() {
        engine = new StubTranscriptionEngine();
    }

    @Test
    public void initialize_returnsTrue() {
        assertTrue(engine.initialize());
    }

    @Test
    public void isAvailable_returnsTrue() {
        assertTrue(engine.isAvailable());
    }

    @Test
    public void startTranscription_setsTranscribing() {
        engine.initialize();
        engine.startTranscription(new NoOpCallback());
        assertTrue(engine.isTranscribing());
    }

    @Test
    public void stopTranscription_clearsTranscribing() {
        engine.initialize();
        engine.startTranscription(new NoOpCallback());
        engine.stopTranscription();
        assertFalse(engine.isTranscribing());
    }

    @Test
    public void feedAudio_triggersPartialResult() {
        engine.initialize();

        AtomicBoolean called = new AtomicBoolean(false);
        engine.startTranscription(new TranscriptionEngine.TranscriptionCallback() {
            @Override
            public void onPartialResult(String partialText) {
                called.set(true);
            }
            @Override
            public void onFinalResult(String finalText, float confidence) {}
            @Override
            public void onError(int errorCode, String errorMessage) {}
        });

        engine.feedAudio(new byte[1024], 1024);
        assertTrue(called.get());
    }

    @Test
    public void setStubResponse_changesOutput() {
        engine.initialize();
        engine.setStubResponse("Test response");

        AtomicReference<String> result = new AtomicReference<>();
        engine.startTranscription(new TranscriptionEngine.TranscriptionCallback() {
            @Override
            public void onPartialResult(String partialText) {
                result.set(partialText);
            }
            @Override
            public void onFinalResult(String finalText, float confidence) {}
            @Override
            public void onError(int errorCode, String errorMessage) {}
        });

        engine.feedAudio(new byte[1024], 1024);
        assertEquals("Test response", result.get());
    }

    private static class NoOpCallback implements TranscriptionEngine.TranscriptionCallback {
        @Override public void onPartialResult(String partialText) {}
        @Override public void onFinalResult(String finalText, float confidence) {}
        @Override public void onError(int errorCode, String errorMessage) {}
    }
}
