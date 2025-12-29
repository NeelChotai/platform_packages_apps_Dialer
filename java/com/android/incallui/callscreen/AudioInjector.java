package com.android.incallui.callscreen;

import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioTrack;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.dialer.common.LogUtil;

/**
 * Plays audio to the caller via the telephony audio device.
 *
 * <p>This class manages an AudioTrack configured to output to TYPE_TELEPHONY,
 * allowing audio (like TTS) to be heard by the remote caller during a call.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>Create instance</li>
 *   <li>Call {@link #initialize(AudioDeviceInfo)} with telephony device</li>
 *   <li>Call {@link #playAudio(byte[], int)} to send audio</li>
 *   <li>Call {@link #release()} when done</li>
 * </ol>
 */
public class AudioInjector {

    private static final String TAG = "AudioInjector";

    /** Sample rate for audio playback (16kHz for speech). */
    public static final int SAMPLE_RATE = 16000;

    /** Audio channel configuration (mono for speech). */
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;

    /** Audio encoding format. */
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    /** States for the audio injector. */
    public enum State {
        IDLE,
        INITIALIZED,
        PLAYING,
        RELEASED
    }

    @Nullable
    private AudioTrack audioTrack;
    private State state = State.IDLE;

    /**
     * Initializes the audio injector with the given telephony device.
     *
     * @param telephonyDevice The TYPE_TELEPHONY audio device to use
     * @return true if initialization succeeded
     */
    public boolean initialize(@NonNull AudioDeviceInfo telephonyDevice) {
        if (state != State.IDLE) {
            LogUtil.w(TAG, "Cannot initialize: already in state " + state);
            return false;
        }

        try {
            int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

            AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();

            AudioFormat format = new AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_CONFIG)
                .setEncoding(AUDIO_FORMAT)
                .build();

            audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();

            audioTrack.setPreferredDevice(telephonyDevice);
            state = State.INITIALIZED;
            return true;

        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to initialize AudioTrack", e);
            return false;
        }
    }

    /**
     * Plays audio data to the caller.
     *
     * @param audioData The PCM audio data to play
     * @param sampleRate The sample rate of the audio data
     * @return Number of bytes written, or negative error code
     */
    public int playAudio(@NonNull byte[] audioData, int sampleRate) {
        if (audioTrack == null || state == State.RELEASED) {
            return -1;
        }

        if (audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.play();
            state = State.PLAYING;
        }

        return audioTrack.write(audioData, 0, audioData.length);
    }

    /** Stops playback if currently playing. */
    public void stop() {
        if (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.stop();
            state = State.INITIALIZED;
        }
    }

    /** Releases all resources. Safe to call multiple times. */
    public void release() {
        if (audioTrack != null) {
            try {
                audioTrack.stop();
            } catch (IllegalStateException e) {
                // Already stopped
            }
            audioTrack.release();
            audioTrack = null;
        }
        state = State.RELEASED;
    }

    /** Returns true if audio is currently being played. */
    public boolean isPlaying() {
        return audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
    }

    /** Returns true if the injector has been initialized. */
    public boolean isInitialized() {
        return state == State.INITIALIZED || state == State.PLAYING;
    }

    /** Returns the current state. */
    @NonNull
    public State getState() {
        return state;
    }
}
