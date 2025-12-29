package com.android.incallui.callscreen;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class CallScreenFragmentTest {

    @Test
    public void newInstance_returnsFragment() {
        CallScreenFragment fragment = CallScreenFragment.newInstance("call-123");
        assertNotNull(fragment);
    }

    @Test
    public void newInstance_setsCallIdArgument() {
        CallScreenFragment fragment = CallScreenFragment.newInstance("call-456");
        assertEquals("call-456", fragment.getArguments().getString(CallScreenFragment.ARG_CALL_ID));
    }
}
