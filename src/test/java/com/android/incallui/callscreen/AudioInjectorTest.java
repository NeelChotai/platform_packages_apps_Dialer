package com.android.incallui.callscreen;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AudioInjectorTest {

    @Test
    public void newInjector_isNotPlaying() {
        AudioInjector injector = new AudioInjector();
        assertFalse(injector.isPlaying());
    }

    @Test
    public void newInjector_isNotInitialized() {
        AudioInjector injector = new AudioInjector();
        assertFalse(injector.isInitialized());
    }

    @Test
    public void release_canBeCalledMultipleTimes() {
        AudioInjector injector = new AudioInjector();
        injector.release();
        injector.release(); // Should not throw
    }

    @Test
    public void getState_returnsIdle_whenNew() {
        AudioInjector injector = new AudioInjector();
        assertEquals(AudioInjector.State.IDLE, injector.getState());
    }
}
