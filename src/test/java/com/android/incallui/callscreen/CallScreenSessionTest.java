package com.android.incallui.callscreen;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class CallScreenSessionTest {

    @Test
    public void newSession_startsInIdleState() {
        CallScreenSession session = new CallScreenSession("call-123");
        assertEquals(CallScreenState.IDLE, session.getState());
    }

    @Test
    public void newSession_hasCorrectCallId() {
        CallScreenSession session = new CallScreenSession("call-456");
        assertEquals("call-456", session.getCallId());
    }

    @Test
    public void newSession_hasEmptyTranscript() {
        CallScreenSession session = new CallScreenSession("call-123");
        assertTrue(session.getTranscript().isEmpty());
    }

    @Test
    public void appendTranscript_addsText() {
        CallScreenSession session = new CallScreenSession("call-123");
        session.appendTranscript("Hello");
        assertEquals("Hello", session.getTranscript());
    }

    @Test
    public void appendTranscript_concatenatesWithSpace() {
        CallScreenSession session = new CallScreenSession("call-123");
        session.appendTranscript("Hello");
        session.appendTranscript("world");
        assertEquals("Hello world", session.getTranscript());
    }

    @Test
    public void setState_updatesState() {
        CallScreenSession session = new CallScreenSession("call-123");
        session.setState(CallScreenState.INITIALIZING);
        assertEquals(CallScreenState.INITIALIZING, session.getState());
    }

    @Test
    public void getStartTime_returnsCreationTime() {
        long before = System.currentTimeMillis();
        CallScreenSession session = new CallScreenSession("call-123");
        long after = System.currentTimeMillis();

        assertTrue(session.getStartTime() >= before);
        assertTrue(session.getStartTime() <= after);
    }
}
