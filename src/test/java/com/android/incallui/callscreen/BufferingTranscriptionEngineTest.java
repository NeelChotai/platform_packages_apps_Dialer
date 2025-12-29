package com.android.incallui.callscreen;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Tests for {@link BufferingTranscriptionEngine}.
 *
 * <p>Verifies that the engine correctly buffers audio data, tracks bytes received,
 * and properly implements the TranscriptionEngine interface.
 */
class BufferingTranscriptionEngineTest {

    private BufferingTranscriptionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new BufferingTranscriptionEngine();
    }

    @Test
    @DisplayName("Initialize returns true")
    void initialize_returnsTrue() {
        assertTrue(engine.initialize());
    }

    @Test
    @DisplayName("isAvailable returns true")
    void isAvailable_returnsTrue() {
        assertTrue(engine.isAvailable());
    }

    @Test
    @DisplayName("feedAudio accumulates bytes")
    void feedAudio_accumulatesBytes() {
        engine.initialize();
        engine.startTranscription(new NoOpCallback());

        engine.feedAudio(new byte[1024], 1024);
        engine.feedAudio(new byte[1024], 1024);

        assertEquals(2048, engine.getTotalBytesReceived());
    }

    @Test
    @DisplayName("feedAudio without startTranscription does nothing")
    void feedAudio_withoutStart_doesNothing() {
        engine.initialize();

        engine.feedAudio(new byte[1024], 1024);

        assertEquals(0, engine.getTotalBytesReceived());
    }

    @Test
    @DisplayName("Release stops transcribing")
    void release_stopsTranscribing() {
        engine.initialize();
        engine.startTranscription(new NoOpCallback());
        engine.feedAudio(new byte[1024], 1024);

        engine.release();

        assertFalse(engine.isTranscribing());
    }

    @Test
    @DisplayName("getTotalBytesReceived returns accurate count")
    void getTotalBytesReceived_accurate() {
        engine.initialize();
        engine.startTranscription(new NoOpCallback());

        engine.feedAudio(new byte[500], 500);
        engine.feedAudio(new byte[300], 300);
        engine.feedAudio(new byte[200], 200);

        assertEquals(1000, engine.getTotalBytesReceived());
    }

    @Test
    @DisplayName("startTranscription requires initialize")
    void startTranscription_requiresInitialize() {
        assertFalse(engine.startTranscription(new NoOpCallback()));
    }

    @Test
    @DisplayName("isTranscribing returns correct state")
    void isTranscribing_returnsCorrectState() {
        assertFalse(engine.isTranscribing());

        engine.initialize();
        assertFalse(engine.isTranscribing());

        engine.startTranscription(new NoOpCallback());
        assertTrue(engine.isTranscribing());

        engine.stopTranscription();
        assertFalse(engine.isTranscribing());
    }

    @Test
    @DisplayName("stopTranscription emits final result")
    void stopTranscription_emitsFinalResult() {
        engine.initialize();
        final boolean[] finalResultCalled = {false};
        final String[] finalText = {null};

        engine.startTranscription(new TranscriptionEngine.TranscriptionCallback() {
            @Override
            public void onPartialResult(String partialText) {}

            @Override
            public void onFinalResult(String text, float confidence) {
                finalResultCalled[0] = true;
                finalText[0] = text;
            }

            @Override
            public void onError(int errorCode, String errorMessage) {}
        });

        engine.feedAudio(new byte[1024], 1024);
        engine.stopTranscription();

        assertTrue(finalResultCalled[0]);
        assertNotNull(finalText[0]);
    }

    private static class NoOpCallback implements TranscriptionEngine.TranscriptionCallback {
        @Override
        public void onPartialResult(String partialText) {}

        @Override
        public void onFinalResult(String finalText, float confidence) {}

        @Override
        public void onError(int errorCode, String errorMessage) {}
    }
}
