package com.android.incallui.callscreen;

import androidx.annotation.Nullable;

/**
 * Defines the TTS prompts used during call screening.
 *
 * <p>All prompts are hardcoded for security - we don't allow arbitrary
 * text to be spoken to callers.
 */
public final class CallScreenPrompts {

    private CallScreenPrompts() {} // Constants class

    // Prompt IDs
    public static final int ID_GREETING = 0;
    public static final int ID_IS_URGENT = 1;
    public static final int ID_CALL_BACK = 2;
    public static final int ID_NOT_INTERESTED = 3;
    public static final int ID_REPEAT = 4;
    public static final int ID_TIMEOUT = 5;

    /** Initial greeting when screening starts. */
    public static final String GREETING =
        "Hi, this call is being screened. Please say your name and why you're calling.";

    /** Follow-up to ask if the call is urgent. */
    public static final String IS_URGENT = "Is this urgent?";

    /** Response indicating user will call back. */
    public static final String CALL_BACK = "They'll call you back.";

    /** Response indicating user is not interested. */
    public static final String NOT_INTERESTED = "They're not available. Goodbye.";

    /** Request for caller to repeat themselves. */
    public static final String REPEAT = "Sorry, I didn't catch that. Could you repeat?";

    /** Message when screening times out. */
    public static final String TIMEOUT =
        "They're not currently available, but will be notified about your call. Goodbye.";

    private static final String[] ALL_PROMPTS = {
        GREETING, IS_URGENT, CALL_BACK, NOT_INTERESTED, REPEAT, TIMEOUT
    };

    /**
     * Gets a prompt by its ID.
     *
     * @param promptId The prompt ID constant
     * @return The prompt text, or null if invalid ID
     */
    @Nullable
    public static String getPromptById(int promptId) {
        if (promptId >= 0 && promptId < ALL_PROMPTS.length) {
            return ALL_PROMPTS[promptId];
        }
        return null;
    }
}
