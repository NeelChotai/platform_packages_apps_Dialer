package com.android.incallui.callscreen;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CallScreenPromptsTest {

    @Test
    public void greeting_isNotEmpty() {
        assertFalse(CallScreenPrompts.GREETING.isEmpty());
    }

    @Test
    public void greeting_containsScreening() {
        assertTrue(CallScreenPrompts.GREETING.toLowerCase().contains("screen"));
    }

    @Test
    public void allPrompts_areNotNull() {
        assertNotNull(CallScreenPrompts.GREETING);
        assertNotNull(CallScreenPrompts.IS_URGENT);
        assertNotNull(CallScreenPrompts.CALL_BACK);
        assertNotNull(CallScreenPrompts.NOT_INTERESTED);
        assertNotNull(CallScreenPrompts.REPEAT);
        assertNotNull(CallScreenPrompts.TIMEOUT);
    }

    @Test
    public void promptId_greeting_hasCorrectValue() {
        assertEquals(0, CallScreenPrompts.ID_GREETING);
    }

    @Test
    public void getPromptById_returnsCorrectPrompt() {
        assertEquals(CallScreenPrompts.GREETING, CallScreenPrompts.getPromptById(CallScreenPrompts.ID_GREETING));
        assertEquals(CallScreenPrompts.IS_URGENT, CallScreenPrompts.getPromptById(CallScreenPrompts.ID_IS_URGENT));
    }

    @Test
    public void getPromptById_returnsNull_forInvalidId() {
        assertNull(CallScreenPrompts.getPromptById(-1));
        assertNull(CallScreenPrompts.getPromptById(999));
    }
}
