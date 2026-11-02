package com.example.expensetracker.ui;

import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.expensetracker.ExpenseTrackerApplication;
import com.example.expensetracker.R;
import com.example.expensetracker.viewmodel.AuthViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import javax.inject.Inject;

/**
 * Sign in / sign up, shown as a bottom sheet over the live camera when the avatar is tapped.
 * Open it with {@code new AuthFragment().show(activityFragmentManager, TAG)}.
 */
public class AuthFragment extends BottomSheetDialogFragment {

    public static final String TAG = "auth_sheet";

    /** Fragment result key sent whenever the signed-in user changes. */
    public static final String RESULT_AUTH_CHANGED = "auth_changed";

    @Inject
    ViewModelProvider.Factory viewModelFactory;
    private AuthViewModel viewModel;

    private boolean signUpMode = false;

    private TextInputLayout tilName;
    private TextInputLayout tilUsername;
    private TextInputLayout tilEmail;
    private TextView tvTitle;
    private MaterialButton btnModeSignIn;
    private MaterialButton btnModeSignUp;
    private TextInputLayout tilConfirm;
    private TextInputEditText etName;
    private TextInputEditText etUsername;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirm;
    private MaterialButton btnSubmit;
    private View btnForgot;
    private View progress;

    @Override
    public int getTheme() {
        return R.style.ThemeOverlay_ExpenseTracker_AuthSheet;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Open fully: the form is the whole point of the sheet, a half-collapsed state only hides fields.
        BottomSheetBehavior<?> behavior = ((BottomSheetDialog) requireDialog()).getBehavior();
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        Window window = requireDialog().getWindow();
        if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Frosted camera behind the sheet; devices without cross-window blur keep the plain dim.
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.setBlurBehindRadius(getResources().getDimensionPixelSize(R.dimen.spacing_l));
            window.setAttributes(params);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((ExpenseTrackerApplication) requireActivity().getApplication()).getAppComponent().inject(this);
        viewModel = new ViewModelProvider(this, viewModelFactory).get(AuthViewModel.class);

        tilName = view.findViewById(R.id.til_display_name);
        tilUsername = view.findViewById(R.id.til_username);
        tilEmail = view.findViewById(R.id.til_email);
        tvTitle = view.findViewById(R.id.tv_auth_title);
        btnModeSignIn = view.findViewById(R.id.btn_mode_sign_in);
        btnModeSignUp = view.findViewById(R.id.btn_mode_sign_up);
        tilConfirm = view.findViewById(R.id.til_confirm_password);
        etName = view.findViewById(R.id.et_display_name);
        etUsername = view.findViewById(R.id.et_username);
        etEmail = view.findViewById(R.id.et_email);
        etPassword = view.findViewById(R.id.et_password);
        etConfirm = view.findViewById(R.id.et_confirm_password);
        btnSubmit = view.findViewById(R.id.btn_auth_submit);
        btnForgot = view.findViewById(R.id.btn_forgot_password);
        progress = view.findViewById(R.id.progress_auth);

        btnModeSignIn.setOnClickListener(v -> setSignUpMode(false));
        btnModeSignUp.setOnClickListener(v -> setSignUpMode(true));
        setSignUpMode(false);

        btnSubmit.setOnClickListener(v -> submit());
        btnForgot.setOnClickListener(v -> {
            clearErrors();
            viewModel.resetPassword(text(etEmail));
        });

        viewModel.getAuthState().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            setLoading(result.isLoading());
            if (result.isSuccess()) {
                Toast.makeText(requireContext(),
                        signUpMode ? R.string.msg_sign_up_success : R.string.msg_sign_in_success,
                        Toast.LENGTH_SHORT).show();
                int onDevice = viewModel.getUnsyncedOnSignIn();
                if (onDevice > 0) askMergeLocalData(onDevice); else finishAuth();
            } else if (result.isError()) {
                showError(result.getMessage());
            }
        });

        viewModel.getResetState().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            setLoading(result.isLoading());
            if (result.isSuccess()) {
                Snackbar.make(view, getString(R.string.msg_reset_sent, text(etEmail)), Snackbar.LENGTH_LONG)
                        .setAnchorView(btnSubmit)
                        .show();
            } else if (result.isError()) {
                showError(result.getMessage());
            }
        });
    }

    private void setSignUpMode(boolean signUp) {
        signUpMode = signUp;
        // Checkable buttons toggle themselves on click; keep exactly one selected.
        btnModeSignIn.setChecked(!signUp);
        btnModeSignUp.setChecked(signUp);
        tvTitle.setText(signUp ? R.string.auth_title_sign_up : R.string.auth_title_sign_in);
        int visibleInSignUp = signUp ? View.VISIBLE : View.GONE;
        tilName.setVisibility(visibleInSignUp);
        tilUsername.setVisibility(visibleInSignUp);
        // Sign in (and password reset) accept a username, sign up needs a real email.
        tilEmail.setHint(signUp ? R.string.hint_email : R.string.hint_email_or_username);
        tilEmail.setStartIconDrawable(signUp ? R.drawable.ic_mail : R.drawable.ic_person);
        etEmail.setInputType(signUp
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        tilConfirm.setVisibility(visibleInSignUp);
        btnForgot.setVisibility(signUp ? View.GONE : View.VISIBLE);
        btnSubmit.setText(submitLabel());
        clearErrors();
    }

    private void submit() {
        clearErrors();
        if (signUpMode) {
            if (!text(etPassword).equals(text(etConfirm))) {
                tilConfirm.setError(getString(R.string.error_password_mismatch));
                return;
            }
            viewModel.signUp(text(etName), text(etUsername), text(etEmail), text(etPassword));
        } else {
            viewModel.signIn(text(etEmail), text(etPassword));
        }
    }

    private void showError(String message) {
        if (message != null && getView() != null) {
            // Anchored so it never covers the button the user is about to tap again.
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).setAnchorView(btnSubmit).show();
        }
    }

    private void clearErrors() {
        tilConfirm.setError(null);
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        // The spinner replaces the label, so the button keeps its size and place.
        if (loading) {
            btnSubmit.setText("");
        } else {
            btnSubmit.setText(submitLabel());
        }
        btnSubmit.setEnabled(!loading);
        btnForgot.setEnabled(!loading);
        btnModeSignIn.setEnabled(!loading);
        btnModeSignUp.setEnabled(!loading);
    }

    private int submitLabel() {
        return signUpMode ? R.string.auth_btn_sign_up : R.string.auth_tab_sign_in;
    }

    private void close() {
        dismiss();
    }

    /** Expenses made before signing in: add them to the account, or drop them and download the account's data. */
    private void askMergeLocalData(int count) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialog_merge_title, count))
                .setMessage(R.string.dialog_merge_message)
                .setCancelable(false)
                .setPositiveButton(R.string.action_merge, (d, w) -> {
                    viewModel.mergeLocalData();
                    finishAuth();
                })
                .setNegativeButton(R.string.action_discard_local, (d, w) -> {
                    viewModel.discardLocalData();
                    finishAuth();
                })
                .show();
    }

    private void finishAuth() {
        getParentFragmentManager().setFragmentResult(RESULT_AUTH_CHANGED, Bundle.EMPTY);
        close();
    }

    private static String text(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }
}
