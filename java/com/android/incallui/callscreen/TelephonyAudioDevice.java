package com.android.incallui.callscreen;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Helper for finding and working with the telephony audio output device.
 *
 * <p>The TYPE_TELEPHONY audio device allows routing audio to the caller
 * during an active call. This is used by Google's Call Screen feature
 * and is available on Pixel 6+ devices.
 */
public final class TelephonyAudioDevice {

    private TelephonyAudioDevice() {} // Static utility class

    /**
     * Finds the telephony audio output device.
     *
     * @param context The context to use for getting AudioManager
     * @return The telephony device, or null if not available
     */
    @Nullable
    public static AudioDeviceInfo findTelephonyOutput(@NonNull Context context) {
        AudioManager audioManager = context.getSystemService(AudioManager.class);
        if (audioManager == null) {
            return null;
        }

        for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            if (device.getType() == AudioDeviceInfo.TYPE_TELEPHONY) {
                return device;
            }
        }
        return null;
    }

    /**
     * Checks if call screening audio injection is supported on this device.
     *
     * @param context The context to use for checking
     * @return true if TYPE_TELEPHONY output is available
     */
    public static boolean isSupported(@NonNull Context context) {
        return findTelephonyOutput(context) != null;
    }
}
