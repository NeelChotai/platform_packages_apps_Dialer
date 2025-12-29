package com.android.incallui.callscreen;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Tests for {@link TtsEngine}.
 *
 * <p>These tests verify the TtsEngine state machine and API contracts
 * without requiring Android framework initialization.
 */
class TtsEngineTest {

    @Test
    @DisplayName("New engine is not ready")
    void newEngine_isNotReady() {
        TtsEngine engine = new TtsEngine();
        assertFalse(engine.isReady());
    }

    @Test
    @DisplayName("New engine is not speaking")
    void newEngine_isNotSpeaking() {
        TtsEngine engine = new TtsEngine();
        assertFalse(engine.isSpeaking());
    }

    @Test
    @DisplayName("Shutdown can be called multiple times")
    void shutdown_canBeCalledMultipleTimes() {
        TtsEngine engine = new TtsEngine();
        engine.shutdown();
        engine.shutdown(); // Should not throw
    }

    @Test
    @DisplayName("New engine state is NOT_INITIALIZED")
    void getState_returnsNotInitialized_whenNew() {
        TtsEngine engine = new TtsEngine();
        assertEquals(TtsEngine.State.NOT_INITIALIZED, engine.getState());
    }

    @Test
    @DisplayName("Shutdown sets state to SHUTDOWN")
    void shutdown_setsStateToShutdown() {
        TtsEngine engine = new TtsEngine();
        engine.shutdown();
        assertEquals(TtsEngine.State.SHUTDOWN, engine.getState());
    }

    @Test
    @DisplayName("SAMPLE_RATE constant is 16000")
    void sampleRate_is16000() {
        assertEquals(16000, TtsEngine.SAMPLE_RATE);
    }

    @Test
    @DisplayName("Speak returns false when not initialized")
    void speak_returnsFalse_whenNotInitialized() {
        TtsEngine engine = new TtsEngine();
        assertFalse(engine.speak("test", "test_id"));
    }

    @Test
    @DisplayName("synthesizeToBuffer returns false when not initialized")
    void synthesizeToBuffer_returnsFalse_whenNotInitialized() {
        TtsEngine engine = new TtsEngine();

        boolean result = engine.synthesizeToBuffer("test", "test_id",
                new TtsEngine.AudioSynthesisCallback() {
                    @Override
                    public void onAudioReady(byte[] audioData, String utteranceId) {}

                    @Override
                    public void onSynthesisError(String utteranceId, int errorCode) {}
                });

        assertFalse(result);
    }
}
