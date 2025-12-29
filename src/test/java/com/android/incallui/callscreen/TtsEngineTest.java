package com.android.incallui.callscreen;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TtsEngineTest {

    @Test
    public void newEngine_isNotReady() {
        TtsEngine engine = new TtsEngine();
        assertFalse(engine.isReady());
    }

    @Test
    public void newEngine_isNotSpeaking() {
        TtsEngine engine = new TtsEngine();
        assertFalse(engine.isSpeaking());
    }

    @Test
    public void shutdown_canBeCalledMultipleTimes() {
        TtsEngine engine = new TtsEngine();
        engine.shutdown();
        engine.shutdown(); // Should not throw
    }

    @Test
    public void getState_returnsNotInitialized_whenNew() {
        TtsEngine engine = new TtsEngine();
        assertEquals(TtsEngine.State.NOT_INITIALIZED, engine.getState());
    }
}
