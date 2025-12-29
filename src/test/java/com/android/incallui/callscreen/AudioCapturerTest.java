package com.android.incallui.callscreen;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AudioCapturerTest {

    @Test
    public void newCapturer_isNotRecording() {
        AudioCapturer capturer = new AudioCapturer();
        assertFalse(capturer.isRecording());
    }

    @Test
    public void newCapturer_isNotInitialized() {
        AudioCapturer capturer = new AudioCapturer();
        assertFalse(capturer.isInitialized());
    }

    @Test
    public void release_canBeCalledMultipleTimes() {
        AudioCapturer capturer = new AudioCapturer();
        capturer.release();
        capturer.release(); // Should not throw
    }

    @Test
    public void getState_returnsIdle_whenNew() {
        AudioCapturer capturer = new AudioCapturer();
        assertEquals(AudioCapturer.State.IDLE, capturer.getState());
    }

    @Test
    public void getSampleRate_returns16000() {
        assertEquals(16000, AudioCapturer.SAMPLE_RATE);
    }
}
