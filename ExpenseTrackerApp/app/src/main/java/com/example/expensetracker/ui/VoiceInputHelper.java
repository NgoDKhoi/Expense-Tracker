package com.example.expensetracker.ui;

import android.content.Context;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.example.expensetracker.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.function.Consumer;

/**
 * Shared helpers for launching speech recognition and its text-input fallback dialog.
 */
final class VoiceInputHelper {

    private VoiceInputHelper() {
    }

    static Intent createSpeechIntent(@NonNull Context context) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.voice_prompt_hint));
        return intent;
    }

    static void showTextFallbackDialog(@NonNull Context context, @NonNull Consumer<String> onSubmit) {
        int pad = Math.round(20 * context.getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(context);
        container.setPadding(pad, pad / 2, pad, 0);

        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint(R.string.voice_fallback_input_hint);
        container.addView(input);

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.voice_fallback_dialog_title)
                .setView(container)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String text = input.getText() != null ? input.getText().toString().trim() : "";
                    if (!text.isEmpty()) {
                        onSubmit.accept(text);
                    }
                })
                .show();
    }
}
