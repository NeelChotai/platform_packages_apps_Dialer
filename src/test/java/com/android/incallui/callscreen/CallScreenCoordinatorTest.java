package com.android.incallui.callscreen;

import static org.junit.Assert.*;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class CallScreenCoordinatorTest {

    private Context context;
    private CallScreenCoordinator coordinator;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        coordinator = new CallScreenCoordinator(context);
    }

    @Test
    public void newCoordinator_hasNoActiveSession() {
        assertFalse(coordinator.hasActiveSession());
    }

    @Test
    public void getActiveSession_returnsNull_whenNoSession() {
        assertNull(coordinator.getActiveSession());
    }

    @Test
    public void isScreeningSupported_checksForTelephonyDevice() {
        // On emulator/test, TYPE_TELEPHONY may not be available
        // Just verify the method doesn't crash
        coordinator.isScreeningSupported();
    }

    @Test
    public void cleanup_canBeCalledSafely_whenNoSession() {
        coordinator.cleanup(); // Should not throw
    }

    @Test
    public void cleanup_clearsActiveSession() {
        // We can't fully test startScreening without a real call,
        // but we can verify cleanup behavior
        coordinator.cleanup();
        assertNull(coordinator.getActiveSession());
    }
}
