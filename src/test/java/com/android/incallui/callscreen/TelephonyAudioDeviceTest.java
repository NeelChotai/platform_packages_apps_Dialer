package com.android.incallui.callscreen;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TelephonyAudioDeviceTest {

    @Mock
    private Context mockContext;

    @Mock
    private AudioManager mockAudioManager;

    @Mock
    private AudioDeviceInfo mockTelephonyDevice;

    @Mock
    private AudioDeviceInfo mockSpeakerDevice;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockContext.getSystemService(AudioManager.class)).thenReturn(mockAudioManager);
    }

    @Test
    public void findTelephonyOutput_returnsTelephonyDevice_whenAvailable() {
        when(mockTelephonyDevice.getType()).thenReturn(AudioDeviceInfo.TYPE_TELEPHONY);
        when(mockSpeakerDevice.getType()).thenReturn(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
        when(mockAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS))
            .thenReturn(new AudioDeviceInfo[]{mockSpeakerDevice, mockTelephonyDevice});

        AudioDeviceInfo result = TelephonyAudioDevice.findTelephonyOutput(mockContext);

        assertEquals(mockTelephonyDevice, result);
    }

    @Test
    public void findTelephonyOutput_returnsNull_whenNotAvailable() {
        when(mockSpeakerDevice.getType()).thenReturn(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
        when(mockAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS))
            .thenReturn(new AudioDeviceInfo[]{mockSpeakerDevice});

        AudioDeviceInfo result = TelephonyAudioDevice.findTelephonyOutput(mockContext);

        assertNull(result);
    }

    @Test
    public void findTelephonyOutput_returnsNull_whenNoDevices() {
        when(mockAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS))
            .thenReturn(new AudioDeviceInfo[]{});

        AudioDeviceInfo result = TelephonyAudioDevice.findTelephonyOutput(mockContext);

        assertNull(result);
    }

    @Test
    public void isSupported_returnsTrue_whenTelephonyAvailable() {
        when(mockTelephonyDevice.getType()).thenReturn(AudioDeviceInfo.TYPE_TELEPHONY);
        when(mockAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS))
            .thenReturn(new AudioDeviceInfo[]{mockTelephonyDevice});

        assertTrue(TelephonyAudioDevice.isSupported(mockContext));
    }

    @Test
    public void isSupported_returnsFalse_whenTelephonyNotAvailable() {
        when(mockSpeakerDevice.getType()).thenReturn(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
        when(mockAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS))
            .thenReturn(new AudioDeviceInfo[]{mockSpeakerDevice});

        assertFalse(TelephonyAudioDevice.isSupported(mockContext));
    }
}
