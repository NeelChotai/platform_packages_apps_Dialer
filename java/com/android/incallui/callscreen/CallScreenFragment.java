package com.android.incallui.callscreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.dialer.R;

/**
 * Fragment that displays the call screening UI.
 *
 * <p>Shows the real-time transcript of what the caller is saying,
 * along with response buttons and primary answer/decline actions.
 */
public class CallScreenFragment extends Fragment {

    private static final String TAG = "CallScreenFragment";
    public static final String ARG_CALL_ID = "call_id";

    /** Callback for user actions on the call screen. */
    public interface CallScreenCallback {
        void onAnswerClicked();
        void onDeclineClicked();
        void onResponseClicked(int promptId);
    }

    @Nullable
    private CallScreenCallback callback;
    private TextView statusText;
    private TextView transcriptText;
    private Button answerButton;
    private Button declineButton;
    private Button isUrgentButton;
    private Button callBackButton;
    private Button repeatButton;

    /**
     * Creates a new instance for the given call.
     *
     * @param callId The ID of the call being screened
     * @return A new fragment instance
     */
    public static CallScreenFragment newInstance(@NonNull String callId) {
        CallScreenFragment fragment = new CallScreenFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CALL_ID, callId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_call_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        statusText = view.findViewById(R.id.screening_status);
        transcriptText = view.findViewById(R.id.transcript_text);
        answerButton = view.findViewById(R.id.button_answer);
        declineButton = view.findViewById(R.id.button_decline);
        isUrgentButton = view.findViewById(R.id.button_is_urgent);
        callBackButton = view.findViewById(R.id.button_call_back);
        repeatButton = view.findViewById(R.id.button_repeat);

        setupClickListeners();
    }

    private void setupClickListeners() {
        answerButton.setOnClickListener(v -> {
            if (callback != null) callback.onAnswerClicked();
        });

        declineButton.setOnClickListener(v -> {
            if (callback != null) callback.onDeclineClicked();
        });

        isUrgentButton.setOnClickListener(v -> {
            if (callback != null) callback.onResponseClicked(CallScreenPrompts.ID_IS_URGENT);
        });

        callBackButton.setOnClickListener(v -> {
            if (callback != null) callback.onResponseClicked(CallScreenPrompts.ID_CALL_BACK);
        });

        repeatButton.setOnClickListener(v -> {
            if (callback != null) callback.onResponseClicked(CallScreenPrompts.ID_REPEAT);
        });
    }

    /**
     * Sets the callback for user actions.
     */
    public void setCallback(@Nullable CallScreenCallback callback) {
        this.callback = callback;
    }

    /**
     * Updates the transcript display.
     *
     * @param transcript The current transcript text
     */
    public void updateTranscript(@NonNull String transcript) {
        if (transcriptText != null) {
            transcriptText.setText(transcript);
        }
    }

    /**
     * Updates the status message.
     *
     * @param status The status message to display
     */
    public void updateStatus(@NonNull String status) {
        if (statusText != null) {
            statusText.setText(status);
        }
    }

    /**
     * Sets the visibility of response buttons.
     *
     * @param visible true to show buttons, false to hide
     */
    public void setResponseButtonsVisible(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        if (isUrgentButton != null) isUrgentButton.setVisibility(visibility);
        if (callBackButton != null) callBackButton.setVisibility(visibility);
        if (repeatButton != null) repeatButton.setVisibility(visibility);
    }
}
