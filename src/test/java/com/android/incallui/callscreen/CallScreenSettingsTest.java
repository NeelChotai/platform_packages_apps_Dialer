package com.android.incallui.callscreen;

import static org.junit.Assert.*;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class CallScreenSettingsTest {

    private Context context;
    private CallScreenSettings settings;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        settings = new CallScreenSettings(context);
        // Clear prefs before each test
        context.getSharedPreferences("call_screen_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit();
    }

    @Test
    public void isEnabled_returnsFalse_byDefault() {
        assertFalse(settings.isEnabled());
    }

    @Test
    public void setEnabled_persistsValue() {
        settings.setEnabled(true);
        assertTrue(settings.isEnabled());

        settings.setEnabled(false);
        assertFalse(settings.isEnabled());
    }

    @Test
    public void getAutoScreenMode_returnsManualOnly_byDefault() {
        assertEquals(CallScreenSettings.AutoScreenMode.MANUAL_ONLY, settings.getAutoScreenMode());
    }

    @Test
    public void setAutoScreenMode_persistsValue() {
        settings.setAutoScreenMode(CallScreenSettings.AutoScreenMode.UNKNOWN_NUMBERS);
        assertEquals(CallScreenSettings.AutoScreenMode.UNKNOWN_NUMBERS, settings.getAutoScreenMode());
    }

    @Test
    public void isSaveAudioEnabled_returnsFalse_byDefault() {
        assertFalse(settings.isSaveAudioEnabled());
    }

    @Test
    public void setSaveAudioEnabled_persistsValue() {
        settings.setSaveAudioEnabled(true);
        assertTrue(settings.isSaveAudioEnabled());
    }

    @Test
    public void shouldAutoScreen_returnsFalse_whenDisabled() {
        settings.setEnabled(false);
        settings.setAutoScreenMode(CallScreenSettings.AutoScreenMode.ALL_CALLS);
        assertFalse(settings.shouldAutoScreen(true, false));
    }

    @Test
    public void shouldAutoScreen_returnsFalse_forManualOnly() {
        settings.setEnabled(true);
        settings.setAutoScreenMode(CallScreenSettings.AutoScreenMode.MANUAL_ONLY);
        assertFalse(settings.shouldAutoScreen(true, false));
    }

    @Test
    public void shouldAutoScreen_returnsTrue_forUnknownNumbers_whenCallerUnknown() {
        settings.setEnabled(true);
        settings.setAutoScreenMode(CallScreenSettings.AutoScreenMode.UNKNOWN_NUMBERS);
        assertTrue(settings.shouldAutoScreen(true, false));
    }

    @Test
    public void shouldAutoScreen_returnsFalse_forUnknownNumbers_whenCallerKnown() {
        settings.setEnabled(true);
        settings.setAutoScreenMode(CallScreenSettings.AutoScreenMode.UNKNOWN_NUMBERS);
        assertFalse(settings.shouldAutoScreen(false, false));
    }
}
