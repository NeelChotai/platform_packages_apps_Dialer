package com.android.incallui.callscreen;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * Manages settings for the Call Screen feature.
 */
public class CallScreenSettings {

    private static final String PREFS_NAME = "call_screen_prefs";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_AUTO_MODE = "auto_mode";
    private static final String KEY_SAVE_AUDIO = "save_audio";

    /** Auto-screening mode options. */
    public enum AutoScreenMode {
        MANUAL_ONLY(0),
        UNKNOWN_NUMBERS(1),
        PRIVATE_NUMBERS(2),
        ALL_CALLS(3);

        private final int value;

        AutoScreenMode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static AutoScreenMode fromValue(int value) {
            for (AutoScreenMode mode : values()) {
                if (mode.value == value) return mode;
            }
            return MANUAL_ONLY;
        }
    }

    private final SharedPreferences prefs;

    public CallScreenSettings(@NonNull Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Returns true if Call Screen is enabled. */
    public boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, false);
    }

    /** Sets whether Call Screen is enabled. */
    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    /** Returns the auto-screen mode. */
    @NonNull
    public AutoScreenMode getAutoScreenMode() {
        return AutoScreenMode.fromValue(prefs.getInt(KEY_AUTO_MODE, 0));
    }

    /** Sets the auto-screen mode. */
    public void setAutoScreenMode(@NonNull AutoScreenMode mode) {
        prefs.edit().putInt(KEY_AUTO_MODE, mode.getValue()).apply();
    }

    /** Returns true if caller audio saving is enabled. */
    public boolean isSaveAudioEnabled() {
        return prefs.getBoolean(KEY_SAVE_AUDIO, false);
    }

    /** Sets whether to save caller audio. */
    public void setSaveAudioEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SAVE_AUDIO, enabled).apply();
    }

    /**
     * Determines if a call should be auto-screened based on current settings.
     *
     * @param isUnknownCaller true if the caller is not in contacts
     * @param isPrivateNumber true if the caller ID is hidden
     * @return true if the call should be automatically screened
     */
    public boolean shouldAutoScreen(boolean isUnknownCaller, boolean isPrivateNumber) {
        if (!isEnabled()) {
            return false;
        }

        switch (getAutoScreenMode()) {
            case ALL_CALLS:
                return true;
            case UNKNOWN_NUMBERS:
                return isUnknownCaller;
            case PRIVATE_NUMBERS:
                return isPrivateNumber;
            case MANUAL_ONLY:
            default:
                return false;
        }
    }
}
